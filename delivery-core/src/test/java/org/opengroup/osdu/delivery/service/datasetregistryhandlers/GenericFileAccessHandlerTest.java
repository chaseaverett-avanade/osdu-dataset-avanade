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

package org.opengroup.osdu.delivery.service.datasetregistryhandlers;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.delivery.DeliveryApplication;
import org.opengroup.osdu.delivery.model.Records;
import org.opengroup.osdu.delivery.model.SignedUrl;
import org.opengroup.osdu.delivery.provider.interfaces.IStorageService;
import org.opengroup.osdu.delivery.provider.interfaces.IUnsignedUrlLocationMapper;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest(classes={DeliveryApplication.class})
public class GenericFileAccessHandlerTest {

    @InjectMocks
    private GenericFileAccessHandler sut;

    @Mock
    private IStorageService storageService;

    @Mock
    private IUnsignedUrlLocationMapper unsignedUrlLocationMapper;

    @Mock
    private DpsHeaders headers;

    @Test
    public void testGetGenericFileAccess() throws DatasetRegistryAccessException {
        // Arrange
        Map<String, Object> recordData = new HashMap<>();
        recordData.put("test", "testing");
        String testPath = "some-unsigned-path";
        String testRecordId = "test-record-id";
        recordData.put("Data.GroupTypeProperties.PreLoadFilePath", testPath);
        SignedUrl signedUrl = new SignedUrl();
        signedUrl.setConnectionString("test-signed-url");

        Mockito.when(unsignedUrlLocationMapper.getUnsignedURLFromRecordData(Mockito.eq(recordData)))
                .thenReturn(testPath);

        Mockito.doReturn(signedUrl).when(storageService)
                .createSignedUrl(Mockito.eq(testRecordId), Mockito.eq(testPath), Mockito.anyObject());

        Records.Entity recordsEntity = new Records.Entity();
        recordsEntity.setData(recordData);
        recordsEntity.setId(testRecordId);

        // Act
        Object delivery = sut.getAccess(recordsEntity);

        SignedUrl respSignedUrl = (SignedUrl) delivery;

        // Assert
        Assert.assertEquals(signedUrl.getConnectionString(), respSignedUrl.getConnectionString());
    }
}
