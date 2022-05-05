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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.dms.model.RetrievalInstructionsResponse;
import org.opengroup.osdu.core.common.http.json.HttpResponseBodyMapper;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.storage.Record;
import org.opengroup.osdu.dataset.dms.DmsException;
import org.opengroup.osdu.dataset.dms.DmsServiceProperties;
import org.opengroup.osdu.dataset.dms.IDmsFactory;
import org.opengroup.osdu.dataset.dms.IDmsProvider;
import org.opengroup.osdu.dataset.model.request.GetDatasetRegistryRequest;
import org.opengroup.osdu.dataset.model.response.GetDatasetRetrievalInstructionsResponse;
import org.opengroup.osdu.dataset.model.response.GetDatasetStorageInstructionsResponse;
import org.opengroup.osdu.dataset.model.validation.DmsValidationDoc;
import org.opengroup.osdu.dataset.provider.interfaces.IDatasetDmsServiceMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import static org.opengroup.osdu.dataset.dms.util.ExceptionUtils.handleDmsException;

@Service
@RequiredArgsConstructor
public class DatasetDmsServiceImpl implements DatasetDmsService {

    @Inject
    private DpsHeaders headers;

    @Inject
    private IDmsFactory dmsFactory;

    @Inject
    private IDatasetDmsServiceMap dmsServiceMap;

    private final HttpResponseBodyMapper bodyMapper;

    @Override
    public GetDatasetStorageInstructionsResponse getStorageInstructions(String kindSubType) {

        Map<String, DmsServiceProperties> kindSubTypeToDmsServiceMap = dmsServiceMap.getResourceTypeToDmsServiceMap();

        DmsServiceProperties dmsServiceProperties = null;
        
        String kindSubTypeCatchAll = getKindSubTypeCatchAll(kindSubType);
        String dmsMapId = null;

        if (kindSubTypeToDmsServiceMap.containsKey(kindSubType)) {
            dmsMapId = kindSubType;
        }
        else if (kindSubTypeToDmsServiceMap.containsKey(kindSubTypeCatchAll)) {
            dmsMapId = kindSubTypeCatchAll;
        }
        
        dmsServiceProperties = kindSubTypeToDmsServiceMap.get(dmsMapId);

        if (dmsServiceProperties == null) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    String.format(DmsValidationDoc.RESOURCE_TYPE_NOT_REGISTERED_ERROR, kindSubType));
        }

        if (!dmsServiceProperties.isAllowStorage()) {
            HttpStatus status = HttpStatus.METHOD_NOT_ALLOWED;
            throw new AppException(status.value(), "DMS - Storage Not Supported",
                    String.format(DmsValidationDoc.DMS_STORAGE_NOT_SUPPORTED_ERROR, kindSubType));
        }

        GetDatasetStorageInstructionsResponse response = null;

        try {

            IDmsProvider dmsProvider = dmsFactory.create(headers, dmsServiceProperties);
            response = dmsProvider.getStorageInstructions();

        } catch (DmsException e) {
            handleDmsException(e);
        }

        return response;
    }

    /**
     * 1. Parse the KindSubType from Dataset Registry ID
     * 
     * 2. Check all dataset registries to see if they have registered resource type
     * dms handlers a. map datasets to DMS for use by DMS caller b. throw exception
     * if any unhandled
     * 
     * 3. Call each DMS one by one and get the delivery instructions a. group all
     * same type dms types b. merge all responses into a single delivery object
     */
    @Override
    public GetDatasetRetrievalInstructionsResponse getDatasetRetrievalInstructions(List<String> datasetRegistryIds) {

        Map<String, DmsServiceProperties> kindSubTypeToDmsServiceMap = dmsServiceMap.getResourceTypeToDmsServiceMap();
        HashMap<String, GetDatasetRegistryRequest> datasetRegistryRequestMap =
                segregateDatasetIdsToDms(datasetRegistryIds, kindSubTypeToDmsServiceMap);

        GetDatasetRetrievalInstructionsResponse mergedResponse = new GetDatasetRetrievalInstructionsResponse(new ArrayList<>());
          
        for (Map.Entry<String,GetDatasetRegistryRequest> datasetRegistryRequestEntry : datasetRegistryRequestMap.entrySet()) {
            try {
                IDmsProvider dmsProvider = dmsFactory.create(headers, kindSubTypeToDmsServiceMap.get(datasetRegistryRequestEntry.getKey()));
                GetDatasetRetrievalInstructionsResponse entryResponse = dmsProvider.getDatasetRetrievalInstructions(datasetRegistryRequestEntry.getValue());
                mergedResponse.getDelivery().addAll(entryResponse.getDelivery());
            }
            catch(DmsException e) {
                handleDmsException(e);
            }
        }
        return mergedResponse;
    }

    @Override
    public RetrievalInstructionsResponse getRetrievalInstructions(List<String> datasetRegistryIds) {
        Map<String, DmsServiceProperties> kindSubTypeToDmsServiceMap = dmsServiceMap.getResourceTypeToDmsServiceMap();
        HashMap<String, GetDatasetRegistryRequest> datasetRegistryRequestMap =
                segregateDatasetIdsToDms(datasetRegistryIds, kindSubTypeToDmsServiceMap);

        RetrievalInstructionsResponse response = new RetrievalInstructionsResponse();

        for (Map.Entry<String,GetDatasetRegistryRequest> datasetRegistryRequestEntry : datasetRegistryRequestMap.entrySet()) {
            try {
                IDmsProvider dmsProvider = dmsFactory.create(headers, kindSubTypeToDmsServiceMap.get(datasetRegistryRequestEntry.getKey()));
                RetrievalInstructionsResponse entryResponse = dmsProvider.getRetrievalInstructions(datasetRegistryRequestEntry.getValue());
                response.getDatasets().addAll(entryResponse.getDatasets());
                response.setProviderKey(entryResponse.getProviderKey());
            }
            catch(DmsException e) {
                handleDmsException(e);
            }
        }
        return response;
    }



    private HashMap<String, GetDatasetRegistryRequest> segregateDatasetIdsToDms(List<String> datasetRegistryIds, Map<String, DmsServiceProperties> kindSubTypeToDmsServiceMap) {
        HashMap<String, GetDatasetRegistryRequest> datasetRegistryRequestMap = new HashMap<>();

        for (String datasetRegistryId : datasetRegistryIds) {

            if (!Record.isRecordIdValidFormatAndTenant(datasetRegistryId, headers.getPartitionId())) {
                throw new AppException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), String.format("Dataset Registry: '%s' is an Invalid ID", datasetRegistryId), datasetRegistryId);
            }

            String kindSubType = getKindSubTypeFromID(datasetRegistryId);
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
                GetDatasetRegistryRequest request = new GetDatasetRegistryRequest();
                request.datasetRegistryIds = new ArrayList<String>();
                request.datasetRegistryIds.add(datasetRegistryId);
                datasetRegistryRequestMap.put(dmsMapId, request);
            }
            else {
                GetDatasetRegistryRequest request = datasetRegistryRequestMap.get(dmsMapId);
                request.datasetRegistryIds.add(datasetRegistryId);
            }
        }
        return datasetRegistryRequestMap;
    }

    private String getKindSubTypeFromID(String id) {
        String[] idSplitByColon = id.split(":");

        String kindSubType = idSplitByColon[1]; //grab GroupType/IndividualType

        return kindSubType;
    }

    private String getKindSubTypeCatchAll(String kindSubType) {
        String[] splitByPeriod = kindSubType.split("\\.");

        String kindSubTypeCatchAll = splitByPeriod[0] + ".*";

        return kindSubTypeCatchAll;
    }
}
