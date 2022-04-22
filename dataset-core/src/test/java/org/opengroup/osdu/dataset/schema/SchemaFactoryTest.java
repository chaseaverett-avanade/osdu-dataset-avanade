/*
 * Copyright 2021 Google LLC
 * Copyright 2021 EPAM Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.dataset.schema;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.http.json.HttpResponseBodyMapper;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;

@RunWith(MockitoJUnitRunner.class)

public class SchemaFactoryTest {
    @Mock
    private HttpResponseBodyMapper bodyMapper;

    @Mock
    private SchemaAPIConfig schemaAPIConfig;

    @Mock
    private DpsHeaders headers;

    @Before
    public void init() {
    }

    @Test
    public void get_schema_success() {
        SchemaFactory schemaFactory = new SchemaFactory(schemaAPIConfig, bodyMapper);
        ISchemaService schemaService =  schemaFactory.create(headers);
        assertNotNull(schemaService);
    }

    @Test
    public void get_schema_failure_IllegalArgumentException() {

        try {
            SchemaFactory schemaFactory = new SchemaFactory(null, bodyMapper);
            schemaFactory.create(headers);
        }
        catch (Exception exception) {
            assertTrue(exception instanceof IllegalArgumentException);
            assertEquals("SchemaAPIConfig cannot be empty", exception.getMessage());
        }
    }

    @Test
    public void get_schema_failure_NullPointerException() {

        try {
            SchemaFactory schemaFactory = new SchemaFactory(schemaAPIConfig, bodyMapper);
            schemaFactory.create(null);
        }
        catch (Exception exception) {
            assertTrue(exception instanceof NullPointerException);
            assertEquals("headers cannot be null", exception.getMessage());
        }
    }
}
