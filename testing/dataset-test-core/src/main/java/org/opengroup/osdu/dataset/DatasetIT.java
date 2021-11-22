/*
 * Copyright 2021 Microsoft Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.dataset;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.ClientResponse;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.opengroup.osdu.core.common.dms.model.DatasetRetrievalProperties;
import org.opengroup.osdu.core.common.dms.model.RetrievalInstructionsRequest;
import org.opengroup.osdu.core.common.dms.model.RetrievalInstructionsResponse;
import org.opengroup.osdu.core.common.dms.model.StorageInstructionsResponse;
import org.opengroup.osdu.core.common.model.entitlements.Acl;
import org.opengroup.osdu.core.common.model.legal.Legal;
import org.opengroup.osdu.core.common.model.legal.LegalCompliance;
import org.opengroup.osdu.core.common.model.storage.Record;
import org.opengroup.osdu.dataset.model.shared.TestGetCreateUpdateDatasetRegistryRequest;
import org.opengroup.osdu.dataset.util.LegalTagUtils;

import java.util.*;

public abstract class DatasetIT extends TestBase {
    protected static CloudStorageUtil cloudStorageUtil;

    protected static ArrayList<String> uploadedCloudFileUnsignedUrls = new ArrayList<>();
    protected static ArrayList<String> registeredDatasetRegistryIds = new ArrayList<>();

    protected static boolean runTests = false;

    protected ObjectMapper jsonMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);

    public abstract void validateRetrievalInstructions(RetrievalInstructionsResponse retrievalInstructionsResponse,
                                                       int expectedDatasets);

    public abstract void validateStorageInstructions(StorageInstructionsResponse storageInstructionsResponse);

    protected static String LEGAL_TAG = LegalTagUtils.createRandomName();

    public static void classSetup(String token) throws Exception {
        ClientResponse legalTagCreateResponse = LegalTagUtils.create(
                HeaderUtils.getHeaders(TenantUtils.getTenantName(), token),
                LEGAL_TAG);
    }

    public static void classTearDown(String token) throws Exception {
        LegalTagUtils.delete(HeaderUtils.getHeaders(TenantUtils.getTenantName(), token), LEGAL_TAG);
    }

    @Test
    public void upload_file_register_it_and_retrieve_it() throws Exception {
        Assume.assumeTrue(runTests);
        String kindSubType = "dataset--File.Generic";

        //Step 1: Get Storage Instructions for File
        StorageInstructionsResponse getStorageInstResponse = storageInstructions(kindSubType);

        //Step 2: Upload File
        String fileName = "testFile.txt";
        String fileContents = "Hello World!";
        String fileSource = uploadFileToSignedUrl(getStorageInstResponse, fileName, fileContents);

        //Step 3: Register File
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String datasetRegistryId = String.format("%s:%s:%s", TenantUtils.getTenantName(),kindSubType, uuid);

        Record datasetRegistry = createDatasetRegistry(datasetRegistryId, fileName, fileSource);

        ClientResponse datasetRegistryResponse = testRegisterDatasetRequest(Collections.singletonList(datasetRegistry));
        System.out.println("Registry response " + datasetRegistryResponse.getEntity(String.class));
        Assert.assertEquals(201, datasetRegistryResponse.getStatus());

        registeredDatasetRegistryIds.add(datasetRegistryId);

        //Step 4: Retrieve File and validate contents
        ClientResponse retrievalClientResponse = getTestRetrievalInstructions(Collections.singletonList(datasetRegistryId));
        Assert.assertEquals(200, retrievalClientResponse.getStatus());

        String getRetrievalRespStr = retrievalClientResponse.getEntity(String.class);
        RetrievalInstructionsResponse getRetrievalInstResponse = jsonMapper.readValue(getRetrievalRespStr,
                RetrievalInstructionsResponse.class);
        validateRetrievalInstructions(getRetrievalInstResponse, 1);

        DatasetRetrievalProperties datasetRetrievalItem = getRetrievalInstResponse.getDatasets().get(0);

        String downloadedContent = cloudStorageUtil.downloadCloudFileUsingDeliveryItem(datasetRetrievalItem.getRetrievalProperties());
        Assert.assertEquals(fileContents, downloadedContent);
    }

    @Test
    public void upload_multiple_files_register_it_and_retrieve_it() throws Exception {
        Assume.assumeTrue(runTests);
        String kindSubType = "dataset--File.Generic";

        //Step 1: Get Storage Instructions for File 1
        StorageInstructionsResponse getStorageInstResponse1 = storageInstructions(kindSubType);

        //Step 1: Get Storage Instructions for File 2
        StorageInstructionsResponse getStorageInstResponse2 = storageInstructions(kindSubType);

        String fileName = "testFile.txt";
        String fileContents = "Hello World!";

        //Step 2: Upload File 1
        String fileSource1 = uploadFileToSignedUrl(getStorageInstResponse1, fileName, fileContents);

        //Step 2: Upload File 2
        String fileSource2 = uploadFileToSignedUrl(getStorageInstResponse2, fileName, fileContents);

        //Step 3: Register Datasets
        String uuid1 = UUID.randomUUID().toString().replace("-", "");
        String datasetRegistryId1 = String.format("%s:%s:%s", TenantUtils.getTenantName(),kindSubType, uuid1);

        String uuid2 = UUID.randomUUID().toString().replace("-", "");
        String datasetRegistryId2 = String.format("%s:%s:%s", TenantUtils.getTenantName(),kindSubType, uuid2);

        Record datasetRegistry1 = createDatasetRegistry(datasetRegistryId1, fileName, fileSource1);
        Record datasetRegistry2 = createDatasetRegistry(datasetRegistryId2, fileName, fileSource2);
        List<Record> datasetsToRegister = new ArrayList<>();
        datasetsToRegister.add(datasetRegistry1);
        datasetsToRegister.add(datasetRegistry2);

        ClientResponse datasetRegistryResponse = testRegisterDatasetRequest(datasetsToRegister);
        Assert.assertEquals(201, datasetRegistryResponse.getStatus());

        registeredDatasetRegistryIds.add(datasetRegistryId1);
        registeredDatasetRegistryIds.add(datasetRegistryId2);

        //Step 4: Retrieve File and validate contents
        List<String> datasetsToRetrieve = new ArrayList<>();
        datasetsToRetrieve.add(datasetRegistryId1);
        datasetsToRetrieve.add(datasetRegistryId2);

        ClientResponse retrievalClientResponse = getTestRetrievalInstructions(datasetsToRetrieve);
        Assert.assertEquals(200, retrievalClientResponse.getStatus());

        String getRetrievalRespStr = retrievalClientResponse.getEntity(String.class);
        RetrievalInstructionsResponse getRetrievalInstResponse = jsonMapper.readValue(getRetrievalRespStr,
                RetrievalInstructionsResponse.class);
        validateRetrievalInstructions(getRetrievalInstResponse, 2);

        for (DatasetRetrievalProperties dataset: getRetrievalInstResponse.getDatasets()) {
            String downloadedContent = cloudStorageUtil.downloadCloudFileUsingDeliveryItem(dataset.getRetrievalProperties());
            Assert.assertEquals(fileContents, downloadedContent);
        }
    }

    @Test
    public void invalid_kind_subtype_returns_400() throws Exception {
        Assume.assumeTrue(runTests);
        String kindSubType = "dataset--Foo.Generic";
        getTestStorageInstructions(kindSubType, 400);
    }

    private StorageInstructionsResponse storageInstructions(String kindSubType) throws Exception {
        StorageInstructionsResponse getStorageInstResponse = getTestStorageInstructions(kindSubType, 200);
        validateStorageInstructions(getStorageInstResponse);
        return getStorageInstResponse;
    }

    private String uploadFileToSignedUrl(StorageInstructionsResponse getStorageInstResponse, String fileName, String fileContents) throws Exception {
        String fileSource = cloudStorageUtil.uploadCloudFileUsingProvidedCredentials(fileName,
                getStorageInstResponse.getStorageLocation(), fileContents);
        uploadedCloudFileUnsignedUrls.add(fileSource);
        return fileSource;
    }

    private ClientResponse getTestRetrievalInstructions(List<String> datasetRegistryIds) throws Exception {
        RetrievalInstructionsRequest getDatasetRequest = new RetrievalInstructionsRequest();
        getDatasetRequest.setDatasetRegistryIds(datasetRegistryIds);

        return TestUtils.send("retrievalInstructions", "POST",
                HeaderUtils.getHeaders(TenantUtils.getTenantName(), testUtils.getToken()),
                jsonMapper.writeValueAsString(getDatasetRequest),
                "");
    }

    private ClientResponse testRegisterDatasetRequest(List<Record> datasetRegistries) throws Exception {
        TestGetCreateUpdateDatasetRegistryRequest datasetRegistryRequest = new TestGetCreateUpdateDatasetRegistryRequest(new ArrayList<>());
        datasetRegistryRequest.getDatasetRegistries().addAll(datasetRegistries);

        return TestUtils.send(TestUtils.datasetBaseUrl, "registerDataset", "PUT",
                HeaderUtils.getHeaders(TenantUtils.getTenantName(), testUtils.getToken()),
                jsonMapper.writeValueAsString(datasetRegistryRequest), "");
    }

    private StorageInstructionsResponse getTestStorageInstructions(String kindSubType, int expectedStatusCode) throws Exception {
        ClientResponse getStorageInstClientResp = TestUtils.send("storageInstructions", "POST",
                HeaderUtils.getHeaders(TenantUtils.getTenantName(), testUtils.getToken()),
                "", String.format("?kindSubType=%s", kindSubType));

        int actualStatusCode = getStorageInstClientResp.getStatus();
        Assert.assertEquals(expectedStatusCode, actualStatusCode);

        if (actualStatusCode != 200) {
            return null;
        }

        String getStorageRespStr = getStorageInstClientResp.getEntity(String.class);
        return jsonMapper.readValue(getStorageRespStr, StorageInstructionsResponse.class);
    }

    private static Record createDatasetRegistry(String id, String filename, String fileSource) {
        TestGetCreateUpdateDatasetRegistryRequest request = new TestGetCreateUpdateDatasetRegistryRequest();

        Record datasetRegistry = new Record();

        datasetRegistry.setId(id);
        datasetRegistry.setKind(String.format("%s:wks:dataset--File.Generic:1.0.0", TestUtils.getSchemaAuthority()));

        //set legal
        Legal legal = new Legal();
        HashSet<String> legalTags = new HashSet<>();
        legalTags.add(LEGAL_TAG);
        legal.setLegaltags(legalTags);
        HashSet<String> otherRelevantDataCountries = new HashSet<>();
        otherRelevantDataCountries.add("US");
        legal.setOtherRelevantDataCountries(otherRelevantDataCountries);
        legal.setStatus(LegalCompliance.compliant);
        datasetRegistry.setLegal(legal);

        //set acl
        Acl acl = new Acl();
        String[] viewers = new String[] { String.format("data.default.viewers@%s", getAclSuffix()) };
        acl.setViewers(viewers);
        String[] owners = new String[] { String.format("data.default.owners@%s",getAclSuffix()) };
        acl.setOwners(owners);
        datasetRegistry.setAcl(acl);

        // Data
        HashMap<String, Object> data = new HashMap<>();
        HashMap<String, Object> datasetProperties = new HashMap<>();
        HashMap<String, Object> fileSourceInfo = new HashMap<>();

        fileSourceInfo.put("FileSource", fileSource);
        datasetProperties.put("FileSourceInfo", fileSourceInfo);
        data.put("DatasetProperties", datasetProperties);
        datasetRegistry.setData(data);

        return datasetRegistry;
    }

    private static String getAclSuffix() {
        String environment = TestUtils.getEnvironment();
        if (environment.equalsIgnoreCase("empty")) {
            environment = "";
        }

        if (!environment.isEmpty()) {
            environment = "." + environment;
        }

        return String.format("%s%s.%s", TestUtils.getTenantName(), environment, TestUtils.getDomain());
    }
}
