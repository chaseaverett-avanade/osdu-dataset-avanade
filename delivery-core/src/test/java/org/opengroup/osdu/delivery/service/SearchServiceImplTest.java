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

import static org.mockito.MockitoAnnotations.initMocks;

import com.google.gson.Gson;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.http.FetchServiceHttpRequest;
import org.opengroup.osdu.core.common.http.IUrlFetchService;
import org.opengroup.osdu.core.common.model.http.HttpResponse;
import org.opengroup.osdu.core.common.model.search.QueryResponse;
import org.opengroup.osdu.delivery.DeliveryApplication;
import org.opengroup.osdu.delivery.model.SrnFileData;
import org.opengroup.osdu.delivery.model.UrlSigningResponse;
import org.opengroup.osdu.delivery.provider.interfaces.IUnsignedUrlLocationMapper;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest(classes={DeliveryApplication.class})
public class SearchServiceImplTest {

    @InjectMocks
    private SearchServiceImpl CUT;

    @Mock
    private IUrlFetchService urlFetchService;

    @Mock
    private IUnsignedUrlLocationMapper unsignedUrlLocationMapper;

    private final Gson gson = new Gson();

    private final String QUERY_MAX = "1000";

    private String query = "{\"offset\":0,\"kind\":\"*:*:*:*.*.*\",\"limit\":"+ QUERY_MAX + ",\"query\"" +
            ":\"data.ResourceID: (\\\"srn:file/csv:7344999246049527:\\\" OR \\\"srn:file/csv:69207556434748899880399:\\\"" +
            " OR \\\"srn:file/csv:59158134479121976019:\\\")\",\"returnHighlightedFields\":false,\"returnedFields\":[],\"" +
            "queryAsOwner\":false}";

