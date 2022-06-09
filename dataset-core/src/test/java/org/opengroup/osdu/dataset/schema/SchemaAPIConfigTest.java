package org.opengroup.osdu.dataset.schema;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class SchemaAPIConfigTest {

    @Test
    public void testDefault() {
        String rootUrl = "https://os-schema/api/schema-service/v2";
        SchemaAPIConfig schemaAPIConfig = SchemaAPIConfig.Default();
        assertEquals(schemaAPIConfig.getRootUrl(), rootUrl);
    }
}
