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

package org.opengroup.osdu.util;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import org.opengroup.osdu.core.aws.s3.S3Config;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.List;

public class CloudStorageUtilsAws extends CloudStorageUtils {

    private static final String BASE_INTEGRATION_TEST_BUCKET_NAME = "osdu-delivery-integration-test-bucket";
    private static final String DELIVERY_INT_TEST_BUCKET_PARAM = "DELIVERY_INT_TEST_BUCKET_NAME";

    private AmazonS3 s3;
    private String testBucketName;

    public CloudStorageUtilsAws() {
        String region = AwsConfig.getCloudStorageRegion();
        String storageEndpoint = String.format("s3.%s.amazonaws.com", region);
        S3Config config = new S3Config(storageEndpoint, region);
        testBucketName = System.getenv(DELIVERY_INT_TEST_BUCKET_PARAM);
        s3 = config.amazonS3();
    }

    @Override
    public void createBucket()
    {
        createBucketIfNotExists(testBucketName);
        clearTestBucket(testBucketName);
    }

    @Override
    public void deleteBucket(){
        ListObjectsV2Result result = s3.listObjectsV2(testBucketName);
        for (S3ObjectSummary summary: result.getObjectSummaries()) {
            deleteCloudFile(testBucketName, summary.getKey());
        }
    }

    @Override
    public String createCloudFile(String fileName){
        s3.putObject(testBucketName, fileName, "");
        return String.format("s3://%s/%s", testBucketName, fileName);
    }

    @Override
    public void deleteCloudFile(String bucketName,String fileName) {
        s3.deleteObject(bucketName, fileName);
    }

    private void createBucketIfNotExists(String testBucketName){
        boolean exists = s3.doesBucketExistV2(testBucketName);
        if(!exists){
            s3.createBucket(testBucketName);
        }
    }

    private void clearTestBucket(String testBucketName){
        ListObjectsV2Result result = s3.listObjectsV2(testBucketName);
        for(S3ObjectSummary summary: result.getObjectSummaries()){
            deleteCloudFile(testBucketName, summary.getKey());
        }
    }

    private void deleteAllBuckets() {
        List<Bucket> buckets = s3.listBuckets();
        for(Bucket bucket : buckets) {
            if(bucket.getName().contains(BASE_INTEGRATION_TEST_BUCKET_NAME)){

                ListObjectsV2Result result = s3.listObjectsV2(bucket.getName());
                for(S3ObjectSummary summary: result.getObjectSummaries()){
                    deleteCloudFile(bucket.getName(), summary.getKey());
                }
                s3.deleteBucket(bucket.getName());
            }
        }

    }
}