    private String responseBody = "{\"results\":[{\"data\":{\"ResourceID\":\"srn:file/csv:7344999246049527:\"," +
            "\"Data.GroupTypeProperties.PreLoadFilePath\":\"s3://aws-osdu-demo-r2/data/provided/markers_csv/3676.csv\"," +
            "\"Data.GroupTypeProperties.FileSource\":\"\",\"ResourceTypeID\":\"srn:type:file/csv:\"," +
            "\"ResourceSecurityClassification\":\"srn:reference-data/ResourceSecurityClassification:RESTRICTED:\"," +
            "\"AssociativeID\":\"f-1\"},\"kind\":\"opendes:osdu:file:0.0.4\",\"namespace\":\"opendes:osdu\",\"legal\":" +
            "{\"legaltags\":[\"opendes-public-usa-dataset-1\"],\"otherRelevantDataCountries\":[\"US\"],\"status\":\"compliant\"}," +
            "\"id\":\"opendes:doc:543b05af7d094739a7e6ad1496cbf5ec\",\"acl\":{\"viewers\":[\"data.default.viewers@opendes.testing.com\"]," +
            "\"owners\":[\"data.default.owners@opendes.testing.com\"]},\"type\":\"file\",\"version\":1585246320682077}," +
            "{\"data\":{\"ResourceID\":\"srn:file/csv:69207556434748899880399:\",\"Data.GroupTypeProperties.PreLoadFilePath\":" +
            "\"s3://aws-osdu-demo-r2/data/provided/markers_csv/3675.csv\",\"Data.GroupTypeProperties.FileSource\":\"\"," +
            "\"ResourceTypeID\":\"srn:type:file/csv:\",\"ResourceSecurityClassification\":\"srn:reference-data/ResourceSecurityClassification:RESTRICTED:\"," +
            "\"AssociativeID\":\"f-1\"},\"kind\":\"opendes:osdu:file:0.0.4\",\"namespace\":\"opendes:osdu\",\"legal\"" +
            ":{\"legaltags\":[\"opendes-public-usa-dataset-1\"],\"otherRelevantDataCountries\":[\"US\"],\"status\"" +
            ":\"compliant\"},\"id\":\"opendes:doc:d66f60d44adc4cafa6ad82a35c0f038f\",\"acl\":{\"viewers\":" +
            "[\"data.default.viewers@opendes.testing.com\"],\"owners\":[\"data.default.owners@opendes.testing.com\"]}," +
            "\"type\":\"file\",\"version\":1585246320682077},{\"data\":{\"ResourceID\":\"srn:file/csv:59158134479121976019:\"," +
            "\"Data.GroupTypeProperties.PreLoadFilePath\":\"s3://aws-osdu-demo-r2/data/provided/markers_csv/3713.csv\"," +
            "\"Data.GroupTypeProperties.FileSource\":\"\",\"ResourceTypeID\":\"srn:type:file/csv:\"," +
            "\"ResourceSecurityClassification\":\"srn:reference-data/ResourceSecurityClassification:RESTRICTED:\"," +
            "\"AssociativeID\":\"f-1\"},\"kind\":\"opendes:osdu:file:0.0.4\",\"namespace\":\"opendes:osdu\",\"legal\"" +
            ":{\"legaltags\":[\"opendes-public-usa-dataset-1\"],\"otherRelevantDataCountries\":[\"US\"],\"status\":" +
            "\"compliant\"},\"id\":\"opendes:doc:9aeb123f35394422b84983f6464d8d68\",\"acl\":{\"viewers\":" +
            "[\"data.default.viewers@opendes.testing.com\"],\"owners\":[\"data.default.owners@opendes.testing.com\"]}," +
            "\"type\":\"file\",\"version\":1585246327481017}],\"aggregations\":null,\"totalCount\":3}";

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void GetUnsignedUrlsBySrn() throws URISyntaxException {
        // Arrange
        List<String> srns = new ArrayList<>();
        srns.add("srn:file/csv:7344999246049527:");
        srns.add("srn:file/csv:69207556434748899880399:");
        srns.add("srn:file/csv:59158134479121976019:");

        HttpResponse response = new HttpResponse();
        response.setBody(responseBody);
        Mockito.when(urlFetchService.sendRequest(Mockito.any()))
                .thenReturn(response);

        QueryResponse queryResponse = this.gson.fromJson(responseBody, QueryResponse.class);

        Map<String, Object> searchResult1 = queryResponse.getResults().get(0);
        Map<String, Object> searchResult2 = queryResponse.getResults().get(1);
        Map<String, Object> searchResult3 = queryResponse.getResults().get(2);
        String unsignedUrl1 = "s3://aws-osdu-demo-r2/data/provided/markers_csv/3676.csv";
        Mockito.when(unsignedUrlLocationMapper.getUnsignedURLFromSearchResponse(searchResult1)).thenReturn(unsignedUrl1);
        String unsignedUrl2 = "s3://aws-osdu-demo-r2/data/provided/markers_csv/3675.csv";
        Mockito.when(unsignedUrlLocationMapper.getUnsignedURLFromSearchResponse(searchResult2)).thenReturn(unsignedUrl2);
        Mockito.when(unsignedUrlLocationMapper.getUnsignedURLFromSearchResponse(searchResult3)).thenReturn(null);

        Map<String, SrnFileData> processed = new HashMap<>();
        processed.put("srn:file/csv:7344999246049527:", new SrnFileData(null, unsignedUrl1, searchResult1.get("kind").toString()));
        processed.put("srn:file/csv:69207556434748899880399:", new SrnFileData(null, unsignedUrl2, searchResult2.get("kind").toString()));

        List<String> unprocessed = new ArrayList<>();
        unprocessed.add("srn:file/csv:59158134479121976019:");

        // Manually set test environment variables
        ReflectionTestUtils.setField(CUT, "SEARCH_BATCH_SIZE", "100", String.class);
        ReflectionTestUtils.setField(CUT, "SEARCH_QUERY_LIMIT", QUERY_MAX, String.class);

        // Act
        UrlSigningResponse actual = CUT.GetUnsignedUrlsBySrn(srns);

        // Assert
        Assert.assertEquals(processed, actual.getProcessed());
        Assert.assertEquals(unprocessed, actual.getUnprocessed());
    }

