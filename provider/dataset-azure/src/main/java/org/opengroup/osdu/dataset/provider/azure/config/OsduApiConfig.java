package org.opengroup.osdu.dataset.provider.azure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties("osdu.api.endpoints")
@Configuration
@Getter
@Setter
public class OsduApiConfig {

    String file;
    String fileCollection;
    String connectedSource;
}
