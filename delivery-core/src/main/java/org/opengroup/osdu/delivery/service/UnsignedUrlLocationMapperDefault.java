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

package org.opengroup.osdu.delivery.service;

import org.opengroup.osdu.delivery.provider.interfaces.IUnsignedUrlLocationMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UnsignedUrlLocationMapperDefault implements IUnsignedUrlLocationMapper {

    //TODO Update to use a database and map by kind
    private final String UNSIGNED_URL_PATH = "Data.GroupTypeProperties.PreLoadFilePath";

    @Override
    public String getUnsignedURLFromSearchResponse(Map<String, Object> searchResponse) {
        String unsignedUrl = null;
        Map<String,Object> data = null;

        Object currentNode = searchResponse.get("data");
        if(currentNode != null) {
            try {
                data = (Map<String, Object>) currentNode;
            } catch (ClassCastException ignored) {} // Unable to parse the current node; add this to the unprocessed list.

            if(data != null)
                unsignedUrl = data.get(UNSIGNED_URL_PATH).toString();
        }

        return unsignedUrl;
    }
}
