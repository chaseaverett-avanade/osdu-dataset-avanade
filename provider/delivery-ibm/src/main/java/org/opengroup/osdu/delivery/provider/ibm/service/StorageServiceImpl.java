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

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.entitlements.IEntitlementsFactory;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.ibm.objectstorage.CloudObjectStorageFactory;
import org.opengroup.osdu.delivery.model.SignedUrl;
import org.opengroup.osdu.delivery.provider.interfaces.IStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ibm.cloud.objectstorage.HttpMethod;
import com.ibm.cloud.objectstorage.SdkClientException;
import com.ibm.cloud.objectstorage.services.s3.AmazonS3;
import com.ibm.cloud.objectstorage.services.s3.model.GeneratePresignedUrlRequest;
//import com.ibm.cloud.objectstorage.auth.policy.Resource;
//import com.ibm.cloud.objectstorage.auth.policy.Statement;
//import com.ibm.cloud.objectstorage.auth.policy.Policy;
//import com.ibm.cloud.objectstorage.auth.policy.actions.S3Actions;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.google.gson.Gson;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.auth.policy.actions.S3Actions;
import com.amazonaws.auth.policy.Resource;

import com.amazonaws.services.securitytoken.model.AssumeRoleResult;






@Service
@Slf4j
@RequiredArgsConstructor
public class StorageServiceImpl implements IStorageService {

	@Value("${ibm.cos.signed-url.expiration-days:1}")
	private int s3SignedUrlExpirationTimeInDays;
	
	@Value("${ibm.cos.endpoint_url}")
	private String endpointurl;
	
	@Value("${ibm.cos.region:us-south}")
	private String region;
	
	@Value("${ibm.cos.subuser}")
	private String subuser;
	
	@Value("${ibm.cos.subpassword}")
	private String subpassword;

	@Inject
	private CloudObjectStorageFactory cosFactory;
	
	@Inject
	private IEntitlementsFactory factory;
	
	@Inject
	private DpsHeaders headers;

	private AmazonS3 s3Client;

	private ExpirationDateHelper expirationDateHelper;

	private InstantHelper instantHelper;
	
	private String testConnectionString;

	private final static String AWS_SDK_EXCEPTION_MSG = "There was an error communicating with the Amazon S3 SDK request for S3 URL signing.";
	private final static String URI_EXCEPTION_REASON = "Exception creating signed url";
	private final static String INERNAL_SERVER_ERROR = "Internal Server Error Exception";
	private final static String INVALID_S3_PATH_REASON = "Unsigned url invalid, needs to be full S3 path";
	private final static String UNSUPPORTED_EXCEPTION_REASON = "Unsupported operation exception";
	
	@PostConstruct
	public void init() {
		s3Client = cosFactory.getClient();
		expirationDateHelper = new ExpirationDateHelper();
		instantHelper = new InstantHelper();
	}

	@Override
	public SignedUrl createSignedUrl(String srn,String unsignedUrl, String authorizationToken) {
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

		URL s3SignedUrl = generateSignedS3Url(bucketName, s3Key, "GET");
		
		// code to generate temp credentials
		
	    Policy policy = createPolicy(srn,bucketName,s3Key);
	    String profileName= getProfileName();
		
		try {
			String connectionString = generateConnectionString(policy,profileName);
			SignedUrl url = new SignedUrl();
			url.setUri(new URI(s3SignedUrl.toString()));
			url.setUrl(s3SignedUrl);
			url.setConnectionString(connectionString);
			url.setCreatedAt(instantHelper.getCurrentInstant());
			return url;
		} catch (URISyntaxException e) {
			log.error("There was an error generating the URI.", e);
			throw new AppException(HttpStatus.SC_BAD_REQUEST, "Malformed URL", URI_EXCEPTION_REASON, e);
		} catch (Exception excp) {
			log.error("Internal server error", excp);
			throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", INERNAL_SERVER_ERROR, excp);
		}
		
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
		
		// Generate the presigned URL
		GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(s3BucketName, s3ObjectKey)
				.withMethod(HttpMethod.valueOf(httpMethod)).withExpiration(expiration);
		try {
			// Attempt to generate the signed S3 URL
			URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
			return url;
		} catch (SdkClientException e) {
			// Catch any SDK client exceptions, and return a 500 error
			log.error("There was an AWS SDK error processing the signing request.", e);
			throw new AppException(HttpStatus.SC_SERVICE_UNAVAILABLE, "Remote Service Unavailable",
					AWS_SDK_EXCEPTION_MSG, e);
		}
	}
	
	 @Override
	  public SignedUrl createSignedUrl(String unsignedUrl, String authorizationToken) {
	    throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Unsupported Operation Exception",UNSUPPORTED_EXCEPTION_REASON);
	  }
	 
	 public Policy createPolicy(String srn, String s3BucketName, String s3ObjectKey) {
	    	
     	
	    	Policy policy = new Policy();
	    	Statement statement = new Statement(Statement.Effect.Allow);
	    	String resource;
	    	
	    	if(srn.toLowerCase().contains("ovds")) {
	    		
	    		resource = String.format("arn:aws:s3:::%s/%s/*",s3BucketName,s3ObjectKey);
	    	}else {
	    		
	    		resource = String.format("arn:aws:s3:::%s/%s",s3BucketName,s3ObjectKey);
	    	}
	    	
	    	statement.withActions(S3Actions.GetObject).withResources(new Resource(resource));
	    	policy.withStatements(statement);
	    	
	       	return policy;
	    }
	    
	    
	    public String  generateConnectionString(Policy policy, String profileName) {
	    	 // If testConnection present implies that it is running under test mode
	    	return (testConnectionString != null) ? testConnectionString : getConnectionString(policy, profileName);	
	    }
	    
	    private String getConnectionString(Policy policy, String profileName) {
	    	
	    	AssumeRoleRequest roleRequest = new AssumeRoleRequest()
	                .withRoleArn("arn:123:456:789:1234")
	                .withRoleSessionName(profileName)
	                .withDurationSeconds(7200)
	                .withPolicy(policy.toJson());
	    	
	    	 AWSCredentials credentials = new BasicAWSCredentials(subuser,
	    			 subpassword); 
	    	 ClientConfiguration clientConfiguration = new ClientConfiguration();
	    	 
	    	 AWSSecurityTokenService stsClient =
	                 AWSSecurityTokenServiceClientBuilder.standard()
	                 .withEndpointConfiguration(new
	                 AwsClientBuilder.EndpointConfiguration("https://"+endpointurl, region))
	                 .withClientConfiguration(clientConfiguration) .withCredentials(new
	                 AWSStaticCredentialsProvider(credentials)) .build();
	    	
	    	
		     AssumeRoleResult response = stsClient.assumeRole(roleRequest);
             Credentials session_creds = response.getCredentials();
		     
		     	       
		     String connectionString = "Region="+region+";"+"AccessKeyId="+session_creds.getAccessKeyId()+";"+"SecretKey="+session_creds.getSecretAccessKey()+";"+"SessionToken="+session_creds.getSessionToken()+";"+"EndpointOverride="+"https://"+endpointurl   ;
	    	 
		     return connectionString;
	    }
	 
	    public String getProfileName() {
		  
		     String profileName=headers.getPartitionId();
		     return profileName;
	    }
	    
	    public void setTestConnectionString(String testConnectionString) {
	    	 this.testConnectionString = testConnectionString;
	    }
}
