// Copyright © Microsoft Corporation
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


import java.util.List;

public class CloudStorageUtilsTestAzure extends CloudStorageUtils {

    private static final String BASE_INTEGRATION_TEST_CONTAINER_NAME = "osdu-delivery-integration-test-bucket";

    private static String storageAccount = AzureBlobService.getStorageAccount();

    private AzureBlobService blobService;
    private String testContainerName;

    public CloudStorageUtilsTestAzure() {
        AzureBlobServiceConfig config = new AzureBlobServiceConfig(storageAccount);
        testContainerName = String.format("%s-%s", Long.toString(System.currentTimeMillis()), BASE_INTEGRATION_TEST_CONTAINER_NAME);
        blobService = config.azureBlobService();
    }

    @Override
    public void createBucket() {
        deleteAllContainers();
        blobService.createContainer(testContainerName);
    }

    @Override
    public void deleteBucket() {
        BlobServiceListObjectsResult result = blobService.listObjects(testContainerName);
        for (BlobServiceObjectSummary summary: result.getObjectSummaries()) {
            deleteCloudFile(testContainerName, summary.getKey());
        }
        blobService.deleteContainer(testContainerName);
    }

    @Override
    public String createCloudFile(String fileName) {
        String filePath = blobService.putObject(testContainerName, fileName);
        return filePath;
    }

    @Override
    public void deleteCloudFile(String bucketName,String fileName) {
        blobService.deleteObject(bucketName, fileName);
    }

    private void deleteAllContainers() {
        List<Container> containers = blobService.listContainers();
        for(Container container : containers) {
            if(container.getName().contains(BASE_INTEGRATION_TEST_CONTAINER_NAME)){
                BlobServiceListObjectsResult result = blobService.listObjects(container.getName());
                for(BlobServiceObjectSummary summary: result.getObjectSummaries()){
                    deleteCloudFile(container.getName(), summary.getKey());
                }
                blobService.deleteContainer(container.getName());
          }
        }
    }
}
