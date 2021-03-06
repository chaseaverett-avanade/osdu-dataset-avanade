/*
 * Copyright 2021 Google LLC
 * Copyright 2021 EPAM Systems, Inc
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

package org.opengroup.osdu.dataset.provider.gcp.config;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.opengroup.osdu.core.gcp.multitenancy.GcsMultiTenantAccess;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GcpStorageFactory {

	@Bean
	public Storage googleCloudStorage() {
		return StorageOptions.getDefaultInstance().getService();
	}

	@Bean
	public GcsMultiTenantAccess gcsMultiTenantAccess() {
		return new GcsMultiTenantAccess();
	}
}
