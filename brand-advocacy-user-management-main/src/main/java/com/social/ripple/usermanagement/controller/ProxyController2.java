package com.social.ripple.usermanagement.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Enumeration;
import java.util.Map;

@RestController
@RequestMapping("/v1/proxy")
public class ProxyController2 {

    private final RestTemplate restTemplate;

    @Value("${app.proxy.external-ingestion-url:http://localhost:8080}")
    private String externalIngestionUrl;

    @Value("${app.proxy.products-url:http://localhost:8082}")
    private String productsUrl;

    @Value("${app.proxy.orders-url:http://localhost:8083}")
    private String ordersUrl;

    public ProxyController2(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    @GetMapping("/{serviceName}/**")
    public ResponseEntity<?> proxyGet(@PathVariable String serviceName,
                                      HttpServletRequest request) {
        String target = buildTargetUrl(serviceName, request);
        HttpHeaders headers = extractHeaders(request);
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);
        return restTemplate.exchange(target, HttpMethod.GET, httpEntity, String.class);
    }

    @PostMapping("/{serviceName}/**")
    public ResponseEntity<?> proxyPost(@PathVariable String serviceName,
                                       HttpServletRequest request,
                                       @RequestBody(required = false) String body) {
        String target = buildTargetUrl(serviceName, request);
        HttpHeaders headers = extractHeaders(request);

        HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);

        return restTemplate.exchange(target, HttpMethod.POST, httpEntity, String.class);
    }

    @PutMapping("/{serviceName}/**")
    public ResponseEntity<?> proxyPut(@PathVariable String serviceName,
                                      HttpServletRequest request,
                                      @RequestBody(required = false) String body) {
        String target = buildTargetUrl(serviceName, request);
        HttpHeaders headers = extractHeaders(request);
        HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);
        return restTemplate.exchange(target, HttpMethod.PUT, httpEntity, String.class);
    }

    @DeleteMapping("/{serviceName}/**")
    public ResponseEntity<?> proxyDelete(@PathVariable String serviceName,
                                         HttpServletRequest request) {
        String target = buildTargetUrl(serviceName, request);
        HttpHeaders headers = extractHeaders(request);
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);
        return restTemplate.exchange(target, HttpMethod.DELETE, httpEntity, String.class);
    }

    private String buildTargetUrl(String serviceName, HttpServletRequest request) {

        String restOfPath = request.getRequestURI().substring(29);

        // Map service names to actual base URLs
        String baseUrl = getBaseUrlForService(serviceName);

        return baseUrl + "/" + restOfPath + getQueryString(request);

//        return baseUrl + "/" + restOfPath + getQueryString(request);
    }

    private HttpHeaders extractHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.set(headerName, request.getHeader(headerName));
        }
        return headers;
    }

    private String getBaseUrlForService(String serviceName) {
        Map<String, String> serviceMap = Map.of(
                "external-ingestion", externalIngestionUrl,
                "products", productsUrl,
                "orders", ordersUrl
        );
        return serviceMap.getOrDefault(serviceName, externalIngestionUrl);
    }

    private String getQueryString(HttpServletRequest request) {
        String queryString = request.getQueryString();
        return queryString != null ? "?" + queryString : "";
    }
}
