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

package org.opengroup.osdu.delivery.provider.reference.service;

import io.minio.MinioClient;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidBucketNameException;
import io.minio.errors.InvalidExpiresRangeException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;

import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.delivery.model.SignedUrl;
import org.opengroup.osdu.delivery.provider.reference.factory.CloudObjectStorageFactory;
import org.opengroup.osdu.delivery.provider.interfaces.IStorageService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageServiceImpl implements IStorageService {

	@Autowired
	private CloudObjectStorageFactory factory;

	@Value("${minio.signed-url.expiration-days:1}")
	private int signedUrlExpirationTimeInDays;

	private InstantHelper instantHelper;
	private MinioClient minioClient;

	private final static String INVALID_MI_PATH_REASON = "Unsigned url invalid, needs to be full MI path";
	private final static String URI_EXCEPTION_REASON = "Exception creating signed url";
	private final static String SDK_EXCEPTION_MSG = "There was an error communicating with the SDK request for URL signing.";

	@PostConstruct
	public void init() {
		minioClient = factory.getClient();
		instantHelper = new InstantHelper();
	}

	@Override
	public SignedUrl createSignedUrl(String unsignedUrl, String authorizationToken) {

		String[] miPathParts = unsignedUrl.split("s3://");
		if (miPathParts.length < 2) {
			throw new AppException(HttpStatus.BAD_REQUEST.value(), "Malformed URL", INVALID_MI_PATH_REASON);
		}

		String[] miObjectKeyParts = miPathParts[1].split("/");
		if (miObjectKeyParts.length < 1) {
			throw new AppException(HttpStatus.BAD_REQUEST.value(), "Malformed URL", INVALID_MI_PATH_REASON);
		}

		String bucketName = miObjectKeyParts[0];
		String key = String.join("/", Arrays.copyOfRange(miObjectKeyParts, 1, miObjectKeyParts.length));
		SignedUrl url = new SignedUrl();

		try {
			URL signedUrl = generateSignedUrl(bucketName, key, "GET");
			url.setUri(new URI(signedUrl.toString()));
			url.setUrl(signedUrl);
			url.setCreatedAt(instantHelper.getCurrentInstant());
		} catch (Exception e) {
			log.error("There was an error generating the URI.", e);
			throw new AppException(org.apache.http.HttpStatus.SC_BAD_REQUEST, "Malformed URL", URI_EXCEPTION_REASON, e);
		}
		return url;
	}

	private URL generateSignedUrl(String bucketName, String ObjectKey, String httpMethod) {
		Date expiration = getExpirationDate();
		log.debug("Requesting a signed URL with an expiration of: " + expiration.toString() + " ("
				+ signedUrlExpirationTimeInDays + " minutes from now)");

		try {
			log.debug("creating signed url from minio ");
			int expiryTime = 24 * 60 * 60 * signedUrlExpirationTimeInDays;
			String url = minioClient.presignedGetObject(bucketName, ObjectKey, expiryTime);
			log.debug("url from minio " + url);
			return new URL(url);
		} catch (InvalidKeyException | ErrorResponseException | IllegalArgumentException | InsufficientDataException
				| InternalException | InvalidBucketNameException | InvalidExpiresRangeException | InvalidResponseException
				| NoSuchAlgorithmException | XmlParserException | IOException | ServerException e) {
			log.error("error creating signed url from minio ", e);
			throw new AppException(HttpStatus.SERVICE_UNAVAILABLE.value(), "Remote Service Unavailable",
					SDK_EXCEPTION_MSG, e);
		}
	}

	private Date getExpirationDate(){
		Date expiration = new Date();
		long expTimeMillis = expiration.getTime();
		expTimeMillis += 1000 * 60 * 60 * 24 * signedUrlExpirationTimeInDays;
		expiration.setTime(expTimeMillis);
		return expiration;
	}
}
