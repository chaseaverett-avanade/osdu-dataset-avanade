// Copyright © 2021 Amazon Web Services
// Copyright 2017-2019, Schlumberger
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

//TODO: Move to os-core-common

package org.opengroup.osdu.dataset.dms;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.opengroup.osdu.core.common.http.HttpRequest;
import org.opengroup.osdu.core.common.http.HttpResponse;
import org.opengroup.osdu.core.common.http.IHttpClient;
import org.opengroup.osdu.core.common.http.json.HttpResponseBodyMapper;
import org.opengroup.osdu.core.common.http.json.HttpResponseBodyParsingException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.dataset.model.request.GetDatasetRegistryRequest;
import org.opengroup.osdu.dataset.model.response.GetDatasetRetrievalInstructionsResponse;
import org.opengroup.osdu.dataset.model.response.GetDatasetStorageInstructionsResponse;

public class DmsService implements IDmsProvider {

    private String dmsServiceUrl;
    private DmsServiceProperties dmsServiceProperties;
    private final IHttpClient httpClient;
    private final DpsHeaders headers;

    private final ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
    private final HttpResponseBodyMapper bodyMapper = new HttpResponseBodyMapper(objectMapper);

    public DmsService(DmsServiceProperties dmsServiceProperties, IHttpClient httpClient, DpsHeaders headers) {

        this.dmsServiceProperties = dmsServiceProperties;
        this.dmsServiceUrl = dmsServiceProperties.getDmsServiceBaseUrl();
        this.httpClient = httpClient;
        this.headers = headers;

        if (dmsServiceProperties.getApiKey() != null) {
            headers.put("AppKey", dmsServiceProperties.getApiKey());
        }

    }

    @Override
    public GetDatasetStorageInstructionsResponse getStorageInstructions() throws DmsException {

        String url = this.createUrl("/getStorageInstructions");
        HttpResponse result = this.httpClient
                .send(HttpRequest.get().url(url).headers(this.headers.getHeaders()).build());
        return this.getResult(result, GetDatasetStorageInstructionsResponse.class);
    }

    @Override
    public GetDatasetRetrievalInstructionsResponse getDatasetRetrievalInstructions(
            GetDatasetRegistryRequest datasetRegistryRequest) throws DmsException {

        String url = this.createUrl("/getRetrievalInstructions");
        HttpResponse result = this.httpClient
                .send(HttpRequest.post(datasetRegistryRequest).url(url).headers(this.headers.getHeaders()).build());
        return this.getResult(result, GetDatasetRetrievalInstructionsResponse.class);
    }

    private String createUrl(String requestPathAndQuery) {
        return StringUtils.join(this.dmsServiceUrl, requestPathAndQuery);
    }

    private <T> T getResult(HttpResponse result, Class<T> type) throws DmsException {
        if (result.isSuccessCode()) {
            try {
                return bodyMapper.parseBody(result, type);
            } catch (JsonSyntaxException | HttpResponseBodyParsingException e) {
                throw new DmsException("Error parsing response. Check the inner HttpResponse for more info.",
                        result);
            }
        } else {
            throw this.generateException(result);
        }
    }

    private DmsException generateException(HttpResponse result) {
        return new DmsException(
                "Error making request to DMS service. Check the inner HttpResponse for more info.", result);
    }

}
