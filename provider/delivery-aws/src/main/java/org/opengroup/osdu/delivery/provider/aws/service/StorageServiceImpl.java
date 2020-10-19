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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.delivery.model.SignedUrl;
import org.opengroup.osdu.delivery.provider.aws.AwsServiceConfig;
import org.opengroup.osdu.delivery.provider.aws.model.S3Location;
import org.opengroup.osdu.delivery.provider.aws.model.TemporaryCredentials;
import org.opengroup.osdu.delivery.provider.interfaces.IStorageService;
import org.springframework.stereotype.Service;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
@Slf4j
@RequiredArgsConstructor
public class StorageServiceImpl implements IStorageService {

  @Inject
  private DpsHeaders headers;

  @Inject
  private AwsServiceConfig awsServiceConfig;

  @Inject
  private S3Helper s3Helper;

  @Inject
  private STSHelper stsHelper;

  @Inject
  private ExpirationDateHelper expirationDateHelper;

  @Inject InstantHelper instantHelper;

  private String roleArn;

  private Duration expirationDuration;

  private final static String AWS_SDK_EXCEPTION_MSG = "There was an error communicating with the Amazon SDK for signing.";
  private final static String URI_EXCEPTION_REASON = "Exception creating signed url";
  private final static String STS_EXCEPTION_REASON = "Exception creating credentials";
  private final static String INVALID_S3_PATH_REASON = "Unsigned url invalid, needs to be full S3 path";

  @PostConstruct
  public void init() {
    roleArn = awsServiceConfig.stsRoleArn;
    expirationDuration = Duration.ofDays(awsServiceConfig.s3SignedUrlExpirationTimeInDays);
  }

  @Override
  public SignedUrl createSignedUrl(String identifier, String unsignedUrl, String authorizationToken) {
    SignedUrl url = new SignedUrl();
    URL s3SignedUrl;
    TemporaryCredentials credentials;

    S3Location fileLocation = new S3Location(unsignedUrl);

    if (!fileLocation.isValid){
      throw new AppException(HttpStatus.SC_BAD_REQUEST, "Malformed URL", INVALID_S3_PATH_REASON);
    }

    Instant now = instantHelper.now();

    Date expiration = expirationDateHelper.getExpiration(now, expirationDuration);

    try {
      s3SignedUrl = s3Helper.generatePresignedUrl(fileLocation, HttpMethod.GET, expiration);
    } catch (SdkClientException e) {
      throw new AppException(HttpStatus.SC_SERVICE_UNAVAILABLE, "S3 Error", URI_EXCEPTION_REASON, e);
    }
    try {
      credentials = stsHelper.getCredentials(identifier, fileLocation, roleArn, this.headers.getUserEmail(), expiration);
    } catch (SdkClientException e) {
      log.error("STS Exception " + e.getMessage());
      credentials = TemporaryCredentials.builder().accessKeyId("").secretAccessKey("").expiration(new Date()).sessionToken("").build();
      // throw new AppException(HttpStatus.SC_SERVICE_UNAVAILABLE, "STS Error", STS_EXCEPTION_REASON, e);
    }

    try {

      url.setUri(new URI(s3SignedUrl.toString()));
      url.setUrl(s3SignedUrl);
      url.setCreatedAt(instantHelper.now());
      url.setConnectionString(credentials.toConnectionString());
    } catch(URISyntaxException e) {
      log.error("There was an error generating the URI.",e);
      throw new AppException(HttpStatus.SC_BAD_REQUEST, "Malformed URL", URI_EXCEPTION_REASON, e);
    }
    return url;
  }

  @Override
  public SignedUrl createSignedUrl(String unsignedUrl, String authorizationToken) {
    throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Unsupported Operation Exception", "Unsupported Operation Exception");
  }

}
