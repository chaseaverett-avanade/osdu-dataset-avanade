// Copyright © Amazon Web Services
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.jersey.api.client.ClientResponse;
import cucumber.api.DataTable;
import lombok.extern.java.Log;
import org.apache.commons.text.StringSubstitutor;
import org.opengroup.osdu.core.common.model.entitlements.Acl;
import org.opengroup.osdu.core.common.model.search.QueryRequest;
import org.opengroup.osdu.core.common.model.search.QueryResponse;
import org.opengroup.osdu.delivery.model.*;
import org.opengroup.osdu.models.TestingDatasetRegistryAccessResponse;
import org.opengroup.osdu.models.TestingDatasetRegistryAccessResponseItem;
import org.opengroup.osdu.models.Setup;
import org.opengroup.osdu.models.TestIndex;
import org.opengroup.osdu.util.CloudStorageUtils;
import org.opengroup.osdu.util.Config;
import org.opengroup.osdu.util.FileHandler;
import org.opengroup.osdu.util.HTTPClient;

import javax.ws.rs.HttpMethod;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.opengroup.osdu.util.Config.*;

@Log
public class RecordSteps extends TestsBase {
    private Map<String, TestIndex> inputIndexMap = new HashMap<>();
    private boolean shutDownHookAdded = false;

    private String timeStamp = String.valueOf(System.currentTimeMillis());
    private List<Map<String, Object>> records;
    private Map<String, String> headers = httpClient.getCommonHeader();

    private UrlSigningRequest urlSigningRequest = new UrlSigningRequest();


    public RecordSteps(HTTPClient httpClient, CloudStorageUtils cloudStorageUtils) {
        super(httpClient, cloudStorageUtils);
    }


    /******************One time cleanup for whole feature**************/
    public void tearDown() {
        for (String kind : inputIndexMap.keySet()) {
            TestIndex testIndex = inputIndexMap.get(kind);
            testIndex.deleteSchema(kind);
        }
        if (records != null && records.size() > 0) {
            for (Map<String, Object> testRecord : records) {
                String id = testRecord.get("id").toString();
                httpClient.send(HttpMethod.DELETE, getStorageBaseURL() + "records/" + id, null, headers, httpClient.getAccessToken());
                log.info("Deleted the records");
            }
        }

        cloudStorageUtils.deleteBucket();
    }

