// Copyright © 2020 Amazon Web Services
// Copyright 2017-2019, Schlumberger
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

//TODO: move to os-core-common

package org.opengroup.osdu.dataset.dms;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class DmsAPIConfig {
    @Builder.Default
    String rootUrl = "https://osdu-api";

    String apiKey;

    public static DmsAPIConfig Default() {
        return DmsAPIConfig.builder().build();
    }
}
