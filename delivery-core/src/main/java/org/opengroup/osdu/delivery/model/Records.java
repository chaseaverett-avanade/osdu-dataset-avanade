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

package org.opengroup.osdu.delivery.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.opengroup.osdu.core.common.model.entitlements.Acl;
import org.opengroup.osdu.core.common.model.legal.Legal;
import org.opengroup.osdu.core.common.model.storage.ConversionStatus;
import org.opengroup.osdu.core.common.model.storage.RecordAncestry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Records {

    @Builder.Default
    private List<Entity> records = new ArrayList<>();
    @Builder.Default
    private List<String> notFound = new ArrayList<>();
    @Builder.Default
    private List<ConversionStatus> conversionStatuses = new ArrayList<>();

    // missing records ids -- mismatch in requested records and storage response
    @JsonIgnore
    @Builder.Default
    private List<String> missingRetryRecords = new ArrayList<>();

    public int getTotalRecordCount() {
        return this.getRecords().size() + this.getNotFound().size();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Entity {
        private String id;
        private long version;
        private String kind;
        private Acl acl;
        private Legal legal;
        private RecordAncestry ancestry;
        private Map<String, Object> data;
        private List<Object> meta;
    }

    @Data
    @Builder
    public static class Type {
        private String type;
    }

    @Data
    @Builder
    public static class Analyzer {
        private String type;
        private String analyzer;
        private String search_analyzer;
}
}
