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

import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.FileInputStream;
import java.io.IOException;

public class StorageServiceAccountCredentialsProvider {

	private static GoogleCredentials credentials = null;

	public static Credentials getCredentials() {
		try {
			credentials = GoogleCredentials
				.fromStream(new FileInputStream(GcpConfig.getStorageAccount()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return credentials;
	}
}