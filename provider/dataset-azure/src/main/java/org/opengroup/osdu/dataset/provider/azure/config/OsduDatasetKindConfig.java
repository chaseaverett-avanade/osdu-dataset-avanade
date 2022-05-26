package org.opengroup.osdu.dataset.provider.azure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties("osdu.dataset.kind")
@Configuration
@Getter
@Setter
public class OsduDatasetKindConfig {

    private String file;
    private String fileCollection;
    private String connectedSource;
}
