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

package org.opengroup.osdu.dataset.schema.dms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.http.HttpRequest;
import org.opengroup.osdu.core.common.http.HttpResponse;
import org.opengroup.osdu.core.common.http.IHttpClient;
import org.opengroup.osdu.core.common.http.json.HttpResponseBodyParsingException;
import org.opengroup.osdu.core.common.model.http.DpsException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.dataset.dms.DmsException;
import org.opengroup.osdu.dataset.dms.DmsService;
import org.opengroup.osdu.dataset.dms.DmsServiceProperties;
import org.opengroup.osdu.dataset.model.request.GetDatasetRegistryRequest;
import org.opengroup.osdu.dataset.model.response.GetDatasetRetrievalInstructionsResponse;
import org.opengroup.osdu.dataset.model.response.GetDatasetStorageInstructionsResponse;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class DmsServiceTest {

    private final String API_KEY = "key";
    private final String GetDatasetStorageInstructionsResponse_body = "{\"providerKey\":\"dummy-key\",\"storageLocation\":{\"key1\":{},\"key2\":{}}}";
    private final String RetrievalInstructionsResponse_body =  "{\"providerKey\":\"dummy-key\",\"datasets\":[{\"datasetRegistryId\":\"dummyid\",\"retrievalProperties\":{\"key1\":{}}}]}";
    @Mock
    private DmsServiceProperties dmsServiceProperties;

    @Mock
    private IHttpClient httpClient;

    @Mock
    private Map<String, String> headersMap;

    @Mock
    private HttpResponse response;

    @Mock
    private DpsHeaders headers;

    @Before
    public void init() {
        when(headers.getHeaders()).thenReturn(headersMap);
        when(httpClient.send(any())).thenReturn(response);
        when(dmsServiceProperties.getApiKey()).thenReturn(API_KEY);
    }

    @Test
    public void getStorageInstructions_success() throws DmsException {
        DmsService dmsService = new DmsService(dmsServiceProperties, httpClient, headers);
        when(response.isSuccessCode()).thenReturn(true);
        when(response.getBody()).thenReturn(GetDatasetStorageInstructionsResponse_body);

        GetDatasetStorageInstructionsResponse result = dmsService.getStorageInstructions();
        assertNotNull(result);
        assertEquals(result.getProviderKey(), "dummy-key");
        verify(response, times(1)).isSuccessCode();
        verify(headers, times(1)).getHeaders();
        verify(dmsServiceProperties, times(2)).getApiKey();
        verify(httpClient, times(1)).send(any(HttpRequest.class));
    }

    @Test
    public void getStorageInstructions_DmsException() {
        DmsService dmsService = new DmsService(dmsServiceProperties, httpClient, headers);
        when(response.isSuccessCode()).thenReturn(true);
        when(httpClient.send(any())).thenReturn(response);
        when(response.getBody()).thenReturn("{\"key\": \"value\"");

        try {
            dmsService.getStorageInstructions();
        }
        catch (Exception exception) {
            assertTrue(exception instanceof DmsException);
            assertEquals(exception.getMessage(), "Error parsing response. Check the inner HttpResponse for more info.");
        }
        verify(response, times(1)).isSuccessCode();
        verify(headers, times(1)).getHeaders();
        verify(dmsServiceProperties, times(2)).getApiKey();
        verify(httpClient, times(1)).send(any(HttpRequest.class));
    }

    @Test
    public void getStorageInstructions_generateException() throws DpsException, HttpResponseBodyParsingException {
        DmsService dmsService = new DmsService(dmsServiceProperties, httpClient, headers);
        when(response.isSuccessCode()).thenReturn(false);
        when(httpClient.send(any())).thenReturn(response);

        try {
            dmsService.getStorageInstructions();
        }
        catch (Exception exception) {
            assertTrue(exception instanceof DmsException);
            assertEquals(exception.getMessage(), "Error making request to DMS service. Check the inner HttpResponse for more info.");
        }
        verify(response, times(1)).isSuccessCode();
        verify(headers, times(1)).getHeaders();
        verify(dmsServiceProperties, times(2)).getApiKey();
        verify(httpClient, times(1)).send(any(HttpRequest.class));
    }

    /////////////////////////////////////////////////////////

    @Test
    public void getDatasetRetrievalInstructions_success() throws DmsException {
        DmsService dmsService = new DmsService(dmsServiceProperties, httpClient, headers);
        GetDatasetRegistryRequest getDatasetRegistryRequest = new GetDatasetRegistryRequest();
        when(response.isSuccessCode()).thenReturn(true);
        when(response.getBody()).thenReturn(RetrievalInstructionsResponse_body);

        GetDatasetRetrievalInstructionsResponse result = dmsService.getDatasetRetrievalInstructions(getDatasetRegistryRequest);
        assertNotNull(result);
        verify(response, times(1)).isSuccessCode();
        verify(headers, times(1)).getHeaders();
        verify(dmsServiceProperties, times(2)).getApiKey();
        verify(httpClient, times(1)).send(any(HttpRequest.class));
    }

    @Test
    public void getDatasetRetrievalInstructions_DmsException() {
        DmsService dmsService = new DmsService(dmsServiceProperties, httpClient, headers);
        when(response.isSuccessCode()).thenReturn(true);
        when(httpClient.send(any())).thenReturn(response);
        when(response.getBody()).thenReturn("{\"key\": \"value\"");

        try {
            GetDatasetRegistryRequest getDatasetRegistryRequest = new GetDatasetRegistryRequest();
            dmsService.getDatasetRetrievalInstructions(getDatasetRegistryRequest);
        }
        catch (Exception exception) {
            assertTrue(exception instanceof DmsException);
            assertEquals(exception.getMessage(), "Error parsing response. Check the inner HttpResponse for more info.");
        }
        verify(response, times(1)).isSuccessCode();
        verify(headers, times(1)).getHeaders();
        verify(dmsServiceProperties, times(2)).getApiKey();
        verify(httpClient, times(1)).send(any(HttpRequest.class));
    }

    @Test
    public void getDatasetRetrievalInstructions_generateException() throws DpsException, HttpResponseBodyParsingException {
        DmsService dmsService = new DmsService(dmsServiceProperties, httpClient, headers);
        when(response.isSuccessCode()).thenReturn(false);
        when(httpClient.send(any())).thenReturn(response);

        try {
            GetDatasetRegistryRequest getDatasetRegistryRequest = new GetDatasetRegistryRequest();
            dmsService.getDatasetRetrievalInstructions(getDatasetRegistryRequest);
        }
        catch (Exception exception) {
            assertTrue(exception instanceof DmsException);
            assertEquals(exception.getMessage(), "Error making request to DMS service. Check the inner HttpResponse for more info.");
        }
        verify(response, times(1)).isSuccessCode();
        verify(headers, times(1)).getHeaders();
        verify(dmsServiceProperties, times(2)).getApiKey();
        verify(httpClient, times(1)).send(any(HttpRequest.class));
    }
}
