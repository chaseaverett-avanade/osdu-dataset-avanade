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

package org.opengroup.osdu.dataset.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.lang.reflect.Method;
import java.util.HashMap;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.dataset.logging.AuditLogger;
import org.opengroup.osdu.dataset.model.request.DeliveryRole;
import org.opengroup.osdu.dataset.model.response.DatasetRetrievalDeliveryItem;
import org.opengroup.osdu.dataset.model.response.GetDatasetStorageInstructionsResponse;
import org.opengroup.osdu.dataset.service.DatasetDmsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

@RunWith(MockitoJUnitRunner.class)
public class DatasetDmsApiTest {

    private final String USER = "user";
    private final String TENANT = "tenant1";

    @Mock
    private DpsHeaders httpHeaders;

    @Mock
    private DatasetDmsService datasetDmsService;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private DatasetDmsApi datasetDmsApi;

    @Before
    public void setup() {
        initMocks(this);

        when(this.httpHeaders.getUserEmail()).thenReturn(this.USER);
        when(this.httpHeaders.getPartitionIdWithFallbackToAccountId()).thenReturn(this.TENANT);

        TenantInfo tenant = new TenantInfo();
        tenant.setName(this.TENANT);
    }

    @Test
    public void should_returnsHttp200_when_gettingStorageInstructionsSuccessfully() {

        String resourceType = "srn:type:file";
      
        GetDatasetStorageInstructionsResponse expectedResponse = new GetDatasetStorageInstructionsResponse(new HashMap<String, Object>(), "DUMMY");
        when(this.datasetDmsService.getStorageInstructions(resourceType)).thenReturn(expectedResponse);

        ResponseEntity response = this.datasetDmsApi.getStorageInstructions(resourceType);

        assertEquals(HttpStatus.SC_OK, response.getStatusCodeValue());
        assertEquals(expectedResponse, response.getBody());
    }

    @Test
    public void should_allowAccessToGetStorageInstructions_when_userBelongsToViewerGroup() throws Exception {

        Method method = this.datasetDmsApi.getClass().getMethod("getStorageInstructions", String.class);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);

        assertTrue(annotation.value().contains(DeliveryRole.VIEWER));
    }



    
}
