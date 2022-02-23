// Copyright © 2021 Amazon Web Services
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

package org.opengroup.osdu.dataset.provider.aws.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.opengroup.osdu.core.aws.cache.DummyCache;
import org.opengroup.osdu.core.aws.ssm.K8sLocalParameterProvider;
import org.opengroup.osdu.core.aws.ssm.K8sParameterNotFoundException;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.cache.VmCache;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;

import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.core.common.util.Crc32c;
import org.opengroup.osdu.dataset.provider.aws.model.DmsRegistrations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("DmsRegistrationCache")
public class DmsRegistrationCache implements ICache<String, DmsRegistrations> {
    @Value("${aws.elasticache.cluster.endpoint:null}")
    String REDIS_SEARCH_HOST;
    @Value("${aws.elasticache.cluster.port:null}")
    String REDIS_SEARCH_PORT;
    @Value("${aws.elasticache.cluster.key:null}")
    String REDIS_SEARCH_KEY;
    private ICache cache;

    public DmsRegistrationCache() throws K8sParameterNotFoundException, JsonProcessingException {
        K8sLocalParameterProvider provider = new K8sLocalParameterProvider();
        if (provider.getLocalMode()) {
            if (Boolean.parseBoolean(System.getenv("DISABLE_CACHE"))) {
                this.cache = new DummyCache();
            }
            this.cache = new VmCache<>(60, 10);
        } else {
            String host = provider.getParameterAsStringOrDefault("CACHE_CLUSTER_ENDPOINT", REDIS_SEARCH_HOST);
            int port = Integer.parseInt(provider.getParameterAsStringOrDefault("CACHE_CLUSTER_PORT", REDIS_SEARCH_PORT));
            Map<String, String> credential = provider.getCredentialsAsMap("CACHE_CLUSTER_KEY");
            String password;
            if (credential != null) {
                password = credential.get("token");
            } else {
                password = REDIS_SEARCH_KEY;
            }
            this.cache = new RedisCache(host, port, password, 60, String.class, DmsRegistrations.class);
        }
    }

    public static String getCacheKey(DpsHeaders headers) {
        String key = String.format("dms-registration:%s:%s", headers.getPartitionIdWithFallbackToAccountId(),
                headers.getAuthorization());
        return Crc32c.hashToBase64EncodedString(key);
    }

    @Override
    public void put(String k, DmsRegistrations o) {
        this.cache.put(k, o);
    }

    @Override
    public DmsRegistrations get(String k) {
        return (DmsRegistrations) this.cache.get(k);
    }

    @Override
    public void delete(String k) {
        this.cache.delete(k);
    }

    @Override
    public void clearAll() {
        this.cache.clearAll();
    }
}
