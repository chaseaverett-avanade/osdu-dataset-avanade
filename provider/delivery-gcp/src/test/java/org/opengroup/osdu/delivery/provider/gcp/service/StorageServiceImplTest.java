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

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.SignUrlOption;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.delivery.DeliveryApplication;
import org.opengroup.osdu.delivery.model.SignedUrl;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest(classes = {DeliveryApplication.class})
public class StorageServiceImplTest {

	@Mock
	private InstantHelper instantHelper;

	@Mock
	private Storage storage;

	@InjectMocks
	private StorageServiceImpl storageService;

	private String bucketName = "osdu-sample-osdu-file";
	private String key = "/common-user/1590050272122-2020-05-21-08-37-52-122/cebdd5780fc74f24b518c9676160136f";
	private String unsignedUrl = "gs://" + bucketName + "/" + key;

	@Test
	public void createSignedUrl() throws IOException, URISyntaxException {

		URL url = new URL("http://testsignedurl.com");

		Mockito.when(storage
			.signUrl(Mockito.any(BlobInfo.class),
				Mockito.any(Long.class),
				Mockito.any(TimeUnit.class),
				Mockito.any(SignUrlOption.class),
				Mockito.any(SignUrlOption.class)))
			.thenReturn(url);

		Instant instant = Instant.now();
		Mockito.when(instantHelper.getCurrentInstant()).thenReturn(instant);

		SignedUrl expected = new SignedUrl();
		expected.setUri(new URI(url.toString()));
		expected.setUrl(url);
		expected.setCreatedAt(instant);

		SignedUrl actual = storageService.createSignedUrl(unsignedUrl, null);

		Assert.assertEquals(expected, actual);
	}

	@Test
	public void createSignedUrl_malformedUnsignedUrl_throwsAppException() {
		try {
			String unsignedUrl = "malformedUrlString";
			String authorizationToken = "testAuthorizationToken";

			storageService.createSignedUrl(unsignedUrl, authorizationToken);

			fail("Should not succeed!");
		} catch (AppException e) {
			assertEquals(HttpStatus.SC_BAD_REQUEST, e.getError().getCode());
			assertEquals("Malformed URL", e.getError().getReason());
			assertEquals( "Unsigned url invalid, needs to be full GS path", e.getError().getMessage());
		} catch (Exception e) {
			fail("Should not get different exception");
		}
	}
}