// Copyright © Microsoft Corporation
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

package org.opengroup.osdu.delivery.provider.azure.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobUrlParts;
import com.azure.storage.blob.models.UserDelegationKey;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import lombok.extern.java.Log;
import org.opengroup.osdu.azure.blobstorage.IBlobServiceClientFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/*
For a given blob object, generator a SAS Token that'll let bearers access the blob for 24 hours.
 */
@Log
@Component
public class AzureBlobSasTokenServiceImpl {

    @Autowired
    private IBlobServiceClientFactory blobServiceClientFactory;

    public String signContainer(final String dataPartitionId, final String containerUrl) {
        BlobUrlParts parts = BlobUrlParts.parse(containerUrl);
        BlobServiceClient rbacKeySource = this.blobServiceClientFactory.getBlobServiceClient(dataPartitionId);
        BlobContainerClient blobContainerClient = rbacKeySource.getBlobContainerClient(parts.getBlobContainerName());

        OffsetDateTime expiresInHalfADay = calcTokenExpirationDate();
        UserDelegationKey key = rbacKeySource.getUserDelegationKey(null, expiresInHalfADay);

        BlobSasPermission readOnlyPerms = BlobSasPermission.parse("r");
        BlobServiceSasSignatureValues tokenProps = new BlobServiceSasSignatureValues(expiresInHalfADay, readOnlyPerms);

        String sasToken = blobContainerClient.generateUserDelegationSas(tokenProps, key);
        return String.format("%s?%s", containerUrl, sasToken);
    }

    public String sign(final String dataPartitionId, final String blobUrl) {
        BlobUrlParts parts = BlobUrlParts.parse(blobUrl);
        BlobServiceClient rbacKeySource = this.blobServiceClientFactory.getBlobServiceClient(dataPartitionId);
        BlobContainerClient blobContainerClient = rbacKeySource.getBlobContainerClient(parts.getBlobContainerName());
        BlobClient tokenSource = blobContainerClient.getBlobClient(blobUrl);

        OffsetDateTime expiresInHalfADay = calcTokenExpirationDate();
        UserDelegationKey key = rbacKeySource.getUserDelegationKey(null, expiresInHalfADay);
        BlobSasPermission readOnlyPerms = BlobSasPermission.parse("r");
        BlobServiceSasSignatureValues tokenProps = new BlobServiceSasSignatureValues(expiresInHalfADay, readOnlyPerms);

        String sasToken = tokenSource.generateUserDelegationSas(tokenProps, key);
        return String.format("%s?%s", blobUrl, sasToken);
    }

    private OffsetDateTime calcTokenExpirationDate() {
        return OffsetDateTime.now(ZoneOffset.UTC).plusHours(12);
    }
}
