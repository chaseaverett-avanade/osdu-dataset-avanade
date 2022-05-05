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

package org.opengroup.osdu.dataset.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.dms.model.CopyDmsResponse;
import org.opengroup.osdu.core.common.http.HttpResponse;
import org.opengroup.osdu.core.common.http.json.HttpResponseBodyMapper;
import org.opengroup.osdu.core.common.http.json.HttpResponseBodyParsingException;
import org.opengroup.osdu.core.common.model.entitlements.Acl;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.storage.MultiRecordInfo;
import org.opengroup.osdu.core.common.model.storage.Record;
import org.opengroup.osdu.core.common.model.storage.StorageException;
import org.opengroup.osdu.core.common.model.storage.UpsertRecords;
import org.opengroup.osdu.core.common.storage.IStorageFactory;
import org.opengroup.osdu.core.common.storage.IStorageService;
import org.opengroup.osdu.dataset.dms.DmsServiceProperties;
import org.opengroup.osdu.dataset.dms.IDmsFactory;
import org.opengroup.osdu.dataset.dms.IDmsProvider;
import org.opengroup.osdu.dataset.model.request.SchemaExceptionResponse;
import org.opengroup.osdu.dataset.model.request.SchemaExceptionResponseBody;
import org.opengroup.osdu.dataset.model.request.StorageExceptionResponse;
import org.opengroup.osdu.dataset.model.response.GetCreateUpdateDatasetRegistryResponse;
import org.opengroup.osdu.dataset.provider.interfaces.IDatasetDmsServiceMap;
import org.opengroup.osdu.dataset.schema.ISchemaFactory;
import org.opengroup.osdu.dataset.schema.ISchemaService;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class DatasetRegistryServiceImplTest {

    private final String datasetRegistryId = "id";
    private final String exceptionReason = "reason";
    private final String exceptionMessage = "message";
    private final String RECORD_ID = "opendes:dataset--file:data";
    private final String INVALID_RECORD_ID = "closedes:dataset--file:data";
    private final String KIND = "opendes:wks:dataset--file:1.0.0";
    private final String INVALID_KIND = "opendes:wks:dataset--file:1.0.0.0";
    private final String ACL_VIEWER = "data.default.viewers@opendes.contoso.com";
    private final String ACL_OWNER = "data.default.owners@opendes.contoso.com";
    private final String DATA_PARTITION_ID = "opendes";
    private final String API_KEY = "key";
    private final String URL = "https://contoso.com";

    @Mock
    private HttpResponseBodyMapper bodyMapper;

    @Mock
    private DpsHeaders headers;

    @Mock
    private IStorageService storageService;

    @Mock
    private ISchemaService schemaService;

    @Mock
    IStorageFactory storageFactory;

    @Mock
    ISchemaFactory schemaFactory;

    @Mock
    private IDmsFactory dmsFactory;

    @Mock
    private HttpResponse httpResponse;

    @Mock
    private DpsException dpsException;

    @Mock
    IDmsProvider dmsProvider;

    @Mock
    private IDatasetDmsServiceMap dmsServiceMap;

    @Mock
    private StorageException storageException;

    @Mock
    private HttpResponseBodyParsingException httpResponseBodyParsingException;

    @Mock
    private StorageExceptionResponse storageExceptionResponse;

    @Mock
    private SchemaExceptionResponseBody schemaExceptionResponseBody;

    @Mock
    private Object schemaObject;

    @Mock
    private MultiRecordInfo multiRecordInfo;

    @Mock
    private SchemaExceptionResponse schemaExceptionResponse;

    @Mock
    private UpsertRecords upsertRecords;

    @InjectMocks
    private DatasetRegistryServiceImpl datasetRegistryService;

    @Before
    public void init() {
        initMocks(this);
        when(storageFactory.create(headers)).thenReturn(storageService);
        when(schemaFactory.create(headers)).thenReturn(schemaService);
        when(headers.getPartitionId()).thenReturn(DATA_PARTITION_ID);
    }

    @Test
    public void deleteDatasetRegistrySuccess() {
        datasetRegistryService.deleteDatasetRegistry(datasetRegistryId);

        verify(storageFactory, times(1)).create(headers);
    }

    @Test
    public void deleteDatasetRegistryException() throws HttpResponseBodyParsingException, StorageException {
        doThrow(storageException).when(storageService).deleteRecord(datasetRegistryId);
        when(storageException.getHttpResponse()).thenReturn(httpResponse);
        when(storageExceptionResponse.getMessage()).thenReturn(exceptionMessage);
        when(storageExceptionResponse.getReason()).thenReturn(exceptionReason);
        when(bodyMapper.parseBody(httpResponse, StorageExceptionResponse.class)).thenReturn(storageExceptionResponse);
        try {
            datasetRegistryService.deleteDatasetRegistry(datasetRegistryId);
        }
        catch (Exception exception) {
            assertTrue(exception instanceof AppException);
            assertEquals(exceptionMessage, exception.getMessage());
            assertEquals("Storage Service: " + exceptionReason, ((AppException) exception).getError().getReason());
        }

        verify(storageFactory, times(1)).create(headers);
        verify(bodyMapper, times(1)).parseBody(httpResponse, StorageExceptionResponse.class);
        verify(storageExceptionResponse, times(1)).getReason();
        verify(storageExceptionResponse, times(1)).getMessage();
    }

    @Test
    public void deleteDatasetRegistryHttpResponseBodyParsingException() throws HttpResponseBodyParsingException, StorageException {
        doThrow(storageException).when(storageService).deleteRecord(datasetRegistryId);
        when(storageException.getHttpResponse()).thenReturn(httpResponse);
        when(bodyMapper.parseBody(httpResponse, StorageExceptionResponse.class)).thenThrow(httpResponseBodyParsingException);
        try {
            datasetRegistryService.deleteDatasetRegistry(datasetRegistryId);
        }
        catch (Exception exception) {
            assertTrue(exception instanceof AppException);
            assertEquals("Failed to parse error from Storage Service", exception.getMessage());
        }

        verify(storageFactory, times(1)).create(headers);
    }

    @Test
    public void createOrUpdateDatasetRegistrySuccess() throws DpsException {
        when(schemaService.getSchema(KIND)).thenReturn(schemaObject);
        when(multiRecordInfo.getRecords()).thenReturn(Collections.singletonList(getRecord(RECORD_ID, KIND)));
        when(storageService.getRecords(any())).thenReturn(multiRecordInfo);
        when(upsertRecords.getRecordIds()).thenReturn((Collections.singletonList(RECORD_ID)));
        when(storageService.upsertRecord(any(Record[].class))).thenReturn(upsertRecords);
        when(dmsServiceMap.getResourceTypeToDmsServiceMap()).thenReturn(getDmsServicePropertiesMap());
        when(dmsProvider.copyDmsToPersistentStorage(any())).thenReturn(Collections.singletonList(new CopyDmsResponse(true, "")));
        when(dmsFactory.create(any(), any())).thenReturn(dmsProvider);

        GetCreateUpdateDatasetRegistryResponse result = datasetRegistryService.createOrUpdateDatasetRegistry(Collections.singletonList(getRecord(RECORD_ID, KIND)));

        assertNotNull(result);
        assertEquals(1, result.getDatasetRegistries().size());
        assertEquals(RECORD_ID, result.getDatasetRegistries().get(0).getId());
        assertEquals(KIND, result.getDatasetRegistries().get(0).getKind());
        verify(storageFactory, times(1)).create(headers);
        verify(schemaFactory, times(1)).create(headers);
        verify(schemaService, times(1)).getSchema(KIND);
        verify(headers, times(1)).getPartitionId();
        verify(dmsServiceMap, times(2)).getResourceTypeToDmsServiceMap();
        verify(dmsFactory, times(1)).create(eq(headers), any(DmsServiceProperties.class));
        verify(dmsProvider, times(1)).copyDmsToPersistentStorage(any());
        verify(storageService, times(1)).upsertRecord(any(Record[].class));
        verify(storageService, times(1)).getRecords(eq(Collections.singletonList(RECORD_ID)));
        verify(upsertRecords, times(1)).getRecordIds();
    }


































































    @Test
    public void createOrUpdateDatasetRegistryAppExceptionInUpsertRecord() throws DpsException, HttpResponseBodyParsingException {
        when(schemaService.getSchema(KIND)).thenReturn(schemaObject);
        when(multiRecordInfo.getRecords()).thenReturn(Collections.singletonList(getRecord(RECORD_ID, KIND)));
        when(storageService.getRecords(any())).thenReturn(multiRecordInfo);
        when(storageExceptionResponse.getMessage()).thenReturn(exceptionMessage);
        when(storageExceptionResponse.getReason()).thenReturn(exceptionReason);
        when(upsertRecords.getRecordIds()).thenReturn((Collections.singletonList(RECORD_ID)));
        when(storageException.getHttpResponse()).thenReturn(httpResponse);
        when(storageService.upsertRecord(any(Record[].class))).thenThrow(storageException);
        when(bodyMapper.parseBody(httpResponse, StorageExceptionResponse.class)).thenReturn(storageExceptionResponse);
        when(dmsServiceMap.getResourceTypeToDmsServiceMap()).thenReturn(getDmsServicePropertiesMap());
        when(dmsProvider.copyDmsToPersistentStorage(any())).thenReturn(Collections.singletonList(new CopyDmsResponse(true, "")));
        when(dmsFactory.create(any(), any())).thenReturn(dmsProvider);

        try {
            datasetRegistryService.createOrUpdateDatasetRegistry(Collections.singletonList(getRecord(RECORD_ID, KIND)));
        }
        catch (Exception exception) {
            assertTrue(exception instanceof AppException);
            assertEquals(exceptionMessage, exception.getMessage());
            assertEquals("Storage Service: " + exceptionReason, ((AppException) exception).getError().getReason());
        }

        verify(storageFactory, times(1)).create(headers);
        verify(schemaFactory, times(1)).create(headers);
        verify(schemaService, times(1)).getSchema(KIND);
        verify(headers, times(1)).getPartitionId();
        verify(dmsServiceMap, times(2)).getResourceTypeToDmsServiceMap();
        verify(dmsFactory, times(1)).create(eq(headers), any(DmsServiceProperties.class));
        verify(dmsProvider, times(1)).copyDmsToPersistentStorage(any());
        verify(storageService, times(1)).upsertRecord(any(Record[].class));
        verify(bodyMapper, times(1)).parseBody(httpResponse, StorageExceptionResponse.class);
        verify(storageExceptionResponse, times(1)).getReason();
        verify(storageExceptionResponse, times(1)).getMessage();
    }

    @Test
    public void createOrUpdateDatasetRegistryHttpResponseBodyParsingExceptionInUpsertRecord() throws DpsException, HttpResponseBodyParsingException {
        when(schemaService.getSchema(KIND)).thenReturn(schemaObject);
        when(multiRecordInfo.getRecords()).thenReturn(Collections.singletonList(getRecord(RECORD_ID, KIND)));
        when(storageService.getRecords(any())).thenReturn(multiRecordInfo);
        when(upsertRecords.getRecordIds()).thenReturn((Collections.singletonList(RECORD_ID)));
        when(storageException.getHttpResponse()).thenReturn(httpResponse);
        when(storageService.upsertRecord(any(Record[].class))).thenThrow(storageException);
        when(bodyMapper.parseBody(httpResponse, StorageExceptionResponse.class)).thenThrow(httpResponseBodyParsingException);
        when(dmsServiceMap.getResourceTypeToDmsServiceMap()).thenReturn(getDmsServicePropertiesMap());
        when(dmsProvider.copyDmsToPersistentStorage(any())).thenReturn(Collections.singletonList(new CopyDmsResponse(true, "")));
        when(dmsFactory.create(any(), any())).thenReturn(dmsProvider);

        try {
            datasetRegistryService.createOrUpdateDatasetRegistry(Collections.singletonList(getRecord(RECORD_ID, KIND)));
        }
        catch (Exception exception) {
            assertTrue(exception instanceof AppException);
            assertEquals("Failed to parse error from Storage Service", exception.getMessage());
        }

        verify(storageFactory, times(1)).create(headers);
        verify(schemaFactory, times(1)).create(headers);
        verify(schemaService, times(1)).getSchema(KIND);
        verify(headers, times(1)).getPartitionId();
        verify(dmsServiceMap, times(2)).getResourceTypeToDmsServiceMap();
        verify(dmsFactory, times(1)).create(eq(headers), any(DmsServiceProperties.class));
        verify(dmsProvider, times(1)).copyDmsToPersistentStorage(any());
        verify(storageService, times(1)).upsertRecord(any(Record[].class));
        verify(bodyMapper, times(1)).parseBody(httpResponse, StorageExceptionResponse.class);
    }


    @Test
    public void createOrUpdateDatasetRegistryAppExceptionInGetRecords() throws DpsException, HttpResponseBodyParsingException {
        when(schemaService.getSchema(KIND)).thenReturn(schemaObject);
        when(multiRecordInfo.getRecords()).thenReturn(Collections.singletonList(getRecord(RECORD_ID, KIND)));
        when(storageService.getRecords(any())).thenReturn(multiRecordInfo);
        when(upsertRecords.getRecordIds()).thenReturn((Collections.singletonList(RECORD_ID)));
        when(storageService.upsertRecord(any(Record[].class))).thenReturn(upsertRecords);
        when(storageException.getHttpResponse()).thenReturn(httpResponse);
        when(storageService.getRecords(eq(Collections.singletonList(RECORD_ID)))).thenThrow(storageException);
        when(storageExceptionResponse.getMessage()).thenReturn(exceptionMessage);
        when(storageExceptionResponse.getReason()).thenReturn(exceptionReason);
        when(bodyMapper.parseBody(httpResponse, StorageExceptionResponse.class)).thenReturn(storageExceptionResponse);
        when(dmsServiceMap.getResourceTypeToDmsServiceMap()).thenReturn(getDmsServicePropertiesMap());
        when(dmsProvider.copyDmsToPersistentStorage(any())).thenReturn(Collections.singletonList(new CopyDmsResponse(true, "")));
        when(dmsFactory.create(any(), any())).thenReturn(dmsProvider);

        try {
            datasetRegistryService.createOrUpdateDatasetRegistry(Collections.singletonList(getRecord(RECORD_ID, KIND)));
        }
        catch (Exception exception) {
            assertTrue(exception instanceof AppException);
            assertEquals(exceptionMessage, exception.getMessage());
            assertEquals("Storage Service: " + exceptionReason, ((AppException) exception).getError().getReason());
        }

        verify(storageFactory, times(1)).create(headers);
        verify(schemaFactory, times(1)).create(headers);
        verify(schemaService, times(1)).getSchema(KIND);
        verify(headers, times(1)).getPartitionId();
        verify(dmsServiceMap, times(2)).getResourceTypeToDmsServiceMap();
        verify(dmsFactory, times(1)).create(eq(headers), any(DmsServiceProperties.class));
        verify(dmsProvider, times(1)).copyDmsToPersistentStorage(any());
        verify(storageService, times(1)).upsertRecord(any(Record[].class));
        verify(bodyMapper, times(1)).parseBody(httpResponse, StorageExceptionResponse.class);
        verify(storageExceptionResponse, times(1)).getReason();
        verify(storageExceptionResponse, times(1)).getMessage();
    }

    @Test
    public void createOrUpdateDatasetRegistryHttpResponseBodyParsingExceptionInGetRecord() throws DpsException, HttpResponseBodyParsingException {
        when(schemaService.getSchema(KIND)).thenReturn(schemaObject);
        when(multiRecordInfo.getRecords()).thenReturn(Collections.singletonList(getRecord(RECORD_ID, KIND)));
        when(storageService.getRecords(any())).thenReturn(multiRecordInfo);
        when(upsertRecords.getRecordIds()).thenReturn((Collections.singletonList(RECORD_ID)));
        when(storageException.getHttpResponse()).thenReturn(httpResponse);
        when(storageService.upsertRecord(any(Record[].class))).thenReturn(upsertRecords);
        when(upsertRecords.getRecordIds()).thenReturn((Collections.singletonList(RECORD_ID)));
        when(storageService.getRecords(eq(Collections.singletonList(RECORD_ID)))).thenThrow(storageException);
        when(bodyMapper.parseBody(httpResponse, StorageExceptionResponse.class)).thenThrow(httpResponseBodyParsingException);
        when(dmsServiceMap.getResourceTypeToDmsServiceMap()).thenReturn(getDmsServicePropertiesMap());
        when(dmsProvider.copyDmsToPersistentStorage(any())).thenReturn(Collections.singletonList(new CopyDmsResponse(true, "")));
        when(dmsFactory.create(any(), any())).thenReturn(dmsProvider);

        try {
            datasetRegistryService.createOrUpdateDatasetRegistry(Collections.singletonList(getRecord(RECORD_ID, KIND)));
        }
        catch (Exception exception) {
            assertTrue(exception instanceof AppException);
            assertEquals("Failed to parse error from Storage Service", exception.getMessage());
        }

        verify(storageFactory, times(1)).create(headers);
        verify(schemaFactory, times(1)).create(headers);
        verify(schemaService, times(1)).getSchema(KIND);
        verify(headers, times(1)).getPartitionId();
        verify(dmsServiceMap, times(2)).getResourceTypeToDmsServiceMap();
        verify(dmsFactory, times(1)).create(eq(headers), any(DmsServiceProperties.class));
        verify(dmsProvider, times(1)).copyDmsToPersistentStorage(any());
        verify(storageService, times(1)).upsertRecord(any(Record[].class));
        verify(bodyMapper, times(1)).parseBody(httpResponse, StorageExceptionResponse.class);
    }

    @Test
    public void createOrUpdateDatasetRegistryAppExceptionInValidateDatasets() {
        try {
            datasetRegistryService.createOrUpdateDatasetRegistry(Collections.singletonList(getRecord(INVALID_RECORD_ID, KIND)));
        }
        catch (Exception exception) {
            assertTrue(exception instanceof AppException);
            String msg = String.format(
                    "The record '%s' does not have a valid ID",	INVALID_RECORD_ID);
            assertEquals(msg, exception.getMessage());
            assertEquals("Invalid record id", ((AppException) exception).getError().getReason());
        }

        verify(storageFactory, times(1)).create(headers);
        verify(schemaFactory, times(1)).create(headers);
        verify(headers, times(1)).getPartitionId();
    }

    @Test
    public void createOrUpdateDatasetRegistryAppExceptionInValidateDatasetsInvalidSchema() {
        try {
            datasetRegistryService.createOrUpdateDatasetRegistry(Collections.singletonList(getRecord(RECORD_ID, INVALID_KIND)));
        }
        catch (Exception exception) {
            assertTrue(exception instanceof AppException);
            assertEquals("One or more records has an invalid Kind. Must use 'dataset' group type", exception.getMessage());
        }

        verify(storageFactory, times(1)).create(headers);
        verify(schemaFactory, times(1)).create(headers);
        verify(headers, times(1)).getPartitionId();
    }

    @Test
    public void createOrUpdateDatasetRegistryDPSExceptionInValidateDatasetsInvalidSchema() throws DpsException, HttpResponseBodyParsingException {
        when(dpsException.getHttpResponse()).thenReturn(httpResponse);
        when(bodyMapper.parseBody(httpResponse, SchemaExceptionResponse.class)).thenReturn(schemaExceptionResponse);
        when(schemaExceptionResponse.getError()).thenReturn(schemaExceptionResponseBody);
        when(schemaExceptionResponseBody.getCode()).thenReturn(500);
        when(schemaExceptionResponseBody.getMessage()).thenReturn(exceptionMessage);
        when(schemaService.getSchema(KIND)).thenThrow(dpsException);
        try {
            datasetRegistryService.createOrUpdateDatasetRegistry(Collections.singletonList(getRecord(RECORD_ID, KIND)));
        }
        catch (Exception exception) {
            assertTrue(exception instanceof AppException);
            assertEquals(500, ((AppException) exception).getError().getCode());
            assertEquals(String.format("Schema Service: get '%s'", KIND), ((AppException) exception).getError().getReason());
            assertEquals(exceptionMessage, exception.getMessage());
        }

        verify(storageFactory, times(1)).create(headers);
        verify(schemaFactory, times(1)).create(headers);
        verify(headers, times(1)).getPartitionId();
        verify(bodyMapper, times(1)).parseBody(httpResponse, SchemaExceptionResponse.class);
        verify(schemaExceptionResponse, times(1)).getError();
        verify(schemaExceptionResponseBody, times(1)).getCode();
        verify(schemaExceptionResponseBody, times(1)).getMessage();
        verify(dpsException, times(1)).getHttpResponse();
    }

    @Test
    public void createOrUpdateDatasetRegistryHttpResponseBodyParsingExceptionInValidateDatasetsInvalidSchema() throws DpsException, HttpResponseBodyParsingException {
        when(dpsException.getHttpResponse()).thenReturn(httpResponse);
        when(bodyMapper.parseBody(httpResponse, SchemaExceptionResponse.class)).thenThrow(httpResponseBodyParsingException);
        when(schemaService.getSchema(KIND)).thenThrow(dpsException);
        try {
            datasetRegistryService.createOrUpdateDatasetRegistry(Collections.singletonList(getRecord(RECORD_ID, KIND)));
        }
        catch (Exception exception) {
            assertTrue(exception instanceof AppException);
            assertEquals("Failed to parse error from Schema Service", exception.getMessage());
        }

        verify(storageFactory, times(1)).create(headers);
        verify(schemaFactory, times(1)).create(headers);
        verify(headers, times(1)).getPartitionId();
        verify(bodyMapper, times(1)).parseBody(httpResponse, SchemaExceptionResponse.class);
        verify(dpsException, times(1)).getHttpResponse();
    }

    @Test
    public void getDatasetRegistriesSuccess() throws DpsException {
        when(multiRecordInfo.getRecords()).thenReturn(Collections.singletonList(getRecord(RECORD_ID, KIND)));
        when(storageService.getRecords(eq(Collections.singletonList(RECORD_ID)))).thenReturn(multiRecordInfo);
        GetCreateUpdateDatasetRegistryResponse result = datasetRegistryService.getDatasetRegistries(Collections.singletonList(RECORD_ID));

        assertNotNull(result);
        assertEquals(1, result.getDatasetRegistries().size());
        assertEquals(RECORD_ID, result.getDatasetRegistries().get(0).getId());
        assertEquals(KIND, result.getDatasetRegistries().get(0).getKind());
        verify(storageFactory, times(1)).create(headers);
        verify(storageService, times(1)).getRecords(eq(Collections.singletonList(RECORD_ID)));
    }

    @Test
    public void getDatasetRegistriesAppException() throws DpsException, HttpResponseBodyParsingException {
        when(storageException.getHttpResponse()).thenReturn(httpResponse);
        when(storageExceptionResponse.getMessage()).thenReturn(exceptionMessage);
        when(storageExceptionResponse.getReason()).thenReturn(exceptionReason);
        when(bodyMapper.parseBody(httpResponse, StorageExceptionResponse.class)).thenReturn(storageExceptionResponse);
        when(storageService.getRecords(eq(Collections.singletonList(RECORD_ID)))).thenThrow(storageException);

        try {
            datasetRegistryService.getDatasetRegistries(Collections.singletonList(RECORD_ID));
        }

        catch (Exception exception) {
            assertTrue(exception instanceof AppException);
            assertEquals(exceptionMessage, exception.getMessage());
            assertEquals("Storage Service: " + exceptionReason, ((AppException) exception).getError().getReason());
        }

        verify(storageFactory, times(1)).create(headers);
        verify(storageService, times(1)).getRecords(eq(Collections.singletonList(RECORD_ID)));
    }

    @Test
    public void getDatasetRegistriesHttpResponseBodyParsingException() throws DpsException, HttpResponseBodyParsingException {
        when(storageException.getHttpResponse()).thenReturn(httpResponse);
        when(bodyMapper.parseBody(httpResponse, StorageExceptionResponse.class)).thenThrow(httpResponseBodyParsingException);
        when(storageService.getRecords(eq(Collections.singletonList(RECORD_ID)))).thenThrow(storageException);

        try {
            datasetRegistryService.getDatasetRegistries(Collections.singletonList(RECORD_ID));
        }

        catch (Exception exception) {
            assertTrue(exception instanceof AppException);
            assertEquals("Failed to parse error from Storage Service", exception.getMessage());
        }

        verify(storageFactory, times(1)).create(headers);
        verify(storageService, times(1)).getRecords(eq(Collections.singletonList(RECORD_ID)));
    }


    private Record getRecord(String recordId, String kind) {
        Record record = new Record();
        record.setId(recordId);
        record.setKind(kind);
        record.setAcl(new Acl(new String[]{ACL_VIEWER}, new String[]{ACL_OWNER}));

        return record;
    }

    private Map<String, DmsServiceProperties> getDmsServicePropertiesMap() {
        Map<String, DmsServiceProperties> data = new HashMap<>();
        data.put("dataset--file", new DmsServiceProperties(URL, true, API_KEY, true));
        return data;
    }

}
