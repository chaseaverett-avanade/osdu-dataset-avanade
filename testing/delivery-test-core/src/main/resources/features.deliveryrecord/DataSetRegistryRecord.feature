Feature: Delivery of dataset registries
  This feature allows access to datasets for ingestion

  Background:
    Given the schema is created with the following kind
      | kind                                    | index                                   | schemaFile      |
      | tenant1:testindex<timestamp>:dataset-registry:1.0.0 | tenant1-testindex<timestamp>-registry-1.0.0 | dataset_registry |


  Scenario Outline: Ingest a valid dataset registry record and get a signed url and connection string
    When I ingest records with the <recordFile> with <acl> with record <recordId> for a given <kind>
    Then I should get the <number> documents for the record <recordId> in the Storage Service
    Then I should get a signed url for the <recordId> from the Delivery Service

    Examples:
      | kind                                      | recordFile        | number | acl | recordId |
      | "tenant1:testindex<timestamp>:dataset-registry:1.0.0" | "dataset_registry_record_valid" | 1      | "data.default.viewers@${tenant}.${domain}" | "opendes:osdu:delivery-int-test1" |


  Scenario Outline: Ingest a invalid dataset registry record and get an unsupported message
    When I ingest records with the <recordFile> with <acl> with record <recordId> for a given <kind>
    Then I should get the <number> documents for the record <recordId> in the Storage Service
    Then I should get an unsupported message with no handler error code for the <recordId> from the Delivery Service

    Examples:
      | kind                                      | recordFile        | number | acl | recordId |
      | "tenant1:testindex<timestamp>:dataset-registry:1.0.0" | "dataset_registry_record_invalid_resourcetype" | 1      | "data.default.viewers@${tenant}.${domain}" | "opendes:osdu:delivery-int-test2" |


  Scenario Outline: Ingest a dataset registry with no resourcetype and get an unsupported message
    When I ingest records with the <recordFile> with <acl> with record <recordId> for a given <kind>
    Then I should get the <number> documents for the record <recordId> in the Storage Service
    Then I should get an unsupported message with no resource type error code for the <recordId> from the Delivery Service

    Examples:
      | kind                                      | recordFile        | number | acl | recordId |
      | "tenant1:testindex<timestamp>:dataset-registry:1.0.0" | "dataset_registry_record_resourcetype_not_found" | 1      | "data.default.viewers@${tenant}.${domain}" | "opendes:osdu:delivery-int-test3" |


  Scenario Outline: Ingest a dataset registry for a file with no preload file path and get an failure message
    When I ingest records with the <recordFile> with <acl> with record <recordId> for a given <kind> incorrectly
    Then I should get the <number> documents for the record <recordId> in the Storage Service
    Then I should get a failure message for the <recordId> from the Delivery Service

    Examples:
      | kind                                      | recordFile        | number | acl | recordId |
      | "tenant1:testindex<timestamp>:dataset-registry:1.0.0" | "dataset_registry_record_failure" | 1      | "data.default.viewers@${tenant}.${domain}" | "opendes:osdu:delivery-int-test4" |
