/*
 * Copyright 2021 Google LLC
 * Copyright 2021 EPAM Systems, Inc
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.sun.jersey.api.client.ClientResponse;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.commons.text.StringSubstitutor;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengroup.osdu.core.common.model.entitlements.Acl;
import org.opengroup.osdu.dataset.configuration.DatasetConfiguration;
import org.opengroup.osdu.dataset.configuration.GcpConfig;
import org.opengroup.osdu.dataset.configuration.MapperConfig;
import org.opengroup.osdu.dataset.model.IntTestFileCollectionInstructionsItem;
import org.opengroup.osdu.dataset.model.IntTestFileInstructionsItem;
import org.opengroup.osdu.dataset.model.IntTestGetCreateUpdateDatasetRegistryResponse;
import org.opengroup.osdu.dataset.model.request.IntTestGetDatasetRegistryRequest;
import org.opengroup.osdu.dataset.model.response.IntTestDatasetRetrievalDeliveryItem;
import org.opengroup.osdu.dataset.model.response.IntTestGetDatasetRetrievalInstructionsResponse;
import org.opengroup.osdu.dataset.model.response.IntTestGetDatasetStorageInstructionsResponse;
import org.opengroup.osdu.dataset.util.CloudStorageUtilGcp;
import org.opengroup.osdu.dataset.util.FileUtils;
import org.opengroup.osdu.dataset.util.GcpTestUtils;

public class TestDataset extends Dataset {

	public static final String INPUT_DATASET_FILE_JSON = "input/datasetFile.json";

	public static final String INPUT_DATASET_FILE_COLLECTION_JSON = "input/datasetFileCollection.json";

	private static final GcpTestUtils gcpTestUtils = new GcpTestUtils();

	private static final CloudStorageUtilGcp cloudStorageUtilGcp = new CloudStorageUtilGcp();

	private static final ObjectMapper objectMapper = MapperConfig.getObjectMapper();

	@BeforeClass
	public static void classSetup() throws Exception {
		cloudStorageUtil = cloudStorageUtilGcp;
		DatasetConfiguration.datasetSetup(gcpTestUtils.getToken());
	}

	@AfterClass
	public static void classTearDown() throws Exception {
		Dataset.classTearDown(gcpTestUtils.getToken());
	}

	@Override
	public void should_getUploadLocation() throws Exception {
		String fileDataset = "?kindSubType=dataset--File." + GcpConfig.getDatasetKindSubType();
		IntTestGetDatasetStorageInstructionsResponse datasetInstructions = getDatasetInstructions(fileDataset);
		validate_storageLocation(datasetInstructions.getStorageLocation());
	}

	@Test
	public void should_getConnectionStringForCollection() throws Exception {
		String collectionDataset = "?kindSubType=dataset--FileCollection." + GcpConfig.getDatasetKindSubType();
		IntTestGetDatasetStorageInstructionsResponse datasetInstructions = getDatasetInstructions(collectionDataset);
		validate_collectionStorageInstructions(datasetInstructions.getStorageLocation());
	}

	@Override
	public void upload_file_register_it_and_retrieve_it() throws Exception {
		String kindSubType = "?kindSubType=dataset--File." + GcpConfig.getDatasetKindSubType();

		//Step 1: Get Storage Instructions for File
		IntTestGetDatasetStorageInstructionsResponse datasetInstructions = getDatasetInstructions(kindSubType);

		//Step 2: Upload File
		String fileName = "testFile.txt";
		String fileContents = "Hello World!";
		String unsignedUploadUrl = cloudStorageUtilGcp
			.uploadCloudFileUsingProvidedCredentials(fileName, datasetInstructions.getStorageLocation(),
				fileContents);
		uploadedCloudFileUnsignedUrls.add(unsignedUploadUrl);

		//Step 3: Register File
		String datasetRegistry = createDatasetRegistry(INPUT_DATASET_FILE_JSON, unsignedUploadUrl);
		String recordId = registerDataset(datasetRegistry);

		//Step 4: Retrieve File and validate contents
		IntTestDatasetRetrievalDeliveryItem datasetRetrievalItem = getDatasetRetrievalItem(recordId);

		validate_dataset_retrieval_delivery_item(datasetRetrievalItem);

		String downloadedContent = cloudStorageUtilGcp
			.downloadCloudFileUsingDeliveryItem(datasetRetrievalItem.getRetrievalProperties());

		Assert.assertEquals(fileContents, downloadedContent);
	}


	@Test
	public void upload_datasetCollection_register_it_and_retrieve_it() throws Exception {
		String kindSubType = "?kindSubType=dataset--FileCollection." + GcpConfig.getDatasetKindSubType();

		//Step 1: Get Storage Instructions for File
		IntTestGetDatasetStorageInstructionsResponse datasetInstructions = getDatasetInstructions(kindSubType);

		//Step 2: Upload File
		String fileName = "testFile.txt";
		String fileContents = "Hello World!";
		String unsignedUploadUrl = cloudStorageUtilGcp
			.uploadCollectionUsingProvidedCredentials(fileName, datasetInstructions.getStorageLocation(),
				fileContents);
		uploadedCloudFileUnsignedUrls.add(unsignedUploadUrl);

		//Step 3: Register File
		String datasetRegistry = createDatasetRegistry(INPUT_DATASET_FILE_COLLECTION_JSON, unsignedUploadUrl);
		String recordId = registerDataset(datasetRegistry);

		//Step 4: Retrieve File and validate contents
		IntTestDatasetRetrievalDeliveryItem datasetRetrievalItem = getDatasetRetrievalItem(recordId);

		validate_CollectionRetrievalItem(datasetRetrievalItem);

		String downloadedContent = cloudStorageUtilGcp
			.downloadCollectionFileUsingDeliveryItem(datasetRetrievalItem.getRetrievalProperties(), fileName);

		Assert.assertEquals(fileContents, downloadedContent);
	}

	private IntTestDatasetRetrievalDeliveryItem getDatasetRetrievalItem(String recordId) throws Exception {
		IntTestGetDatasetRegistryRequest getDatasetRequest = new IntTestGetDatasetRegistryRequest(new ArrayList<>());
		getDatasetRequest.getDatasetRegistryIds().add(recordId);

		ClientResponse retrievalClientResponse = TestUtils.send("getRetrievalInstructions", "POST",
			HeaderUtils.getHeaders(TenantUtils.getTenantName(), gcpTestUtils.getToken()),
			jsonMapper.writeValueAsString(getDatasetRequest),
			"");

		Assert.assertEquals(200, retrievalClientResponse.getStatus());

		String getRetrievalRespStr = retrievalClientResponse.getEntity(String.class);

		IntTestGetDatasetRetrievalInstructionsResponse getRetrievalInstResponse = jsonMapper
			.readValue(getRetrievalRespStr, IntTestGetDatasetRetrievalInstructionsResponse.class);

		return getRetrievalInstResponse.getDelivery().get(0);
	}

	private IntTestGetDatasetStorageInstructionsResponse getDatasetInstructions(String dataset) throws Exception {
		ClientResponse response = TestUtils.send(
			"getStorageInstructions",
			"GET",
			HeaderUtils.getHeaders(TenantUtils.getTenantName(), gcpTestUtils.getToken()),
			"",
			dataset);
		Assert.assertEquals(200, response.getStatus());

		String respStr = response.getEntity(String.class);

		IntTestGetDatasetStorageInstructionsResponse<IntTestFileInstructionsItem> resp = jsonMapper
			.readValue(respStr, IntTestGetDatasetStorageInstructionsResponse.class);

		Assert.assertEquals(TestUtils.getProviderKey(), resp.getProviderKey());
		Assert.assertNotNull(resp.getStorageLocation());

		return resp;
	}


	private String registerDataset(String datasetRegistry) throws Exception {
		ClientResponse datasetRegistryResponse = TestUtils.send(TestUtils.datasetBaseUrl, "registerDataset", "PUT",
			HeaderUtils.getHeaders(TenantUtils.getTenantName(), gcpTestUtils.getToken()),
			datasetRegistry, "");

		Assert.assertTrue(datasetRegistryResponse.getStatus() == 201);

		IntTestGetCreateUpdateDatasetRegistryResponse registryResponse = objectMapper
			.readValue(datasetRegistryResponse.getEntity(String.class),
				IntTestGetCreateUpdateDatasetRegistryResponse.class);
		String recordId = registryResponse.getDatasetRegistries().get(0).getId();

		registeredDatasetRegistryIds.add(recordId);
		return recordId;
	}

	public void validate_dataset_retrieval_delivery_item(IntTestDatasetRetrievalDeliveryItem deliveryItem) {
		IntTestFileInstructionsItem fileInstructionsItem = objectMapper
			.convertValue(deliveryItem.getRetrievalProperties(), IntTestFileInstructionsItem.class);

		Assert.assertNotNull(fileInstructionsItem.getUnsignedUrl());
		Assert.assertNotNull(fileInstructionsItem.getSignedUrl());
		Assert.assertNotNull(fileInstructionsItem.getCreatedAt());
	}

	public void validate_CollectionRetrievalItem(IntTestDatasetRetrievalDeliveryItem deliveryItem) {
		IntTestFileCollectionInstructionsItem collectionInstructionsItem = objectMapper
			.convertValue(deliveryItem.getRetrievalProperties(), IntTestFileCollectionInstructionsItem.class);

		Assert.assertNotNull(collectionInstructionsItem.getUnsignedUrl());
		Assert.assertNotNull(collectionInstructionsItem.getConnectionString());
		Assert.assertNotNull(collectionInstructionsItem.getCreatedAt());
		Assert.assertEquals(deliveryItem.getProviderKey(), GcpTestUtils.providerKey);
	}

	public void validate_storageLocation(Object storageLocation) {
		IntTestFileInstructionsItem fileInstructionsItem = objectMapper
			.convertValue(storageLocation, IntTestFileInstructionsItem.class);

		Assert.assertNotNull(fileInstructionsItem.getUnsignedUrl());
		Assert.assertNotNull(fileInstructionsItem.getSignedUrl());
		Assert.assertNotNull(fileInstructionsItem.getCreatedAt());

		uploadedCloudFileUnsignedUrls.add(fileInstructionsItem.getUnsignedUrl());
	}

	@Override
	protected Acl createAcl() {
		return null;
	}

	public void validate_collectionStorageInstructions(Object collectionInstructions) {
		IntTestFileCollectionInstructionsItem collectionInstructionsItem = objectMapper
			.convertValue(collectionInstructions, IntTestFileCollectionInstructionsItem.class);

		Assert.assertNotNull(collectionInstructionsItem.getUnsignedUrl());
		Assert.assertNotNull(collectionInstructionsItem.getConnectionString());
		Assert.assertNotNull(collectionInstructionsItem.getCreatedAt());

		uploadedCloudFileUnsignedUrls.add(collectionInstructionsItem.getUnsignedUrl());
	}

	private String createDatasetRegistry(String filename, String unsignedUrl)
		throws IOException {
		String datasetRegistry = FileUtils.readFileFromResources(filename);
		StringSubstitutor stringSubstitutor = new StringSubstitutor(
			ImmutableMap.of(
				"tenant", TenantUtils.getTenantName(),
				"domain", TestUtils.getDomain(),
				"kind-subtype", GcpConfig.getDatasetKindSubType(),
				"unsigned_url", unsignedUrl,
				"legal-tag", GcpConfig.getLegalTag())
		);
		return stringSubstitutor.replace(datasetRegistry);
	}

	@Override
	public void setup() throws Exception {
	}


	@Override
	public void tearDown() throws Exception {
	}

}
