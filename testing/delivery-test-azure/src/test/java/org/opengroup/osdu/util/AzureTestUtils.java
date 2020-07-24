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

package org.opengroup.osdu.util;

import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.junit.Ignore;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@Ignore
public class AzureTestUtils {

	protected String baseUrl;

	public AzureTestUtils(){
		baseUrl = System.getProperty("LEGAL_HOST", System.getenv("LEGAL_HOST"));
	}

	public boolean isLocalHost(){
		return baseUrl.contains("//localhost");
	}

	public static String getMyProjectAccountId(){
		return System.getProperty("MY_TENANT_PROJECT", System.getenv("MY_TENANT_PROJECT"));
	}

	public String getBaseHost() {return baseUrl.substring(8,baseUrl.length()-1);}

	public String getApiPath(String api) throws MalformedURLException {
		URL mergedURL = new URL(baseUrl + api);
		return mergedURL.toString();
	}
	
	public static String getMyDataPartition(){
		return Config.getDataPartitionIdTenant1();
	}

	public ClientResponse send(String path, String httpMethod, String token, String requestBody, String query)
			throws Exception {

        Map<String, String> headers = getHeaders();

		return send(path, httpMethod, token, requestBody, query, headers);
	}

    public Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();

        //either header should work the same for now so assign either to validate this
		headers.put("data-partition-id", getMyDataPartition());

        return headers;
    }

    public ClientResponse send(String path, String httpMethod, String token, String requestBody, String query, Map<String,String> headers) throws MalformedURLException {

        Client client = getClient();
        WebResource webResource = client.resource(getApiPath(path + query));
        final WebResource.Builder builder = webResource.accept("application/json").type("application/json")
                .header("Authorization", token);
        headers.forEach((k, v) -> builder.header(k, v));
        ClientResponse response = builder.method(httpMethod, ClientResponse.class, requestBody);

        return response;
    }

	@SuppressWarnings("unchecked")
	public <T> T getResult(ClientResponse response, int exepectedStatus, Class<T> classOfT) {
		String json = response.getEntity(String.class);
		System.out.println(json);

		assertEquals(exepectedStatus, response.getStatus());
		if (exepectedStatus == 204) {
			return null;
		}

		assertEquals("application/json; charset=UTF-8", response.getType().toString());
		if (classOfT == String.class) {
			return (T) json;
		}

		Gson gson = new Gson();
		return gson.fromJson(json, classOfT);
	}

	public Client getClient() {
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
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
		} };

		try {
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, trustAllCerts, new SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception e) {
		}

		return Client.create();
	}
}

