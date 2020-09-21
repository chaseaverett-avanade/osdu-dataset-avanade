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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.delivery.model.SignedUrl;
import org.opengroup.osdu.delivery.model.SrnFileData;
import org.opengroup.osdu.delivery.model.UrlSigningResponse;
import org.opengroup.osdu.delivery.provider.interfaces.IStorageService;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class LocationServiceImpl implements ILocationService {

  @Inject
  private DpsHeaders headers;
  final IStorageService storageService;
  final ISearchService searchService;

  @Override
  public UrlSigningResponse getSignedUrlsBySrn(List<String> srns) {

    UrlSigningResponse unsignedUrls = searchService.GetUnsignedUrlsBySrn(srns);

    return getSignedUrls(unsignedUrls);
  }

  private UrlSigningResponse getSignedUrls(UrlSigningResponse unsignedUrls) {

    List<String> unprocessed = unsignedUrls.getUnprocessed();
    Map<String, SrnFileData> processed = new HashMap<>();

    for (Map.Entry<String, SrnFileData> entry : unsignedUrls.getProcessed().entrySet()) {

      SrnFileData value = entry.getValue();

      SignedUrl signedUrl = storageService.createSignedUrl(entry.getKey(), value.getUnsignedUrl(), headers.getAuthorization());

      if(signedUrl != null && signedUrl.getUrl() != null){
    	if(!(entry.getKey().toLowerCase().contains("ovds"))){
        value.setSignedUrl(signedUrl.getUrl().toString());
    	}
        value.setConnectionString(signedUrl.getConnectionString());
        processed.put(entry.getKey(), value);
      } else {
        unprocessed.add(entry.getKey());
      }
    }

    return UrlSigningResponse.builder().processed(processed).unprocessed(unprocessed).build();
  }
}
