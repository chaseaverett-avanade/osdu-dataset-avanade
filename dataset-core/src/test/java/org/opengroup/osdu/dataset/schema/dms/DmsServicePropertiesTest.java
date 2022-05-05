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

package org.opengroup.osdu.dataset.schema.dms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengroup.osdu.dataset.dms.DmsServiceProperties;

@RunWith(MockitoJUnitRunner.class)

public class DmsServicePropertiesTest {

    private final String dmsServiceBaseUrl = "url";
    private final boolean allowStorage = true;
    private final String apiKey = "key";
    private final boolean stagingLocationSupported = true;

    @Test
    public void should_successfully_create_four_args_constructor() {
        DmsServiceProperties properties = new DmsServiceProperties(dmsServiceBaseUrl, allowStorage, apiKey, stagingLocationSupported);
        assertNotNull(properties);
        assertEquals(dmsServiceBaseUrl, properties.getDmsServiceBaseUrl());
        assertEquals(allowStorage, properties.isAllowStorage());
        assertEquals(apiKey, properties.getApiKey());
        assertEquals(stagingLocationSupported, properties.isStagingLocationSupported());
    }

    @Test
    public void should_successfully_create_one_args_constructor() {
        DmsServiceProperties properties = new DmsServiceProperties(dmsServiceBaseUrl);
        assertNotNull(properties);
        assertEquals(dmsServiceBaseUrl, properties.getDmsServiceBaseUrl());
        assertTrue(allowStorage);
        assertNull(properties.getApiKey());
        assertFalse(properties.isStagingLocationSupported());
    }

    @Test
    public void should_successfully_create_two_args_constructor() {
        DmsServiceProperties properties = new DmsServiceProperties(dmsServiceBaseUrl, allowStorage);
        assertNotNull(properties);
        assertEquals(dmsServiceBaseUrl, properties.getDmsServiceBaseUrl());
        assertTrue(allowStorage);
        assertNull(properties.getApiKey());
        assertFalse(properties.isStagingLocationSupported());
    }

    @Test
    public void should_successfully_create_no_args_constructor() {
        DmsServiceProperties properties = new DmsServiceProperties();
        properties.setAllowStorage(allowStorage);
        properties.setApiKey(apiKey);
        properties.setDmsServiceBaseUrl(dmsServiceBaseUrl);
        properties.setStagingLocationSupported(stagingLocationSupported);

        properties.equals(new DmsServiceProperties());
        assertNotNull(properties);
        assertEquals(dmsServiceBaseUrl, properties.getDmsServiceBaseUrl());
        assertEquals(allowStorage, properties.isAllowStorage());
        assertEquals(apiKey, properties.getApiKey());
        assertEquals(stagingLocationSupported, properties.isStagingLocationSupported());
    }
}
