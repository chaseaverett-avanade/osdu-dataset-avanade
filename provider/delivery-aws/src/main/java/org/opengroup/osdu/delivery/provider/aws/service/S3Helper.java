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
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import org.opengroup.osdu.delivery.provider.aws.AwsServiceConfig;
import org.opengroup.osdu.delivery.provider.aws.model.S3Location;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.net.URL;
import java.util.Date;

@Component
public class S3Helper {

  @Inject
  private AwsServiceConfig awsServiceConfig;

  private AmazonS3 s3;

  @PostConstruct
  public void init() {
    s3 = AmazonS3ClientBuilder
            .standard()
            .withRegion(Regions.fromName(awsServiceConfig.amazonRegion))
            .build();
  }

  /**
   * Generates a presignedurl for the S3location
   * @param location
   * @param httpMethod
   * @param expiration
   * @return
   * @throws SdkClientException
   */
  public URL generatePresignedUrl(S3Location location, HttpMethod httpMethod, Date expiration) throws SdkClientException {

    GeneratePresignedUrlRequest generatePresignedUrlRequest =
            new GeneratePresignedUrlRequest(location.bucket, location.key, httpMethod)
                    .withExpiration(expiration);

    return s3.generatePresignedUrl(generatePresignedUrlRequest);
  }
}
