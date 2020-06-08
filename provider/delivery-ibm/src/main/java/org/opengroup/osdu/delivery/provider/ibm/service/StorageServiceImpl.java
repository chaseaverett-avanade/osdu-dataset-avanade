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
 */

package org.opengroup.osdu.delivery.provider.ibm.service;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.ibm.objectstorage.CloudObjectStorageFactory;
import org.opengroup.osdu.delivery.model.SignedUrl;
import org.opengroup.osdu.delivery.provider.interfaces.IStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.minio.MinioClient;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidBucketNameException;
import io.minio.errors.InvalidExpiresRangeException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.XmlParserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class StorageServiceImpl implements IStorageService {

	@Value("${ibm.cos.signed-url.expiration-days:1}")
	private int s3SignedUrlExpirationTimeInDays;

	@Inject
	private CloudObjectStorageFactory cosFactory;

	private MinioClient minioClient;

	private ExpirationDateHelper expirationDateHelper;

	private InstantHelper instantHelper;

	private final static String AWS_SDK_EXCEPTION_MSG = "There was an error communicating with the Amazon S3 SDK request for S3 URL signing.";
	private final static String URI_EXCEPTION_REASON = "Exception creating signed url";
	private final static String INVALID_S3_PATH_REASON = "Unsigned url invalid, needs to be full S3 path";

	@PostConstruct
	public void init() {
	  minioClient = cosFactory.getClient();
		expirationDateHelper = new ExpirationDateHelper();
		instantHelper = new InstantHelper();
	}

	@Override
	public SignedUrl createSignedUrl(String unsignedUrl, String authorizationToken) {
		String[] s3PathParts = unsignedUrl.split("s3://");
		if (s3PathParts.length < 2) {
			throw new AppException(HttpStatus.SC_BAD_REQUEST, "Malformed URL", INVALID_S3_PATH_REASON);
		}

		String[] s3ObjectKeyParts = s3PathParts[1].split("/");
		if (s3ObjectKeyParts.length < 1) {
			throw new AppException(HttpStatus.SC_BAD_REQUEST, "Malformed URL", INVALID_S3_PATH_REASON);
		}

		String bucketName = s3ObjectKeyParts[0];
		String s3Key = String.join("/", Arrays.copyOfRange(s3ObjectKeyParts, 1, s3ObjectKeyParts.length));
		SignedUrl url = new SignedUrl();


		try {
			URL s3SignedUrl = generateSignedS3Url(bucketName, s3Key, "GET");
			url.setUri(new URI(s3SignedUrl.toString()));
			url.setUrl(s3SignedUrl);
			url.setCreatedAt(instantHelper.getCurrentInstant());
		} catch (Exception e) {
			log.error("There was an error generating the URI.", e);
			throw new AppException(HttpStatus.SC_BAD_REQUEST, "Malformed URL", URI_EXCEPTION_REASON, e);
		}
		return url;
	}

	/**
	 * This method will take a string of a pre-validated S3 bucket name, and use the
	 * AWS Java SDK to generate a signed URL with an expiration date set to be
	 * as-configured
	 *
	 * @param s3BucketName - pre-validated S3 bucket name
	 * @param s3ObjectKey  - pre-validated S3 object key (keys include the path +
	 *                     filename)
	 * @return - String of the signed S3 URL to allow file access temporarily
	 */
	private URL generateSignedS3Url(String s3BucketName, String s3ObjectKey, String httpMethod)  {
		// Set the presigned URL to expire after the amount of time specified by the
		// configuration variables
		Date expiration = expirationDateHelper.getExpirationDate(s3SignedUrlExpirationTimeInDays);

		log.debug("Requesting a signed S3 URL with an expiration of: " + expiration.toString() + " ("
				+ s3SignedUrlExpirationTimeInDays + " minutes from now)");

		try {
			log.debug("creating signed url from minio ");
			int expiryTime = 24 * 60 * 60 * s3SignedUrlExpirationTimeInDays;
			String url = minioClient.presignedGetObject(s3BucketName, s3ObjectKey,expiryTime);
			log.debug("url from minio " + url);
			return new URL(url);
		} catch (InvalidKeyException | ErrorResponseException | IllegalArgumentException | InsufficientDataException
				| InternalException | InvalidBucketNameException | InvalidExpiresRangeException
				| InvalidResponseException | NoSuchAlgorithmException | XmlParserException | IOException e) {
			log.error("error creating signed url from minio ", e);
			throw new AppException(HttpStatus.SC_SERVICE_UNAVAILABLE, "Remote Service Unavailable",
					AWS_SDK_EXCEPTION_MSG, e);
		}
	}
}
