/*
 * Copyright 2020-2022 Google LLC
 * Copyright 2020-2022 EPAM Systems, Inc
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

package org.opengroup.osdu.dataset.anthos.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.RemoveObjectsArgs;
import io.minio.Result;
import io.minio.messages.Item;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.opengroup.osdu.dataset.CloudStorageUtil;
import org.opengroup.osdu.dataset.anthos.configuration.MapperConfig;
import org.opengroup.osdu.dataset.anthos.configuration.MinioConfig;
import org.opengroup.osdu.dataset.anthos.model.IntTestFileCollectionInstructionsItem;
import org.opengroup.osdu.dataset.anthos.model.IntTestFileInstructionsItem;


@Log
public class CloudStorageUtilAnthos extends CloudStorageUtil {

    private final MinioConfig minioConfig = MinioConfig.Instance();
    private final ObjectMapper objectMapper;
    private final MinioClient minioClient;

    public CloudStorageUtilAnthos() {
        objectMapper = MapperConfig.getObjectMapper();
        this.minioClient = MinioClient.builder()
            .endpoint(minioConfig.getMinioEndpoint())
            .credentials(minioConfig.getMinioAccessKey(), minioConfig.getMinioSecretKey())
            .build();
    }

    public String uploadCloudFileUsingProvidedCredentials(String fileName, Object storageLocationProperties,
        String fileContents) {
        IntTestFileInstructionsItem fileInstructionsItem = objectMapper
            .convertValue(storageLocationProperties, IntTestFileInstructionsItem.class);

        Client client = AnthosTestUtil.getClient();

        try {
            WebResource resource = client.resource(fileInstructionsItem.getSignedUrl().toURI());
            Builder builder = resource.accept(MediaType.APPLICATION_JSON).type(MediaType.TEXT_PLAIN);
            ClientResponse put = builder.method(HttpMethod.PUT, ClientResponse.class, fileContents);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Upload file by signed URL FAIL", e);
        }
        return fileInstructionsItem.getFileSource();
    }

    @SneakyThrows
    public String uploadCollectionUsingProvidedCredentials(String fileName, IntTestFileCollectionInstructionsItem collectionInstructionsItem,
        String fileContents) {
        Map<String, String> options = collectionInstructionsItem.getSigningOptions();

        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder();
        multipartBuilder.setType(MultipartBody.FORM);
        for (Map.Entry<String, String> entry : options.entrySet()) {
            multipartBuilder.addFormDataPart(entry.getKey(), entry.getValue());
        }

        String fileFolderAndName = collectionInstructionsItem.getFileCollectionSource() + "/" + fileName;
        multipartBuilder.addFormDataPart("key", fileFolderAndName);
        multipartBuilder.addFormDataPart("Content-Type", "text/txt");

        multipartBuilder.addFormDataPart("file", fileName, RequestBody.create(fileContents.getBytes(), null));
        Request request = new Request.Builder()
            .url(collectionInstructionsItem.getUrl())
            .post(multipartBuilder.build())
            .build();

        OkHttpClient httpClient = new OkHttpClient().newBuilder().build();
        Response response = httpClient.newCall(request).execute();
        assertTrue(response.isSuccessful());
        return collectionInstructionsItem.getUrl() + fileFolderAndName;
    }

    public String downloadCloudFileUsingDeliveryItem(Object deliveryItem) {
        IntTestFileInstructionsItem fileInstructionsItem = objectMapper
            .convertValue(deliveryItem, IntTestFileInstructionsItem.class);
        try {
            return FileUtils.readFileFromUrl(fileInstructionsItem.getSignedUrl());
        } catch (IOException e) {
            log.log(Level.SEVERE, "Download file by signed URL FAIL", e);
        }
        return null;
    }

    public String downloadCollectionFileUsingDeliveryItem(Object deliveryItem, String fileName) {
        Map<String, Object> retrievalProperties = objectMapper.convertValue(deliveryItem,
            new TypeReference<Map<String, Object>>() {
            });

        List<IntTestFileInstructionsItem> collectionInstructionsItem = objectMapper
            .convertValue(retrievalProperties.get("retrievalPropertiesList"), new TypeReference<List<IntTestFileInstructionsItem>>() {
            });

        IntTestFileInstructionsItem instructionsItem = collectionInstructionsItem.get(0);

        try {
            return FileUtils.readFileFromUrl(instructionsItem.getSignedUrl());
        } catch (IOException e) {
            log.log(Level.SEVERE, "Download file by signed URL FAIL", e);
        }
        return null;
    }

    @SneakyThrows
    public void deleteCloudFile(String unsignedUrl) {
        String[] gsPathParts = unsignedUrl.split("https://");
        String[] gsObjectKeyParts = gsPathParts[1].split("/");
        String bucketName = gsObjectKeyParts[1];
        String blobObjectPrefix = gsObjectKeyParts[2];
        ListObjectsArgs listObjectsArgs = ListObjectsArgs.builder().bucket(bucketName).prefix("/" + blobObjectPrefix + "/").build();
        Iterable<Result<Item>> results = minioClient.listObjects(listObjectsArgs);
        for (Result<Item> result : results){
            String objectName = result.get().objectName();
            RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build();
            minioClient.removeObject(removeObjectArgs);
        }
        log.info(String.format("Blob created during tests deleted, location: %s", unsignedUrl));
    }
}
