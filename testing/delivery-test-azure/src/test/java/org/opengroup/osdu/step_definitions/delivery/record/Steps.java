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

package org.opengroup.osdu.step_definitions.delivery.record;

import com.sun.jersey.api.client.ClientResponse;
import cucumber.api.DataTable;
import cucumber.api.Scenario;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.junit.Assert;

import org.opengroup.osdu.common.RecordSteps;
import org.opengroup.osdu.util.AzureHTTPClient;
import org.opengroup.osdu.util.CloudStorageUtilsTestAzure;
import org.opengroup.osdu.util.Config;
import org.opengroup.osdu.util.LegalTagUtilsAzure;

public class Steps extends RecordSteps {

    public Steps() {
        super(new AzureHTTPClient(), new CloudStorageUtilsTestAzure());
    }

    LegalTagUtilsAzure legalTagUtils = new LegalTagUtilsAzure();

    @Override
    protected String generateActualName(String rawName, String timeStamp) {
        for (String tenant : tenantMap.keySet()) {
            rawName = rawName.replaceAll(tenant, getTenantMapping(tenant));
        }
        rawName = rawName.replaceAll("<timestamp>", timeStamp);
        return rawName.replaceAll("well", "file");
    }

    @Before
    public void before(Scenario scenario) throws Exception {
        this.scenario = scenario;
        this.httpClient = new AzureHTTPClient();
        ClientResponse resp = legalTagUtils.create(Config.getLegalTag());
        Assert.assertTrue("Creating LegalTag", resp.getStatus() == 201 || resp.getStatus() == 409);
    }

    @Given("^the schema is created with the following kind$")
    public void the_schema_is_created_with_the_following_kind(DataTable dataTable) {
        super.the_schema_is_created_with_the_following_kind(dataTable);
    }

    @When("^I ingest records with the \"(.*?)\" with \"(.*?)\" for a given \"(.*?)\"$")
    public void i_ingest_records_with_the_for_a_given(String record, String dataGroup, String kind)  {
        super.i_ingest_records_with_the_for_a_given(record, dataGroup, kind);
    }

    @Then("^I should get the (\\d+) documents for the \"([^\"]*)\" in the Search Service$")
    public void i_should_get_the_documents_for_the_index_from_search(int expectedCount, String kind) throws Throwable {
        super.i_should_get_the_documents_for_the_index_from_search(expectedCount, kind);
    }

    @Then("^I should get the (\\d+) signed urls from the File Service$")
    public void i_should_get_the_signed_urls_from_file(int expectedCount) throws Throwable {
        super.i_should_get_the_signed_urls_from_file(expectedCount);
    }

    @Then("^I should get an error with no File Path$")
    public void i_should_get_no_signed_urls_from_file() throws Throwable {
        super.i_should_get_an_error_with_no_file_path();
    }

}

