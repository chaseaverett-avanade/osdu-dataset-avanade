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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.Gson;
import com.sun.jersey.api.client.ClientResponse;
import cucumber.api.Scenario;
import lombok.extern.java.Log;
import org.opengroup.osdu.core.common.model.legal.Legal;
import org.opengroup.osdu.models.TestIndex;
import org.opengroup.osdu.response.ResponseBase;
import org.opengroup.osdu.util.CloudStorageUtils;
import org.opengroup.osdu.util.HTTPClient;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.opengroup.osdu.util.Config.*;
import static org.opengroup.osdu.util.Config.getOtherRelevantDataCountries;

@Log
public abstract class TestsBase {
    protected HTTPClient httpClient;
    protected Scenario scenario;
    protected Map<String, String> tenantMap = new HashMap<>();
    protected Map<String, TestIndex> inputRecordMap = new HashMap<>();
    protected CloudStorageUtils cloudStorageUtils;
    protected ObjectMapper mapper;


    public TestsBase(HTTPClient httpClient, CloudStorageUtils cloudStorageUtils) {
        this.httpClient = httpClient;
        this.generateTenantMapping();
        this.cloudStorageUtils = cloudStorageUtils;
        mapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    protected TestIndex getTextIndex(){
        return new TestIndex();
    }

    /******************One time cleanup for whole feature**************/
    public void tearDown() {
        for (String kind : inputRecordMap.keySet()) {
            TestIndex testIndex = inputRecordMap.get(kind);
        }
    }

    protected abstract String getApi();

    protected abstract String getHttpMethod();

    protected <T extends ResponseBase> T executeQuery(String api, String payLoad, Map<String, String> headers, String token, Class<T> typeParameterClass) {
        ClientResponse clientResponse = httpClient.send(this.getHttpMethod(), api, payLoad, headers, token);
        logCorrelationIdWithFunctionName(clientResponse.getHeaders());
        return getResponse(clientResponse, typeParameterClass);
    }



    protected String getTenantMapping(String tenant) {
        if (tenantMap.containsKey(tenant)) {
            return tenantMap.get(tenant);
        }
        return null;
    }

    protected String generateActualName(String rawName, String timeStamp) {
        for (String tenant : tenantMap.keySet()) {
            rawName = rawName.replaceAll(tenant, getTenantMapping(tenant));
        }
        return rawName.replaceAll("<timestamp>", timeStamp);
    }

    protected Legal generateLegalTag() {
        Legal legal = new Legal();
        Set<String> legalTags = new HashSet<>();
        legalTags.add(getLegalTag());
        legal.setLegaltags(legalTags);
        Set<String> otherRelevantCountries = new HashSet<>();
        otherRelevantCountries.add(getOtherRelevantDataCountries());
        legal.setOtherRelevantDataCountries(otherRelevantCountries);
        return legal;
    }

    private void generateTenantMapping(){
        tenantMap.put("tenant1", getDataPartitionIdTenant1());
        tenantMap.put("tenant2", getDataPartitionIdTenant2());
        tenantMap.put("common", "common");
    }


    private <T extends ResponseBase> T getResponse(ClientResponse clientResponse, Class<T> typeParameterClass) {
        log.info(String.format("Response status: %s, type: %s", clientResponse.getStatus(), clientResponse.getType().toString()));
        assertEquals(MediaType.APPLICATION_JSON, clientResponse.getType().toString());
        String responseEntity = clientResponse.getEntity(String.class);

        T response = new Gson().fromJson(responseEntity, typeParameterClass);
        response.setHeaders(clientResponse.getHeaders());
        response.setResponseCode(clientResponse.getStatus());
        return response;
    }


    private void logCorrelationIdWithFunctionName(MultivaluedMap<String, String> headers) {
        log.info(String.format("Scenario Name: %s, Correlation-Id: %s", scenario.getId(), headers.get("correlation-id")));
    }
}
