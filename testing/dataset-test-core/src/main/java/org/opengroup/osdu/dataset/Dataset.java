// Copyright 2017-2019, Schlumberger
// Copyright © 2021 Amazon Web Services
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

package org.opengroup.osdu.dataset;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;
import org.opengroup.osdu.core.common.model.entitlements.Acl;
import org.opengroup.osdu.core.common.model.legal.Legal;
import org.opengroup.osdu.core.common.model.legal.LegalCompliance;
import org.opengroup.osdu.core.common.model.storage.Record;
import org.opengroup.osdu.dataset.model.IntTestFileDeliveryItem;
import org.opengroup.osdu.dataset.model.request.IntTestGetDatasetRegistryRequest;
import org.opengroup.osdu.dataset.model.response.IntTestDatasetRetrievalDeliveryItem;
import org.opengroup.osdu.dataset.model.response.IntTestGetDatasetRetrievalInstructionsResponse;
import org.opengroup.osdu.dataset.model.response.IntTestGetDatasetStorageInstructionsResponse;
import org.opengroup.osdu.dataset.model.shared.TestGetCreateUpdateDatasetRegistryRequest;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public abstract class Dataset extends TestBase {

	protected ObjectMapper jsonMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);

	String recordId;

	protected static CloudStorageUtil cloudStorageUtil;

	protected static ArrayList<String> uploadedCloudFileUnsignedUrls = new ArrayList<>();
	protected static ArrayList<String> registeredDatasetRegistryIds = new ArrayList<>();

	public static void classSetup(String token) throws Exception {
		// make sure schema is created
		// String datasetRegistrySchema = "{\n" +
		// 		"    \"kind\": \"osdu:wks:dataset-registry:0.0.1\",\n" +
		// 		"    \"schema\": [{\"path\":\"ResourceTypeID\",\"kind\":\"string\",\"ext\":{}},\n" +
		// 		"        {\"path\":\"ResourceID\",\"kind\":\"string\",\"ext\":{}},\n" +
		// 		"        {\"path\":\"ResourceSecurityClassification\",\"kind\":\"string\",\"ext\":{}},\n" +
		// 		"        {\"path\":\"ResourceName\",\"kind\":\"string\",\"ext\":{}},\n" +
		// 		"        {\"path\":\"ResourceDescription\",\"kind\":\"string\",\"ext\":{}},\n" +
		// 		"        {\"path\":\"ResourceSource\",\"kind\":\"string\",\"ext\":{}}]\n" +
		// 		"}";
		// ClientResponse response = TestUtils.send(TestUtils.storageBaseUrl, "schemas", "POST",
		// 		HeaderUtils.getHeaders(TenantUtils.getTenantName(), token),
		// 		datasetRegistrySchema, "");

		// Assert.assertTrue(response.getStatus() == 201 || response.getStatus() == 409);

		// make sure legaltag is created
		String legalBody = "{\t\n" +
				"\t\"name\": \"public-usa-dataset-1\",\t\n" +
				"\t\"properties\": {\t\t\n" +
				"\t\t\"countryOfOrigin\":[\"US\"],        \n" +
				"\t\t\"contractId\":\"A1234\",\n" +
				"\t\t\"expirationDate\":2222222222222,        \n" +
				"\t\t\"originator\":\"Default\",        \n" +
				"\t\t\"dataType\":\"Public Domain Data\",        \n" +
				"\t\t\"securityClassification\":\"Public\",        \n" +
				"\t\t\"personalData\":\"No Personal Data\",        \n" +
				"\t\t\"exportClassification\":\"EAR99\"\t\n" +
				"\t\t},\t\n" +
				"\t\"description\": \"A default legal tag\"\n" +
				"}\n" +
				"\n";

		ClientResponse legalResponse = TestUtils.send(TestUtils.legalBaseUrl, "legaltags", "POST",
				HeaderUtils.getHeaders(TenantUtils.getTenantName(), token),
				legalBody, "");

		Assert.assertTrue(legalResponse.getStatus() == 201 || legalResponse.getStatus() == 409);		
	}

	public static void classTearDown(String token) throws Exception {
		
		for(String unsignedUrl : uploadedCloudFileUnsignedUrls) {
			cloudStorageUtil.deleteCloudFile(unsignedUrl);
		}

		for(String datasetRegistryId : registeredDatasetRegistryIds) {

			System.out.println(String.format("Deleting Dataset Registry: %s", datasetRegistryId));
			
			ClientResponse storageResponse = TestUtils.send(TestUtils.storageBaseUrl, 
			String.format("records/%s", datasetRegistryId), 
			"DELETE",
			HeaderUtils.getHeaders(TenantUtils.getTenantName(), token),
			"", "");

			System.out.println(String.format("Deleting Dataset Registry Response Code: %s", storageResponse.getStatus()));
			
		}
	}

	@Test
	public void should_getUploadLocation() throws Exception {
		ClientResponse response = TestUtils.send("getStorageInstructions", "GET",
				HeaderUtils.getHeaders(TenantUtils.getTenantName(), testUtils.getToken()), "", "?kindSubType=dataset--File.Generic");
		Assert.assertEquals(200, response.getStatus());
		
		// JsonObject json = new JsonParser().parse(response.getEntity(String.class)).getAsJsonObject();

		String respStr = response.getEntity(String.class);		

		IntTestGetDatasetStorageInstructionsResponse<Object> resp = jsonMapper.readValue(respStr, IntTestGetDatasetStorageInstructionsResponse.class);

		Assert.assertEquals(TestUtils.getProviderKey(), resp.getProviderKey());

		Assert.assertNotNull(resp.getStorageLocation());
		validate_storageLocation(resp.getStorageLocation());
	}

	@Test
	public void upload_file_register_it_and_retrieve_it() throws Exception {

		String kindSubType = "dataset--File.Generic";
		
		//Step 1: Get Storage Instructions for File
		ClientResponse getStorageInstClientResp = TestUtils.send("getStorageInstructions", "GET",
				HeaderUtils.getHeaders(TenantUtils.getTenantName(), testUtils.getToken()), "", String.format("?kindSubType=%s", kindSubType));
		
		Assert.assertEquals(200, getStorageInstClientResp.getStatus());

		String getStorageRespStr = getStorageInstClientResp.getEntity(String.class);		

		IntTestGetDatasetStorageInstructionsResponse<Object> getStorageInstResponse = jsonMapper.readValue(getStorageRespStr, IntTestGetDatasetStorageInstructionsResponse.class);

		Assert.assertEquals(TestUtils.getProviderKey(), getStorageInstResponse.getProviderKey());

		//Step 2: Upload File
		String fileName = "testFile.txt";
		String fileContents = "Hello World!";
		String unsignedUploadUrl = cloudStorageUtil.uploadCloudFileUsingProvidedCredentials(fileName, getStorageInstResponse.getStorageLocation(), fileContents);
		uploadedCloudFileUnsignedUrls.add(unsignedUploadUrl);

		//Step 3: Register File
		String uuid = UUID.randomUUID().toString().replace("-", "");
		String datasetRegistryId = String.format("%s:%s:%s", TenantUtils.getTenantName(),kindSubType, uuid);
		Record datasetRegistry = createDatasetRegistry(datasetRegistryId, fileName, unsignedUploadUrl);

		TestGetCreateUpdateDatasetRegistryRequest datasetRegistryRequest = new TestGetCreateUpdateDatasetRegistryRequest(new ArrayList<>());
		datasetRegistryRequest.getDatasetRegistries().add(datasetRegistry);

		ClientResponse datasetRegistryResponse = TestUtils.send(TestUtils.datasetBaseUrl, "registerDataset", "PUT",
				HeaderUtils.getHeaders(TenantUtils.getTenantName(), testUtils.getToken()),
				jsonMapper.writeValueAsString(datasetRegistryRequest), "");

		Assert.assertTrue(datasetRegistryResponse.getStatus() == 201);

		registeredDatasetRegistryIds.add(datasetRegistryId);


		//Step 4: Retrieve File and validate contents
		IntTestGetDatasetRegistryRequest getDatasetRequest = new IntTestGetDatasetRegistryRequest(new ArrayList<>());
		getDatasetRequest.getDatasetRegistryIds().add(datasetRegistryId);

		ClientResponse retrievalClientResponse = TestUtils.send("getRetrievalInstructions", "POST",
				HeaderUtils.getHeaders(TenantUtils.getTenantName(), testUtils.getToken()), 
				jsonMapper.writeValueAsString(getDatasetRequest),
				 "");
		
		Assert.assertEquals(200, retrievalClientResponse.getStatus());

		String getRetrievalRespStr = retrievalClientResponse.getEntity(String.class);	

		IntTestGetDatasetRetrievalInstructionsResponse getRetrievalInstResponse = jsonMapper.readValue(getRetrievalRespStr, IntTestGetDatasetRetrievalInstructionsResponse.class);
		
		IntTestDatasetRetrievalDeliveryItem datasetRetrievalItem = getRetrievalInstResponse.getDelivery().get(0);

		validate_dataset_retrieval_delivery_item(datasetRetrievalItem);
		
		String downloadedContent = cloudStorageUtil.downloadCloudFileUsingDeliveryItem(datasetRetrievalItem.getRetrievalProperties());		

		Assert.assertEquals(fileContents, downloadedContent);
		
		
	}

	public abstract void validate_dataset_retrieval_delivery_item(IntTestDatasetRetrievalDeliveryItem deliveryItem);

	public abstract void validate_storageLocation(Object storageLocation);

	private static Record createDatasetRegistry(String id, String filename, String unsignedUrl) {


		TestGetCreateUpdateDatasetRegistryRequest request = new TestGetCreateUpdateDatasetRegistryRequest();

		Record datasetRegistry = new Record();

		datasetRegistry.setId(id);
		datasetRegistry.setKind(String.format("%s:wks:dataset--File.Generic:1.0.0", TestUtils.getSchemaAuthority()));		
		
		//set legal
		Legal legal = new Legal();
		HashSet<String> legalTags = new HashSet<>();
		legalTags.add(String.format("%s-public-usa-dataset-1", TenantUtils.getTenantName()));
		legal.setLegaltags(legalTags);
		HashSet<String> otherRelevantDataCountries = new HashSet<>();
		otherRelevantDataCountries.add("US");
		legal.setOtherRelevantDataCountries(otherRelevantDataCountries);
		legal.setStatus(LegalCompliance.compliant);
		datasetRegistry.setLegal(legal);

		//set acl
		Acl acl = new Acl();
		String[] viewers = new String[] { String.format("data.default.viewers@%s.testing.com", TenantUtils.getTenantName()) };		
		acl.setViewers(viewers);
		String[] owners = new String[] { String.format("data.default.owners@%s.testing.com", TenantUtils.getTenantName()) };		
		acl.setOwners(owners);
		datasetRegistry.setAcl(acl);

		HashMap<String, Object> data = new HashMap<>();
		data.put("ResourceID", id);
		data.put("ResourceTypeID", "srn:type:file/txt:");
		data.put("ResourceSecurityClassification", "srn:reference-data/ResourceSecurityClassification:RESTRICTED:");
		data.put("ResourceSource", "Dataset Int Test");
		data.put("ResourceName", filename);
		data.put("ResourceDescription", "Test File");
		HashMap<String, Object> datasetProperties = new HashMap<>();
		HashMap<String, Object> fileSourceInfo = new HashMap<>();
		fileSourceInfo.put("FileSource", "");
		fileSourceInfo.put("PreLoadFilePath", unsignedUrl);
		datasetProperties.put("FileSourceInfo", fileSourceInfo);
		data.put("DatasetProperties", datasetProperties);
		datasetRegistry.setData(data);
		
		return datasetRegistry;
	}

	// @Test
	// public void should_registerFiles_and_getFileDelivery() throws Exception {
	// 	ClientResponse uploadLocationResponse = TestUtils.send("getFileUploadLocation", "GET",
	// 			HeaderUtils.getHeaders(TenantUtils.getTenantName(), testUtils.getToken()), "", "");
	// 	Assert.assertEquals(200, uploadLocationResponse.getStatus());

	// 	JsonObject uploadJson = new JsonParser().parse(uploadLocationResponse.getEntity(String.class)).getAsJsonObject();
	// 	JsonObject dataJson = uploadJson.get("uploadLocation").getAsJsonObject();
	// 	String unsignedUrl = dataJson.get("unsignedUrl").getAsString();

	// 	String registerFilesRequestBody = "{\n" +
	// 			"    \"files\": [\n" +
	// 			"        {\n" +
	// 			"            \"unsignedUrl\": \"" + unsignedUrl + "/test.json\",\n" +
	// 			"            \"fileName\": \"test.json\",\n" +
	// 			"            \"fileDescription\": \"test-description\"\n" +
	// 			"        }\n" +
	// 			"    ]\n" +
	// 			"}";

	// 	ClientResponse response = TestUtils.send("registerFiles", "POST",
	// 			HeaderUtils.getHeaders(TenantUtils.getTenantName(), testUtils.getToken()), registerFilesRequestBody, "");
	// 	Assert.assertEquals(200, response.getStatus());

	// 	JsonObject json = new JsonParser().parse(response.getEntity(String.class)).getAsJsonObject();
	// 	JsonObject datasetRegistry = json.get("datasetRegistries").getAsJsonArray().get(0).getAsJsonObject();
	// 	this.recordId = datasetRegistry.get("id").getAsString();
	// 	String preLoadFilePath = datasetRegistry.get("data").getAsJsonObject()
	// 			.get("DatasetProperties").getAsJsonObject()
	// 			.get("FileSourceInfo").getAsJsonObject()
	// 			.get("PreLoadFilePath").getAsString();
	// 	Assert.assertEquals(unsignedUrl + "/test.json", preLoadFilePath);

	// 	// get file delivery of what was just created
	// 	String getFileDeliveryInstructionsBody = "{\n" +
	// 			"    \"datasetRegistryRecordIds\": [\n" +
	// 			"        \"" + this.recordId + "\"\n" +
	// 			"    ]\n" +
	// 			"}";
	// 	ClientResponse fileDeliveryResponse = TestUtils.send("getFileDeliveryInstructions", "POST",
	// 			HeaderUtils.getHeaders(TenantUtils.getTenantName(), testUtils.getToken()), getFileDeliveryInstructionsBody, "");
	// 	Assert.assertEquals(200, response.getStatus());

	// 	json = new JsonParser().parse(fileDeliveryResponse.getEntity(String.class)).getAsJsonObject();
	// 	JsonArray delivery = json.get("delivery").getAsJsonArray();
	// 	String uri = delivery.get(0).getAsJsonObject()
	// 			.get("uri").getAsString();
	// 	String url = delivery.get(0).getAsJsonObject()
	// 			.get("url").getAsString();
	// 	String datasetRegistryRecordId = delivery.get(0).getAsJsonObject()
	// 			.get("datasetRegistryRecordId").getAsString();
	// 	String fileName = delivery.get(0).getAsJsonObject()
	// 			.get("fileName").getAsString();

	// 	Assert.assertEquals(this.recordId, datasetRegistryRecordId);
	// 	Assert.assertNotNull(uri);
	// 	Assert.assertNotNull(url);
	// 	Assert.assertNotNull(fileName);

	// 	// delete record test made
	// 	ClientResponse deleteResponse = TestUtils.send(TestUtils.storageBaseUrl, "records/" + this.recordId, "DELETE",
	// 			HeaderUtils.getHeaders(TenantUtils.getTenantName(), testUtils.getToken()),
	// 			"", "");
	// 	Assert.assertEquals(204, deleteResponse.getStatus());
	// }
}