/*
 * Copyright 2020 Amazon Web Services
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

package org.opengroup.osdu.delivery.provider.aws.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.aws.s3.S3Config;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.delivery.model.SignedUrl;
import org.opengroup.osdu.delivery.provider.interfaces.IStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;

@Service
@Slf4j
@RequiredArgsConstructor
public class StorageServiceImpl implements IStorageService {

  @Value("${aws.s3.signed-url.expiration-days}")
  private int s3SignedUrlExpirationTimeInDays;

  @Value("${aws.s3.region}")
  private String s3Region;

  @Value("${aws.s3.endpoint}")
  private String s3Endpoint;

  private AmazonS3 s3Client;

  private ExpirationDateHelper expirationDateHelper;

  private InstantHelper instantHelper;

  private final static String AWS_SDK_EXCEPTION_MSG = "There was an error communicating with the Amazon S3 SDK request " +
			"for S3 URL signing.";
  private final static String URI_EXCEPTION_REASON = "Exception creating signed url";
  private final static String INVALID_S3_PATH_REASON = "Unsigned url invalid, needs to be full S3 path";

  @PostConstruct
  public void init() {
    S3Config config = new S3Config(s3Endpoint, s3Region);
    s3Client = config.amazonS3();
    expirationDateHelper = new ExpirationDateHelper();
    instantHelper = new InstantHelper();
  }

  @Override
  public SignedUrl createSignedUrl(String unsignedUrl, String authorizationToken) {
    String[] s3PathParts = unsignedUrl.split("s3://");
    if (s3PathParts.length < 2){
      throw new AppException(HttpStatus.SC_BAD_REQUEST, "Malformed URL", INVALID_S3_PATH_REASON);
    }

    String[] s3ObjectKeyParts = s3PathParts[1].split("/");
    if (s3ObjectKeyParts.length < 1){
      throw new AppException(HttpStatus.SC_BAD_REQUEST, "Malformed URL", INVALID_S3_PATH_REASON);
    }

    String bucketName = s3ObjectKeyParts[0];
    String s3Key = String.join("/", Arrays.copyOfRange(s3ObjectKeyParts, 1, s3ObjectKeyParts.length));

    URL s3SignedUrl = generateSignedS3Url(bucketName, s3Key, "GET");
    SignedUrl url = new SignedUrl();
    try {
      url.setUri(new URI(s3SignedUrl.toString()));
      url.setUrl(s3SignedUrl);
      url.setCreatedAt(instantHelper.getCurrentInstant());
    } catch(URISyntaxException e) {
      log.error("There was an error generating the URI.",e);
      throw new AppException(HttpStatus.SC_BAD_REQUEST, "Malformed URL", URI_EXCEPTION_REASON, e);
    }
    return url;
  }

  /**
   * This method will take a string of a pre-validated S3 bucket name, and use the AWS Java SDK
   * to generate a signed URL with an expiration date set to be as-configured
   * @param s3BucketName - pre-validated S3 bucket name
   * @param s3ObjectKey - pre-validated S3 object key (keys include the path + filename)
   * @return - String of the signed S3 URL to allow file access temporarily
   */
  private URL generateSignedS3Url(String s3BucketName, String s3ObjectKey, String httpMethod) {
    // Set the presigned URL to expire after the amount of time specified by the configuration variables
    Date expiration = expirationDateHelper.getExpirationDate(s3SignedUrlExpirationTimeInDays);

    log.debug("Requesting a signed S3 URL with an expiration of: " + expiration.toString() + " (" +
        s3SignedUrlExpirationTimeInDays + " minutes from now)");

    // Generate the presigned URL
    GeneratePresignedUrlRequest generatePresignedUrlRequest =
        new GeneratePresignedUrlRequest(s3BucketName, s3ObjectKey)
            .withMethod(HttpMethod.valueOf(httpMethod))
            .withExpiration(expiration);
    try {
      // Attempt to generate the signed S3 URL
      URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
      return url;
    } catch (SdkClientException e) {
      // Catch any SDK client exceptions, and return a 500 error
      log.error("There was an AWS SDK error processing the signing request.",e);
      throw new AppException(HttpStatus.SC_SERVICE_UNAVAILABLE, "Remote Service Unavailable", AWS_SDK_EXCEPTION_MSG, e);
    }
  }
}
