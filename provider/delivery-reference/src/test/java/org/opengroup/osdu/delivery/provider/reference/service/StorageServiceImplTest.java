package org.opengroup.osdu.delivery.provider.reference.service;

import io.minio.MinioClient;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidBucketNameException;
import io.minio.errors.InvalidExpiresRangeException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.delivery.DeliveryApplication;
import org.opengroup.osdu.delivery.model.SignedUrl;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest(classes = {DeliveryApplication.class})
public class StorageServiceImplTest {

  @Mock
  private InstantHelper instantHelper;

  @Mock
  private MinioClient minioClient;

  @InjectMocks
  private StorageServiceImpl storageService;

  private String bucketName = "osdu-sample-osdu-file";
  private String key = "object.csv";
  private String unsignedUrl = "s3://" + bucketName + "/" + key;
  private String authorizationToken = "123";

  @Test
  public void createSignedUrl() throws IOException, URISyntaxException, InvalidKeyException,
      InvalidResponseException, InsufficientDataException, InvalidExpiresRangeException, ServerException,
      InternalException, NoSuchAlgorithmException, XmlParserException, InvalidBucketNameException, ErrorResponseException {

    String url = "http://testsignedurl.com";
    Mockito.when(minioClient.presignedGetObject(Mockito.any(String.class), Mockito.any(String.class),
        Mockito.any(Integer.class))).thenReturn(url);

    Instant instant = Instant.now();
    Mockito.when(instantHelper.getCurrentInstant()).thenReturn(instant);

    SignedUrl expected = new SignedUrl();
    expected.setUri(new URI(url));
    expected.setUrl(new URL(url));
    expected.setCreatedAt(instant);

    SignedUrl actual = storageService.createSignedUrl(unsignedUrl, authorizationToken);
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void createSignedUrl_malformedUnsignedUrl_throwsAppException() {
    try {
      String unsignedUrl = "malformedUrlString";
      String authorizationToken = "testAuthorizationToken";

      storageService.createSignedUrl(unsignedUrl, authorizationToken);
      fail("Should not succeed!");
    } catch (AppException e) {
      assertEquals(HttpStatus.SC_BAD_REQUEST, e.getError().getCode());
      assertEquals("Malformed URL", e.getError().getReason());
      assertEquals( "Unsigned url invalid, needs to be full MI path", e.getError().getMessage());
    } catch (Exception e) {
      fail("Should not get different exception");
    }
  }
}
