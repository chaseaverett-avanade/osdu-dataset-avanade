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

package org.opengroup.osdu.dataset.anthos.configuration;

import java.util.Objects;
import lombok.Getter;

@Getter
public class MinioConfig {

    private static MinioConfig minioConfig;

    private String minioEndpoint;

    private String minioAccessKey;

    private String minioSecretKey;

    public static MinioConfig Instance() {
        if (Objects.isNull(minioConfig)) {
            minioConfig = new MinioConfig();
            minioConfig.minioEndpoint = System.getProperty("TEST_MINIO_ENDPOINT", System.getenv("TEST_MINIO_ENDPOINT"));
            minioConfig.minioAccessKey = System.getProperty("TEST_MINIO_ACCESS_KEY", System.getenv("TEST_MINIO_ACCESS_KEY"));
            minioConfig.minioSecretKey = System.getProperty("TEST_MINIO_SECRET_KEY", System.getenv("TEST_MINIO_SECRET_KEY"));
        }
        return minioConfig;
    }
}
