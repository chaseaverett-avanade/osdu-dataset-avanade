/**
 * Copyright 2020 IBM Corp. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author alanbraz@br.ibm.com
 *
 */

package org.opengroup.osdu.delivery.provider.ibm.service.test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.util.Date;

import javax.inject.Inject;

import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.delivery.DeliveryApplication;
import org.opengroup.osdu.delivery.model.SignedUrl;
import org.opengroup.osdu.delivery.provider.ibm.service.ExpirationDateHelper;
import org.opengroup.osdu.delivery.provider.ibm.service.InstantHelper;
import org.opengroup.osdu.delivery.provider.ibm.service.StorageServiceImpl;
import org.springframework.boot.test.context.SpringBootTest;

import com.ibm.cloud.objectstorage.SdkClientException;
import com.ibm.cloud.objectstorage.services.s3.AmazonS3;
import com.amazonaws.services.securitytoken.model.Credentials;

import org.opengroup.osdu.core.common.model.entitlements.EntitlementsException;
import org.opengroup.osdu.core.common.model.entitlements.Groups;
import com.ibm.cloud.objectstorage.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest(classes={DeliveryApplication.class})
public class StorageServiceImplTest {

    @InjectMocks
    private StorageServiceImpl CUT = new StorageServiceImpl();

    @Mock
    private AmazonS3 s3Client;

    @Mock
    private ExpirationDateHelper expirationDateHelper;

    @Mock
    private InstantHelper instantHelper;
    
    @Mock
	private DpsHeaders headers;
    
   
    @Mock
    private Credentials creds;
    
    private Groups grp;
    
    @Mock
    private AWSSecurityTokenServiceClient sts;       
    
    
    private String bucketName = "osdu-ibm-storage-records";
    private String key = "object.csv";
    private String unsignedUrl = "s3://" + bucketName + "/" + key;
    private String authorizationToken = "123";

    @Test
    public void createSignedUrl() throws IOException, URISyntaxException, EntitlementsException {
        // Arrange
        Date testDate = new Date();
        Mockito.when(expirationDateHelper.getExpirationDate(Mockito.anyInt())).thenReturn(testDate);
        String srn="srn:file:-965274437";

        URL url = new URL("http://testsignedurl.com");
        Mockito.when(s3Client.generatePresignedUrl(Mockito.any(GeneratePresignedUrlRequest.class))).thenReturn(url);

        Instant instant = Instant.now();
        Mockito.when(instantHelper.getCurrentInstant()).thenReturn(instant);
        
        Mockito.when(headers.getPartitionId()).thenReturn("opendes");
       
       // Set up test credentials
       CUT.setTestConnectionString("connectionstring");

        SignedUrl expected = new SignedUrl();
        expected.setUri(new URI(url.toString()));
        expected.setUrl(url);
        expected.setConnectionString("connectionstring");
        expected.setCreatedAt(instant);

        // Act
        SignedUrl actual = CUT.createSignedUrl(srn,unsignedUrl, authorizationToken);

        // Assert
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void createSignedUrl_malformedUnsignedUrl_throwsAppException() {
        try {
            // Arrange
            String unsignedUrl = "malformedUrlString";
            String authorizationToken = "testAuthorizationToken";
            String srn="srn:file:-965274437";

            // Act
            CUT.createSignedUrl(srn, unsignedUrl, authorizationToken);

            // Assert
            fail("Should not succeed!");
        } catch (AppException e) {
            // Assert
            assertEquals(HttpStatus.SC_BAD_REQUEST, e.getError().getCode());
            assertEquals("Malformed URL", e.getError().getReason());
            assertEquals( "Unsigned url invalid, needs to be full S3 path", e.getError().getMessage());
        } catch (Exception e) {
            // Assert
            fail("Should not get different exception");
        }
    }

    @Test
    public void createSignedUrl_s3ClientServiceError_throwsSdkClientException() {
        try {
            // Arrange
            Date testDate = new Date();
            Mockito.when(expirationDateHelper.getExpirationDate(Mockito.anyInt())).thenReturn(testDate);

            Instant instant = Instant.now();
            Mockito.when(instantHelper.getCurrentInstant()).thenReturn(instant);

            Mockito.when(s3Client.generatePresignedUrl(Mockito.any(GeneratePresignedUrlRequest.class))).thenThrow(SdkClientException.class);

            // Act
            CUT.createSignedUrl(unsignedUrl, authorizationToken);

            // Assert
            fail("Should not succeed!");
        } catch (AppException e) {
        	 // Assert
            assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getError().getCode());
            assertEquals("Unsupported Operation Exception", e.getError().getReason());
        } catch (Exception e) {
            // Assert
            fail("Should not get different exception");
        }
    }
}
