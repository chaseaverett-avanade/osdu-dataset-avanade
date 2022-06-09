// Copyright © 2021 Amazon Web Services
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

package org.opengroup.osdu.dataset.provider.azure.config;

import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertEquals;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OsduApiConfigTest {

    private final String FILE = "file_name";
    private final String FILE_COLLECTION = "file-collection";

    @Test
    public void should_successfully_create_config() {
        OsduApiConfig config = new OsduApiConfig();

        config.setFile(FILE);
        config.setFileCollection(FILE_COLLECTION);

        assertEquals(FILE, config.getFile());
        assertEquals(FILE_COLLECTION, config.getFileCollection());
    }

}
