# Service Configuration for GCP

## Table of Contents <a name="TOC"></a>
* [Environment variables](#Environment-variables)
* [Common properties for all environments](#Common-properties-for-all-environments)
* [Datastore configuration](#Datastore-configuration)
* [Google cloud service account configuration](#Google-cloud-service-account-configuration)

## Environment variables

Define the following environment variables.

Must have:

| name | value | description | sensitive? | source |
| ---  | ---   | ---         | ---        | ---    |
| `GOOGLE_AUDIENCES` | ex `*****.apps.googleusercontent.com` | Client ID for getting access to cloud resources | yes | https://console.cloud.google.com/apis/credentials |
| `SPRING_PROFILES_ACTIVE` | ex `gcp` | Spring profile that activate default configuration for GCP environment | false | - |

### Common properties for all environments

| name | value | description | sensitive? | source |
| ---  | ---   | ---         | ---        | ---    |
| `LOG_PREFIX` | `dataset` | Logging prefix | no | - |
| `SERVER_SERVLET_CONTEXPATH` | `/api/storage/v2/` | Servlet context path | no | - |
| `AUTHORIZE_API` | ex `https://entitlements.com/entitlements/v1` | Entitlements API endpoint | no | output of infrastructure deployment |
| `PARTITION_API` | ex `http://localhost:8081/api/partition/v1` | Partition service endpoint | no | - |
| `STORAGE_API` | ex `http://storage/api/legal/v1` | Storage API endpoint | no | output of infrastructure deployment |
| `SCHEMA_API` | ex `http://schema/api/legal/v1` | Schema API endpoint | no | output of infrastructure deployment |
| `GOOGLE_APPLICATION_CREDENTIALS` | ex `/path/to/directory/service-key.json` | Service account credentials, you only need this if running locally | yes | https://console.cloud.google.com/iam-admin/serviceaccounts |
| `REDIS_GROUP_HOST` |  ex `127.0.0.1` | Redis host for groups | no | https://console.cloud.google.com/memorystore/redis/instances |
| `REDIS_GROUP_PORT` |  ex `1111` | Redis port | no | https://console.cloud.google.com/memorystore/redis/instances |
| `DMS_API_BASE` | ex `http://localhost:8081/api/file/v2/files` | *Only for local usage.* Allows to override DMS service base url value from Datastore. | no | - |

These variables define service behavior, and are used to switch between `anthos` or `gcp` environments, their overriding and usage in mixed mode was not tested.
Usage of spring profiles is preferred.

| name | value | description | sensitive? | source |
| ---  | ---   | ---         | ---        | ---    |
| `PARTITION_AUTH_ENABLED` | ex `true` or `false` | Disable or enable auth token provisioning for requests to Partition service | no | - |
| `OSMDRIVER` | `postgres`| Osm driver mode that defines which KV storage will be used | no | - |
| `OQMDRIVER` | `rabbitmq` | Oqm driver mode that defines which message broker will be used | no | - |
| `SERVICE_TOKEN_PROVIDER` | `GCP` or `OPENID` |Service account token provider, `GCP` means use Google service account `OPEIND` means use OpenId provider like `Keycloak` | no | - |

## Datastore configuration

There must be a kind `DmsServiceProperties` in default namespace, with DMS configuration, 
Example:

| name | apiKey | dmsServiceBaseUrl | isStagingLocationSupported | isStorageAllowed |
| ---  | ---   | ---         | ---        | ---    |
| `name=dataset--File.*` |   | `https://community.gcp.gnrg-osdu.projects.epam.com/api/file/v2/files` | `true` | `true` |
| `name=dataset--FileCollection.*` |   | `https://community.gcp.gnrg-osdu.projects.epam.com/api/file/v2/file-collections` | `true` | `true` |

## Google cloud service account configuration
TBD

| Required roles |
| ---    |
| - |

### Running E2E Tests
This section describes how to run cloud OSDU E2E tests (testing/dataset-test-gcp).

You will need to have the following environment variables defined.

| name | value | description | sensitive? | source |
 | ---  | ---   | ---         | ---        | ---    |
| `DOMAIN` | ex `osdu-gcp.go3-nrg.projects.epam.com` | - | no | - |
| `STORAGE_BASE_URL` | ex `https://os-storage-jvmvia5dea-uc.a.run.app/api/storage/v2/` | Storage API endpoint | no | output of infrastructure deployment |
| `LEGAL_BASE_URL` | ex `https://os-legal-jvmvia5dea-uc.a.run.app/api/legal/v1/` | Legal API endpoint | no | output of infrastructure deployment |
| `DATASET_BASE_URL` | ex `http://localhost:8080/api/dataset/v1/` | Dataset API endpoint | no | output of infrastructure deployment |
| `SCHEMA_API` | ex `https://os-schema-jvmvia5dea-uc.a.run.app/api/schema-service/v1` | Schema API endpoint | no | output of infrastructure deployment |
| `PROVIDER_KEY` | `GCP` | required for response verification | no | - |
| `INTEGRATION_TEST_AUDIENCE` | ex `****.apps.googleusercontent.com;` | Client application ID | yes | https://console.cloud.google.com/apis/credentials |
| `INTEGRATION_TESTER` | `********` | Service account for API calls, passed as a filename or JSON content, plain or Base64 encoded.  Note: this user must have entitlements configured already | yes | https://console.cloud.google.com/iam-admin/serviceaccounts |
| `GCP_DEPLOY_FILE` | `********` | Service account for test data tear down, passed as a filename or JSON content, plain or Base64 encoded. Must have cloud storage role configured | yes | https://console.cloud.google.com/iam-admin/serviceaccounts |
| `TENANT_NAME` | `opendes` | Tenant name | no | - |
| `KIND_SUBTYPE` | `DatasetTest` | Kind subtype that will be used in int tests, schema creation automated , result kind will be `TENANT_NAME::wks-test:dataset--FileCollection.KIND_SUBTYPE:1.0.0`| no | - |
| `LEGAL_TAG` | `public-usa-dataset-1` | Legal tag name, if tag with that name doesn't exist then it will be created during preparing step | no | - |
| `GCLOUD_PROJECT` | `osdu-cicd-epam` | Project id | no | - |
| `GCP_STORAGE_PERSISTENT_AREA` | ex `persistent-area` | persistent area bucket(will be concatenated with project id ex `osdu-cicd-epam-persistent-area` | no | output of infrastructure deployment |
| `LEGAL_HOST` | ex `https://os-legal-jvmvia5dea-uc.a.run.app/api/legal/v1/` | Legal API endpoint | no | output of infrastructure deployment |

**Entitlements configuration for integration accounts**

| INTEGRATION_TESTER | 
 | ---  | 
| users<br/>service.entitlements.user<br/>service.storage.admin<br/>service.legal.user<br/>service.search.user<br/>service.delivery.viewer<br/>service.dataset.viewers<br/>service.dataset.editors | 

**Cloud roles configuration for integration accounts**

| GCP_DEPLOY_FILE|
 | ---  |
| storage.admin access to the Google Cloud Storage |

Execute following command to build code and run all the integration tests:

 ```bash
 # Note: this assumes that the environment variables for integration tests as outlined
 #       above are already exported in your environment.
 # build + install integration test core
 $ (cd testing/dataset-test-core/ && mvn clean install)
 ```
 ```bash
 # build + run GCP integration tests.
 $ (cd testing/dataset-test-gcp/ && mvn clean test)
 ```
