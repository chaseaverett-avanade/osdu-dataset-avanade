// Copyright © 2020 Amazon Web Services
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

package org.opengroup.osdu.delivery.service.datasetregistryhandlers;

import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.delivery.model.Records;
import org.opengroup.osdu.delivery.model.SignedUrl;
import org.opengroup.osdu.delivery.provider.interfaces.IStorageService;
import org.opengroup.osdu.delivery.provider.interfaces.IUnsignedUrlLocationMapper;


public class GenericFileAccessHandler implements IDatasetRegistryAccessHandler {
    private IStorageService storageService;

    private IUnsignedUrlLocationMapper unsignedUrlLocationMapper;

    private DpsHeaders headers;

    public GenericFileAccessHandler(IStorageService storageService, IUnsignedUrlLocationMapper unsignedUrlLocationMapper, DpsHeaders headers){
        this.storageService = storageService;
        this.unsignedUrlLocationMapper = unsignedUrlLocationMapper;
        this.headers = headers;
    }

    /**
     * Finds the unsigned path on a record keying off of a specific data attribute and generates a signed url
     * and connection string via SPI
     * @param record
     * @return
     * @throws DatasetRegistryAccessException
     */
    @Override
    public SignedUrl getAccess(Records.Entity record) throws DatasetRegistryAccessException {
        String unsignedUrl = unsignedUrlLocationMapper.getUnsignedURLFromRecordData(record.getData());
        SignedUrl signedUrl = storageService.createSignedUrl(record.getId(), unsignedUrl, headers.getAuthorization());

        if(unsignedUrl == "" || signedUrl == null){
            throw new DatasetRegistryAccessException(String.format("Unable to get signed url for record: %s", record.getId()));
        }

        return signedUrl;
    }
}
