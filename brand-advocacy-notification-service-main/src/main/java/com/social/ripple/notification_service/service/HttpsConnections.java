/**
 * Filename: HttpsConnections.java
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
package com.social.ripple.notification_service.service;

import java.util.Map;

/**
 * Interface to define various HTTP and HTTPS communication methods.
 * This interface provides methods to send simple messages and perform HTTP/HTTPS 
 * GET and POST requests to third-party services with customizable headers, query parameters, 
 * and request parameters.
 */ 
public interface HttpsConnections {

    /**
     * Sends a simple message via email or another communication service.
     * 
     * @param from The sender's email address or identifier.
     * @param to The recipient's email address or identifier.
     * @param subject The subject of the message.
     * @param text The body content of the message.
     * @return boolean indicating whether the message was sent successfully.
     */
    public boolean sendSimpleMessage(String from, String to, String subject, String text);

    /**
     * Sends an HTTPS GET request to a third-party service with specified headers and query parameters.
     * 
     * @param url The target URL for the GET request.
     * @param headerParams A map containing the headers for the request.
     * @param queryParams A map containing the query parameters for the request.
     * @param connectionTimeout The connection timeout in milliseconds.
     * @param readTimeout The read timeout in milliseconds.
     * @return The response body from the GET request as a string.
     */
    public String httpsGetThirdPartyCall(String url, Map<String, String> headerParams, Map<String, String> queryParams,
            int connectionTimeout, int readTimeout);

    /**
     * Sends an HTTPS POST request to a third-party service with specified headers and request parameters.
     * 
     * @param url The target URL for the POST request.
     * @param headerParams A map containing the headers for the request.
     * @param requestParams A map containing the request parameters for the POST request.
     * @param connectionTimeout The connection timeout in milliseconds.
     * @param readTimeout The read timeout in milliseconds.
     * @return The response body from the POST request as a string.
     */
    public String httpsPostThirdPartyCall(String url, Map<String, String> headerParams, Map<String, String> requestParams,
            int connectionTimeout, int readTimeout);
    
    /**
     * Sends an HTTP GET request to a third-party service with specified headers and query parameters.
     * 
     * @param url The target URL for the GET request.
     * @param headerParams A map containing the headers for the request.
     * @param queryParams A map containing the query parameters for the request.
     * @param connectionTimeout The connection timeout in milliseconds.
     * @param readTimeout The read timeout in milliseconds.
     * @return The response body from the GET request as a string.
     */
    public String httpGetThirdPartyCall(String url, Map<String, String> headerParams, Map<String, String> queryParams,
            int connectionTimeout, int readTimeout);

    /**
     * Sends an HTTP POST request to a third-party service with specified headers and request parameters.
     * 
     * @param url The target URL for the POST request.
     * @param headerParams A map containing the headers for the request.
     * @param requestParams A map containing the request parameters for the POST request.
     * @param connectionTimeout The connection timeout in milliseconds.
     * @param readTimeout The read timeout in milliseconds.
     * @return The response body from the POST request as a string.
     */
    public String httpPostThirdPartyCall(String url, Map<String, String> headerParams, Map<String, String> requestParams,
            int connectionTimeout, int readTimeout);
}


