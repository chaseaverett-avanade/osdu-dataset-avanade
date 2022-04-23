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

package org.opengroup.osdu.dataset.service;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.entitlements.IEntitlementsFactory;
import org.opengroup.osdu.core.common.entitlements.IEntitlementsService;
import org.opengroup.osdu.core.common.http.HttpResponse;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.entitlements.Acl;
import org.opengroup.osdu.core.common.model.entitlements.EntitlementsException;
import org.opengroup.osdu.core.common.model.entitlements.GroupInfo;
import org.opengroup.osdu.core.common.model.entitlements.Groups;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.storage.Record;
import org.opengroup.osdu.core.common.model.storage.RecordMetadata;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class EntitlementsAndCacheServiceImplTest {

    private final String role = "data.default.owners";
    private final String dataPartitionId = "opendes";
    private final String authorization = "auth";
    private final String accountId = "account-id";
    private final String desId = "des-id";

    private static final String ERROR_REASON = "Access denied";
    private static final String ERROR_MSG = "The user is not authorized to perform this action";
    private final String EMAIL = "data.default.viewers@opendes.contoso.com";
    private final String EMAIL_OWNER = "data.default.owners@opendes.contoso.com";
    private final String RECORD_ID = "opendes:dataset--file:data";
    private final String KIND = "opendes:wks:dataset--file:1.0.0";
    private final String ACL_VIEWER = "data.default.viewers@opendes.contoso.com";
    private final String ACL_OWNER = "data.default.owners@opendes.contoso.com";


    @Mock
    private IEntitlementsFactory factory;

    @Mock
    private IEntitlementsService entitlementsService;

    @Mock
    private ICache<String, Groups> cache;

    @Mock
    private JaxRsDpsLog logger;

    @Mock
    private DpsHeaders headers;

    @Mock
    private EntitlementsException entitlementsException;

    @Mock
    private HttpResponse httpResponse;

    @InjectMocks
    private EntitlementsAndCacheServiceImpl entitlementsAndCacheService;

    @Before
    public void init() {
        when(factory.create(headers)).thenReturn(entitlementsService);
        when(headers.getPartitionId()).thenReturn(dataPartitionId);
        when(headers.getAccountId()).thenReturn(accountId);
        when(headers.getAuthorization()).thenReturn(authorization);
    }

    @Test
    public void authorizeSuccess() throws EntitlementsException {
        Groups groups = new Groups();
        groups.setDesId(desId);
        groups.setMemberEmail(EMAIL);
        groups.setGroups(Collections.singletonList(getGroupInfo(role, EMAIL)));

        when(entitlementsService.getGroups()).thenReturn(groups);

        String result = entitlementsAndCacheService.authorize(headers, role);

        verify(factory, times(1)).create(headers);
        verify(entitlementsService, times(1)).getGroups();
        assertEquals(desId, result);
    }

    @Test
    public void authorizeAppExceptionThrown() throws EntitlementsException {
        Groups groups = new Groups();
        groups.setDesId(desId);
        groups.setMemberEmail(EMAIL);
        groups.setGroups(Collections.singletonList(getGroupInfo("xyz", EMAIL)));

        when(entitlementsService.getGroups()).thenReturn(groups);

        try {
            entitlementsAndCacheService.authorize(headers, role);
        }
        catch (Exception exception) {
            assertTrue(exception instanceof AppException);

            assertEquals(HttpStatus.SC_FORBIDDEN, ((AppException) exception).getError().getCode());
            assertEquals(ERROR_REASON, ((AppException) exception).getError().getReason());
            assertEquals(ERROR_MSG, ((AppException) exception).getError().getMessage());
        }
        verify(factory, times(1)).create(headers);
        verify(entitlementsService, times(1)).getGroups();
    }

    @Test
    public void authorizeExceptionThrownInGetGroups() throws EntitlementsException {
        when(entitlementsService.getGroups()).thenThrow(entitlementsException);
        when(entitlementsException.getHttpResponse()).thenReturn(httpResponse);
        when(httpResponse.getResponseCode()).thenReturn(500);

        try {
            entitlementsAndCacheService.authorize(headers, role);
        }
        catch (Exception exception) {
            assertTrue(exception instanceof AppException);

            assertEquals(500, ((AppException) exception).getError().getCode());
            assertEquals(ERROR_REASON, ((AppException) exception).getError().getReason());
            assertEquals(ERROR_MSG, ((AppException) exception).getError().getMessage());
        }
        verify(factory, times(1)).create(headers);
        verify(entitlementsService, times(1)).getGroups();
        verify(entitlementsException, times(1)).printStackTrace();
        verify(entitlementsException, times(2)).getHttpResponse();
        verify(httpResponse, times(1)).getResponseCode();
    }

    @Test
    public void isValidAclAppExceptionBecauseNullGroups() throws EntitlementsException {
        Groups groups = new Groups();
        groups.setDesId(desId);
        groups.setMemberEmail(EMAIL);
        when(entitlementsService.getGroups()).thenReturn(groups);

        try {
            entitlementsAndCacheService.isValidAcl(headers, new HashSet<>(Arrays.asList("a", "b", "c")));
        }
        catch (Exception exception) {
            assertTrue(exception instanceof AppException);
            assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, ((AppException) exception).getError().getCode());
            assertEquals("Unknown error", ((AppException) exception).getError().getReason());
            assertEquals("Unknown error happened when validating ACL", ((AppException) exception).getError().getMessage());
        }
        verify(factory, times(1)).create(headers);
    }

    @Test
    public void isValidAclAppExceptionBecauseInvalidEmail() throws EntitlementsException {
        Groups groups = new Groups();
        groups.setDesId(desId);
        groups.setMemberEmail(EMAIL);
        groups.setGroups(Collections.singletonList(getGroupInfo(role, "invalid-email")));
        when(entitlementsService.getGroups()).thenReturn(groups);

        try {
            entitlementsAndCacheService.isValidAcl(headers, new HashSet<>(Arrays.asList("a", "b", "c")));
        }
        catch (Exception exception) {
            assertTrue(exception instanceof AppException);
            assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, ((AppException) exception).getError().getCode());
            assertEquals("Unknown error", ((AppException) exception).getError().getReason());
            assertEquals("Unknown error happened when validating ACL", ((AppException) exception).getError().getMessage());
        }
        verify(factory, times(1)).create(headers);
    }

    @Test
    public void isValidAclReturnsFalse() throws EntitlementsException {
        Groups groups = new Groups();
        groups.setDesId(desId);
        groups.setMemberEmail(EMAIL);
        groups.setGroups(Collections.singletonList(getGroupInfo(role, EMAIL)));
        when(entitlementsService.getGroups()).thenReturn(groups);

        boolean result = entitlementsAndCacheService.isValidAcl(headers, new HashSet<>(Collections.singletonList("a@b")));

        assertFalse(result);
        verify(factory, times(1)).create(headers);
        verify(entitlementsService, times(1)).getGroups();
    }

    @Test
    public void isValidAclReturnsTrue() throws EntitlementsException {
        Groups groups = new Groups();
        groups.setDesId(desId);
        groups.setMemberEmail(EMAIL);
        groups.setGroups(Collections.singletonList(getGroupInfo(role, EMAIL)));
        when(entitlementsService.getGroups()).thenReturn(groups);

        boolean result = entitlementsAndCacheService.isValidAcl(headers, new HashSet<>(Collections.singletonList("a@opendes.contoso.com")));

        assertTrue(result);
        verify(factory, times(1)).create(headers);
        verify(entitlementsService, times(1)).getGroups();
    }

    @Test
    public void hasOwnerAccess() throws EntitlementsException {
        Groups groups = new Groups();
        groups.setDesId(desId);
        groups.setMemberEmail(EMAIL);
        groups.setGroups(Collections.singletonList(getGroupInfo(role, EMAIL_OWNER)));
        when(entitlementsService.getGroups()).thenReturn(groups);

        boolean result = entitlementsAndCacheService.hasOwnerAccess(headers, new String[] {role});
        assertTrue(result);
        verify(factory, times(1)).create(headers);
        verify(entitlementsService, times(1)).getGroups();
    }

    @Test
    public void hasValidAccess() throws EntitlementsException {
        Groups groups = new Groups();
        groups.setDesId(desId);
        groups.setMemberEmail(EMAIL);
        groups.setGroups(Collections.singletonList(getGroupInfo(role, EMAIL_OWNER)));
        when(entitlementsService.getGroups()).thenReturn(groups);

        RecordMetadata recordMetadata = new RecordMetadata(getRecord());
        List<RecordMetadata> recordMetadataList = Collections.singletonList(recordMetadata);
        List<RecordMetadata> result = entitlementsAndCacheService.hasValidAccess(recordMetadataList, headers);

        assertNotNull(result);
        assertEquals(RECORD_ID, result.get(0).getId());
        assertEquals(KIND, result.get(0).getKind());
        verify(factory, times(1)).create(headers);
        verify(entitlementsService, times(1)).getGroups();
    }

    @Test
    public void hasValidAccessFalse() throws EntitlementsException {
        Groups groups = new Groups();
        groups.setDesId(desId);
        groups.setMemberEmail(EMAIL);
        groups.setGroups(Collections.singletonList(getGroupInfo(role, EMAIL_OWNER)));
        when(entitlementsService.getGroups()).thenReturn(groups);

        RecordMetadata recordMetadata = new RecordMetadata(getRecord());
        recordMetadata.setAcl(new Acl(new String[]{}, new String[]{}));
        List<RecordMetadata> recordMetadataList = Collections.singletonList(recordMetadata);
        List<RecordMetadata> result = entitlementsAndCacheService.hasValidAccess(recordMetadataList, headers);

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(factory, times(1)).create(headers);
        verify(entitlementsService, times(1)).getGroups();
    }

    private GroupInfo getGroupInfo(String name, String email) {
        GroupInfo groupInfo = new GroupInfo();
        groupInfo.setDescription("description");
        groupInfo.setName(name);
        groupInfo.setEmail(email);
        return groupInfo;
    }

    private Record getRecord() {
        Record record = new Record();
        record.setId(RECORD_ID);
        record.setKind(KIND);
        record.setAcl(new Acl(new String[]{ACL_VIEWER}, new String[]{ACL_OWNER}));

        return record;
    }
}
