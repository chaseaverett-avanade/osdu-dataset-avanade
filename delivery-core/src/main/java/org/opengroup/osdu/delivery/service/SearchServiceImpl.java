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

import com.google.api.client.http.HttpMethods;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.http.FetchServiceHttpRequest;
import org.opengroup.osdu.core.common.http.IUrlFetchService;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.HttpResponse;
import org.opengroup.osdu.core.common.model.search.QueryRequest;
import org.opengroup.osdu.core.common.model.search.QueryResponse;
import org.opengroup.osdu.delivery.model.SrnFileData;
import org.opengroup.osdu.delivery.model.UrlSigningResponse;
import org.opengroup.osdu.delivery.provider.interfaces.IUnsignedUrlLocationMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log
@Component
public class SearchServiceImpl implements ISearchService {

    private final Gson gson = new Gson();

    @Inject
    private IUrlFetchService urlFetchService;
    @Inject
    private DpsHeaders dpsHeaders;
    @Inject
    private JaxRsDpsLog jaxRsDpsLog;
    @Inject
    private IUnsignedUrlLocationMapper unsignedUrlLocationMapper;

    @Value("${SEARCH_QUERY_RECORD_HOST}")
    private String SEARCH_QUERY_RECORD_HOST;

    @Value("${SEARCH_BATCH_SIZE}")
    private String SEARCH_BATCH_SIZE;

    @Value("${SEARCH_QUERY_LIMIT}")
    private String SEARCH_QUERY_LIMIT;

    protected DpsHeaders getDpsHeaders() {
        return dpsHeaders;
    }

    @Override
    public UrlSigningResponse GetUnsignedUrlsBySrn(List<String> srns) {
        QueryResponse searchResponse;
        List<String> unprocessedSrns = new ArrayList<>(srns);
        List<String> notFound = new ArrayList<>();
        Map<String, SrnFileData> parsed = new HashMap<>();

        try {
            searchResponse = getSearchRecordsByRecordID(unprocessedSrns);
        } catch (URISyntaxException e) {
            throw new AppException(HttpStatus.SC_BAD_REQUEST, "Malformed URL", "There was an error connecting to search service.", e);
        }

        int processedCount = 0;
        for(Map<String, Object> searchResult : searchResponse.getResults()){
            String kind = null;
            String srn = null;
            Map<String, Object> data = null;

            Object currentNode = searchResult.get("data");
            if(currentNode != null) {
                try {
                    data = (Map<String, Object>) currentNode;
                } catch (ClassCastException ignored) {} // Unable to parse the current node; add this to the unprocessed list.

                if(data != null)
                    srn = data.get("ResourceID").toString();
            }

            if (srn == null) {
                srn = "UnknownSRN:" + processedCount;
            }

            if (searchResult.get("kind") != null) {
                kind = searchResult.get("kind").toString();
            }

            String unsignedURL = unsignedUrlLocationMapper.getUnsignedURLFromSearchResponse(searchResult);

            SrnFileData srnFileData = new SrnFileData(null, unsignedURL, kind, null);
            if(unsignedURL != null) {
                parsed.put(srn, srnFileData);
            } else {
                notFound.add(srn);
            }
            processedCount++;
            unprocessedSrns.remove(srn);
        }

        if (unprocessedSrns != null && unprocessedSrns.size() > 0) {
            notFound.addAll(unprocessedSrns);
        }

        return UrlSigningResponse.builder().processed(parsed).unprocessed(notFound).build();
    }

    @Override
    public QueryResponse getSearchRecordsByRecordID(List<String> recordIds) throws URISyntaxException {
        int totalCount = 0;
        List<Map<String, Object>> results = new ArrayList<>();

        int batchSize = Integer.parseInt(SEARCH_BATCH_SIZE);
        int searchLimit = Integer.parseInt(SEARCH_QUERY_LIMIT);

        // items must be batched in groups smaller than the limit of searchable in order to get all the values.
        if(batchSize > searchLimit)
            batchSize = searchLimit;

        List<List<String>> batch = Lists.partition(recordIds, batchSize);
        for (List<String> recordsBatch : batch) {
            QueryResponse batchResponse = this.searchRecordsByRecordId(recordsBatch, searchLimit);
            totalCount += batchResponse.getTotalCount();
            results.addAll(batchResponse.getResults());
        }
        return QueryResponse.builder().results(results).totalCount(totalCount).build();
    }

    protected void setQueryKind(QueryRequest query) {
        // Query across all Kinds
        query.setKind("*:*:*:*.*.*");
    }

    private QueryResponse searchRecordsByRecordId(List<String> ids, int limit) throws URISyntaxException {
        QueryRequest query = new QueryRequest();

        this.setQueryKind(query);

        query.setLimit(limit);

        // e.g. "data.ResourceID: \"srn:master-data/Well:7806:\" OR data.ResourceID: \"srn:master-data/Well:5587:\""
        query.setQuery(generateSrnQueryString(ids));
        FetchServiceHttpRequest request = FetchServiceHttpRequest
                .builder()
                .httpMethod(HttpMethods.POST)
                .url(SEARCH_QUERY_RECORD_HOST)
                .headers(dpsHeaders)
                .body(query.toString()).build();

        HttpResponse response = this.urlFetchService.sendRequest(request);

        String dataFromSearch = response.getBody();

        QueryResponse queryResponse = this.gson.fromJson(dataFromSearch, QueryResponse.class);

        if (queryResponse == null) {
            queryResponse = QueryResponse.getEmptyResponse();
        }

        if (queryResponse.getTotalCount() > limit){
            //TODO: Use a cursor and get the rest of the results
            jaxRsDpsLog.warning(String.format("Search found more records than could be returned. | search limit is: %d | records found: %d", limit, queryResponse.getTotalCount()));
        }

        return queryResponse;
    }

    private String generateSrnQueryString(List<String> ids) {
        String QUERY_SEARCH_ATTRIBUTE = "data.ResourceID";

        List<String> quotedIds = ids.stream().map(id -> String.format("\"%s\"", id)).collect(Collectors.toList());
        String joinedIds = StringUtils.join(quotedIds, " OR ");
        return  String.format("%s: (%s)",QUERY_SEARCH_ATTRIBUTE,joinedIds);
    }
}