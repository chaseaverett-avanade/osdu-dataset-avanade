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