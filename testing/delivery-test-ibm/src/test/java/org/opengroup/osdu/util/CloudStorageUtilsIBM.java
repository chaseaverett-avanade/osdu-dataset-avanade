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

package org.opengroup.osdu.util;

import java.util.List;

import org.opengroup.osdu.core.ibm.util.Config;

import com.ibm.cloud.objectstorage.ClientConfiguration;
import com.ibm.cloud.objectstorage.auth.AWSCredentials;
import com.ibm.cloud.objectstorage.auth.AWSStaticCredentialsProvider;
import com.ibm.cloud.objectstorage.auth.BasicAWSCredentials;
import com.ibm.cloud.objectstorage.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.ibm.cloud.objectstorage.services.s3.AmazonS3;
import com.ibm.cloud.objectstorage.services.s3.AmazonS3ClientBuilder;
import com.ibm.cloud.objectstorage.services.s3.model.Bucket;
import com.ibm.cloud.objectstorage.services.s3.model.ListObjectsV2Result;
import com.ibm.cloud.objectstorage.services.s3.model.S3ObjectSummary;

public class CloudStorageUtilsIBM extends CloudStorageUtils {

    private static final String BASE_INTEGRATION_TEST_BUCKET_NAME = "osdu-delivery-integration-test-bucket";

    private AmazonS3 s3;
    private String testBucketName;

    public CloudStorageUtilsIBM() {    		
    		
    	String url = Config.getEnvironmentVariable("IBM_COS_ENDPOINT");
    	String region = Config.getEnvironmentVariable("IBM_COS_REGION");
    	String accessKey = Config.getEnvironmentVariable("IBM_COS_ACCESS_KEY");
    	String secretKey = Config.getEnvironmentVariable("IBM_COS_SECRET_KEY");
    	    		
		AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

        ClientConfiguration clientConfig = new ClientConfiguration().withRequestTimeout(5000);
        clientConfig.setUseTcpKeepAlive(true);

        s3 = AmazonS3ClientBuilder.standard()
        		.withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withEndpointConfiguration(new EndpointConfiguration(url, region))
                .withPathStyleAccessEnabled(true)
                .withClientConfiguration(clientConfig).build();
        
        testBucketName = String.format("%s-%s", Long.toString(System.currentTimeMillis()), BASE_INTEGRATION_TEST_BUCKET_NAME);
    }

    @Override
    public void createBucket() {
        deleteAllBuckets();
        s3.createBucket(testBucketName);
    }

    @Override
    public void deleteBucket(){
        ListObjectsV2Result result = s3.listObjectsV2(testBucketName);
        for (S3ObjectSummary summary: result.getObjectSummaries()) {
            deleteCloudFile(testBucketName, summary.getKey());
        }
        s3.deleteBucket(testBucketName);
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
