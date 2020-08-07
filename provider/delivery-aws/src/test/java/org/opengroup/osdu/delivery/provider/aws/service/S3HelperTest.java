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
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengroup.osdu.delivery.provider.aws.AwsServiceConfig;
import org.opengroup.osdu.delivery.provider.aws.model.S3Location;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

@RunWith(MockitoJUnitRunner.class)
public class S3HelperTest {

  @InjectMocks
  private S3Helper CUT;

  @Mock
  private AwsServiceConfig awsServiceConfig;

  @Mock
  private AmazonS3 s3;

  @Test
  public void should_signUrl() throws MalformedURLException {
    URL url = new URL("https", "host", "file");
    awsServiceConfig.amazonRegion = "us-east-1";

    Mockito.when(s3.generatePresignedUrl(Mockito.any())).thenReturn(url);

    S3Location fileLocation = new S3Location("s3://bucket/path/key");

    URL actual = CUT.generatePresignedUrl(fileLocation, HttpMethod.GET,  new Date());

    ArgumentCaptor<GeneratePresignedUrlRequest> requestArgumentCaptor = ArgumentCaptor.forClass(GeneratePresignedUrlRequest.class);
    Mockito.verify(s3, Mockito.times(1)).generatePresignedUrl(requestArgumentCaptor.capture());
    GeneratePresignedUrlRequest request = requestArgumentCaptor.getValue();
    Assert.assertEquals("path/key", request.getKey());
    Assert.assertEquals("bucket", request.getBucketName());
    Assert.assertEquals(HttpMethod.GET, request.getMethod());
    Assert.assertEquals(url, actual);
  }
}