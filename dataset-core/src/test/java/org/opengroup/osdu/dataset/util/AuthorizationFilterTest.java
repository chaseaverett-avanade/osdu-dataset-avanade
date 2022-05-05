package org.opengroup.osdu.dataset.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.entitlements.IEntitlementsAndCacheService;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuthorizationFilterTest {

    @Mock
    private IEntitlementsAndCacheService entitlementsAndCacheService;

    @Mock
    private DpsHeaders headers;

    @InjectMocks
    private AuthorizationFilter authorizationFilter;

    private final String requiredRoles = "data.default.owners";
    private final String EMAIL = "data.default.viewers@opendes.contoso.com";


    @Before
    public void setup() {
        when(headers.getUserEmail()).thenReturn(EMAIL);
    }

    @Test
    public void tesHasRole() {
        boolean hasRole = authorizationFilter.hasRole(requiredRoles);
        assertEquals(hasRole, true);

    }
}
