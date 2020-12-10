/*
 * Copyright 2020 Google LLC
 * Copyright 2020 EPAM Systems, Inc
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

package org.opengroup.osdu.delivery.provider.gcp.service.downscoped;

import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.GenericData;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.gson.Gson;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@Slf4j
@EqualsAndHashCode(callSuper = true)
@ToString
public class DownScopedCredentials extends GoogleCredentials {

    private static final long serialVersionUID = -2133257318957488431L;
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String CLOUD_PLATFORM_SCOPE = "https://www.googleapis.com/auth/cloud-platform";
    private static final String IDENTITY_TOKEN_ENDPOINT = "https://sts.googleapis.com/v1beta/token";

    private static final String TOKEN_INFO_ENDPOINT = "https://www.googleapis.com/oauth2/v3/tokeninfo";
    public static final String EXPIRES_IN = "expires_in";

    private final GoogleCredentials finiteCredentials;
    private final DownScopedOptions downScopedOptions;

    public DownScopedCredentials(ServiceAccountCredentials sourceCredentials, DownScopedOptions downScopedOptions) {

        Collection<String> newScopes = sourceCredentials.createScopedRequired()
                ?  Collections.singletonList(CLOUD_PLATFORM_SCOPE) : sourceCredentials.getScopes();

        this.finiteCredentials = sourceCredentials.createScoped(newScopes);
        this.downScopedOptions = downScopedOptions;
    }

    @Override
    public AccessToken refreshAccessToken() throws IOException {
        log.debug("refreshAccessToken invoked for {}", this);
        try {
            this.finiteCredentials.refreshIfExpired();
        } catch (IOException e) {
            throw new IOException("Unable to refresh sourceCredentials", e);
        }

        AccessToken tok = this.finiteCredentials.getAccessToken();

        return getDownScopedToken(tok);
    }

    private AccessToken getDownScopedToken(AccessToken tok) throws IOException {
        JsonObjectParser parser = new JsonObjectParser(JSON_FACTORY);

        HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory();
        GenericUrl url = new GenericUrl(IDENTITY_TOKEN_ENDPOINT);

        String jsonPayload = getJsonPayload();

        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange");
        params.put("subject_token_type", "urn:ietf:params:oauth:token-type:access_token");
        params.put("requested_token_type", "urn:ietf:params:oauth:token-type:access_token");
        params.put("subject_token", tok.getTokenValue());
        params.put("options", jsonPayload);
        HttpContent content = new UrlEncodedContent(params);

        HttpRequest request = requestFactory.buildPostRequest(url, content);
        request.setParser(parser);

        HttpResponse response;
        try {
            response = request.execute();
        } catch (IOException e) {
            throw new IOException("Error requesting access token " + e.getMessage(), e);
        }

        if (response.getStatusCode() != HttpStatusCodes.STATUS_CODE_OK) {
            throw new IOException("Error getting access token " + response.toString());
        }

        GenericData responseData = response.parseAs(GenericData.class);
        response.disconnect();

        String accessToken = (String) responseData.get("access_token");

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());

        if (responseData.containsKey(EXPIRES_IN)) {
            cal.add(Calendar.SECOND, ((BigDecimal) responseData.get(EXPIRES_IN)).intValue());
        } else {
            GenericUrl genericUrl = new GenericUrl(TOKEN_INFO_ENDPOINT);
            genericUrl.put("access_token", tok.getTokenValue());
            HttpRequest tokenRequest = requestFactory.buildGetRequest(genericUrl);
            tokenRequest.setParser(parser);
            HttpResponse tokenResponse = tokenRequest.execute();
            if (tokenResponse.getStatusCode() != HttpStatusCodes.STATUS_CODE_OK) {
                throw new IOException("Error getting access_token expiration " + response.toString());
            }
            responseData = tokenResponse.parseAs(GenericData.class);
            tokenResponse.disconnect();
            cal.add(Calendar.SECOND, Integer.parseInt(responseData.get(EXPIRES_IN).toString()));
        }
        return new AccessToken(accessToken, cal.getTime());
    }

    private String getJsonPayload() {
        String jsonPayload;
        Gson gson = new Gson();
        jsonPayload = gson.toJson(this.downScopedOptions);
        return jsonPayload;
    }
}