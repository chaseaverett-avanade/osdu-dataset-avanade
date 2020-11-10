package org.opengroup.osdu.delivery.provider.gcp.service.downscoped;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.apache.http.HttpResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.powermock.reflect.Whitebox;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyCollectionOf;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@PrepareForTest(DownScopedCredentials.class)
public class DownScopedCredentialsTest {

    @Mock
    DownScopedOptions downScopedOptions;

    @Mock
    ServiceAccountCredentials sourceCredentials;

    @Mock
    GoogleCredentials finiteCredentials;

    @Mock
    AccessToken accessToken;
    @Mock
    AccessToken downScopedToken;

    @Mock
    HttpResponse httpResponse;

    DownScopedCredentials downScopedCredentials;

    @Test
    public void givenDownScopedCredentials_whenInvoked_thenRequestsToken() throws Exception {

        Whitebox.setInternalState(sourceCredentials, "scopes", Collections.EMPTY_LIST);
        when(sourceCredentials.createScoped(anyCollectionOf(String.class))).thenReturn(finiteCredentials);
        downScopedCredentials = spy(new DownScopedCredentials(sourceCredentials, downScopedOptions));
        Whitebox.setInternalState(finiteCredentials, "temporaryAccess", accessToken);
        PowerMockito.doReturn(downScopedToken).when(downScopedCredentials, "getDownScopedToken", accessToken);

        AccessToken returnedDownScopedToken = downScopedCredentials.refreshAccessToken();

        verifyPrivate(downScopedCredentials).invoke("getDownScopedToken", accessToken);
        assertEquals(downScopedToken, returnedDownScopedToken);
    }
}