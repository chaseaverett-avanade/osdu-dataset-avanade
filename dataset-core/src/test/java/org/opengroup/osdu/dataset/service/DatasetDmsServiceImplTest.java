package org.opengroup.osdu.dataset.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.dms.model.RetrievalInstructionsResponse;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.dataset.dms.DmsException;
import org.opengroup.osdu.dataset.dms.DmsServiceProperties;
import org.opengroup.osdu.dataset.dms.IDmsFactory;
import org.opengroup.osdu.dataset.dms.IDmsProvider;
import org.opengroup.osdu.dataset.model.request.GetDatasetRegistryRequest;
import org.opengroup.osdu.dataset.model.response.DatasetRetrievalDeliveryItem;
import org.opengroup.osdu.dataset.model.response.GetDatasetRetrievalInstructionsResponse;
import org.opengroup.osdu.dataset.model.response.GetDatasetStorageInstructionsResponse;
import org.opengroup.osdu.dataset.provider.interfaces.IDatasetDmsServiceMap;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.when;


@RunWith(PowerMockRunner.class)
@PrepareForTest(DatasetDmsServiceImpl.class)
public class DatasetDmsServiceImplTest {


    @Mock
    private DpsHeaders headers;

    @Mock
    private IDmsFactory dmsFactory;

    @Mock
    private IDatasetDmsServiceMap dmsServiceMap;

    @Mock
    IDmsProvider dmsProvider;

    @Mock
    DmsServiceProperties dmsServiceProperties;

    @InjectMocks
    DatasetDmsServiceImpl datasetDmsService;

    private final String RECORD_ID = "opendes:dataset--file:data";
    private final String INVALID_RECORD_ID = "closedes:dataset--file:data";
    private final String DATA_PARTITION_ID = "opendes";
    private final String KIND = "dataset--file";
    private final String INVALID_KIND = "dataset---file";
    private final String KIND_TYPE_2 = "dataset--file.*";

    Map<String, DmsServiceProperties> kindSubTypeToDmsServiceMap;
    List<String> datasetRegistryIds;

    @Before
    public void setup() {
        initMocks(this);
        kindSubTypeToDmsServiceMap = new HashMap<>();
        kindSubTypeToDmsServiceMap.put(KIND, dmsServiceProperties);
        datasetDmsService = PowerMockito.spy(datasetDmsService);
        when(headers.getPartitionId()).thenReturn(DATA_PARTITION_ID);
    }

    @Test
    public void testGetStorageInstructionsIfSubTypePresent() throws DmsException {
        testGetStorageInstructions();
    }

    @Test
    public void testGetStorageInstructionsIfSubtypePresent() throws Exception {

        addKindType2InMap();
        testGetStorageInstructions();
        removeKindType2InMap();
    }


    @Test(expected = AppException.class)
    public void testGetStorageInstructionsIfDmsServiceIsNull() {
        when(dmsServiceMap.getResourceTypeToDmsServiceMap()).thenReturn(kindSubTypeToDmsServiceMap);
        GetDatasetStorageInstructionsResponse actualResponse = datasetDmsService.getStorageInstructions(INVALID_KIND);
    }

    @Test(expected = AppException.class)
    public void testGetStorageInstructionsIfAllowStorageIsNull() {
        when(dmsServiceMap.getResourceTypeToDmsServiceMap()).thenReturn(kindSubTypeToDmsServiceMap);
        when(dmsServiceProperties.isAllowStorage()).thenReturn(false);
        GetDatasetStorageInstructionsResponse actualResponse = datasetDmsService.getStorageInstructions(KIND);
    }

    @Test
    public void testGetDatasetRetrievalInstructions() throws Exception {

        datasetRegistryIds = Collections.singletonList(RECORD_ID);
        GetDatasetRetrievalInstructionsResponse expectedResponse = new GetDatasetRetrievalInstructionsResponse();
        List<DatasetRetrievalDeliveryItem> datasetRetrievalDeliveryItemList = new ArrayList<>();
        datasetRetrievalDeliveryItemList.add((new DatasetRetrievalDeliveryItem()));
        expectedResponse.setDelivery(datasetRetrievalDeliveryItemList);
        HashMap<String, GetDatasetRegistryRequest> datasetRegistryRequestMap = new HashMap<>();
        datasetRegistryRequestMap.put("dummyKey", new GetDatasetRegistryRequest());
        when(dmsServiceMap.getResourceTypeToDmsServiceMap()).thenReturn(kindSubTypeToDmsServiceMap);
        when(datasetDmsService, "segregateDatasetIdsToDms", datasetRegistryIds, kindSubTypeToDmsServiceMap).
                thenReturn(datasetRegistryRequestMap);

        GetDatasetRetrievalInstructionsResponse mergedResponse = new GetDatasetRetrievalInstructionsResponse(new ArrayList<>());
        Set<Map.Entry<String, GetDatasetRegistryRequest>> datasetRegistryRequestEntrySet = datasetRegistryRequestMap.entrySet();
        Map.Entry<String, GetDatasetRegistryRequest> datasetRegistryRequestEntry = datasetRegistryRequestEntrySet.iterator().next();
        when(dmsFactory.create(headers, kindSubTypeToDmsServiceMap.get(datasetRegistryRequestEntry.getKey()))).
                thenReturn(dmsProvider);
        GetDatasetRetrievalInstructionsResponse entryResponse = new GetDatasetRetrievalInstructionsResponse();
        entryResponse.setDelivery(datasetRetrievalDeliveryItemList);
        when(dmsProvider.getDatasetRetrievalInstructions(datasetRegistryRequestEntry.getValue())).thenReturn(entryResponse);
        GetDatasetRetrievalInstructionsResponse actualResponse = datasetDmsService.getDatasetRetrievalInstructions(datasetRegistryIds);
        assertEquals(expectedResponse.getDelivery().size(), actualResponse.getDelivery().size());

    }

