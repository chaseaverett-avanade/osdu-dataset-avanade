/**
 * Copyright 2020 Google LLC
 * Copyright 2020 EPAM Systems, Inc
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

package org.opengroup.osdu.util;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobListOption;
import com.google.cloud.storage.StorageOptions;


public class CloudStorageUtilsGCP extends CloudStorageUtils {

	private static final String BASE_INTEGRATION_TEST_BUCKET_NAME = "osdu-delivery-integration-test-bucket";

	private static final String PROJECT_ID = "osdu-sample";


	private Storage storage;
	private String testBucketName;

	public CloudStorageUtilsGCP() {
		storage = StorageOptions.newBuilder().setCredentials(AccountCredentialsProvider.getCredentials())
			.setProjectId(PROJECT_ID).build()
			.getService();

		testBucketName = String
			.format("%s-%s", System.currentTimeMillis(), BASE_INTEGRATION_TEST_BUCKET_NAME);
	}

	@Override
	public void createBucket() {
		deleteAllBuckets();
		storage.create(BucketInfo.newBuilder(testBucketName).build());
	}

	@Override
	public void deleteBucket() {
		Page<Blob> list = storage.list(testBucketName);

		for (Blob summary : list.getValues()) {
			deleteCloudFile(testBucketName, summary.getName());
		}
		storage.delete(testBucketName);
	}

	@Override
	public String createCloudFile(String fileName) {
		BlobId blobId = BlobId.of(testBucketName, fileName);
		BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("application/octet-stream").build();
		storage.create(blobInfo);
		return String.format("gs://%s/%s", testBucketName, fileName);
	}

	@Override
	public void deleteCloudFile(String bucketName, String fileName) {
		storage.delete(BlobId.of(bucketName, fileName));
	}

	private void deleteAllBuckets() {
		Page<Bucket> buckets = storage.list();
		for (Bucket bucket : buckets.getValues()) {
			if (bucket.getName().contains(BASE_INTEGRATION_TEST_BUCKET_NAME)) {
				Page<Blob> blobs = bucket.list(BlobListOption.currentDirectory());
				for (Blob summary : blobs.getValues()) {
					deleteCloudFile(testBucketName, summary.getName());
				}
				storage.delete(testBucketName);
			}
		}

	}
}
