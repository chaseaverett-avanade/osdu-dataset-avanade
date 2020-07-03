/*
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

package org.opengroup.osdu.delivery.provider.gcp.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.SignUrlOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.delivery.model.SignedUrl;
import org.opengroup.osdu.delivery.provider.interfaces.IStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class StorageServiceImpl implements IStorageService {

	private final static String URI_EXCEPTION_REASON = "Exception creating signed url";

	private final static String INVALID_GS_PATH_REASON = "Unsigned url invalid, needs to be full GS path";

	private final static HttpMethod signedUrlMethod = HttpMethod.GET;

	private final Storage storage;

	private final InstantHelper instantHelper;

	@Value("${gcp.signed-url.expiration-days}")
	private int gcSignedUrlExpirationTimeInDays;

	@Override
	public SignedUrl createSignedUrl(String unsignedUrl, String authorizationToken) {

		String[] gsPathParts = unsignedUrl.split("gs://");
		if (gsPathParts.length < 2) {
			throw new AppException(HttpStatus.BAD_REQUEST.value(), "Malformed URL", INVALID_GS_PATH_REASON);
		}

		String[] gsObjectKeyParts = gsPathParts[1].split("/");
		if (gsObjectKeyParts.length < 1) {
			throw new AppException(HttpStatus.BAD_REQUEST.value(), "Malformed URL", INVALID_GS_PATH_REASON);
		}

		String bucketName = gsObjectKeyParts[0];
		String filePath = String.join("/", Arrays.copyOfRange(gsObjectKeyParts, 1, gsObjectKeyParts.length));

		URL gcSignedUrl = generateSignedGcURL(bucketName, filePath);

		try {
			return SignedUrl.builder()
				.uri(new URI(gcSignedUrl.toString()))
				.url(gcSignedUrl)
				.createdAt(instantHelper.getCurrentInstant())
				.build();
		} catch (URISyntaxException e) {
			log.error("There was an error generating the URI.", e);
			throw new AppException(org.apache.http.HttpStatus.SC_BAD_REQUEST, "Malformed URL", URI_EXCEPTION_REASON, e);
		}
	}

	private URL generateSignedGcURL(String bucketName, String filePath) {
		BlobId blobId = BlobId.of(bucketName, filePath);
		BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
			.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
			.build();

		URL url = storage.signUrl(blobInfo,
			gcSignedUrlExpirationTimeInDays, TimeUnit.DAYS, SignUrlOption.httpMethod(signedUrlMethod),
			SignUrlOption.withV4Signature());

		return url;
	}
}
