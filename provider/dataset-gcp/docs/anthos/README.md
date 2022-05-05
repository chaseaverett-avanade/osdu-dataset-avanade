# Service Configuration for Anthos

## Table of Contents <a name="TOC"></a>
* [Environment variables](#Environment-variables)
    * [Common properties for all environments](#Common-properties-for-all-environments)
    * [For OSM Postgres](#For-OSM-Postgres)

## Environment variables

Define the following environment variables.

Must have:

| name | value | description | sensitive? | source |
| ---  | ---   | ---         | ---        | ---    |
| `SPRING_PROFILES_ACTIVE` | ex `anthos` | Spring profile that activate default configuration for GCP environment | false | - |
| `<POSTGRES_PASSWORD_ENV_VARIABLE_NAME>` | ex `POSTGRES_PASS_OSDU` | Postgres password env name, name of that variable not defined at the service level, the name will be received through partition service. Each tenant can have it's own ENV name value, and it must be present in ENV of Dataset service | yes | - |

### Common properties for all environments

| name | value | description | sensitive? | source |
| ---  | ---   | ---         | ---        | ---    |
| `LOG_PREFIX` | `dataset` | Logging prefix | no | - |
| `SERVER_SERVLET_CONTEXPATH` | `/api/storage/v2/` | Servlet context path | no | - |
| `AUTHORIZE_API` | ex `https://entitlements.com/entitlements/v1` | Entitlements API endpoint | no | output of infrastructure deployment |
| `PARTITION_API` | ex `http://localhost:8081/api/partition/v1` | Partition service endpoint | no | - |
| `STORAGE_API` | ex `http://storage/api/legal/v1` | Storage API endpoint | no | output of infrastructure deployment |
| `SCHEMA_API` | ex `http://schema/api/legal/v1` | Schema API endpoint | no | output of infrastructure deployment |
| `REDIS_GROUP_HOST` |  ex `127.0.0.1` | Redis host for groups | no | https://console.cloud.google.com/memorystore/redis/instances |
| `REDIS_GROUP_PORT` |  ex `1111` | Redis port | no | https://console.cloud.google.com/memorystore/redis/instances |
| `DMS_API_BASE` | ex `http://localhost:8081/api/file/v2/files` | *Only for local usage.* Allows to override DMS service base url value from Datastore.  | no | - |

These variables define service behavior, and are used to switch between `anthos` or `gcp` environments, their overriding and usage in mixed mode was not tested.
Usage of spring profiles is preferred.

| name | value | description | sensitive? | source |
| ---  | ---   | ---         | ---        | ---    |
| `PARTITION_AUTH_ENABLED` | ex `true` or `false` | Disable or enable auth token provisioning for requests to Partition service | no | - |
| `OSMDRIVER` | `postgres`| Osm driver mode that defines which KV storage will be used | no | - |
| `OQMDRIVER` | `rabbitmq` | Oqm driver mode that defines which message broker will be used | no | - |
| `SERVICE_TOKEN_PROVIDER` | `GCP` or `OPENID` |Service account token provider, `GCP` means use Google service account `OPEIND` means use OpenId provider like `Keycloak` | no | - |


### Properties set in Partition service:

Note that properties can be set in Partition as `sensitive` in that case in property `value` should be present **not value itself**, but **ENV variable name**.
This variable should be present in environment of service that need that variable.

Example:
```
    "elasticsearch.port": {
      "sensitive": false, <- value not sensitive 
      "value": "9243"  <- will be used as is.
    },
      "elasticsearch.password": {
      "sensitive": true, <- value is sensitive 
      "value": "ELASTIC_SEARCH_PASSWORD_OSDU" <- service consumer should have env variable ELASTIC_SEARCH_PASSWORD_OSDU with elastic search password
    }
```

### For OSM Postgres
As a quick shortcut, this example snippet can be used by DevOps DBA:

```

CREATE TABLE public."DmsServiceProperties"(
id text COLLATE pg_catalog."default" NOT NULL,
pk bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
data jsonb NOT NULL,
CONSTRAINT DmsServiceProperties_id UNIQUE (id)
);
CREATE INDEX DmsServiceProperties_datagin ON public."DmsServiceProperties" USING GIN (data);

```

There must be a table `DmsServiceProperties` in default schema, with DMS configuration,
Example:

| name | apiKey | dmsServiceBaseUrl | isStagingLocationSupported | isStorageAllowed |
| ---  | ---   |---| ---        | ---    |
| `name=dataset--File.*` |   | `https://osdu-anthos.osdu.club/api/file/v2/files` | `true` | `true` |
| `name=dataset--FileCollection.*` |   | `https://osdu-anthos.osdu.club/api/file/v2/file-collections` | `true` | `true` |

You can use the `INSERT` script below to bootstrap the data with valid records:
```roomsql
INSERT INTO public."DmsServiceProperties"(id, data)
	VALUES 
	('dataset--File.*', 
	'{
	  "apiKey": "",
	  "datasetKind": "dataset--File.*",
	  "isStorageAllowed": true,
	  "dmsServiceBaseUrl": "https://osdu-anthos.osdu.club/api/file/v2/files",
	  "isStagingLocationSupported": true
	}'),
	
	('dataset--FileCollection.*', 
	'{
	  "apiKey": "",
	  "datasetKind": "dataset--FileCollection.*",
	  "isStorageAllowed": true,
	  "dmsServiceBaseUrl": "https://osdu-anthos.osdu.club/api/file/v2/file-collections",
	  "isStagingLocationSupported": true
	}');
```

**prefix:** `osm.postgres`
It can be overridden by:

- through the Spring Boot property `osm.postgres.partition-properties-prefix`
- environment variable `OSM_POSTGRES_PARTITION_PROPERTIES_PREFIX`

**Propertyset:**

| Property | Description |
| --- | --- |
| osm.postgres.datasource.url | server URL |
| osm.postgres.datasource.username | username |
| osm.postgres.datasource.password | password |

<details><summary>Example of a definition for a single tenant</summary>

```

curl -L -X PATCH 'https://api/partition/v1/partitions/opendes' -H 'data-partition-id: opendes' -H 'Authorization: Bearer ...' -H 'Content-Type: application/json' --data-raw '{
  "properties": {
    "osm.postgres.datasource.url": {
      "sensitive": false,
      "value": "jdbc:postgresql://127.0.0.1:5432/postgres"
    },
    "osm.postgres.datasource.username": {
      "sensitive": false,
      "value": "postgres"
    },
    "osm.postgres.datasource.password": {
      "sensitive": true,
      "value": "<POSTGRES_PASSWORD_ENV_VARIABLE_NAME>" <- (Not actual value, just name of env variable)
    }
  }
}'

```

</details>