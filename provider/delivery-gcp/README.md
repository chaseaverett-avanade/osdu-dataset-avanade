# delivery-gcp

## Running Locally

### Requirements

In order to run this service locally, you will need the following:

- [Maven 3.6.0+](https://maven.apache.org/download.cgi)
- [AdoptOpenJDK8](https://adoptopenjdk.net/)
- Infrastructure dependencies, deployable through the relevant [infrastructure template](https://community.opengroup.org/osdu/platform/deployment-and-operations/infra-gcp-provisioning)

### Environment Variables

In order to run the service locally, you will need to have the following environment variables defined.

| name | value | description | sensitive? | source |
| ---  | ---   | ---         | ---        | ---    |
| `LOG_PREFIX` | `delivery` | Logging prefix | no | - |
| `GCP_REDIS_HOST` | `localhost` | Redis host | no | - |
| `GCP_SEARCH_QUERY_URL` | ex `http://localhost:8080/api/search/v2/query` | Search query endpoint | no | - |
| `GCP_ENTITLEMENTS_URL` | ex `http://localhost:8080/entitlements/v1` |  Entitlements API endpoint | no | - |

**Required to run integration tests**

| name | value | description | sensitive? | source |
| ---  | ---   | ---         | ---        | ---    |
| `DELIVERY_HOST` | `http://localhost:8080/api/delivery/v2/` | Delivery endpoint	 | no | - |
| `SEARCH_HOST` | `http://localhost:8080/api/search/v2/` | Search endpoint | no | - |
| `STORAGE_HOST` | `http://localhost:8080/api/storage/v2/` | Storage endpoint | no | - |
| `ENTITLEMENTS_DOMAIN` | `example.com` | OSDU R2 to run tests under	 | no | - |
| `TENANT` | `opendes` | Tenant name | no | - |
| `DEFAULT_DATA_PARTITION_ID_TENANT1` | `opendes` | Data partition id | no | - |
| `DEFAULT_DATA_PARTITION_ID_TENANT2` | `opendes` | Data partition id | no | - |
| `SEARCH_INTEGRATION_TESTER` | `********` | Service account for API calls, passed as a filename or JSON content, plain or Base64 encoded.  Note: this user must have entitlements configured already | yes | https://console.cloud.google.com/iam-admin/serviceaccounts |
| `INTEGRATION_TEST_AUDIENCE` | `********` | Client application ID | yes | https://console.cloud.google.com/apis/credentials |
| `LEGAL_TAG` | ex `opendes-osdu-demo-legaltag` | Currently existing, not expired legal tag name| no | create one with  POST {{legal_url}}api/legal/v1/legaltags |
| `OTHER_RELEVANT_DATA_COUNTRIES` | ex `US` | - | no | - |
| `GCLOUD_PROJECT` | ex `osdu-cicd-epam` | Google cloud project id | no | - |
| `GCP_DEPLOY_FILE` | `********` | Service account for test data tear down, passed as a filename or JSON content, plain or Base64 encoded. Must have cloud storage role configured | yes | https://console.cloud.google.com/iam-admin/serviceaccounts |

**Entitlements configuration for integration accounts**

| SEARCH_INTEGRATION_TESTER | 
| ---  | 
| users<br/>service.entitlements.user<br/>service.storage.admin<br/>service.legal.user<br/>service.search.user<br/>service.delivery.viewer | 

**Cloud roles configuration for integration accounts**

| GCP_DEPLOY_FILE|
| ---  |
| storage.admin access to the Google Cloud Storage |

### Configure Maven

Check that maven is installed:
```bash
$ mvn --version
Apache Maven 3.6.0
Maven home: /usr/share/maven
Java version: 1.8.0_212, vendor: AdoptOpenJDK, runtime: /usr/lib/jvm/jdk8u212-b04/jre
...
```

You may need to configure access to the remote maven repository that holds the OSDU dependencies. This file should live within `~/.mvn/community-maven.settings.xml`:
```bash
$ cat ~/.m2/settings.xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
    <servers>
        <server>
            <id>community-maven-via-private-token</id>
            <!-- Treat this auth token like a password. Do not share it with anyone, including Microsoft support. -->
            <!-- The generated token expires on or before 11/14/2019 -->
             <configuration>
              <httpHeaders>
                  <property>
                      <name>Private-Token</name>
                      <value>${env.COMMUNITY_MAVEN_TOKEN}</value>
                  </property>
              </httpHeaders>
             </configuration>
        </server>
    </servers>
</settings>
```
### Build and run the application

After configuring your environment as specified above, you can follow these steps to build and run the application. These steps should be invoked from the *repository root.*
```bash
cd provider/delivery-gcp/ && mvn spring-boot:run
```
### Test the application

After the service has started it should be accessible via a web browser by visiting [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html). If the request does not fail, you can then run the integration tests.

```bash
# build + install integration test core
$ (cd testing/delivery-test-core/ && mvn clean install)

# build + run GCP integration tests.
#
# Note: this assumes that the environment variables for integration tests as outlined
#       above are already exported in your environment.
$ (cd testing/delivery-test-gcp/ && mvn clean test)
```

## License
  Copyright 2020 Google LLC
  Copyright 2020 EPAM Systems, Inc

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
