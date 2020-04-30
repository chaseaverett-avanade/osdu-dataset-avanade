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

package org.opengroup.osdu.delivery.service;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.delivery.DeliveryApplication;
import org.opengroup.osdu.delivery.model.SignedUrl;
import org.opengroup.osdu.delivery.model.SrnFileData;
import org.opengroup.osdu.delivery.model.UrlSigningResponse;
import org.opengroup.osdu.delivery.provider.interfaces.IStorageService;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest(classes={DeliveryApplication.class})
public class LocationServiceImplTest {

    @InjectMocks
    private LocationServiceImpl CUT;

    @Mock
    private IStorageService storageService;

    @Mock
    private ISearchService searchService;

    @Test
    public void getSignedUrlsBySrn() throws URISyntaxException, MalformedURLException {
        // Arrange
        String srn1 = "srn:file/csv:7344999246049527:";
        String srn2 = "srn:file/csv:69207556434748899880399:";
        String srn3 = "srn:file/csv:59158134479121976019:";
        String unsignedUrl1 = "http://unsignedurl1.com";
        String unsignedUrl2 = "http://unsignedurl2.com";
        String unsignedUrl3 = "http://unsignedurl3.com";
        String kind = "opendes:osdu:file:0.0.4";

        List<String> srns = new ArrayList<>();
        srns.add(srn1);
        srns.add(srn2);
        srns.add(srn3);

        Map<String, SrnFileData> processed = new HashMap<>();
        processed.put(srn1, new SrnFileData(null, unsignedUrl1, kind));
        processed.put(srn2, new SrnFileData(null, unsignedUrl2, kind));
        processed.put(srn3, new SrnFileData(null, unsignedUrl3, kind));
        List<String> unprocessed = new ArrayList<>();

        UrlSigningResponse unsignedUrlsResponse = UrlSigningResponse.builder().processed(processed).unprocessed(unprocessed).build();
        Mockito.when(searchService.GetUnsignedUrlsBySrn(Mockito.eq(srns))).thenReturn(unsignedUrlsResponse);

        SignedUrl signedUrl1 = new SignedUrl();
        signedUrl1.setUri(new URI(unsignedUrl1));
        signedUrl1.setUrl(new URL(unsignedUrl1));
        signedUrl1.setCreatedAt(Instant.now());

        SignedUrl signedUrl2 = new SignedUrl();
        signedUrl2.setUri(new URI(unsignedUrl2));
        signedUrl2.setUrl(new URL(unsignedUrl2));
        signedUrl2.setCreatedAt(Instant.now());

        SignedUrl signedUrl3 = new SignedUrl();
        signedUrl3.setUri(new URI(unsignedUrl3));
        signedUrl3.setUrl(new URL(unsignedUrl3));
        signedUrl3.setCreatedAt(Instant.now());

        Mockito.when(storageService.createSignedUrl(Mockito.eq(unsignedUrl1), Mockito.any())).thenReturn(signedUrl1);
        Mockito.when(storageService.createSignedUrl(Mockito.eq(unsignedUrl2), Mockito.any())).thenReturn(signedUrl2);
        Mockito.when(storageService.createSignedUrl(Mockito.eq(unsignedUrl3), Mockito.any())).thenReturn(signedUrl3);

        ReflectionTestUtils.setField(CUT, "headers", new DpsHeaders());

        // Act
        UrlSigningResponse actual = CUT.getSignedUrlsBySrn(srns);

        // Assert
        Assert.assertEquals(processed, actual.getProcessed());
        Assert.assertEquals(unprocessed, actual.getUnprocessed());
    }
}
