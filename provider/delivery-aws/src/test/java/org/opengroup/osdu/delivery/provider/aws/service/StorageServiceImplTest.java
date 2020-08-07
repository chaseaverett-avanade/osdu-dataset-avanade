// Copyright © Amazon Web Services
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

package org.opengroup.osdu.delivery.provider.aws.service;
import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
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
import org.opengroup.osdu.delivery.model.SignedUrl;
import org.opengroup.osdu.delivery.provider.aws.AwsServiceConfig;
import org.opengroup.osdu.delivery.provider.aws.model.S3Location;
import org.opengroup.osdu.delivery.provider.aws.model.TemporaryCredentials;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(MockitoJUnitRunner.class)
public class StorageServiceImplTest {

    @InjectMocks
    private StorageServiceImpl CUT;

    @Mock
    private DpsHeaders headers;

    @Mock
    private S3Helper s3Helper;

    @Mock
    private STSHelper stsHelper;

    @Mock
    private AwsServiceConfig awsServiceConfig;

    @Mock
    private ExpirationDateHelper expirationDateHelper;

    @Mock
    private InstantHelper instantHelper;

    private String bucketName = "aws-osdu-demo-r2";
    private String key = "data/provided/tno/well-logs/7845_l0904s1_1989_comp.las";
    private String s3uri = "s3://" + bucketName + "/" + key;
    private String authorizationToken = "authorization_token";
    private String srn = "";

    @Test
    public void should_createSignedUrl() throws IOException, URISyntaxException {

        Date expirationDate = new Date();
        Mockito.when(expirationDateHelper.getExpiration(Mockito.any(Instant.class),
                Mockito.any(Duration.class))).thenReturn(expirationDate);

        URL url = new URL("http://testsignedurl.com");

        TemporaryCredentials credentials = TemporaryCredentials
                .builder()
                .accessKeyId("A")
                .expiration(expirationDate)
                .secretAccessKey("S")
                .sessionToken("ST")
                .build();

        Mockito.when(s3Helper.generatePresignedUrl(Mockito.any(S3Location.class),
                Mockito.any(HttpMethod.class), Mockito.eq(expirationDate)))
                .thenReturn(url);

        Mockito.when(stsHelper.getCredentials(Mockito.eq(srn), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(credentials);

        Instant createdAt = Instant.now();
        Mockito.when(instantHelper.now()).thenReturn(createdAt);

        SignedUrl expected = new SignedUrl();
        expected.setUri(new URI(url.toString()));
        expected.setUrl(url);
        expected.setCreatedAt(createdAt);
        expected.setConnectionString(credentials.toConnectionString());

        SignedUrl actual = CUT.createSignedUrl(srn, s3uri, authorizationToken);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void should_throwAppException_for_malformedUnsignedUrl() {
        try {
            String unsignedUrl = "malformedUrlString";
            String authorizationToken = "testAuthorizationToken";

            CUT.createSignedUrl(null, unsignedUrl, authorizationToken);

            fail("Should not succeed!");
        } catch (AppException e) {

            assertEquals(HttpStatus.SC_BAD_REQUEST, e.getError().getCode());
            assertEquals("Malformed URL", e.getError().getReason());
            assertEquals( "Unsigned url invalid, needs to be full S3 path", e.getError().getMessage());
        } catch (Exception e) {
            fail("Should not get different exception");
        }
    }

    @Test
    public void should_throwSdkClientException_for_s3ClientServiceError() {
        try {
            Date expirationDate = new Date();
            Mockito.when(expirationDateHelper.getExpiration(Mockito.any(Instant.class),
                    Mockito.any(Duration.class))).thenReturn(expirationDate);

            String user = "test-user-with-access@testing.com";


            Instant instant = Instant.now();
            Mockito.when(instantHelper.now()).thenReturn(instant);

            Mockito.when(s3Helper.generatePresignedUrl(Mockito.any(S3Location.class),
                    Mockito.any(HttpMethod.class), Mockito.eq(expirationDate))).thenThrow(SdkClientException.class);

            CUT.createSignedUrl(srn, s3uri, authorizationToken);

            fail("Should not succeed!");
        } catch (AppException e) {
            assertEquals(HttpStatus.SC_SERVICE_UNAVAILABLE, e.getError().getCode());
            assertEquals("S3 Error", e.getError().getReason());
            assertEquals( "Exception creating signed url", e.getError().getMessage());
        } catch (Exception e) {
            fail("Should not get different exception");
        }
    }
}
