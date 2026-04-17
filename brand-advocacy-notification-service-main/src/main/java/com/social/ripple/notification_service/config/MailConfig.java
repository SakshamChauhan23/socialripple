package com.social.ripple.notification_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import com.social.ripple.notification_service.util.ConfigKeys;
import com.social.ripple.notification_service.util.constants.AppCache;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;

@Configuration
@DependsOn("initialConfigurationLoader")
public class MailConfig {

    @Bean
    public SesV2Client sesV2Client() {
        String region = AppCache.configParameters.get(ConfigKeys.AWS_SES_REGION).getConfigValue();
        String accessKey = AppCache.configParameters.get(ConfigKeys.AWS_SES_ACCESS_KEY_ID).getConfigValue();
        String secretKey = AppCache.configParameters.get(ConfigKeys.AWS_SES_SECRET_ACCESS_KEY).getConfigValue();

        return SesV2Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }
}
