## Delivery Service
The Delivery Service is responsible for authenticating an OSDU user, and, if they have access, returns a signed URL to download the data file.

## License
Copyright 2017-2020, Schlumberger

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

## Running integration tests
Integration tests are located in a separate project for each cloud in the ```testing``` directory under the project root directory. 

### GCP
Instructions for running the GCP integration tests can be found [here](./provider/delivery-gcp/README.md).

## Publish
All references on Binary Storage required to publish Maven artifacts are external to `pom.xml` and should be configured through Maven `settings.xml` file.
There are two profiles available in `.mvn/community-maven.settings.xml` that can be used to publish artifacts to Community GitLab:
1. `GitLab-Authenticate-With-Job-Token` - default one, and should be used in CICD pipelines
2. `GitLab-Authenticate-With-Private-Token` -  profile for local development and manual publishing of artifacts. To activate the profile, the developer should have `COMMUNITY_MAVEN_TOKEN` env variable with a configured personal GitLab access token. Please see [GitLab documentation](https://docs.gitlab.com/ee/user/profile/personal_access_tokens.html) on how to set-up Personal Access token in GitLab:
```
export COMMUNITY_MAVEN_TOKEN='Your personal GitLab access token'
mvn deploy --settings .mvn/community-maven.settings.xml

