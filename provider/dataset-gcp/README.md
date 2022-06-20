# Dataset registry service

## Getting Started
These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

* GCloud SDK with java (latest version)
* JDK 8
* Lombok 1.16 or later
* Maven

# Features of implementation
This is a universal solution created using EPAM OSM and OBM mappers technology.
It allows you to work with various implementations of KV stores and Blob stores.

## Limitations of the current version

In the current version, the mappers are equipped with several drivers to the stores:

- OSM (mapper for KV-data): Google Datastore; Postgres
- OBM (mapper to Blob stores): Google Cloud Storage (GCS); MinIO

## Extensibility

To use any other store or message broker, implement a driver for it. With an extensible set of drivers, the solution is unrestrictedly universal and portable without modification to the main code.

Mappers support "multitenancy" with flexibility in how it is implemented.
They switch between datasources of different tenants due to the work of a bunch of classes that implement the following interfaces:

- Destination - takes a description of the current context, e.g., "data-partition-id = opendes"
- DestinationResolver – accepts Destination, finds the resource, connects, and returns Resolution
- DestinationResolution – contains a ready-made connection, the mapper uses it to get to data

## Mapper tuning mechanisms

This service uses specific implementations of DestinationResolvers based on the tenant information provided by the OSDU Partition service.
- for Google Datastore: osm/config/DsTenantDestinationResolver.java
- for Postgres: osm/config/PgTenantDestinationResolver.java

#### Their algorithms are as follows:
- incoming Destination carries data-partition-id
- resolver accesses the Partition service and gets PartitionInfo
- from PartitionInfo resolver retrieves properties for the connection: URL, username, password etc.
- resolver creates a data source, connects to the resource, remembers the datasource
- resolver gives the datasource to the mapper in the Resolution object
- Google Cloud resolvers do not receive special properties from the Partition service for connection, 
because the location of the resources is unambiguously known - they are in the GCP project. 
And credentials are also not needed - access to data is made on behalf of the Google Identity SA
under which the service itself is launched. Therefore, resolver takes only 
the value of the **projectId** property from PartitionInfo and uses it to connect to a resource 
in the corresponding GCP project.

# Configuration

## Service Configuration

Define the following environment variables.
Most of them are common to all hosting environments, but there are properties that are only necessary when running in Google Cloud.

### Anthos Service Configuration:
[Anthos service configuration ](docs/anthos/README.md)
### GCP Service Configuration:
[Gcp service configuration ](docs/gcp/README.md)

### Run Locally
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
* Update the Google cloud SDK to the latest version:

```bash
gcloud components update
```
* Set Google Project Id:

```bash
gcloud config set project <YOUR-PROJECT-ID>
```

* Perform a basic authentication in the selected project:

```bash
gcloud auth application-default login
```

* Navigate to Dataset service root folder and run:

```bash
mvn clean install   
```

* If you wish to build the project without running tests

```bash
mvn clean install -DskipTests
```

After configuring your environment as specified above, you can follow these steps to build and run the application. These steps should be invoked from the *repository root.*

```bash
cd provider/dataset-gcp && mvn spring-boot:run
```
## Testing

 ### Running E2E Tests
 This section describes how to run cloud OSDU E2E tests.
 
 ### Anthos test configuration:
 [Anthos service configuration ](docs/anthos/README.md)
 ### GCP test configuration:
 [Gcp service configuration ](docs/gcp/README.md)

## Deployment

* To deploy into Cloud run, please, use this documentation:
https://cloud.google.com/run/docs/quickstarts/build-and-deploy

* To deploy into App Engine, please, use this documentation:
https://cloud.google.com/appengine/docs/flexible/java/quickstart

## License

Copyright 2021 Google LLC

Copyright 2021 EPAM Systems, Inc

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.