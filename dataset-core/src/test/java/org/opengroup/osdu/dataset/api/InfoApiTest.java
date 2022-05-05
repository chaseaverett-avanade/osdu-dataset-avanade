package org.opengroup.osdu.dataset.api;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.info.VersionInfoBuilder;
import org.opengroup.osdu.core.common.model.info.VersionInfo;


import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class InfoApiTest {

    @Mock
    private VersionInfoBuilder versionInfoBuilder;

    @InjectMocks
    InfoApi infoApi;

    @Before
    public void setup() {
        initMocks(this);
    }

    @Test
    public void infoApiTest() throws IOException {

        VersionInfo response = mock(VersionInfo.class);
        response.setVersion("1.0.0");
        when(versionInfoBuilder.buildVersionInfo()).thenReturn(response);
        VersionInfo actualResponse = infoApi.info();
        assertEquals(response.getVersion(), actualResponse.getVersion());

    }

}
