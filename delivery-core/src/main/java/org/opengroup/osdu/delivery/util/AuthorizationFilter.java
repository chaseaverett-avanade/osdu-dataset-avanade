// Copyright © Amazon Web Services
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

package org.opengroup.osdu.delivery.util;

import org.opengroup.osdu.core.common.entitlements.IEntitlementsAndCacheService;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import javax.inject.Inject;

@Component("authorizationFilter")
@RequestScope
public class AuthorizationFilter {

    @Inject
    private IEntitlementsAndCacheService entitlementsAndCacheService;

    @Inject
    private DpsHeaders dpsHeaders;

    public boolean hasRole(String... requiredRoles) {
        entitlementsAndCacheService.authorize(dpsHeaders, requiredRoles);
        return true;
    }
}
