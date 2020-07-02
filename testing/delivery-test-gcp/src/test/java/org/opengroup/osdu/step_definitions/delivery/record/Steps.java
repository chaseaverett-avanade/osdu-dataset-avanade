/**
 * Copyright 2020 Google LLC
 * Copyright 2020 EPAM Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.step_definitions.delivery.record;

import cucumber.api.DataTable;
import cucumber.api.Scenario;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.junit.Before;
import org.opengroup.osdu.common.RecordSteps;
import org.opengroup.osdu.util.CloudStorageUtilsGCP;
import org.opengroup.osdu.util.GCPHTTPClient;

public class Steps extends RecordSteps {

	public Steps() {
		super(new GCPHTTPClient(), new CloudStorageUtilsGCP());
	}

	@Before
	public void before(Scenario scenario) {
		this.scenario = scenario;
		this.httpClient = new GCPHTTPClient();
	}

	@Given("^the schema is created with the following kind$")
	public void the_schema_is_created_with_the_following_kind(DataTable dataTable) {
		super.the_schema_is_created_with_the_following_kind(dataTable);
	}

	@When("^I ingest records with the \"(.*?)\" with \"(.*?)\" for a given \"(.*?)\"$")
	public void i_ingest_records_with_the_for_a_given(String record, String dataGroup, String kind) {
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

