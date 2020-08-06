// Copyright © Microsoft Corporation
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

package org.opengroup.osdu.util;

import com.google.gson.Gson;
import com.sun.jersey.api.client.ClientResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;


public class LegalTagUtilsAzure extends AzureTestUtils {

	protected static String token = null;

    private static InputStream getTestFileInputStream(String fileName) throws IOException {
        return LegalTagUtilsAzure.class.getResourceAsStream("/" + fileName);
    }

    protected static String readTestFile(String fileName) throws IOException {
        InputStream inputStream = getTestFileInputStream(fileName);
        if(inputStream == null) {
            throw new IOException();
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, length);
        }
        return outputStream.toString(StandardCharsets.UTF_8.toString());
    }

    protected static byte[] getTenantConfigFileContent() throws IOException {
        String content = readTestFile("TenantConfigTestingPurpose.json");
        return content.getBytes();
    }

    public static String createRandomNameTenant() {
        return AzureTestUtils.getMyDataPartition() + "-gae-integration-test-" + System.currentTimeMillis();
    }

    boolean skipHttp() {
        return System.getProperty("SKIP_HTTP_TESTS", System.getenv("SKIP_HTTP_TESTS")) != null;
    }

    public ClientResponse create(String countryOfOrigin, String name) throws Exception {
        return create(countryOfOrigin, name, "2099-12-25", "Transferred Data");
    }
    public ClientResponse create(String countryOfOrigin, String name, String dataType) throws Exception{
        return create(countryOfOrigin, name, "2099-12-25", dataType);
    }
    public ClientResponse create(String countryOfOrigin, String name, String expDate, String dataType)
            throws Exception {
        return create(countryOfOrigin, name, expDate, dataType, AzureTestUtils.getMyDataPartition(),"<my description>");
    }
    public ClientResponse create(String countryOfOrigin, String name, String expDate, String dataType,
                                 String tenantName, String description) throws Exception {
        String body = getBody(countryOfOrigin, name, expDate, dataType, description);
        Map<String, String> headers = getHeaders();
        ClientResponse resp = send("legaltags", "POST", String.format("%s %s", "Bearer", JwtTokenUtil.getAccessToken()),
                body, "", headers);
        Thread.sleep(10);
        return resp;
    }
    public ClientResponse create(String name) throws Exception {
        return create("US", name);
    }
    public ClientResponse delete(String name) throws Exception {
        return delete(name, AzureTestUtils.getMyDataPartition());
    }
    public ClientResponse delete(String name, String tenant) throws Exception {
        Map<String, String> headers = getHeaders();
        return send("legaltags/" + name, "DELETE", String.format("%s %s", "Bearer", JwtTokenUtil.getAccessToken()),
                "", "", headers);
    }

    public class ReadableLegalTag {
        public String name;
        public String description;
        public Properties properties;
    }
    public class ReadableLegalTags {
        public ReadableLegalTag[] legalTags;
    }
    public class InvalidTagWithReason {
        public String name;
        public String reason;
    }
    public class InvalidTagsWithReason {
        public InvalidTagWithReason[] invalidLegalTags;
    }
    public class Properties {
        public String countryOfOrigin[]= new String[0];
        public String contractId = "";
        public String originator= "";
        public String dataType = "";
        public String securityClassification = "";
        public String personalData = "";
        public String exportClassification = "";
        public String expirationDate="";
    }
    public static String createRetrieveBatchBody(String... names){
        RequestLegalTags input = new RequestLegalTags();
        input.names = names;
        Gson gson = new Gson();
        return gson.toJson(input);
    }
    public static String getBody(String name) {
        return getBody("USA", name);
    }
    public static String getBody(String COO, String name) {
        return getBody(COO, name, "2099-12-25", "First Party Data", "<my description>");
    }
    public static String updateBody(String name, String expDate) {
        return "{\"name\": \"" + name + "\","+
                "\"expirationDate\": \"" + expDate + "\",\"contractId\":\"B1234\"}";
    }
    private static String getBody(String countryOfOrigin, String name, String expDate, String dataType, String description) {
        description = description == null ? "" : "\"description\" : \"" + description + "\",";
        expDate = ((expDate == null) || (expDate.length() == 0))  ? "" : "\"expirationDate\" : \"" + expDate + "\",";

        return "{\"name\": \"" + name + "\"," + description +
                "\"properties\": {\"countryOfOrigin\": [\"" + countryOfOrigin + "\"], \"contractId\":\"A1234\"," + expDate + "\"dataType\":\"" + dataType + "\", \"originator\":\" MyCompany     \", \"securityClassification\":\"Public\", \"exportClassification\":\"EAR99\", \"personalData\":\"No Personal Data\"} }";
    }
    public class ReadablePropertyValues {
        public Map<String, String> countriesOfOrigin;
        public Map<String, String> otherRelevantDataCountries;
        public Set<String> securityClassifications;
        public Set<String> exportClassificationControlNumbers;
        public Set<String> personalDataTypes;
        public Set<String> dataTypes;
    }
    public static class RequestLegalTags {
        String[] names;
    }
}