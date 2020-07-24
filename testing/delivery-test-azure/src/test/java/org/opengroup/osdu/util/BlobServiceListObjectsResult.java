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

package org.opengroup.osdu.util;

import lombok.Data;

import java.util.*;

@Data
public class BlobServiceListObjectsResult {

    private HashMap<String, String> objects;

    public BlobServiceListObjectsResult(HashMap<String, String> objects) {
        this.objects = objects;
    }

    public List<BlobServiceObjectSummary> getObjectSummaries() {
        List<BlobServiceObjectSummary> summaries = new ArrayList<>();
        Iterator<Map.Entry<String, String>> iterator = objects.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            BlobServiceObjectSummary summary = new BlobServiceObjectSummary(entry.getKey(), entry.getValue());;
            summaries.add(summary);
        }
        return summaries;
    }
}
