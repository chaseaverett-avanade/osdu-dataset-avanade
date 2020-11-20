/*
 * Copyright 2020 Microsoft Corporation
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

package org.opengroup.osdu.delivery.provider.azure.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.delivery.model.SignedUrl;
import org.opengroup.osdu.delivery.provider.interfaces.IStorageService;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

@Service
@Slf4j
@RequiredArgsConstructor
public class StorageServiceImpl implements IStorageService {

    @Inject
    private AzureBlobSasTokenServiceImpl tokenService;

    private ExpirationDateHelper expirationDateHelper;

    private InstantHelper instantHelper;

    private final static String URI_EXCEPTION_REASON = "Exception creating signed url";
    private final static String UNSUPPORTED_EXCEPTION_REASON = "Unsupported operation exception";
    private final static String INVALID_AZURESTORAGESERVICE_PATH_REASON = "Unsigned url invalid, needs to be full path";


    @PostConstruct
    public void init() {
        expirationDateHelper = new ExpirationDateHelper();
        instantHelper = new InstantHelper();
    }

    @Override
    public SignedUrl createSignedUrl(String srn, String unsignedUrl, String authorizationToken) {
        String signedUrl;
        String[] azureStoragePathParts = unsignedUrl.split("https://");
        if (azureStoragePathParts.length < 2){
            throw new AppException(HttpStatus.SC_BAD_REQUEST, "Malformed URL", INVALID_AZURESTORAGESERVICE_PATH_REASON);
        }

        String[] azureStorageObjectKeyParts = azureStoragePathParts[1].split("/");
        if (azureStorageObjectKeyParts.length < 1){
            throw new AppException(HttpStatus.SC_BAD_REQUEST, "Malformed URL", INVALID_AZURESTORAGESERVICE_PATH_REASON);
        }

        if (isSrnForContainer(srn)) {
            signedUrl = tokenService.signContainer(unsignedUrl);
        }
        else {
            signedUrl = tokenService.sign(unsignedUrl);
        }
        SignedUrl url = new SignedUrl();
        try {
            url.setUri(new URI(signedUrl));
            url.setUrl(new URL(signedUrl));
            url.setCreatedAt(instantHelper.getCurrentInstant());
        } catch(URISyntaxException | MalformedURLException e) {
            log.error("There was an error generating the URI.",e);
            throw new AppException(HttpStatus.SC_BAD_REQUEST, "Malformed URL", URI_EXCEPTION_REASON, e);
        }

        return url;
    }


    @Override
    public SignedUrl createSignedUrl(String unsignedUrl, String authorizationToken) {
        throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Unsupported Operation Exception",UNSUPPORTED_EXCEPTION_REASON);
    }


    private boolean isSrnForContainer(String srn) {
        boolean result = false;

        if (srn != null && srn.contains("ovds")) {
            result = true;
        }
        return result;
    }

}
