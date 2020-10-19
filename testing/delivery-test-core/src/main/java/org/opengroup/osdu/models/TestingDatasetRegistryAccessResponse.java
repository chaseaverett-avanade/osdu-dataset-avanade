// Copyright © 2020 Amazon Web Services
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

package org.opengroup.osdu.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.opengroup.osdu.delivery.model.UnsupportedAccessResponseItem;

import java.util.List;

@Data
@AllArgsConstructor
public class TestingDatasetRegistryAccessResponse {

    //NOTE: Had to make this class due to dynamic nature of delivery object and deserialization for testing

    @JsonProperty("processed")
    private List<TestingDatasetRegistryAccessResponseItem> dataRegistryAccessItems;

    @JsonProperty("unsupported")
    private List<UnsupportedAccessResponseItem> unsupportedDataRegistryAccessItems;

    @JsonProperty("failed")
    private List<TestingDatasetRegistryAccessResponseItem> failedDataRegistryAccessItems;
}
