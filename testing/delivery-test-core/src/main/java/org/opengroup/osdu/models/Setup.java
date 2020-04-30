// Copyright © Amazon Web Services
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

import lombok.Data;
import lombok.NoArgsConstructor;
import org.opengroup.osdu.util.HTTPClient;

import java.util.Map;

@Data
@NoArgsConstructor
public class Setup {
    private String tenantId;
    private String kind;
    private String index;
    private String viewerGroup;
    private String ownerGroup;
    private String mappingFile;
    private String recordFile;
    private String schemaFile;
    private HTTPClient httpClient;
    private Map<String, String> headers;
}
