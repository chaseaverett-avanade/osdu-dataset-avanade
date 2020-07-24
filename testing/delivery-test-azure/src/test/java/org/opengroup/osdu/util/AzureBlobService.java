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

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.BlobUrlParts;
import com.azure.storage.blob.specialized.BlockBlobClient;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AzureBlobService {
    private static String clientSecret = System.getProperty("TESTER_SERVICEPRINCIPAL_SECRET", System.getenv("TESTER_SERVICEPRINCIPAL_SECRET"));
    private static String clientId = System.getProperty("INTEGRATION_TESTER", System.getenv("INTEGRATION_TESTER"));
    private static String tenantId = System.getProperty("AZURE_AD_TENANT_ID", System.getenv("AZURE_AD_TENANT_ID"));

    public static HashMap<String, Container> containers = new HashMap<>();

    private static String storageAccount;

    AzureBlobService(String storageAccount) {
        this.storageAccount = storageAccount;
    }

    public static String getStorageAccount() {
        return System.getProperty("AZURE_STORAGE_ACCOUNT", System.getenv("AZURE_STORAGE_ACCOUNT"));
    }

    private static String generateContainerPath(String accountName, String containerName) {
        return String.format("https://%s.blob.core.windows.net/%s", accountName, containerName);
    }

    public void createContainer(String containerName)
    {
        String containerPath = generateContainerPath(storageAccount, containerName);
        BlobUrlParts parts = BlobUrlParts.parse(containerPath);
        BlobContainerClient blobContainerClient = getBlobContainerClient(parts.getAccountName(), parts.getBlobContainerName());
        if(!blobContainerClient.exists()){
            blobContainerClient.create();
            Container container = new Container(containerName, containerPath, new HashMap<String, String>());
            containers.put(containerName, container);
        }
    }

    private BlobContainerClient getBlobContainerClient(String accountName, String containerName) {
        ClientSecretCredential clientSecretCredential = new ClientSecretCredentialBuilder()
                .clientSecret(clientSecret)
                .clientId(clientId)
                .tenantId(tenantId)
                .build();
        BlobContainerClient blobContainerClient = new BlobContainerClientBuilder()
                .endpoint(getBlobAccountUrl(accountName))
                .credential(clientSecretCredential)
                .containerName(containerName)
                .buildClient();
        return blobContainerClient;
    }

    private static String getBlobAccountUrl(String accountName) {
        return String.format("https://%s.blob.core.windows.net", accountName);
    }

    public void deleteContainer(String containerName){
        String containerPath = generateContainerPath(storageAccount, containerName);
        BlobUrlParts parts = BlobUrlParts.parse(containerPath);
        BlobContainerClient blobContainerClient = getBlobContainerClient(parts.getAccountName(), parts.getBlobContainerName());
        if(blobContainerClient.exists()){
            blobContainerClient.delete();
            containers.remove(containerName);
        }
    }

    private static String generateBlobName(String blobName) {
        return blobName.replace(":","_");
    }

    public String putObject(String containerName, String blobName) {
        String blobPath = generateBlobPath(storageAccount, containerName, generateBlobName(blobName));
        BlobUrlParts parts = BlobUrlParts.parse(blobPath);
        BlobContainerClient blobContainerClient = getBlobContainerClient(parts.getAccountName(), parts.getBlobContainerName());
        if (!blobContainerClient.exists()) {
            createContainer(parts.getBlobContainerName());
        }
        BlockBlobClient blockBlobClient = blobContainerClient.getBlobClient(parts.getBlobName()).getBlockBlobClient();
        if (!blockBlobClient.exists()) {
            String dataSample = "Sample File for Delivery Integration Test";
            try (ByteArrayInputStream dataStream = new ByteArrayInputStream(dataSample.getBytes())) {
                blockBlobClient.upload(dataStream, dataSample.length());
                Container container = containers.get(containerName);
                container.addBlob(blobName, blobPath);
            } catch (Exception e) {
                e.printStackTrace();
                throw new AssertionError(String.format("Error: Could not create test %s file blob", parts.getBlobName()), e);
            }
        }
        return blobPath;
    }

    private static String generateBlobPath(String accountName, String containerName, String blobName) {
        return String.format("https://%s.blob.core.windows.net/%s/%s", accountName, containerName, blobName);
    }

    public void deleteObject(String containerName, String blobName) {
        String blobPath = generateBlobPath(storageAccount, containerName, generateBlobName(blobName));
        BlobUrlParts parts = BlobUrlParts.parse(blobPath);
        BlobContainerClient blobContainerClient = getBlobContainerClient(parts.getAccountName(), parts.getBlobContainerName());
        BlockBlobClient blockBlobClient = blobContainerClient.getBlobClient(parts.getBlobName()).getBlockBlobClient();
        if (blockBlobClient.exists()) {
            blobContainerClient.delete();
            Container container = containers.get(containerName);
            container.removeBlob(blobName);
        }
    }

    public List<Container> listContainers(){
        return new ArrayList(containers.values());
    };

   public BlobServiceListObjectsResult listObjects(String containerName) {
       Container container = containers.get(containerName);
       return new BlobServiceListObjectsResult(container.getBlobs());
   }

}
