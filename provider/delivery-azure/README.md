## Delivery Service
The Delivery Service is responsible for authenticating an OSDU user, and, if they have access, returns a signed URL to download the data file.
The Delivery Service is a [Spring Boot](https://spring.io/projects/spring-boot) service.

## License
Copyright 2017-2020, Schlumberger, Microsoft Corporation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at 

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

## Build
All references on repositories settings are external to `pom.xml` and should be configured through Maven `settings.xml` file.
To build against Community GitLab repositories, use `.mvn/community-maven.settings.xml` settings:
`mvn clean compile test --settings .mvn/community-maven.settings.xml`

## Publish
All references on Binary Storage required to publish Maven artifacts are external to `pom.xml` and should be configured through Maven `settings.xml` file.
There are two profiles available in `.mvn/community-maven.settings.xml` that can be used to publish artifacts to Community GitLab:
1. `GitLab-Authenticate-With-Job-Token` - default one, and should be used in CICD pipelines
2. `GitLab-Authenticate-With-Private-Token` -  profile for local development and manual publishing of artifacts. To activate the profile, the developer should have `COMMUNITY_MAVEN_TOKEN` env variable with a configured personal GitLab access token. Please see [GitLab documentation](https://docs.gitlab.com/ee/user/profile/personal_access_tokens.html) on how to set-up Personal Access token in GitLab:
```
export COMMUNITY_MAVEN_TOKEN='Your personal GitLab access token'
mvn deploy --settings .mvn/community-maven.settings.xml
```

## Running Locally

### Requirements

In order to run this service locally, you will need the following:

- [Maven 3.6.0+](https://maven.apache.org/download.cgi)
- [AdoptOpenJDK8](https://adoptopenjdk.net/)
- Infrastructure dependencies, deployable through the relevant [infrastructure template](https://dev.azure.com/slb-des-ext-collaboration/open-data-ecosystem/_git/infrastructure-templates?path=%2Finfra&version=GBmaster&_a=contents)
- While not a strict dependency, example commands in this document use [bash](https://www.gnu.org/software/bash/)

### General Tips

**Environment Variable Management**
The following tools make environment variable configuration simpler
 - [direnv](https://direnv.net/) - for a shell/terminal environment
 - [EnvFile](https://plugins.jetbrains.com/plugin/7861-envfile) - for [Intellij IDEA](https://www.jetbrains.com/idea/)

**Lombok**
This project uses [Lombok](https://projectlombok.org/) for code generation. You may need to configure your IDE to take advantage of this tool.
 - [Intellij configuration](https://projectlombok.org/setup/intellij)
 - [VSCode configuration](https://projectlombok.org/setup/vscode)

### Understanding Environment Variables

In order to run the service locally, you will need to have the following environment variables defined.

**Note** The following command can be useful to pull secrets from keyvault:
```bash
az keyvault secret show --vault-name $KEY_VAULT_NAME --name $KEY_VAULT_SECRET_NAME --query value -otsv
```

| name | value | description | sensitive? | source |
| ---  | ---   | ---         | ---        | ---    |
| `AZURE_CLIENT_ID` | `********` | Identity to run the service locally. This enables access to Azure resources. You only need this if running locally | yes | keyvault secret: `$KEYVAULT_URI/secrets/app-dev-sp-username` |
| `AZURE_TENANT_ID` | `********` | AD tenant to authenticate users from | yes | -- |
| `AZURE_CLIENT_SECRET` | `********` | Secret for `$AZURE_CLIENT_ID` | yes | keyvault secret: `$KEYVAULT_URI/secrets/app-dev-sp-password` |
| `SEARCH_QUERY_LIMIT` | `1000` | Search query limit | no | -- |
| `BATCH_SIZE` | `100` | Search batch size | no | -- |
| `ENVIRONMENT` | ex. `dev` | The name of the environment | no | environment name |
| `JAVA_HEAP_MEMORY` | ex. `4096` | Java heap memory | no | -- |
| `SEARCH_HOST` | ex. `foo-search.azurewebsites.net` | URI of search host | yes | output of infrastructure deployment |
| `APPLICATION_PORT` | `8082` | Port of application. | no | -- |

**Required to run integration tests**

| name | value | description | sensitive? | source |
| ---  | ---   | ---         | ---        | ---    |
| `DOMAIN` | ex. `contoso.com` | Must match the value of `service_domain_name` above | no | -- |
| `ENTITLEMENTS_DOMAIN` | ex `contoso.com` | Must match the value of entitlements domain_name | no | -- |
| `INTEGRATION_TESTER` | `********` | System identity to assume for API calls. Note: this user must have entitlements configured already | no | -- |
| `TESTER_SERVICEPRINCIPAL_SECRET` | `********` | Secret for `$INTEGRATION_TESTER` | yes | -- |
| `AZURE_AD_TENANT_ID` | `********` | AD tenant to authenticate users from | yes | -- |
| `AZURE_STORAGE_ACCOUNT` | `********` | Storage account | yes | output of infrastructure deployment |
| `AZURE_AD_APP_RESOURCE_ID` | `********` | AAD client application ID | yes | output of infrastructure deployment |
| `LEGAL_HOST` | ex. `https://foo-legal.azurewebsites.net/api/legal/v1/` | The host where the legal service is running | yes | output of infrastructure deployment |
| `DEFAULT_DATA_PARTITION_ID_TENANT1` | ex `opendes` | Tenant 1 | no | -- |
| `DEFAULT_DATA_PARTITION_ID_TENANT2` | ex `common` | Tenant 2 | no | -- |
| `OTHER_RELEVANT_DATA_COUNTRIES` | ex. `US` | Used for testing | no | -- |
| `LEGAL_TAG` | ex. `opendes-public-usa-dataset-1` | Legal tag | no | Created legal tag |
| `SEARCH_HOST` | ex. `https://foo-search.azurewebsites.net/api/search/v2/` | The host where the search service is running | yes | -- |
| `STORAGE_HOST` | ex. `foo-storage.azurewebsites.net/api/storage/v2/` | The URL where the storage service is running | yes | -- | 
| `DELIVERY_HOST` | ex. `http://localhost:8082/api/delivery/v2/` | The URL where the delivery service is running | yes | -- | 


### Configure Maven

Check that maven is installed:
```bash
$ mvn --version
Apache Maven 3.6.0
Maven home: /usr/share/maven
Java version: 1.8.0_212, vendor: AdoptOpenJDK, runtime: /usr/lib/jvm/jdk8u212-b04/jre
...
```

You may need to configure access to the remote maven repository that holds the OSDU dependencies. A default file should live within `~/.m2/settings.xml`:
```bash
$ cat ~/.m2/settings.xml
<settings>
	<profiles>
		<profile>
			<!-- This profile uses the CI-Token to authenticate with the server, and is the default case -->
			<id>Personal Maven Profile</id>
			<activation>
				<activeByDefault>true</activeByDefault>
			</activation>
			<properties>
				<repo.releases.id>community-maven-repo</repo.releases.id>
				<publish.snapshots.id>community-maven-via-private-token</publish.snapshots.id>
				<publish.releases.id>community-maven-via-private-token</publish.releases.id>
				<repo.releases.url>https://community.opengroup.org/api/v4/groups/17/-/packages/maven</repo.releases.url>
				<publish.snapshots.url>https://community.opengroup.org/api/v4/projects/118/packages/maven</publish.snapshots.url>
				<publish.releases.url>https://community.opengroup.org/api/v4/projects/118/packages/maven</publish.releases.url>
			</properties>
		</profile>
	</profiles>
	<servers>
		<server>
			<id>community-maven-via-private-token</id>
			<configuration>
				<httpHeaders>
					<property>
						<name>Private-Token</name>
						<value>${env.COMMUNITY_MAVEN_TOKEN}</value>
					</property>
				</httpHeaders>
			</configuration>
		</server>
		<server>
			<id>azure-auth</id>
			<configuration>
				<tenant>${env.AZURE_TENANT_ID}</tenant>
				<client>${env.AZURE_CLIENT_ID}</client>
				<key>${env.AZURE_CLIENT_SECRET}</key>
				<environment>AZURE</environment>
			</configuration>
		</server>
	</servers>
</settings>
```

### Build, Run and Test the application Locally

After configuring your environment as specified above, you can follow these steps to build and run the application

```bash
# execute build + unit tests
$ mvn clean package --settings .mvn/community-maven.settings.xml
...
[INFO] BUILD SUCCESS

# run service locally **REQUIRES SPECIFIC ENVIRONMENT VARIABLES SET**
$ java -jar $(find ./target/ -name '*.jar')

# Test the application  **REQUIRES SPECIFIC ENVIRONMENT VARIABLES SET**
$ mvn clean test --settings .mvn/community-maven.settings.xml -f integration-tests/pom.xml
```


## Debugging

Jet Brains - the authors of Intellij IDEA, have written an [excellent guide](https://www.jetbrains.com/help/idea/debugging-your-first-java-application.html) on how to debug java programs.

## Configuring User Entitlements

Here is how you can configure user entitlements via the Azure specific API.

###Create a new user or service principal. 

The request body contains the user or service principal to create in JSON format. At a minimum, you must specify the required properties for the user or service principal. 
The required  properties for a user or service principal is the uid and one tenant with one group. The uid is either a user email or a service principal UUID.
You can optionally specify any additional tenants and groups.

####Permissions

The following permission is required to call this API. 

service.entitlements.admin

##### POST /profile
| header | value | required |
| ---  | ---  | ---  |
| Authorization | Bearer {token} | Yes |
| Content-Type | application/json | Yes |
| Request body  |  In the request body, supply a JSON representation of user object. | Yes |

The following table lists the properties that are required when you create a user. 

| Property	| Type	| Description | Required |
| ---  | ---  | ---  | ---  |
| uid | user email or service principal UUID. | The user email or service principal UUID. | Yes |
| id | OID | The OID for the user or service principal. | No value required. |
| tenants| list of TenantInfo | The tenants for the user or service principal. | Yes. |

##### Response
If successful, this method returns 201 response code and user object in the response body.

##### Example: Create a user

##### Request
Here is an example of the request.

###### POST /profile
###### Content-type: application/json

```json
{
    "id": "",
    "uid": "erik.leckner@wipro.com",
    "tenants": [
        {
            "name": "$SOME_OSDU_TENANT",
            "groups": [
                "service.delivery.viewer",
                 ...
            ]
        }
    ]
}
```

In the request body, supply a JSON representation of user object. The following permission is required to call the delivery API. 
                                                                   
service.delivery.viewer

###Update a user or service principal. 

The request body contains the user or service principal to update in JSON format. At a minimum, you must specify the required properties for the user or service principal. 
The required  properties for a user or service principal is the uid and one tenant with one group. The uid is either a user email or a service principal UUID.
You can optionally specify any additional tenants and groups.

####Permissions

The following permission is required to call this API. 

service.entitlements.admin

##### PUT /profile
| header | value | required |
| ---  | ---  | ---  |
| Authorization | Bearer {token} | Yes |
| Content-Type | application/json | Yes |
| Request body  |  In the request body, supply a JSON representation of user object. | Yes |

The following table lists the properties that are required when you create a user. .

| Property	| Type	| Description | Required |
| ---  | ---  | ---  | ---  |
| uid | user email or service principal UUID. | The user email or service principal UUID. | Yes |
| id | OID | The OID for the user or service principal. | No value required. |
| tenants| list of TenantInfo | The tenants for the user or service principal. | Yes. |

##### Response
If successful, this method returns 200 response code and user object in the response body.

##### Example: Update a user

##### Request
Here is an example of the request.

###### PUT /profile
###### Content-type: application/json

```json
{
    "id": "",
    "uid": "erik.leckner@wipro.com",
    "tenants": [
        {
            "name": "$SOME_OSDU_TENANT",
            "groups": [
                "service.delivery.viewer",
                ...
            ]
        }
    ]
}
```

In the request body, supply a JSON representation of user object. The following permission is required to call the delivery API. 
                                                                                                                                     
service.delivery.viewer

## Configuring User Entitlements (Deprecated)

Here is how you can configure user entitlements manually.

 - Identify the correct CosmosDB account. This can be found in the output of the infrastructure template. Alternatively, you should be able to identify it as the singular CosmosDB account that is provisioned in the resource group that hosts the this service
 - Use the `Data Explorer` tool in the CosmosDB UI and navigate to the `UserInfo` container
 - Identify if there already exists a `User` record in the table by applying a filter equal to `WHERE c.id = '$IDENTITY_ID'`. `$IDENTITY_ID` will be one of: (1) Object ID of a user (i.e., `974cb327-1406-76b9-91de-cbd6eb7ec949`) or (2) Application ID of a system assigned identity (i.e., `930b212b-e21f-478f-b993-af9b41abe836`)
 - If the user exists, verify that the permissions are correct. If the user needs to be added to a group, you can edit the document directly and click `Save`
 - If the user does not exist, you can add a document that has the following schema. The exact groups you wish to provision to the user will most likely be different, so be sure to add/remove the appropriate roles. The below listing represents a user with full access to all services.
```json
{
    "id": "$OBJECT_ID",
    "uid": "$IDENTITY_ID",
    "tenants": [
        {
            "name": "$SOME_OSDU_TENANT",
            "groups": [
                "service.delivery.viewer",
                ...
            ]
        }
    ]
}
```

## Deploying the Service

Service deployments into Azure are standardized to make the process the same for all services if using ADO and are closely related to the infrastructure deployed. The steps to deploy into Azure can be [found here](https://github.com/azure/osdu-infrastructure)

The default ADO pipeline is /devops/azure-pipelines.yml

### Manual Deployment Steps

__Environment Settings__

The following environment variables are necessary to properly deploy a service to an Azure OSDU Environment.

```bash
# Group Level Variables
export AZURE_TENANT_ID=""
...

# Pipeline Level Variable
export AZURE_SERVICE="entitlements"
export AZURE_BUILD_SUBDIR="."
export AZURE_TEST_SUBDIR="testing"
export AZURE_OSDU_TENANT="opendes"
export AZURE_COMPANY_DOMAIN="contoso.com"
export AZURE_VALID_GROUPNAME="integ.test.data.creator"
export AZURE_INVALID_GROUPNAME="InvalidTestAdmin"

# Required for Azure Deployment
export AZURE_CLIENT_ID="${AZURE_PRINCIPAL_ID}"
export AZURE_CLIENT_SECRET="${AZURE_PRINCIPAL_SECRET}"
export AZURE_RESOURCE_GROUP="${AZURE_BASENAME}-osdu-r2-app-rg"
export AZURE_APPSERVICE_PLAN="${AZURE_BASENAME}-osdu-r2-sp"
export AZURE_APPSERVICE_NAME="${AZURE_BASENAME_21}-au-${AZURE_SERVICE}"

# Required for Testing
...
```

__Azure Service Deployment__


1. Deploy the service using the Maven Plugin  _(azure_deploy)_

```bash
cd $AZURE_BUILD_SUBDIR
mvn azure-webapp:deploy \
  -DAZURE_DEPLOY_TENANT=$AZURE_TENANT_ID \
  -Dazure.appservice.subscription=$AZURE_SUBSCRIPTION_ID \
  -DAZURE_DEPLOY_CLIENT_ID=$AZURE_CLIENT_ID \
  -DAZURE_DEPLOY_CLIENT_SECRET=$AZURE_CLIENT_SECRET \
  -Dazure.appservice.resourcegroup=$AZURE_RESOURCE_GROUP \
  -Dazure.appservice.plan=$AZURE_APPSERVICE_PLAN \
  -Dazure.appservice.appname=$AZURE_APPSERVICE_NAME
```

2. Configure the Web App to start the SpringBoot Application _(azure_config)_


```bash
az login --service-principal -u $AZURE_CLIENT_ID -p $AZURE_CLIENT_SECRET --tenant $AZURE_TENANT_ID

# Set the JAR FILE as required
TARGET=$(find ./target/ -name '*.jar')
JAR_FILE=${TARGET##*/}

JAVA_COMMAND="java -jar /home/site/wwwroot/${JAR_FILE}"
JSON_TEMPLATE='{"appCommandLine":"%s"}'
JSON_FILE="config.json"
echo $(printf "$JSON_TEMPLATE" "$JAVA_COMMAND") > $JSON_FILE

az webapp config set --resource-group $AZURE_RESOURCE_GROUP --name $AZURE_APPSERVICE_NAME --generic-configurations @$JSON_FILE
```

3. Execute the Integration Tests against the Service Deployment _(azure_test)_


```bash
mvn clean test --settings .mvn/community-maven.settings.xml -f $AZURE_TEST_SUBDIR/pom.xml
```


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
