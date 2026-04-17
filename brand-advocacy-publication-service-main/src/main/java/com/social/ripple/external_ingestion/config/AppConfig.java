/**
 * Filename: AppConfig.java
 *
 * © Copyright 2024 Quasarix. ALL RIGHTS RESERVED.

 * All rights, title and interest (including all intellectual property rights) in this software and any derivative works based upon or derived from
 * this software belongs exclusively to Quasarix.

 * Access to this software is forbidden to anyone except current employees of Quasarix and its affiliated companies who have executed non-disclosure
 * agreements explicitly covering such access. While in the employment of Quasarix or its affiliate companies as the case may be, employees may use
 * this software internally, solely in the course of employment, for the sole purpose of developing new functionalities, features, procedures,
 * routines, customizations or derivative works, or for the purpose of providing maintenance or support for the software. Save as expressly permitted
 * above, no license or right thereto is hereby granted to anyone, either directly, by implication or otherwise. On the termination of employment,
 * the license granted to employee to access the software shall terminate and the software should be returned to the employer, without retaining any
 * copies.

 * This software is (i) proprietary to Quasarix; (ii) is of significant value to it; (iii) contains trade secrets of Quasarix; (iv) is not publicly
 * available; and (v) constitutes the confidential information of Quasarix.

 * Any use, reproduction, modification, distribution, public performance or display of this software or through the use of this software without the
 * prior, express written consent of Quasarix is strictly prohibited and may be in violation of applicable laws.
 *
 */
package com.social.ripple.external_ingestion.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Map;

@Configuration
@Slf4j
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory());

        // Add interceptors to ensure URLs are not modified
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory() {
            @Override
            public URI expand(String uriTemplate, Map<String, ?> uriVariables) {
                try {
                    // Preserve the original URL exactly as provided
                    return new URI(uriTemplate);
                } catch (URISyntaxException e) {
                    throw new RuntimeException("Invalid URI: " + uriTemplate, e);
                }
            }

            @Override
            public URI expand(String uriTemplate, Object... uriVariables) {
                try {
                    // Preserve the original URL exactly as provided
                    return new URI(uriTemplate);
                } catch (URISyntaxException e) {
                    throw new RuntimeException("Invalid URI: " + uriTemplate, e);
                }
            }
        });

        // Add detailed request logging
        restTemplate.getInterceptors().add(new ClientHttpRequestInterceptor() {
            @Override
            public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
                log.debug("Making {} request to: {}", request.getMethod(), request.getURI());
                log.debug("Request headers: {}", request.getHeaders());
                return execution.execute(request, body);
            }
        });

        return restTemplate;
    }


    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        // Timeout configurations
        factory.setConnectTimeout(Duration.ofMinutes(2));
        factory.setReadTimeout(Duration.ofMinutes(2));

        return factory;
    }
}