    public void the_schema_is_created_with_the_following_kind(DataTable dataTable) {

        List<Setup> inputList = dataTable.asList(Setup.class);
        for (Setup input : inputList) {
            TestIndex testIndex = getTextIndex();
            testIndex.setHttpClient(httpClient);
            testIndex.setIndex(generateActualName(input.getIndex(), timeStamp));
            testIndex.setKind(generateActualName(input.getKind(), timeStamp));
            testIndex.setSchemaFile(input.getSchemaFile());
            inputIndexMap.put(testIndex.getKind(), testIndex);
        }

        /******************One time setup for whole feature**************/
        if (!shutDownHookAdded) {
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    tearDown();
                }
            });
            shutDownHookAdded = true;
            for (String kind : inputIndexMap.keySet()) {
                TestIndex testIndex = inputIndexMap.get(kind);
                testIndex.setupSchema();
            }
        }
    }

    public void i_ingest_records_with_the_for_a_given(String record, String dataGroup, String kind) {

        String actualKind = generateActualName(kind, timeStamp);
        try {
            String fileContent = FileHandler.readFile(String.format("%s.%s", record, "json"));
            StringSubstitutor stringSubstitutor = new StringSubstitutor(
                ImmutableMap.of(
                    "tenant", Config.getTenant(),
                    "domain",Config.getEntitlementsDomain())
            );
            records = new Gson().fromJson(fileContent, new TypeToken<List<Map<String, Object>>>() {}.getType());

            cloudStorageUtils.createBucket();

            for (Map<String, Object> testRecord : records) {
                String recordId = generateActualName(testRecord.get("id").toString(), timeStamp);
                String filePath = cloudStorageUtils.createCloudFile(recordId);
                Map<String,String> data = (Map<String, String>) testRecord.get("data");
                data.put("Data.GroupTypeProperties.PreLoadFilePath", filePath);
                data.put("ResourceID", recordId);
                testRecord.put("data", data);
                testRecord.put("id", recordId);
                testRecord.put("kind", actualKind);
                testRecord.put("legal", generateLegalTag());
                String[] x_acl = {stringSubstitutor.replace(generateActualName(dataGroup,timeStamp))};
                Acl acl = Acl.builder().viewers(x_acl).owners(x_acl).build();
                testRecord.put("acl", acl);
            }
            String payLoad = new Gson().toJson(records);
            ClientResponse clientResponse = httpClient.send(HttpMethod.PUT, getStorageBaseURL() + "records", payLoad, headers, httpClient.getAccessToken());
            assertEquals(201, clientResponse.getStatus());
        } catch (Exception ex) {
            throw new AssertionError(ex.getMessage());
        }
    }

    public void i_should_get_the_documents_for_the_index_from_search(int expectedCount, String kind) throws Throwable {

        List<String> recordIds = searchRecordIds(expectedCount, kind);
        assertEquals(expectedCount, recordIds.size());
    }

    public void i_should_get_the_signed_urls_from_file(int expectedCount) throws Throwable {

        ClientResponse response = retrieveSignedResponse();
        String responseBody = response.getEntity(String.class);
        UrlSigningResponse signedResponse = mapper.readValue(responseBody, UrlSigningResponse.class);
        assertEquals(expectedCount, signedResponse.getProcessed().size());
        assertEquals(0, signedResponse.getUnprocessed().size());
    }

    // Method to be overridden by cloud providers to validate connectionString property in the response
    public void validate_cloud_provider_connection_string(UrlSigningResponse signedResponse) { }

    public void i_should_get_an_error_with_no_file_path() throws Throwable {
        ClientResponse response = retrieveSignedResponse();
        assertEquals(500, response.getStatus());
    }

    public void i_ingest_records_with_the_record_with_data_group_with_record_id_for_a_given_kind(String record, String dataGroup,
                                                                                                 String recordId, String kind) throws Throwable {
        String actualKind = generateActualName(kind, timeStamp);
        try {
            String fileContent = FileHandler.readFile(String.format("%s.%s", record, "json"));
            StringSubstitutor stringSubstitutor = new StringSubstitutor(
                    ImmutableMap.of(
                            "tenant", Config.getTenant(),
                            "domain",Config.getEntitlementsDomain())
            );
            records = new Gson().fromJson(fileContent, new TypeToken<List<Map<String, Object>>>() {}.getType());

            cloudStorageUtils.createBucket();

            for (Map<String, Object> testRecord : records) {
                String filePath = cloudStorageUtils.createCloudFile(recordId);
                Map<String,Map> data = (Map<String, Map>) testRecord.get("data");
                Map<String,Map> dataSetProperties = new HashMap<>();
                Map<String,String> fileSourceInfo = new HashMap<>();
                fileSourceInfo.put("PreLoadFilePath", filePath);
                dataSetProperties.put("FileSourceInfo", fileSourceInfo);
                data.put("DatasetProperties", dataSetProperties);
                testRecord.put("data", data);
                testRecord.put("id", recordId);
                testRecord.put("kind", actualKind);
                testRecord.put("legal", generateLegalTag());
                String[] x_acl = {stringSubstitutor.replace(generateActualName(dataGroup,timeStamp))};
                Acl acl = Acl.builder().viewers(x_acl).owners(x_acl).build();
                testRecord.put("acl", acl);
            }
            String payLoad = new Gson().toJson(records);
            ClientResponse clientResponse = httpClient.send(HttpMethod.PUT, getStorageBaseURL() + "records", payLoad, headers, httpClient.getAccessToken());
            assertEquals(201, clientResponse.getStatus());
        } catch (Exception ex) {
            throw new AssertionError(ex.getMessage());
        }
    }

    public void i_ingest_records_with_the_record_with_data_group_with_record_id_for_a_given_kind_incorrectly(String record, String dataGroup, String recordId, String kind) throws Throwable {
        String actualKind = generateActualName(kind, timeStamp);
        try {
            String fileContent = FileHandler.readFile(String.format("%s.%s", record, "json"));
            StringSubstitutor stringSubstitutor = new StringSubstitutor(
                    ImmutableMap.of(
                            "tenant", Config.getTenant(),
                            "domain",Config.getEntitlementsDomain())
            );
            records = new Gson().fromJson(fileContent, new TypeToken<List<Map<String, Object>>>() {}.getType());

            for (Map<String, Object> testRecord : records) {
                Map<String,Map> data = (Map<String, Map>) testRecord.get("data");
                testRecord.put("data", data);
                testRecord.put("id", recordId);
                testRecord.put("kind", actualKind);
                testRecord.put("legal", generateLegalTag());
                String[] x_acl = {stringSubstitutor.replace(generateActualName(dataGroup,timeStamp))};
                Acl acl = Acl.builder().viewers(x_acl).owners(x_acl).build();
                testRecord.put("acl", acl);
            }
            String payLoad = new Gson().toJson(records);
            ClientResponse clientResponse = httpClient.send(HttpMethod.PUT, getStorageBaseURL() + "records", payLoad, headers, httpClient.getAccessToken());
            assertEquals(201, clientResponse.getStatus());
        } catch (Exception ex) {
            throw new AssertionError(ex.getMessage());
        }
    }

    public void i_shoud_get_the_documents_for_the_record_id_from_storage(int expectedCount, String recordId) throws Throwable {
        ClientResponse clientResponse = httpClient.send(HttpMethod.GET, getStorageBaseURL() + "records/" + recordId,
                null, headers, httpClient.getAccessToken());
        assertEquals(200, clientResponse.getStatus());
    }

    public void i_should_get_a_signed_url_for_the_record_id_from_delivery(String recordId) throws Throwable {
        Map datasetRegistryAccessResponse = retrieveDatasetRegistryAccessResponse(recordId);
        List<Object> dataRegistryAccessItems = (List) datasetRegistryAccessResponse.get("processed");
        Map<String, Object> processed = mapper.convertValue(dataRegistryAccessItems.get(0), Map.class);
        assertEquals((String) processed.get("dataRegistryRecordId"), recordId);

        Map delivery = (Map) processed.get("delivery");
        assertNotNull(delivery.get("url"));
        assertNotNull(delivery.get("connectionString"));
    }

    public void i_should_get_an_unsupported_message_with_no_handler_error_code_for_the_record_id_from_delivery(String recordId) throws Throwable {
        Map datasetRegistryAccessResponse = retrieveDatasetRegistryAccessResponse(recordId);
        List<Object> unsupportedItems = (List) datasetRegistryAccessResponse.get("unsupported");
        Map<String, Object> unsupported = mapper.convertValue(unsupportedItems.get(0), Map.class);
        assertEquals(unsupported.get("code"), UnsupportedAccessErrorCode.RESOURCE_TYPE_HANDLER_NOT_FOUND.toString());
    }

    public void i_should_get_an_unsupported_message_with_no_resource_type_error_code_for_the_record_id_from_delivery(String recordId) throws Throwable {
        Map datasetRegistryAccessResponse = retrieveDatasetRegistryAccessResponse(recordId);
        List<Object> unsupportedItems = (List) datasetRegistryAccessResponse.get("unsupported");
        Map<String, Object> unsupported = mapper.convertValue(unsupportedItems.get(0), Map.class);
        assertEquals(unsupported.get("code"), UnsupportedAccessErrorCode.RESOURCE_TYPE_FIELD_NOT_FOUND.toString());
    }

    public void i_should_get_a_failure_message_for_the_record_id_from_delivery(String recordId) throws Throwable {
        Map datasetRegistryAccessResponse = retrieveDatasetRegistryAccessResponse(recordId);
        List<Object> failedItems = (List) datasetRegistryAccessResponse.get("failed");
        assertEquals(failedItems.size(), 1);
    }

    private Map retrieveDatasetRegistryAccessResponse(String recordId) throws JsonProcessingException {
        DatasetRegistryAccessRequest request = new DatasetRegistryAccessRequest();
        List recordIds = new ArrayList();
        recordIds.add(recordId);
        request.setDataRegistryRecordIds(recordIds);
        String payload = mapper.writeValueAsString(request);

        ClientResponse clientResponse = httpClient.send(HttpMethod.POST, getDeliveryBaseURL() + "GetDataRegistryAccess", payload, headers, httpClient.getAccessToken());

        String responseBody = clientResponse.getEntity(String.class);
        Map<String, Object> response = mapper.readValue(responseBody, Map.class);
        return response;
    }

    private ClientResponse retrieveSignedResponse() throws IOException {
        String payload = mapper.writeValueAsString(urlSigningRequest);
        return httpClient.send(HttpMethod.POST, getDeliveryBaseURL() + "GetFileSignedUrl", payload, headers, httpClient.getAccessToken());
    }

    private List<String> searchRecordIds(int expectedCount, String kind) throws InterruptedException {
        Gson gson = new Gson();
        String actualKind = generateActualName(kind, timeStamp);
        List<String> recordIds = new ArrayList<>();
        int iterator;

        Thread.sleep(40000);
        QueryRequest queryRequest = new QueryRequest();
        queryRequest.setKind(actualKind);
        String payload = gson.toJson(queryRequest);
        for (iterator = 0; iterator < 20; iterator++) {
            ClientResponse clientResponse = httpClient.send(HttpMethod.POST, getSearchBaseURL() + "query", payload, headers, httpClient.getAccessToken());
            String responseBody = clientResponse.getEntity(String.class);
            QueryResponse response = gson.fromJson(responseBody, QueryResponse.class);
            if(response.getTotalCount() > 0){
                log.info(String.format("index: %s | attempts: %s | documents acknowledged by elastic: %s", kind, iterator, response.getTotalCount()));
                if(expectedCount == response.getTotalCount()){
                    recordIds = response.getResults().stream().map(item -> item.get("id").toString()).collect(Collectors.toList());
                    urlSigningRequest.setSrns(recordIds);
                    break;
                }
            }
            Thread.sleep(5000);
        }
        return recordIds;
    }



    private Boolean areJsonEqual(String firstJson, String secondJson) {
        Gson gson = new Gson();
        Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
        Map<String, Object> firstMap = gson.fromJson(firstJson, mapType);
        Map<String, Object> secondMap = gson.fromJson(secondJson, mapType);

        MapDifference<String, Object> result = Maps.difference(firstMap, secondMap);
        if (result != null && result.entriesDiffering().isEmpty()) return true;
        log.info(String.format("difference: %s", result.entriesDiffering()));
        return false;
    }

    @Override
    protected String getApi() {
        return null;
    }

    @Override
    protected String getHttpMethod() {
        return null;
    }
}
