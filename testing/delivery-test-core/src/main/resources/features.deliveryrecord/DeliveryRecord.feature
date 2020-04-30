Feature: Delivery of the documents
  This feature deals with validation of the documents in Elastic Search ingested with different kinds and attributes.

  Background:
    Given the schema is created with the following kind
      | kind                                    | index                                   | schemaFile      |
      | tenant1:testindex<timestamp>:well:1.0.0 | tenant1-testindex<timestamp>-well-1.0.0 | index_records_1 |
      | tenant1:testindex<timestamp>:well:2.0.0 | tenant1-testindex<timestamp>-well-2.0.0 | index_records_2 |
      | tenant1:testindex<timestamp>:well:3.0.0 | tenant1-testindex<timestamp>-well-3.0.0 | index_records_1 |


  Scenario Outline: Ingest the record and Index in the Search Service
    When I ingest records with the <recordFile> with <acl> for a given <kind>
    Then I should get the <number> documents for the <kind> in the Search Service
    Then I should get the <number> signed urls from the File Service

    Examples:
      | kind                                      | recordFile        | number | acl |
      | "tenant1:testindex<timestamp>:well:3.0.0" | "index_records_1" | 5      | "data.default.viewers@opendes" |
      | "tenant1:testindex<timestamp>:well:1.0.0" | "index_records_1" | 5      | "data.default.viewers@opendes" |


  Scenario Outline: Ingest the record and Index in the Elastic Search with bad file paths
    When I ingest records with the <recordFile> with <acl> for a given <kind>
    Then I should get the <number> documents for the <kind> in the Search Service
    Then I should get an Error with no File Path
    Examples:
      | kind                                      | recordFile        | number | expected | acl                                   |
      | "tenant1:testindex<timestamp>:well:2.0.0" | "index_records_2" | 5      | 0 | "data.default.viewers@opendes" |