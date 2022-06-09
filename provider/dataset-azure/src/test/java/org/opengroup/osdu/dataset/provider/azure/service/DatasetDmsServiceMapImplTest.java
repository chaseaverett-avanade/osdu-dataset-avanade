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

package org.opengroup.osdu.dataset.provider.azure.service;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengroup.osdu.dataset.dms.DmsServiceProperties;
import org.opengroup.osdu.dataset.provider.azure.config.OsduApiConfig;
import org.opengroup.osdu.dataset.provider.azure.config.OsduDatasetKindConfig;
import org.opengroup.osdu.dataset.provider.azure.service.DatasetDmsServiceMapImpl;

import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class DatasetDmsServiceMapImplTest {

    private final String FILE = "file_name";
    private final String FILE_COLLECTION = "file-collection";

    @Mock
    private OsduDatasetKindConfig osduDatasetKindConfig;

    @Mock
    private OsduApiConfig osduApiConfig;

    @InjectMocks
    private DatasetDmsServiceMapImpl datasetDmsServiceMapImpl;

    @Before
    public void setup() {
        initMocks(this);

        when(osduApiConfig.getFile()).thenReturn(FILE);
        when(osduApiConfig.getFileCollection()).thenReturn(FILE_COLLECTION);

        when(osduDatasetKindConfig.getFileCollection()).thenReturn(FILE_COLLECTION);
    }

    @Test
    public void should_successfully_call_init() {

        datasetDmsServiceMapImpl.init();

        verify(osduApiConfig, times(1)).getFile();
        verify(osduDatasetKindConfig, times(1)).getFile();
        verify(osduApiConfig, times(1)).getFileCollection();
        verify(osduDatasetKindConfig, times(1)).getFileCollection();
    }

    @Test
    public void should_call_getResourceTypeToDmsServiceMap() {
        datasetDmsServiceMapImpl.init();
        Map<String, DmsServiceProperties> resourceTypeToDmsServiceMap = datasetDmsServiceMapImpl.getResourceTypeToDmsServiceMap();
        assertNotNull(resourceTypeToDmsServiceMap);
    }

}
