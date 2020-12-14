package org.opengroup.osdu.delivery.provider.azure.di;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.inject.Named;

@Component
public class AzureBootstrapConfig {

    @Value("${azure.keyvault.url}")
    private String keyVaultURL;

    @Bean
    @Named("KEY_VAULT_URL")
    public String keyVaultURL() {
        return keyVaultURL;
    }
}