    @Test
    public void GetUnsignedUrlsBySrn_unsignedLocationMapperReturnsNull_addsSrnToUnprocessedList() throws URISyntaxException {
        // Arrange
        List<String> srns = new ArrayList<>();
        srns.add("srn:file/csv:7344999246049527:");
        srns.add("srn:file/csv:69207556434748899880399:");
        srns.add("srn:file/csv:59158134479121976019:");

        HttpResponse response = new HttpResponse();
        response.setBody(responseBody);
        Mockito.when(urlFetchService.sendRequest(Mockito.any()))
                .thenReturn(response);

        Mockito.when(unsignedUrlLocationMapper.getUnsignedURLFromSearchResponse(Mockito.anyMap())).thenReturn(null);

        // Manually set test environment variables
        ReflectionTestUtils.setField(CUT, "SEARCH_BATCH_SIZE", "100", String.class);
        ReflectionTestUtils.setField(CUT, "SEARCH_QUERY_LIMIT", QUERY_MAX, String.class);

        // Act
        UrlSigningResponse actual = CUT.GetUnsignedUrlsBySrn(srns);

        // Assert
        Assert.assertEquals(srns, actual.getUnprocessed());
        Assert.assertTrue(actual.getProcessed().isEmpty());
    }

    @Test
    public void getSearchRecordsByRecordID() throws URISyntaxException {
        // Arrange
        List<String> recordIds = new ArrayList<>();
        recordIds.add("srn:file/csv:7344999246049527:");
        recordIds.add("srn:file/csv:69207556434748899880399:");
        recordIds.add("srn:file/csv:59158134479121976019:");

        HttpResponse response = new HttpResponse();
        response.setBody(responseBody);
        Mockito.when(urlFetchService.sendRequest(Mockito.any()))
                .thenReturn(response);

        QueryResponse expected = this.gson.fromJson(responseBody, QueryResponse.class);

        // Manually set test environment variables
        ReflectionTestUtils.setField(CUT, "SEARCH_BATCH_SIZE", "100", String.class);
        ReflectionTestUtils.setField(CUT, "SEARCH_QUERY_LIMIT", QUERY_MAX, String.class);

        // Act
        QueryResponse actual = CUT.getSearchRecordsByRecordID(recordIds);

        // Assert
        Assert.assertEquals(expected.getResults(), actual.getResults());
        Assert.assertEquals(expected.getTotalCount(), actual.getTotalCount());
        Assert.assertEquals(expected.getAggregations(), actual.getAggregations());
    }

    @Test
    public void getSearchRecordsByRecordID_urlFetchServiceReturnsEmptyResponse_returnsUnprocessed() throws URISyntaxException {
        // Arrange
        List<String> recordIds = new ArrayList<>();
        recordIds.add("srn:file/csv:7344999246049527:");
        recordIds.add("srn:file/csv:69207556434748899880399:");
        recordIds.add("srn:file/csv:59158134479121976019:");
        FetchServiceHttpRequest.builder().httpMethod("POST").build();
        Mockito.when(urlFetchService.sendRequest(Mockito.any()))
                .thenReturn(new HttpResponse());

        // Manually set test environment variables
        ReflectionTestUtils.setField(CUT, "SEARCH_BATCH_SIZE", "100", String.class);
        ReflectionTestUtils.setField(CUT, "SEARCH_QUERY_LIMIT", QUERY_MAX, String.class);

        // Act
        QueryResponse actual = CUT.getSearchRecordsByRecordID(recordIds);

        // Assert
        Assert.assertTrue(actual.getResults().isEmpty());
        Assert.assertEquals(actual.getTotalCount(), 0);
    }
}
