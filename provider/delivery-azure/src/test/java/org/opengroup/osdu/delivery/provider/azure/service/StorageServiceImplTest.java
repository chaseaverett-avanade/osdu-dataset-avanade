/*
 * Copyright 2020 Microsoft Corporation
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

package org.opengroup.osdu.delivery.provider.azure.service;

import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengroup.osdu.azure.blobstorage.IBlobServiceClientFactory;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.delivery.DeliveryApplication;
import org.opengroup.osdu.delivery.model.SignedUrl;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest(classes = {DeliveryApplication.class})
public class StorageServiceImplTest {

    @InjectMocks
    private StorageServiceImpl CUT = new StorageServiceImpl();

    @Mock
    private AzureBlobSasTokenServiceImpl tokenService;

    @Mock
    private ExpirationDateHelper expirationDateHelper;

    @Mock
    private InstantHelper instantHelper;

    @Mock
    private IBlobServiceClientFactory blobServiceClientFactory;

    @Mock
    private DpsHeaders headers;

    private String containerName = "azure-osdu-demo-r2";
    private String key = "data/provided/tno/well-logs/7845_l0904s1_1989_comp.las";
    private String unsignedUrl = "https://adodev3353335343xesa.blob.core.windows.net/" + containerName + "/" + key;
    private String authorizationToken = "eyJraWQiOiJ5eWFDS2VmNmJTNFZEbDU2NnBSTm5kS1pIRDFzZllMbDZmYkpyNGtuU1dVPSIsImFsZyI6IlJ" +
            "TMjU2In0.eyJzdWIiOiI3NGY0OTEwOC1mNjJlLTQ3ZjYtODlmMy1lN2RkNmNjN2NmZWMiLCJldmVudF9pZCI6Ijc0MGQwNjQ5LWRmND" +
            "UtNDVjMS1hYjJjLWVkOGMxNzllZWQ4MCIsInRva2VuX3VzZSI6ImFjY2VzcyIsInNjb3BlIjoiYXdzLmNvZ25pdG8uc2lnbmluLnVzZ" +
            "XIuYWRtaW4iLCJhdXRoX3RpbWUiOjE1ODU3NjQwMzYsImlzcyI6Imh0dHBzOlwvXC9jb2duaXRvLWlkcC51cy1lYXN0LTEuYW1hem9u" +
            "YXdzLmNvbVwvdXMtZWFzdC0xX2FuaFZPUkc4RCIsImV4cCI6MTU4NTc2NzYzNiwiaWF0IjoxNTg1NzY0MDM2LCJqdGkiOiI1MmJhNjM" +
            "2Ny01OGNjLTRmYzItYTc0OC01ZWJjNzRiZmEyMDYiLCJjbGllbnRfaWQiOiIzcm1nbWc4bXVwMjgxdHRjMW1idXQxcGltYyIsInVzZX" +
            "JuYW1lIjoidGVzdC11c2VyLXdpdGgtYWNjZXNzQHRlc3RpbmcuY29tIn0.bCbxjZQ0ocJhfRVc_Je6EVgoCHhnqtTK1gr9QSBXrA5dm" +
            "G8iS09Jk6fdnnjhkjGc6ekKU5KRLt1YAfLCWK0DajOBt_5amDzrkm43B_ISgmm9B2SacpJANrm3wvtjQleP8BlgutKcpmGDXwXOEznQ" +
            "6NhDKtNJxjzS0i1vU3QsbQT5YYsyqvmvlXLSEsrufFl_tkxNY09W40NBafORzo5Mfv7cJxWp64WtPLQezVSyGP5i-ocGQ4zD_48xM4v" +
            "ep4FuVe5BLkENO5BDlT8rUv7T-VSybqxahJS8tRgtEfZhLE71mzplqV9ovLcM_-bhyNZSqJ0mOfTgwDY-QmpNNlXYtg";

    @Test
    public void createSignedUrl() throws IOException, URISyntaxException {
        // Arrange
        Date testDate = new Date();
        when(expirationDateHelper.getExpirationDate(anyInt())).thenReturn(testDate);
        String srn = "srn:file:-965274437";

        URL url = new URL("http://testsignedurl.com");

        Instant instant = Instant.now();
        when(instantHelper.getCurrentInstant()).thenReturn(instant);

        SignedUrl expected = new SignedUrl();
        expected.setUri(new URI(url.toString()));
        expected.setUrl(url);
        expected.setCreatedAt(instant);

        when(tokenService.sign(any(String.class))).thenReturn(url.toString());

        // Act
        SignedUrl actual = CUT.createSignedUrl(srn, unsignedUrl, authorizationToken);

        // Assert
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void createSignedUrlForBlob() throws IOException, URISyntaxException {
        // Arrange
        Date testDate = new Date();
        when(expirationDateHelper.getExpirationDate(anyInt())).thenReturn(testDate);
        String srn = "srn:file:-965274437";

        URL url = new URL("http://testsignedurl.com");

        Instant instant = Instant.now();
        when(instantHelper.getCurrentInstant()).thenReturn(instant);


        SignedUrl expected = new SignedUrl();
        expected.setUri(new URI(url.toString()));
        expected.setUrl(url);
        expected.setCreatedAt(instant);

        when(tokenService.sign(any(String.class))).thenReturn(url.toString());

        // Act
        SignedUrl actual = CUT.createSignedUrl(srn, unsignedUrl, authorizationToken);

        // Assert
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void createSignedUrlForContainer() throws IOException, URISyntaxException {
        // Arrange
        Date testDate = new Date();
        when(expirationDateHelper.getExpirationDate(anyInt())).thenReturn(testDate);
        String srn = "srn:file/ovds:-965274437";

        URL url = new URL("http://testsignedurl.com");

        Instant instant = Instant.now();
        when(instantHelper.getCurrentInstant()).thenReturn(instant);


        SignedUrl expected = new SignedUrl();
        expected.setUri(new URI(url.toString()));
        expected.setUrl(url);
        expected.setCreatedAt(instant);

        when(tokenService.signContainer(any(String.class))).thenReturn(url.toString());

        // Act
        SignedUrl actual = CUT.createSignedUrl(srn, unsignedUrl, authorizationToken);

        // Assert
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void createSignedUrl_malformedUnsignedUrl_throwsAppException() {
        try {
            // Arrange
            String unsignedUrl = "testunsignedurl";
            String authorizationToken = "testAuthorizationToken";
            String srn = "srn:file/:-965274437";

            when(tokenService.sign(any(String.class))).thenReturn(unsignedUrl);

            // Act
            CUT.createSignedUrl(srn, unsignedUrl, authorizationToken);

            // Assert
            fail("Should not succeed!");
        } catch (AppException e) {
            // Assert
            assertEquals(HttpStatus.SC_BAD_REQUEST, e.getError().getCode());
            assertEquals("Malformed URL", e.getError().getReason());
            assertEquals("Unsigned url invalid, needs to be full path", e.getError().getMessage());
        } catch (Exception e) {
            // Assert
            fail("Should not get different exception");
        }
    }

    @Test
    public void createSignedUrl_malformedUnsignedUrl2_throwsAppException() {
        try {
            // Arrange p
            String unsignedUrl = "http://testunsignedurl.com/";
            String authorizationToken = "testAuthorizationToken";
            String srn = "srn:file/:-965274437";

            when(tokenService.sign(any(String.class))).thenReturn(unsignedUrl);

            // Act
            CUT.createSignedUrl(srn, unsignedUrl, authorizationToken);

            // Assert
            fail("Should not succeed!");
        } catch (AppException e) {
            // Assert
            assertEquals(HttpStatus.SC_BAD_REQUEST, e.getError().getCode());
            assertEquals("Malformed URL", e.getError().getReason());
            assertEquals("Unsigned url invalid, needs to be full path", e.getError().getMessage());
        } catch (Exception e) {
            // Assert
            fail("Should not get different exception");
        }
    }

    @Test
    public void createSignedUrl_unsupportedOperationServiceError_throwsUnsupportedOperationException() {
        try {
            Date testDate = new Date();
            when(expirationDateHelper.getExpirationDate(anyInt())).thenReturn(testDate);


            Instant instant = Instant.now();
            when(instantHelper.getCurrentInstant()).thenReturn(instant);

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
