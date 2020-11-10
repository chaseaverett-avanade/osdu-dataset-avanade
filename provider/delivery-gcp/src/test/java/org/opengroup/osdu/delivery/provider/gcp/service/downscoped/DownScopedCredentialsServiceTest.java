package org.opengroup.osdu.delivery.provider.gcp.service.downscoped;

import com.google.auth.oauth2.ServiceAccountCredentials;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.reflect.Whitebox;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
public class DownScopedCredentialsServiceTest {

    @Mock
    DownScopedOptions downScopedOptions;

    @Mock
    ServiceAccountCredentials serviceAccountCredentials;

    @InjectMocks
    DownScopedCredentialsService downScopedCredentialsService;

    @Test
    public void givenService_whenRequestDownscopedCredentials_thenCreatedWithProperArgs() {

        DownScopedCredentials dsc = downScopedCredentialsService.getDownScopedCredentials(serviceAccountCredentials, downScopedOptions);
        verify(serviceAccountCredentials).createScopedRequired();
        verify(serviceAccountCredentials).createScoped(anyCollectionOf(String.class));
        assertEquals(Whitebox.getInternalState(dsc, "downScopedOptions"), downScopedOptions);
    }
}