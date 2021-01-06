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

package org.opengroup.osdu.util;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import lombok.ToString;
import lombok.extern.java.Log;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.MediaType;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Log
@ToString
public abstract class HTTPClient {

    private static Random random = new Random();
    private final int MAX_ID_SIZE = 50;

    private static final String HEADER_CORRELATION_ID = "correlation-id";
    
    public abstract String getAccessToken();

    private static Client getClient() {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }};

        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception ignored) {
        }
        return Client.create();
    }

    public ClientResponse send(String httpMethod, String url, String payLoad, Map<String, String> headers, String token) {
        ClientResponse response;
        try {
            String correlationId = java.util.UUID.randomUUID().toString();
            log.info(String.format("Request correlation id: %s", correlationId));
            headers.put(HEADER_CORRELATION_ID, correlationId);
            Client client = getClient();
            client.setReadTimeout(180000);
            client.setConnectTimeout(10000);
            WebResource webResource = client.resource(url);
            response = this.getClientResponse(httpMethod, payLoad, webResource, headers, token);
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError("Error: Send request error", e);
        }
        log.info("waiting on response");
        return response;
    }

    private ClientResponse getClientResponse(String httpMethod, String requestBody, WebResource webResource, Map<String, String> headers, String token) {
        final WebResource.Builder builder = webResource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON).header("Authorization", token);
        headers.forEach(builder::header);
        log.info("making request to datalake api");
        return builder.method(httpMethod, ClientResponse.class, requestBody);
    }

    public Map<String, String> getCommonHeader() {
        Map<String, String> headers = new HashMap<>();
        headers.put("data-partition-id", Config.getDataPartitionIdTenant1());
        return headers;
    }

    public static Map<String, String> overrideHeader(Map<String, String> currentHeaders, String... partitions) {
        String value = String.join(",", partitions);
        currentHeaders.put("data-partition-id", value);
        return currentHeaders;
    }
}