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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.http.FetchServiceHttpRequest;
import org.opengroup.osdu.core.common.http.IUrlFetchService;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.HttpResponse;
import org.opengroup.osdu.delivery.DeliveryApplication;
import org.opengroup.osdu.delivery.model.DatasetRegistryAccessResponse;
import org.opengroup.osdu.delivery.model.SignedUrl;
import org.opengroup.osdu.delivery.model.UnsupportedAccessErrorCode;
import org.opengroup.osdu.delivery.provider.interfaces.IStorageService;
import org.opengroup.osdu.delivery.provider.interfaces.IUnsignedUrlLocationMapper;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest(classes={DeliveryApplication.class})
public class DatasetRegistryAccessServiceImplTest {

    @InjectMocks
    private DatasetRegistryAccessServiceImpl sut;

    @Mock
    private IUrlFetchService urlFetchService;

    @Mock
    private IStorageService storageService;

    @Mock
    private IUnsignedUrlLocationMapper unsignedUrlLocationMapper;

    @Mock
    private JaxRsDpsLog jaxRsDpsLog;

    @Mock
    private DpsHeaders headers;

    @Test
    public void testGetDataRegistryAccess() throws Exception {
        // Arrange
        String recordIdResourceTypeFound = "datapartition:doc:test-record-resource-type-found";
        String recordIdResourceTypeNotFound = "datapartition:doc:test-record-resource-type-not-found";
        String recordIdNoResourceTypeField = "datapartition:doc:test-record-no-resource-type-field";

        String normalResourceType = "srn:type:file/json:";
        String unknownResourceType = "srn:type:madeup/notfound:";

        String foundRecord = String.format(
                "{\n" +
                "    \"data\": {\n" +
                "        \"ResourceTypeID\": \"%s\",\n" +
                "        \"ResourceID\": \"srn:file/json:936621344819546083016:\",\n" +
                "        \"ResourceSecurityClassification\": \"srn:reference-data/ResourceSecurityClassification:RESTRICTED:\",\n" +
                "        \"Data.GroupTypeProperties.PreLoadFilePath\": \"s3://some_s3_object_path/manifest.json\",\n" +
                "        \"Data.GroupTypeProperties.FileSource\": \"\"\n" +
                "    },\n" +
                "    \"meta\": [],\n" +
                "    \"id\": \"%s\",\n" +
                "    \"version\": 1602269654224812,\n" +
                "    \"kind\": \"opendes:osdu:data-registry:0.0.1\",\n" +
                "    \"acl\": {\n" +
                "        \"viewers\": [\n" +
                "            \"data.default.viewers@opendes.testing.com\"\n" +
                "        ],\n" +
                "        \"owners\": [\n" +
                "            \"data.default.owners@opendes.testing.com\"\n" +
                "        ]\n" +
                "    },\n" +
                "    \"legal\": {\n" +
                "        \"legaltags\": [\n" +
                "            \"opendes-public-usa-dataset-1\"\n" +
                "        ],\n" +
                "        \"otherRelevantDataCountries\": [\n" +
                "            \"US\"\n" +
                "        ],\n" +
                "        \"status\": \"compliant\"\n" +
                "    },\n" +
                "    \"createUser\": \"admin@testing.com\",\n" +
                "    \"createTime\": \"2020-10-09T18:54:14.541Z\"\n" +
                "},\n",
                normalResourceType, recordIdResourceTypeFound
        );

        String notFoundRecord = String.format(
                "{\n" +
                        "    \"data\": {\n" +
                        "        \"ResourceTypeID\": \"%s\",\n" +
                        "        \"ResourceID\": \"srn:file/json:936621344819546083016:\",\n" +
                        "        \"ResourceSecurityClassification\": \"srn:reference-data/ResourceSecurityClassification:RESTRICTED:\",\n" +
                        "        \"Data.GroupTypeProperties.PreLoadFilePath\": \"s3://some_s3_object_path/manifest.json\",\n" +
                        "        \"Data.GroupTypeProperties.FileSource\": \"\"\n" +
                        "    },\n" +
                        "    \"meta\": [],\n" +
                        "    \"id\": \"%s\",\n" +
                        "    \"version\": 1602269654224812,\n" +
                        "    \"kind\": \"opendes:osdu:data-registry:0.0.1\",\n" +
                        "    \"acl\": {\n" +
                        "        \"viewers\": [\n" +
                        "            \"data.default.viewers@opendes.testing.com\"\n" +
                        "        ],\n" +
                        "        \"owners\": [\n" +
                        "            \"data.default.owners@opendes.testing.com\"\n" +
                        "        ]\n" +
                        "    },\n" +
                        "    \"legal\": {\n" +
                        "        \"legaltags\": [\n" +
                        "            \"opendes-public-usa-dataset-1\"\n" +
                        "        ],\n" +
                        "        \"otherRelevantDataCountries\": [\n" +
                        "            \"US\"\n" +
                        "        ],\n" +
                        "        \"status\": \"compliant\"\n" +
                        "    },\n" +
                        "    \"createUser\": \"admin@testing.com\",\n" +
                        "    \"createTime\": \"2020-10-09T18:54:14.541Z\"\n" +
                        "},\n",
                unknownResourceType, recordIdResourceTypeNotFound
        );

        String noResourceTypeFieldRecord = String.format(
                "{\n" +
                        "    \"data\": {\n" +
                        "        \"ResourceID\": \"srn:file/json:936621344819546083016:\",\n" +
                        "        \"ResourceSecurityClassification\": \"srn:reference-data/ResourceSecurityClassification:RESTRICTED:\",\n" +
                        "        \"Data.GroupTypeProperties.PreLoadFilePath\": \"s3://some_s3_object_path/manifest.json\",\n" +
                        "        \"Data.GroupTypeProperties.FileSource\": \"\"\n" +
                        "    },\n" +
                        "    \"meta\": [],\n" +
                        "    \"id\": \"%s\",\n" +
                        "    \"version\": 1602269654224812,\n" +
                        "    \"kind\": \"opendes:osdu:data-registry:0.0.1\",\n" +
                        "    \"acl\": {\n" +
                        "        \"viewers\": [\n" +
                        "            \"data.default.viewers@opendes.testing.com\"\n" +
                        "        ],\n" +
                        "        \"owners\": [\n" +
                        "            \"data.default.owners@opendes.testing.com\"\n" +
                        "        ]\n" +
                        "    },\n" +
                        "    \"legal\": {\n" +
                        "        \"legaltags\": [\n" +
                        "            \"opendes-public-usa-dataset-1\"\n" +
                        "        ],\n" +
                        "        \"otherRelevantDataCountries\": [\n" +
                        "            \"US\"\n" +
                        "        ],\n" +
                        "        \"status\": \"compliant\"\n" +
                        "    },\n" +
                        "    \"createUser\": \"admin@testing.com\",\n" +
                        "    \"createTime\": \"2020-10-09T18:54:14.541Z\"\n" +
                        "}\n",
                recordIdNoResourceTypeField
        );

        List<String> dataRegistryRecordIds = new ArrayList<>();
        dataRegistryRecordIds.add(recordIdResourceTypeFound);
        dataRegistryRecordIds.add(recordIdResourceTypeNotFound);
        dataRegistryRecordIds.add(recordIdNoResourceTypeField);

        String testBody = "{\n" +
                "    \"records\": [\n" +
                foundRecord +
                notFoundRecord +
                noResourceTypeFieldRecord +
                "    ],\n" +
                "    \"notFound\": [],\n" +
                "    \"conversionStatuses\": []\n" +
                "}";
        HttpResponse storageResponse = new HttpResponse();
        storageResponse.setBody(testBody);
        Mockito.when(urlFetchService.sendRequest(Mockito.any(FetchServiceHttpRequest.class)))
                .thenReturn(storageResponse);

        SignedUrl signedUrl = new SignedUrl();
        signedUrl.setConnectionString("test-connection-string");
        Mockito.doReturn(signedUrl).when(storageService)
                .createSignedUrl(Mockito.any(), Mockito.any(), Mockito.any());

        // Act
        sut.init();
        DatasetRegistryAccessResponse response = sut.getDataRegistryAccess(dataRegistryRecordIds);

        // Assert
        Assert.assertEquals(1, response.getDataRegistryAccessItems().size());
        Assert.assertEquals(2, response.getUnsupportedDataRegistryAccessItems().size());
        Assert.assertEquals(recordIdResourceTypeFound, response.getDataRegistryAccessItems().get(0).getDataRegistryRecordId());
        Assert.assertEquals(recordIdResourceTypeNotFound, response.getUnsupportedDataRegistryAccessItems().get(0).getDataRegistryRecordId());
        Assert.assertEquals(UnsupportedAccessErrorCode.RESOURCE_TYPE_HANDLER_NOT_FOUND, response.getUnsupportedDataRegistryAccessItems().get(0).getCode());
        Assert.assertEquals(recordIdNoResourceTypeField, response.getUnsupportedDataRegistryAccessItems().get(1).getDataRegistryRecordId());
        Assert.assertEquals(UnsupportedAccessErrorCode.RESOURCE_TYPE_FIELD_NOT_FOUND, response.getUnsupportedDataRegistryAccessItems().get(1).getCode());
        Assert.assertNotNull(response.getUnsupportedDataRegistryAccessItems().get(0).getRecord());
        Assert.assertNotNull(response.getUnsupportedDataRegistryAccessItems().get(1).getRecord());
        Assert.assertEquals(String.format("Unsupported data registry resource type: %s", unknownResourceType),
                response.getUnsupportedDataRegistryAccessItems().get(0).getMessage());
        Assert.assertEquals(String.format("Could not find field ResourceTypeId in record's data field", unknownResourceType),
                response.getUnsupportedDataRegistryAccessItems().get(1).getMessage());
    }

}
