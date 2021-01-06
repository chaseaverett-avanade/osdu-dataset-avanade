// Copyright © Microsoft Corporation
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

public class AzureTestConfig {

    public static String getsp_id() {
        return System.getProperty("INTEGRATION_TESTER", System.getenv("INTEGRATION_TESTER"));
    }

    public static String getsp_secret() {
        return System.getProperty("TESTER_SERVICEPRINCIPAL_SECRET", System.getenv("TESTER_SERVICEPRINCIPAL_SECRET"));
    }

    public static String getTenant_id() {
        return System.getProperty("AZURE_AD_TENANT_ID", System.getenv("AZURE_AD_TENANT_ID"));
    }

    public static String getApp_resource_id() {
        return System.getProperty("AZURE_AD_APP_RESOURCE_ID", System.getenv("AZURE_AD_APP_RESOURCE_ID"));
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