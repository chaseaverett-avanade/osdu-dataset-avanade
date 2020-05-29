// Copyright © Amazon Web Services
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.util;

public class AwsConfig {

    private static final String DEFAULT_CLOUD_STORAGE_REGION="us-east-1";

    public static String getAWSCognitoClientId() {
        return getEnvironmentVariableOrDefaultValue("AWS_COGNITO_CLIENT_ID", "");
    }

    public static String getAWSCognitoAuthFlow() {
        return getEnvironmentVariableOrDefaultValue("AWS_COGNITO_AUTH_FLOW", "");
    }

    public static String getAWSCognitoUser() {
        return getEnvironmentVariableOrDefaultValue("AWS_COGNITO_AUTH_PARAMS_USER", "");
    }

    public static String getAWSCognitoPassword() {
        return getEnvironmentVariableOrDefaultValue("AWS_COGNITO_AUTH_PARAMS_PASSWORD", "");
    }


    public static String getCloudStorageRegion() {
        return getEnvironmentVariableOrDefaultValue("AWS_S3_REGION", DEFAULT_CLOUD_STORAGE_REGION);
    }

    private static String getEnvironmentVariableOrDefaultValue(String key, String defaultValue) {
        String environmentVariable = getEnvironmentVariable(key);
        if (environmentVariable == null) {
            environmentVariable = defaultValue;
        }
        return environmentVariable;
    }

    private static String getEnvironmentVariable(String propertyKey) {
        return System.getProperty(propertyKey, System.getenv(propertyKey));
    }
}
