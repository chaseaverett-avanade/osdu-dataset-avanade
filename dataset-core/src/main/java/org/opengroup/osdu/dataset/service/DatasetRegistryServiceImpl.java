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

package org.opengroup.osdu.dataset.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.dms.model.CopyDmsRequest;
import org.opengroup.osdu.core.common.dms.model.CopyDmsResponse;
import org.opengroup.osdu.core.common.http.json.HttpResponseBodyMapper;
import org.opengroup.osdu.core.common.http.json.HttpResponseBodyParsingException;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.storage.MultiRecordInfo;
import org.opengroup.osdu.core.common.model.storage.Record;
import org.opengroup.osdu.core.common.model.storage.StorageException;
import org.opengroup.osdu.core.common.model.storage.UpsertRecords;
import org.opengroup.osdu.core.common.storage.IStorageFactory;
import org.opengroup.osdu.core.common.storage.IStorageService;
import org.opengroup.osdu.dataset.dms.DmsException;
import org.opengroup.osdu.dataset.dms.DmsServiceProperties;
import org.opengroup.osdu.dataset.dms.IDmsFactory;
import org.opengroup.osdu.dataset.dms.IDmsProvider;
import org.opengroup.osdu.dataset.model.request.SchemaExceptionResponse;
import org.opengroup.osdu.dataset.model.request.SchemaExceptionResponseBody;
import org.opengroup.osdu.dataset.model.request.StorageExceptionResponse;
import org.opengroup.osdu.dataset.model.response.GetCreateUpdateDatasetRegistryResponse;
import org.opengroup.osdu.dataset.model.validation.DmsValidationDoc;
import org.opengroup.osdu.dataset.provider.interfaces.IDatasetDmsServiceMap;
import org.opengroup.osdu.dataset.schema.ISchemaFactory;
import org.opengroup.osdu.dataset.schema.ISchemaService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import static org.opengroup.osdu.dataset.util.ExceptionUtils.handleDmsException;

@Service
@RequiredArgsConstructor
public class DatasetRegistryServiceImpl implements DatasetRegistryService {

    /**
     * Dataset Kind Regex is tied to the R3 Data Definitions official REGEX format,
     * but with the stricter enforcement that the groupType is a dataset.
     * example: dataset--File.Generic
     * Official Dataset Kinds: https://community.opengroup.org/osdu/data/data-definitions/-/tree/master/E-R/dataset
     * Regex defined per ADR: https://community.opengroup.org/osdu/platform/system/storage/-/issues/26
     * 
     */
    final String DATASET_KIND_REGEX = "^[\\w\\-\\.]+:[\\w\\-\\.]+:dataset--+[\\w\\-\\.]+:[0-9]+.[0-9]+.[0-9]+$";

    private HttpResponseBodyMapper bodyMapper;

    @Inject
    private DpsHeaders headers;

    @Inject
    IStorageFactory storageFactory;

    @Inject
    ISchemaFactory schemaFactory;

    @Inject
    private IDmsFactory dmsFactory;

    @Inject
    private IDatasetDmsServiceMap dmsServiceMap;

    Pattern datasetKindPattern = Pattern.compile(DATASET_KIND_REGEX);

