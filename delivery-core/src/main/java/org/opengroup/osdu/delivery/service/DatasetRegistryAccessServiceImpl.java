// Copyright © 2020 Amazon Web Services
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

package org.opengroup.osdu.delivery.service;

import com.google.api.client.http.HttpMethods;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.http.FetchServiceHttpRequest;
import org.opengroup.osdu.core.common.http.IUrlFetchService;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.HttpResponse;
import org.opengroup.osdu.core.common.model.storage.RecordIds;
import org.opengroup.osdu.delivery.model.*;
import org.opengroup.osdu.delivery.provider.interfaces.IStorageService;
import org.opengroup.osdu.delivery.provider.interfaces.IUnsignedUrlLocationMapper;
import org.opengroup.osdu.delivery.service.datasetregistryhandlers.DatasetRegistryAccessException;
import org.opengroup.osdu.delivery.service.datasetregistryhandlers.GenericFileAccessHandler;
import org.opengroup.osdu.delivery.service.datasetregistryhandlers.IDatasetRegistryAccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class DatasetRegistryAccessServiceImpl implements IDatasetRegistryAccessService {
    private final Gson gson = new Gson();

    private final static String RESOURCE_TYPE_ID_NAME = "ResourceTypeID";

    @Inject
    final DpsHeaders headers;
    final IUrlFetchService urlFetchService;
    final IStorageService storageService;
    final IUnsignedUrlLocationMapper unsignedUrlLocationMapper;
    final JaxRsDpsLog jaxRsDpsLog;

    @Value("${STORAGE_QUERY_RECORD_HOST}")
    private String STORAGE_QUERY_RECORD_HOST;

    private Map<String, IDatasetRegistryAccessHandler> supportedResourceTypes;

    @PostConstruct
    /**
     * Initializes a map field that maps resource types to their accompanying handler class.
     * Using this approach to avoid a very long if/else statement chaining all the different resource types
     */
    public void init() {
        GenericFileAccessHandler genericFileAccessHandler = new GenericFileAccessHandler(storageService, unsignedUrlLocationMapper, headers);

        supportedResourceTypes = new HashMap<String, IDatasetRegistryAccessHandler>(){{
            put("srn:type:file/json:", genericFileAccessHandler);
            put("srn:type:file/csv:", genericFileAccessHandler);
            put("srn:type:file/xml:", genericFileAccessHandler);
            put("srn:type:file/txt:", genericFileAccessHandler);
            put("srn:type:file/pdf:", genericFileAccessHandler);
            put("srn:type:file/dat:", genericFileAccessHandler);
            put("srn:type:file/las:", genericFileAccessHandler);
        }};
    }

    /**
     *  Takes a list of record ids, retrieves those records from storage service, looks up
     *  the resource type on each record and then executes that record's accompanying access handler
     * @param recordIds
     * @return
     */
    @Override
    public DatasetRegistryAccessResponse getDataRegistryAccess(List<String> recordIds) {
        Records dataRegistryRecords = getRecordsFromStorage(recordIds);
        return executeAccessGeneration(dataRegistryRecords.getRecords());
    }

    /**
     * Iterates through all the data registry records, looks up the resource type, tries to find a matching
     * handler for access generation and then executes those handlers
     * @param dataRegistryRecords
     * @return
     */
    private DatasetRegistryAccessResponse executeAccessGeneration(List<Records.Entity> dataRegistryRecords){
        List<DatasetRegistryAccessResponseItem> datasetRegistryAccessResponseItems = new ArrayList<>();
        List<UnsupportedAccessResponseItem> unsupportedAccessResponseItems = new ArrayList<>();
        List<DatasetRegistryAccessResponseItem> failedDatasetRegistryAccessResponseItems = new ArrayList<>();

        for(Records.Entity dataRegistryRecord : dataRegistryRecords){
            jaxRsDpsLog.info(String.format("Attempting to get access for dataset registry record: %s", dataRegistryRecord.getId()));

            String resourceType = getResourceType(dataRegistryRecord);
            IDatasetRegistryAccessHandler handler = supportedResourceTypes.get(resourceType);

            // no resourcetypeid was found on record
            if(resourceType == null){
                jaxRsDpsLog.warning(String.format("Could not find resource type id field for data registry record: %s", dataRegistryRecord.getId()));
                unsupportedAccessResponseItems.add(
                        createUnsupportedAccessResponseItem(dataRegistryRecord)
                );
            }
            // no handler found for resource type
            else if(handler == null){
                jaxRsDpsLog.warning(String.format("Could not find access handler for data registry record: %s", dataRegistryRecord.getId()));
                unsupportedAccessResponseItems.add(
                        createUnsupportedAccessResponseItem(dataRegistryRecord, resourceType)
                );
            }
            // resourcetypeid field found and handler found for resourcetype
            else {
                try {
                    DatasetRegistryAccessResponseItem datasetRegistryAccessResponseItem = createDataRegistryAccessResponseItem(dataRegistryRecord, handler);
                    datasetRegistryAccessResponseItems.add(datasetRegistryAccessResponseItem);
                } catch (DatasetRegistryAccessException e){
                    jaxRsDpsLog.error("Failed to get access for data registry record: %s. With exception: %s", dataRegistryRecord.getId(), e);
                    failedDatasetRegistryAccessResponseItems.add(createFailedDataRegistryAccessResponseItem(dataRegistryRecord));
                }
            }
        }

        return new DatasetRegistryAccessResponse(datasetRegistryAccessResponseItems, unsupportedAccessResponseItems, failedDatasetRegistryAccessResponseItems);
    }

    /**
     * Creates an item in the response to the user to indicate that the data registry record needs the ResourceTypeId field.
     * @param dataRegistryRecord
     * @return
     */
    private UnsupportedAccessResponseItem createUnsupportedAccessResponseItem(Records.Entity dataRegistryRecord){
        String message = String.format("Could not find field ResourceTypeId in record's data field");
        return new UnsupportedAccessResponseItem(dataRegistryRecord.getId(), message, UnsupportedAccessErrorCode.RESOURCE_TYPE_FIELD_NOT_FOUND, dataRegistryRecord);
    }

    /**
     * Creates an item in the response to the user to indicate that the resource type was found but no handler to generate
     * access was found for it.
     * @param dataRegistryRecord
     * @param resourceType
     * @return
     */
    private UnsupportedAccessResponseItem createUnsupportedAccessResponseItem(Records.Entity dataRegistryRecord, String resourceType){
        String message = String.format("Unsupported data registry resource type: %s", resourceType);
        return new UnsupportedAccessResponseItem(dataRegistryRecord.getId(), message, UnsupportedAccessErrorCode.RESOURCE_TYPE_HANDLER_NOT_FOUND, dataRegistryRecord);
    }

    /**
     * Executes the handler accompanying a record's resource type i.e. a generic file access for a csv data registry record and so on.
     * Creates an item in the response to hold that access object that gets generated.
     * @param dataRegistryRecord
     * @param handler
     * @return
     */
    private DatasetRegistryAccessResponseItem createDataRegistryAccessResponseItem(Records.Entity dataRegistryRecord,
                                                                                   IDatasetRegistryAccessHandler handler) throws DatasetRegistryAccessException {
        IDeliveryData delivery = handler.getAccess(dataRegistryRecord);
        return new DatasetRegistryAccessResponseItem(dataRegistryRecord.getId(), delivery, dataRegistryRecord);
    }

    private DatasetRegistryAccessResponseItem createFailedDataRegistryAccessResponseItem(Records.Entity dataRegistryRecord){
        return new DatasetRegistryAccessResponseItem(dataRegistryRecord.getId(), null, dataRegistryRecord);
    }

    /**
     * Looks up the resource type id on a record's data
     * @param dataRegistryRecord
     * @return
     */
    private String getResourceType(Records.Entity dataRegistryRecord){
        return (String) dataRegistryRecord.getData().get(RESOURCE_TYPE_ID_NAME);
    }

    /**
     * Queries storage service query batch api and returns the full record objects
     * @param recordIds
     * @return
     */
    private Records getRecordsFromStorage(List<String> recordIds){
        jaxRsDpsLog.info(String.format("Attempting to get records from storage for dataset access generation: %s", recordIds.toString()));

        String body = this.gson.toJson(RecordIds.builder().records(recordIds).build());

        FetchServiceHttpRequest request = FetchServiceHttpRequest
                .builder()
                .httpMethod(HttpMethods.POST)
                .url(STORAGE_QUERY_RECORD_HOST)
                .headers(headers)
                .body(body)
                .build();

        String dataFromStorage = "";
        try {
            HttpResponse response = this.urlFetchService.sendRequest(request);
            dataFromStorage = response.getBody();
            if (dataFromStorage == null || dataFromStorage == "") {
                throw new AppException(HttpStatus.SC_NOT_FOUND, "Invalid request", "Storage service returned empty response");
            }
        }
        catch (URISyntaxException e) {
            throw new AppException(HttpStatus.SC_BAD_REQUEST, "Malformed request to storage", "There was an error connecting to storage service.", e);
        }

        Type recordsListType = new TypeToken<Records>() {}.getType();
        Records records = this.gson.fromJson(dataFromStorage, recordsListType);
        return records;
    }
}
