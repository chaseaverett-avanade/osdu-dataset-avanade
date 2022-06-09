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

package org.opengroup.osdu.dataset.schema;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.http.HttpResponse;
import org.opengroup.osdu.core.common.http.IHttpClient;
import org.opengroup.osdu.core.common.http.json.HttpResponseBodyMapper;
import org.opengroup.osdu.core.common.http.json.HttpResponseBodyParsingException;
import org.opengroup.osdu.core.common.model.http.DpsException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;

@RunWith(MockitoJUnitRunner.class)
public class SchemaServiceTest {

    private final String KIND = "opendes:wks:1.0.0";
    private final String URL = "contoso.com";
    private final String API_KEY = "key";

    @Mock
    private IHttpClient httpClient;

    @Mock
    private DpsHeaders headers;

    @Mock
    private HttpResponseBodyMapper bodyMapper;

    @Mock
    private SchemaAPIConfig schemaAPIConfig;

    @Mock
    private HttpResponse response;

    @Mock
    private Map<String, String> headersMap;

    @Mock
    private HttpResponseBodyParsingException httpResponseBodyParsingException;

    @Mock
    private Object schemaObject;

    @Before
    public void init() {
        schemaAPIConfig.apiKey = API_KEY;
        when(schemaAPIConfig.getRootUrl()).thenReturn(URL);
        when(schemaAPIConfig.getApiKey()).thenReturn(API_KEY);
        when(headers.getHeaders()).thenReturn(headersMap);
    }

    @Test
    public void get_schema_success() throws DpsException, HttpResponseBodyParsingException {
        when(response.isSuccessCode()).thenReturn(true);
        when(httpClient.send(any())).thenReturn(response);
        when(bodyMapper.parseBody(any(HttpResponse.class), any())).thenReturn(schemaObject);
        SchemaService schemaService = new SchemaService(schemaAPIConfig, httpClient, headers, bodyMapper);
        Object result = schemaService.getSchema(KIND);

        assertNotNull(result);
        verify(response, times(1)).isSuccessCode();
        verify(headers, times(1)).getHeaders();
        verify(bodyMapper, times(1)).parseBody(any(HttpResponse.class), any());
    }

    @Test
    public void get_schema_failure_DpsException() throws HttpResponseBodyParsingException {
        when(response.isSuccessCode()).thenReturn(true);
        when(httpClient.send(any())).thenReturn(response);
        when(bodyMapper.parseBody(any(HttpResponse.class), any())).thenThrow(httpResponseBodyParsingException);

        try {
            SchemaService schemaService = new SchemaService(schemaAPIConfig, httpClient, headers, bodyMapper);
            schemaService.getSchema(KIND);
        }
        catch (Exception exception) {
            assertTrue(exception instanceof DpsException);
            assertEquals(exception.getMessage(), "Error parsing response. Check the inner HttpResponse for more info.");
        }
        verify(response, times(1)).isSuccessCode();
        verify(headers, times(1)).getHeaders();
        verify(bodyMapper, times(1)).parseBody(any(HttpResponse.class), any());
    }

    @Test
    public void get_schema_failure_generateException() throws DpsException, HttpResponseBodyParsingException {
        when(response.isSuccessCode()).thenReturn(false);
        when(httpClient.send(any())).thenReturn(response);

        try {
            SchemaService schemaService = new SchemaService(schemaAPIConfig, httpClient, headers, bodyMapper);
            schemaService.getSchema(KIND);
        }
        catch (Exception exception) {
            assertTrue(exception instanceof DpsException);
            assertEquals(exception.getMessage(), "Error making request to Schema service. Check the inner HttpResponse for more info.");
        }
        verify(response, times(1)).isSuccessCode();
        verify(headers, times(1)).getHeaders();
    }
}
