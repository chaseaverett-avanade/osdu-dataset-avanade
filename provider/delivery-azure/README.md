# Delivery-Azure

## Requirements

In order to run this service locally, you will need the following:

- [Maven 3.6.0+](https://maven.apache.org/download.cgi)
- [AdoptOpenJDK8](https://adoptopenjdk.net/)
- [OSDU on Azure infrastructure](https://community.opengroup.org/osdu/platform/deployment-and-operations/infra-azure-provisioning) deployed

## Service Dependencies
- [Search](https://community.opengroup.org/osdu/platform/system/search-service)
- [Partition](https://community.opengroup.org/osdu/platform/system/partition)
- [Entitlements](https://community.opengroup.org/osdu/platform/security-and-compliance/entitlements-azure)

## Entitlements

| Endpoint | Request Type | Entitlement Required
| ---  | ---   | --- |
| `/GetFileSignedUrl` | `POST` | `service.delivery.viewer` |

## General Tips

**Environment Variable Management**
The following tools make environment variable configuration simpler
 - [direnv](https://direnv.net/) - for a shell/terminal environment
 - [EnvFile](https://plugins.jetbrains.com/plugin/7861-envfile) - for [Intellij IDEA](https://www.jetbrains.com/idea/)

**Lombok**
This project uses [Lombok](https://projectlombok.org/) for code generation. You may need to configure your IDE to take advantage of this tool.
 - [Intellij configuration](https://projectlombok.org/setup/intellij)
 - [VSCode configuration](https://projectlombok.org/setup/vscode)


## Environment Variables

In order to run the service locally, you will need to have the following environment variables defined. We have created a helper script to generate .yaml files to set the environment variables to run and test the service using the InteliJ IDEA plugin and generate a .envrc file to set the environment variables to run and test the service using direnv [here](https://community.opengroup.org/osdu/platform/deployment-and-operations/infra-azure-provisioning/-/blob/master/tools/variables/delivery.sh).

**Note** The following command can be useful to pull secrets from keyvault:
```bash
az keyvault secret show --vault-name $KEY_VAULT_NAME --name $KEY_VAULT_SECRET_NAME --query value -otsv
```

**Required to run service**

| name | value | description | sensitive? | source |
| ---  | ---   | ---         | ---        | ---    |
| `AZURE_TENANT_ID` | `******` | AD tenant to authenticate users from | yes | `tenant-id` in environment keyvault |
| `AZURE_CLIENT_ID` | `******` | Identity to run the service locally. This enables access to Azure resources. You only need this if running locally | yes | Value of `app-dev-sp-username` in environment keyvault |
| `AZURE_CLIENT_SECRET` | `******` | Secret for `AZURE_CLIENT_ID` | yes | Value of `app-dev-sp-password` in environment keyvault |
| `aad_client_id` | `******` | AAD client application ID | yes | Value of `aad-client-id` in environment keyvault |
| `appinsights_key` | `******` | API Key for App Insights | yes | Value of `appinsights-key` in environment keyvault |
| `KEYVAULT_URI` | ex: `https://osdu-mvp-<your_environment_name_here>-xxxx-kv.vault.azure.net/` | URL for keyvault that holds environment secrets. Deployed as part of infrastructure. | no | URL of environment keyvault | Check central_resources resource group in Azure Portal |
| `AUTHORIZE_API` | ex: `https://osdu-dev-abc-test.org/entitlements/v1` | URL of Entitlements service | no | Append `/entitlements/v1` to environment URL. Note the lack of trailing `/` in the URL |
| `partition_service_endpoint` | ex: `https://osdu-dev-abc-test.org/api/partition/v1/` | URL of Partition service | no | Append `/api/partition/v1/` to environment URL. Note the trailing `/` in the URL |
| `SEARCH_HOST` | ex: `https://osdu-dev-abc-test.org/api/search/v2/` | URL of Search service | no | Append `/api/search/v2/` to environment URL. Note the trailing `/` in the URL |
| `BATCH_SIZE` | ex: `100` | Batch size for Search service queries | no | User-configurable |
| `SEARCH_QUERY_LIMIT` | ex: `1000` | Limit on number of results returned from Search service | no | User-configurable |
| `azure_istioauth_enabled` | `true` (depends on if service is running in Kubernetes environment with Istio installed) | Configuring use of Istio | no | Set to true when deploying the service into a Kubernetes cluster with Istio configured. Set to false and uncomment the three lines [here](https://community.opengroup.org/osdu/platform/system/delivery/-/blob/master/provider/delivery-azure/src/main/resources/application.properties) defining `azure.activedirectory.client-id`, `azure.activedirectory.AppIdUri`, and `azure.activedirectory.session-stateless=true` if running locally |
| `server_port` | ex: `8080` | Port for server to run on | no | User-configurable |

**Required to run integration tests**

| name | value | description | sensitive? | source |
| ---  | ---   | ---         | ---        | ---    |
| `AZURE_AD_TENANT_ID` | `******` | AD tenant to authenticate users from | yes | Run `az account show --query tenantId -otsv` |
| `INTEGRATION_TESTER` | `******` | System identity to assume for API calls. | yes | Value of `app-dev-sp-username` in environment keyvault |
| `TESTER_SERVICEPRINCIPAL_SECRET` | `******` | Secret for `INTEGRATION_TESTER` | yes | Value of `app-dev-sp-password` in environment keyvault  |
| `AZURE_STORAGE_ACCOUNT` | `******` | Storage account name | yes | Value of `opendes-storage` in environment keyvault |
| `AZURE_AD_APP_RESOURCE_ID` | `******` | AAD client application ID | yes | Value of `aad-client-id` in environment keyvault |
| `DELIVERY_HOST` | `https://osdu-dev-abc-test.org/api/delivery/v2/` | URL of Delivery service | no | Append `/api/delivery/v2/` to environment URL (will be localhost:8080 if running locally). Note the trailing `/` in the URL |
| `LEGAL_HOST` | `https://osdu-dev-abc-test.org/api/legal/v1/` | URL of Legal service | no | Append `/api/legal/v1/` to environment URL. Note the trailing `/` in the URL |
| `SEARCH_HOST` | `https://osdu-dev-abc-test.org/api/search/v2/` | URL of Search service | no | Append `/api/search/v2/` to environment URL. Note the trailing `/` in the URL |
| `TENANT_NAME` | `opendes` | Name of data partition | no | Constant |
| `DEFAULT_DATA_PARTITION_ID_TENANT1` | `opendes` | Name of data partition | no | Constant |
| `DOMAIN` | `contoso.com` | URL of Search service | no | Append `/api/search/v2/` to environment URL. Note the trailing `/` in the URL |
| `ENTITLEMENTS_DOMAIN` | `contoso.com` | Domain for requests to Storage service | no | Constant |
| `LEGAL_TAG` | `opendes-public-usa-dataset-7643990` | Legal tag for requests to storage service | no | Constant (legal tag uploaded as test data) |
| `OTHER_RELEVANT_DATA_COUNTRIES` | `US` | Used for legal tags | no | Constant |

## Running Locally

### Configure Maven

Check that maven is installed:
```bash
$ mvn --version
Apache Maven 3.6.0
Maven home: /usr/share/maven
Java version: 1.8.0_212, vendor: AdoptOpenJDK, runtime: /usr/lib/jvm/jdk8u212-b04/jre
...
```

### Build and run the application

After configuring your environment as specified above, you can follow these steps to build and run the application. These steps should be invoked from the repository root.

```bash
# build + test + install core service code from repository root
$ (cd delivery-core && mvn clean install)

# build + test + package azure service code
$ (cd provider/delivery-azure/ && mvn clean package)

# run service from repository root
#
# Note: this assumes that the environment variables for running the service as outlined above are already exported in your environment.
$ java -jar $(find provider/delivery-azure/target/ -name '*-spring-boot.jar')

```

### Test the Application

_After the service has started it should be accessible via a web browser by visiting [http://localhost:8080/api/delivery/v2/swagger](http://localhost:8080/api/delivery/v2/swagger). If the request does not fail, you can then run the integration tests._

```bash
# build + install integration test core
$ (cd testing/delivery-test-core/ && mvn clean install)

# build + run Azure integration tests.
#
# Note: this assumes that the environment variables for integration tests as outlined above are already exported in your environment.
$ (cd testing/delivery-test-azure/ && mvn clean test)
```

### Debugging

Jet Brains - the authors of Intellij IDEA, have written an [excellent guide](https://www.jetbrains.com/help/idea/debugging-your-first-java-application.html) on how to debug java programs.

## License
Copyright © Microsoft Corporation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
