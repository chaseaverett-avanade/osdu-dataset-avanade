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

package org.opengroup.osdu.dataset.dms;

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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.dms.model.CopyDmsRequest;
import org.opengroup.osdu.core.common.dms.model.CopyDmsResponse;
import org.opengroup.osdu.core.common.dms.model.RetrievalInstructionsResponse;
import org.opengroup.osdu.core.common.http.HttpRequest;
import org.opengroup.osdu.core.common.http.HttpResponse;
import org.opengroup.osdu.core.common.http.IHttpClient;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.dataset.model.request.GetDatasetRegistryRequest;
import org.opengroup.osdu.dataset.model.response.GetDatasetStorageInstructionsResponse;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class DmsRestServiceTest {

    private final String URL = "https://contoso.com";
    private final String INVALID_URL = "http://finance.yahoo.com/q/h?s=^IXIC";
    private final String GetDatasetStorageInstructionsResponse_body = "{\"providerKey\":\"dummy-key\",\"storageLocation\":{\"key1\":{},\"key2\":{}}}";
    private final String RetrievalInstructionsResponse_body =  "{\"providerKey\":\"dummy-key\",\"datasets\":[{\"datasetRegistryId\":\"dummyid\",\"retrievalProperties\":{\"key1\":{}}}]}";
    private final String CopyDmsResponse_body = "[{\"success\": true, \"datasetBlobStoragePath\": \"string\"}]";
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

    @InjectMocks
    private DmsRestService dmsRestService;

    @Before
    public void init() {
        when(headers.getHeaders()).thenReturn(headersMap);
        when(httpClient.send(any())).thenReturn(response);
    }

    @Test
    public void getStorageInstructions_success() {
        when(response.getBody()).thenReturn(GetDatasetStorageInstructionsResponse_body);
        when(dmsServiceProperties.getDmsServiceBaseUrl()).thenReturn(URL);
        GetDatasetStorageInstructionsResponse body = dmsRestService.getStorageInstructions();

        assertNotNull(body);
        verify(headers, times(1)).getHeaders();
        verify(httpClient, times(1)).send(any(HttpRequest.class));
        verify(dmsServiceProperties, times(1)).getDmsServiceBaseUrl();
    }

    @Test
    public void getStorageInstructions_URISyntaxException() {
        when(dmsServiceProperties.getDmsServiceBaseUrl()).thenReturn(INVALID_URL);
        try {
            dmsRestService.getStorageInstructions();
        }
        catch (Exception e) {
            assertNotNull(e);
            assertTrue(e instanceof AppException);
            assertEquals("Invalid URL", ((AppException) e).getError().getReason());
            assertEquals(500, ((AppException) e).getError().getCode());
        }

        verify(dmsServiceProperties, times(1)).getDmsServiceBaseUrl();
        verify(httpClient, times(0)).send(any(HttpRequest.class));
    }

    @Test
    public void getStorageInstructions_JsonProcessingException() {

        when(response.getBody()).thenReturn("");
        when(dmsServiceProperties.getDmsServiceBaseUrl()).thenReturn(URL);

        try {
            dmsRestService.getStorageInstructions();
        }
        catch (Exception e) {
            assertNotNull(e);
            assertTrue(e instanceof AppException);
            assertEquals("Internal Server Error", ((AppException) e).getError().getReason());
            assertEquals(500, ((AppException) e).getError().getCode());
        }
        verify(dmsServiceProperties, times(1)).getDmsServiceBaseUrl();
        verify(httpClient, times(1)).send(any(HttpRequest.class));
        verify(headers, times(1)).getHeaders();
    }


    @Test
    public void getRetrievalInstructions_success() {
        when(response.getBody()).thenReturn(RetrievalInstructionsResponse_body);
        when(dmsServiceProperties.getDmsServiceBaseUrl()).thenReturn(URL);
        GetDatasetRegistryRequest getDatasetRegistryRequest = new GetDatasetRegistryRequest();
        getDatasetRegistryRequest.datasetRegistryIds = Arrays.asList("sup1", "sup2", "sup3");

        RetrievalInstructionsResponse body = dmsRestService.getRetrievalInstructions(getDatasetRegistryRequest);

        assertNotNull(body);
        verify(headers, times(1)).getHeaders();
        verify(httpClient, times(1)).send(any(HttpRequest.class));
        verify(dmsServiceProperties, times(1)).getDmsServiceBaseUrl();
    }

    @Test
    public void getRetrievalInstructions_URISyntaxException() {
        when(dmsServiceProperties.getDmsServiceBaseUrl()).thenReturn(INVALID_URL);
        GetDatasetRegistryRequest getDatasetRegistryRequest = new GetDatasetRegistryRequest();
        getDatasetRegistryRequest.datasetRegistryIds = Arrays.asList("sup1", "sup2", "sup3");
        try {
            dmsRestService.getRetrievalInstructions(getDatasetRegistryRequest);
        }
        catch (Exception e) {
            assertNotNull(e);
            assertTrue(e instanceof AppException);
            assertEquals("Invalid URL", ((AppException) e).getError().getReason());
            assertEquals(500, ((AppException) e).getError().getCode());
        }

        verify(dmsServiceProperties, times(1)).getDmsServiceBaseUrl();
        verify(httpClient, times(0)).send(any(HttpRequest.class));
    }

    @Test
    public void getRetrievalInstructions_JsonProcessingException() {

        when(response.getBody()).thenReturn("");
        when(dmsServiceProperties.getDmsServiceBaseUrl()).thenReturn(URL);
        GetDatasetRegistryRequest getDatasetRegistryRequest = new GetDatasetRegistryRequest();
        getDatasetRegistryRequest.datasetRegistryIds = Arrays.asList("sup1", "sup2", "sup3");
        try {
            dmsRestService.getRetrievalInstructions(getDatasetRegistryRequest);
        }
        catch (Exception e) {
            assertNotNull(e);
            assertTrue(e instanceof AppException);
            assertEquals("Internal Server Error", ((AppException) e).getError().getReason());
            assertEquals(500, ((AppException) e).getError().getCode());
        }
        verify(dmsServiceProperties, times(1)).getDmsServiceBaseUrl();
        verify(httpClient, times(1)).send(any(HttpRequest.class));
        verify(headers, times(1)).getHeaders();
    }


    @Test
    public void copyDmsToPersistentStorage_success() {
        when(response.getBody()).thenReturn(CopyDmsResponse_body);
        when(dmsServiceProperties.getDmsServiceBaseUrl()).thenReturn(URL);
        List<CopyDmsResponse> data = dmsRestService.copyDmsToPersistentStorage(new CopyDmsRequest());
        assertNotNull(data);
        verify(headers, times(1)).getHeaders();
        verify(httpClient, times(1)).send(any(HttpRequest.class));
        verify(dmsServiceProperties, times(1)).getDmsServiceBaseUrl();
    }

    @Test
    public void copyDmsToPersistentStorage_URISyntaxException() {
        when(dmsServiceProperties.getDmsServiceBaseUrl()).thenReturn(INVALID_URL);
        try {
            dmsRestService.copyDmsToPersistentStorage(new CopyDmsRequest());
        }
        catch (Exception e) {
            assertNotNull(e);
            assertTrue(e instanceof AppException);
            assertEquals("Invalid URL", ((AppException) e).getError().getReason());
            assertEquals(500, ((AppException) e).getError().getCode());
        }

        verify(dmsServiceProperties, times(1)).getDmsServiceBaseUrl();
        verify(httpClient, times(0)).send(any(HttpRequest.class));
    }

    @Test
    public void copyDmsToPersistentStorage_JsonProcessingException() {

        when(response.getBody()).thenReturn("");
        when(dmsServiceProperties.getDmsServiceBaseUrl()).thenReturn(URL);
        try {
            dmsRestService.copyDmsToPersistentStorage(new CopyDmsRequest());
        }
        catch (Exception e) {
            assertNotNull(e);
            assertTrue(e instanceof AppException);
            assertEquals("Internal Server Error", ((AppException) e).getError().getReason());
            assertEquals(500, ((AppException) e).getError().getCode());
        }
        verify(dmsServiceProperties, times(1)).getDmsServiceBaseUrl();
        verify(httpClient, times(1)).send(any(HttpRequest.class));
        verify(headers, times(1)).getHeaders();
    }
}