    @Test
    public void testGetRetrievalInstructions() throws Exception {
        datasetRegistryIds = Collections.singletonList(RECORD_ID);
        testRetrievalInstructions();
    }

    @Test
    public void testGetRetrievalInstructionsWithKindType2() throws Exception {

        addKindType2InMap();
        datasetRegistryIds = Collections.singletonList(RECORD_ID);
        testRetrievalInstructions();
        removeKindType2InMap();

    }

    @Test(expected = AppException.class)
    public void testGetRetrievalInstructionsWithInvalidRecordID() throws Exception {

        datasetRegistryIds = Collections.singletonList(INVALID_RECORD_ID);
        testRetrievalInstructions();
    }

    private void addKindType2InMap() {
        kindSubTypeToDmsServiceMap.remove(KIND);
        kindSubTypeToDmsServiceMap.put(KIND_TYPE_2, dmsServiceProperties);
    }

    private void removeKindType2InMap() {
        kindSubTypeToDmsServiceMap.remove(KIND_TYPE_2);
    }

    private void testGetStorageInstructions() throws DmsException {

        when(dmsServiceMap.getResourceTypeToDmsServiceMap()).thenReturn(kindSubTypeToDmsServiceMap);
        when(dmsFactory.create(headers, dmsServiceProperties)).thenReturn(dmsProvider);
        GetDatasetStorageInstructionsResponse response = new GetDatasetStorageInstructionsResponse();
        when(dmsServiceProperties.isAllowStorage()).thenReturn(true);
        response.setProviderKey("dummyProvider");
        when(dmsProvider.getStorageInstructions()).thenReturn(response);
        GetDatasetStorageInstructionsResponse actualResponse = datasetDmsService.getStorageInstructions(KIND);
        GetDatasetStorageInstructionsResponse expectedResponse = new GetDatasetStorageInstructionsResponse();
        expectedResponse.setProviderKey("dummyProvider");
        assertEquals(actualResponse.getProviderKey(), expectedResponse.getProviderKey());
    }

    private void testRetrievalInstructions() throws Exception {

        RetrievalInstructionsResponse expectedResponse = new RetrievalInstructionsResponse();
        expectedResponse.setProviderKey("dummyKey");
        HashMap<String, GetDatasetRegistryRequest> datasetRegistryRequestMap = new HashMap<>();
        datasetRegistryRequestMap.put("key", new GetDatasetRegistryRequest());
        when(dmsServiceMap.getResourceTypeToDmsServiceMap()).thenReturn(kindSubTypeToDmsServiceMap);
        when(datasetDmsService, "segregateDatasetIdsToDms", datasetRegistryIds, kindSubTypeToDmsServiceMap).
                thenReturn(datasetRegistryRequestMap);
        RetrievalInstructionsResponse response = new RetrievalInstructionsResponse();

        Set<Map.Entry<String, GetDatasetRegistryRequest>> datasetRegistryRequestEntrySet = datasetRegistryRequestMap.entrySet();
        Map.Entry<String, GetDatasetRegistryRequest> datasetRegistryRequestEntry = datasetRegistryRequestEntrySet.iterator().next();
        when(dmsFactory.create(headers, kindSubTypeToDmsServiceMap.get(datasetRegistryRequestEntry.getKey()))).
                thenReturn(dmsProvider);
        RetrievalInstructionsResponse entryResponse = new RetrievalInstructionsResponse();
        entryResponse.setProviderKey("dummyKey");
        when(dmsProvider.getRetrievalInstructions(datasetRegistryRequestEntry.getValue())).thenReturn(entryResponse);
        RetrievalInstructionsResponse actualResponse = datasetDmsService.getRetrievalInstructions(datasetRegistryIds);
        assertEquals(expectedResponse.getProviderKey(), actualResponse.getProviderKey());

    }
}