    @Override
    public void deleteDatasetRegistry(String datasetRegistryId) {

        IStorageService storageService = this.storageFactory.create(headers);
        try {
            storageService.deleteRecord(datasetRegistryId);
        } catch (StorageException e) {
            try {
                StorageExceptionResponse body = bodyMapper.parseBody(e.getHttpResponse(), StorageExceptionResponse.class);
                throw new AppException(body.getCode(), "Storage Service: " + body.getReason(), body.getMessage());
            } catch (HttpResponseBodyParsingException e1) {
                throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                        "Failed to parse error from Storage Service");
            }
        }
    }

    @Override
    public GetCreateUpdateDatasetRegistryResponse createOrUpdateDatasetRegistry(List<Record> datasetRegistries) {

        IStorageService storageService = this.storageFactory.create(headers);
        ISchemaService schemaService = this.schemaFactory.create(headers);

        this.validateDatasets(schemaService, datasetRegistries);

        Map<String, DmsServiceProperties> kindSubTypeToDmsServiceMap = dmsServiceMap.getResourceTypeToDmsServiceMap();
        Map<String, CopyDmsRequest> datasetRegistryRequestMap = this.segregateDatasetsBasedOnKind(datasetRegistries);

        this.copyDmsToPersistentStorage(datasetRegistryRequestMap, kindSubTypeToDmsServiceMap);

        UpsertRecords storageResponse = null;
        try {
            storageResponse = storageService.upsertRecord((datasetRegistries.toArray(new Record[0])));
        } catch (StorageException e) {
            try {
                StorageExceptionResponse body = bodyMapper.parseBody(e.getHttpResponse(), StorageExceptionResponse.class);
                throw new AppException(body.getCode(), "Storage Service: " + body.getReason(), body.getMessage());
            } catch (HttpResponseBodyParsingException e2) {
                throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Failed to parse error from Storage Service");
            }

        }

        List<String> recordIds = storageResponse.getRecordIds();

        MultiRecordInfo getRecordsResponse = null;

        try {
            getRecordsResponse = storageService.getRecords(recordIds);
        } catch (StorageException e) {
            try {
                StorageExceptionResponse body = bodyMapper.parseBody(e.getHttpResponse(), StorageExceptionResponse.class);
                throw new AppException(body.getCode(), "Storage Service: " + body.getReason(), body.getMessage());
            } catch (HttpResponseBodyParsingException e1) {
                throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                        "Failed to parse error from Storage Service");
            }
        }

        GetCreateUpdateDatasetRegistryResponse response = new GetCreateUpdateDatasetRegistryResponse(
                getRecordsResponse.getRecords());

        return response;
    }

    public GetCreateUpdateDatasetRegistryResponse getDatasetRegistries(List<String> datasetRegistryIds) {

        MultiRecordInfo getRecordsResponse = null;

        try {

            IStorageService storageService = this.storageFactory.create(headers);

            getRecordsResponse = storageService.getRecords(datasetRegistryIds);

        } catch (StorageException e) {

            try {
                StorageExceptionResponse body = bodyMapper.parseBody(e.getHttpResponse(), StorageExceptionResponse.class);
                throw new AppException(body.getCode(), "Storage Service: " + body.getReason(), body.getMessage());
            } catch (HttpResponseBodyParsingException e1) {
                throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                        "Failed to parse error from Storage Service");
            }
        }

        GetCreateUpdateDatasetRegistryResponse response = new GetCreateUpdateDatasetRegistryResponse(
                getRecordsResponse.getRecords());

        return response;
    }

    //this should be in os-core-common, but placing here until it's able to be put inside the Record class
    private boolean isOsduRecordIdValid(String recordId, String tenant, String kind) {

            //Check format and tenant
            if (!Record.isRecordIdValid(recordId, tenant, kind))
                return false;

            //id should be split by colons. ex: tenant:groupType--individualType:uniqueId
            String[] recordIdSplitByColon = recordId.split(":");

            //make sure groupType/individualType is correct
            String[] kindSplitByColon = kind.split(":");
            String kindSubType = kindSplitByColon[2]; //grab GroupType/IndividualType

            if (!recordIdSplitByColon[1].equalsIgnoreCase(kindSubType))
                return false;

            return true;
    }

    private boolean validateDatasets(ISchemaService schemaService, List<Record> datasets) {

        HashMap<String, Object> schemaKindsCache = new HashMap<>();

        for (Record dataset : datasets) {
            String datasetKind = dataset.getKind();

            if (dataset.getId() != null && !isOsduRecordIdValid(dataset.getId(), headers.getPartitionId(), datasetKind)) {
                String msg = String.format(
							"The record '%s' does not have a valid ID",	dataset.getId());
					throw new AppException(HttpStatus.BAD_REQUEST.value(), "Invalid record id", msg);
            }

            if (!this.validateKindIsValidAndGroupTypeIsDataset(datasetKind)) {
                throw new AppException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        "One or more records has an invalid Kind. Must use 'dataset' group type");
            }

            Object schema = schemaKindsCache.get(datasetKind);

            if (schema == null) { //schema was not found in cache
                try {
                    schema = schemaService.getSchema(datasetKind); //make sure schema is valid and store it
                    schemaKindsCache.put(datasetKind, schema);
                } catch (DpsException e) {
                    try {
                        SchemaExceptionResponse ser = bodyMapper.parseBody(e.getHttpResponse(), SchemaExceptionResponse.class);
                        SchemaExceptionResponseBody body = ser.getError();
                        throw new AppException(body.getCode(), String.format("Schema Service: get '%s'", datasetKind), body.getMessage());
                    } catch (HttpResponseBodyParsingException | NullPointerException e1) {
                        throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                                "Failed to parse error from Schema Service", e1);
                    }
                }
            }           


            /* TODO: The R3 schema object is not yet available, 
             * it will be hard to validate the properties without it, 
             * so skipping further validation for now            
            */

            //validate record against schema
            //this.validateDatasetRegistrySchema(dataset, datasetRegistrySchema);
            
            //validate dataset properties field exists
            // if (!dataset.getData().containsKey(DATASET_REGISTRY_DATASET_PROPERTIES_NAME))
            //     throw new AppException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), DatasetRegistryValidationDoc.MISSING_DATASET_PROPERTIES_VALIDATION);
        }

        return true;

    }

    private boolean validateKindIsValidAndGroupTypeIsDataset(String kind) {
        Matcher matcher = datasetKindPattern.matcher(kind);
        boolean matchFound = matcher.find();
        return matchFound;
    }

    private String getKindSubtype(String kind) {
        // TODO: change this to regex.
        String[] kindSplitByColon = kind.split(":");
        String kindSubType = kindSplitByColon[2]; //grab GroupType/IndividualType
        return kindSubType;
    }

    private String getKindSubTypeCatchAll(String kindSubType) {
        String[] splitByPeriod = kindSubType.split("\\.");

        String kindSubTypeCatchAll = splitByPeriod[0] + ".*";

        return kindSubTypeCatchAll;
    }

    public void copyDmsToPersistentStorage(Map<String, CopyDmsRequest> datasetRegistryRequestMap,
                    Map<String, DmsServiceProperties> kindSubTypeToDmsServiceMap) {
        for (Map.Entry<String, CopyDmsRequest> datasetRegistryRequestEntry : datasetRegistryRequestMap.entrySet()) {
            try {
                DmsServiceProperties dmsServiceProperties = kindSubTypeToDmsServiceMap.get(datasetRegistryRequestEntry.getKey());
                if (dmsServiceProperties.isStagingLocationSupported()) {
                    IDmsProvider dmsProvider = dmsFactory.create(headers, dmsServiceProperties);
                    List<CopyDmsResponse> entryResponse = dmsProvider.copyDmsToPersistentStorage(datasetRegistryRequestEntry.getValue());
                    for (CopyDmsResponse response: entryResponse) {
                        if (!response.isSuccess()) {
                            throw new AppException(400, "Bad Request", "Invalid dataset metadata");
                        }
                    }
                }
            }
            catch(DmsException e) {
                handleDmsException(e);
            }
        }
    }

    private Map<String, CopyDmsRequest> segregateDatasetsBasedOnKind(List<Record> datasets) {
        Map<String, CopyDmsRequest> datasetRegistryRequestMap = new HashMap<>();
        Map<String, DmsServiceProperties> kindSubTypeToDmsServiceMap = dmsServiceMap.getResourceTypeToDmsServiceMap();

        for (Record datasetRegistryRecord : datasets) {
            String kindSubType = getKindSubtype(datasetRegistryRecord.getKind());
            String kindSubTypeCatchAll = getKindSubTypeCatchAll(kindSubType);
            String dmsMapId = null;

            if (kindSubTypeToDmsServiceMap.containsKey(kindSubType)) {
                dmsMapId = kindSubType;
            }
            else if (kindSubTypeToDmsServiceMap.containsKey(kindSubTypeCatchAll)) {
                dmsMapId = kindSubTypeCatchAll;
            }
            else {
                throw new AppException(HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        String.format(DmsValidationDoc.KIND_SUB_TYPE_NOT_REGISTERED_ERROR, kindSubType));
            }

            if (!datasetRegistryRequestMap.containsKey(dmsMapId)) {
                CopyDmsRequest request = new CopyDmsRequest();
                request.getDatasetSources().add(datasetRegistryRecord);
                datasetRegistryRequestMap.put(dmsMapId, request);
            }
            else {
                CopyDmsRequest request = datasetRegistryRequestMap.get(dmsMapId);
                request.getDatasetSources().add(datasetRegistryRecord);
            }
        }
        return datasetRegistryRequestMap;
    }
}