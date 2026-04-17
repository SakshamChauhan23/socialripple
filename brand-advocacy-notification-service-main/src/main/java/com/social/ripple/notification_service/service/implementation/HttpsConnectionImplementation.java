/**
 * Filename: HttpsConnectionImplementation.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all
 * intellectual property rights) in this software and any derivative works based upon or derived from this software belongs exclusively to Quasarix.
 * Access to this software is forbidden to anyone except current employees of Quasarix and its affiliated companies who have executed non-disclosure
 * agreements explicitly covering such access. While in the employment of Quasarix or its affiliate companies as the case may be, employees may use
 * this software internally, solely in the course of employment, for the sole purpose of developing new functionalities, features, procedures,
 * routines, customizations or derivative works, or for the purpose of providing maintenance or support for the software. Save as expressly permitted
 * above, no license or right thereto is hereby granted to anyone, either directly, by implication or otherwise. On the termination of employment, the
 * license granted to employee to access the software shall terminate and the software should be returned to the employer, without retaining any
 * copies. This software is (i) proprietary to Quasarix; (ii) is of significant value to it; (iii) contains trade secrets of Quasarix; (iv) is not
 * publicly available; and (v) constitutes the confidential information of Quasarix. Any use, reproduction, modification, distribution, public
 * performance or display of this software or through the use of this software without the prior, express written consent of Quasarix is strictly
 * prohibited and may be in violation of applicable laws.
 */
package com.social.ripple.notification_service.service.implementation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import com.social.ripple.notification_service.service.HttpsConnections;
import com.social.ripple.notification_service.util.ConfigKeys;
import com.social.ripple.notification_service.util.constants.AppCache;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import java.io.IOException;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;

@Service
@Slf4j
@RequiredArgsConstructor
public class HttpsConnectionImplementation implements HttpsConnections {

	private final SesV2Client sesV2Client;

	@Override
	public boolean sendSimpleMessage(String from, String to, String subject, String message) {
		log.info("|SES|SendSimpleMessage|Init|Preparing to send email to: {} from: {}", to, from);

		try {
			SendEmailRequest request = SendEmailRequest.builder()
					.fromEmailAddress(from)
					.destination(Destination.builder().toAddresses(to).build())
					.content(EmailContent.builder()
							.simple(Message.builder()
									.subject(Content.builder().data(subject).charset("UTF-8").build())
									.body(Body.builder()
											.html(Content.builder().data(message).charset("UTF-8").build())
											.build())
									.build())
							.build())
					.build();

			sesV2Client.sendEmail(request);
			log.info("|SES|SendSimpleMessage|Success|Email sent to: {}", to);
			return true;

		} catch (Exception ex) {
			log.error("|SES|SendSimpleMessage|Exception|Failed to send email to: {} | {}", to, ex.getMessage(), ex);
			return false;
		}
	}

	public String generateOtpHtml(String otp) {
		String htmlTemplate = AppCache.configParameters.get(ConfigKeys.OTP_MAIL_BODY).getConfigValue();
		return String.format(htmlTemplate, otp);
	}

	public String generateInvitationLinkHtml(String link, String recipientName, String organizationName, String inviterName) {
		String htmlTemplate = AppCache.configParameters.get(ConfigKeys.INVITATION_LINK_MAIL_BODY).getConfigValue();
		return htmlTemplate
				.replace("{invitation_link}", link != null ? link : "#")
				.replace("{recipient_name}", recipientName != null ? recipientName : "there")
				.replace("{organization_name}", organizationName != null ? organizationName : "SocialRipple")
				.replace("{inviter_name}", inviterName != null ? inviterName : "Your team");
	}

	public String httpsPostThirdPartyCall(String url, Map<String, String> headerParams, String requestBody, int connectionTimeout, int readTimeout) {

		log.info("|HTTPS|PostThirdPartyCall|Start|URL: {}", url);

		RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(connectionTimeout).setSocketTimeout(readTimeout).build();

		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			HttpPost httpPost = new HttpPost(url);
			httpPost.setConfig(requestConfig);

			if (headerParams != null && !headerParams.isEmpty()) {
				headerParams.forEach((key, value) -> httpPost.addHeader(new BasicHeader(key, value)));
				log.debug("|HTTPS|PostThirdPartyCall|Headers|{}", headerParams);
			}

			if (requestBody != null && !requestBody.isEmpty()) {
				httpPost.setEntity(new StringEntity(requestBody));
				httpPost.setHeader("Content-Type", "application/json");
				log.debug("|HTTPS|PostThirdPartyCall|RequestBody|{}", requestBody);
			}

			HttpResponse response = httpClient.execute(httpPost);
			int statusCode = response.getStatusLine().getStatusCode();
			String responseBody = EntityUtils.toString(response.getEntity());

			log.info("|HTTPS|PostThirdPartyCall|ResponseStatus|Status: {}", statusCode);

			if (statusCode == 200) {
				log.debug("|HTTPS|PostThirdPartyCall|ResponseBody|{}", responseBody);
				return responseBody;
			}
			else {
				log.warn("|HTTPS|PostThirdPartyCall|Failed|Status: {} | Body: {}", statusCode, responseBody);
				throw new IOException("HTTP request failed with status code: " + statusCode);
			}

		}
		catch (IOException ex) {
			log.error("|HTTPS|PostThirdPartyCall|Exception|Failed to post to URL: {} | {}", url, ex.getMessage(), ex);
			return null;
		}
	}

	@Override
	public String httpsGetThirdPartyCall(String url, Map<String, String> headerParams, Map<String, String> queryParams, int connectionTimeout,
			int readTimeout) {
		StringBuilder fullUrl = new StringBuilder(url);

		if (queryParams != null && !queryParams.isEmpty()) {
			fullUrl.append("?");
			queryParams.forEach((key, value) -> fullUrl.append(key).append("=").append(value).append("&"));
			fullUrl.setLength(fullUrl.length() - 1);
		}

		log.info("|HTTPS|GetThirdPartyCall|Start|URL: {}", fullUrl);

		RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(connectionTimeout).setSocketTimeout(readTimeout).build();

		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			HttpGet httpGet = new HttpGet(fullUrl.toString());
			httpGet.setConfig(requestConfig);

			if (headerParams != null && !headerParams.isEmpty()) {
				headerParams.forEach((key, value) -> httpGet.addHeader(new BasicHeader(key, value)));
				log.debug("|HTTPS|GetThirdPartyCall|Headers|{}", headerParams);
			}

			HttpResponse response = httpClient.execute(httpGet);
			int statusCode = response.getStatusLine().getStatusCode();
			String responseBody = EntityUtils.toString(response.getEntity());

			log.info("|HTTPS|GetThirdPartyCall|ResponseStatus|Status: {}", statusCode);

			if (statusCode == 200) {
				log.debug("|HTTPS|GetThirdPartyCall|ResponseBody|{}", responseBody);
				return responseBody;
			}
			else {
				log.warn("|HTTPS|GetThirdPartyCall|Failed|Status: {} | Body: {}", statusCode, responseBody);
				throw new IOException("HTTP GET failed with status code: " + statusCode);
			}

		}
		catch (IOException ex) {
			log.error("|HTTPS|GetThirdPartyCall|Exception|Failed to GET from URL: {} | {}", fullUrl, ex.getMessage(), ex);
			return null;
		}
	}

	@Override
	public String httpsPostThirdPartyCall(String url, Map<String, String> headerParams, Map<String, String> requestParams, int connectionTimeout,
			int readTimeout) {

		log.info("|HTTPS|PostThirdPartyCall|Start|URL: {}", url);

		RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(connectionTimeout).setSocketTimeout(readTimeout).build();

		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			HttpPost httpPost = new HttpPost(url);
			httpPost.setConfig(requestConfig);

			log.debug("|HTTPS|PostThirdPartyCall|Config|Timeouts - Connect: {}ms, Read: {}ms", connectionTimeout, readTimeout);

			if (headerParams != null && !headerParams.isEmpty()) {
				headerParams.forEach((key, value) -> httpPost.addHeader(new BasicHeader(key, value)));
				log.debug("|HTTPS|PostThirdPartyCall|Headers|{}", headerParams);
			}

			if (requestParams != null && !requestParams.isEmpty()) {
				List<NameValuePair> formParams = new ArrayList<>();
				requestParams.forEach((key, value) -> formParams.add(new BasicNameValuePair(key, value)));
				httpPost.setEntity(new UrlEncodedFormEntity(formParams));
				log.debug("|HTTPS|PostThirdPartyCall|FormParams|{}", requestParams);
			}

			log.debug("|HTTPS|PostThirdPartyCall|Execution|Sending POST request...");
			HttpResponse response = httpClient.execute(httpPost);

			int statusCode = response.getStatusLine().getStatusCode();
			String responseBody = EntityUtils.toString(response.getEntity());

			log.info("|HTTPS|PostThirdPartyCall|ResponseStatus|Code: {}", statusCode);

			if (statusCode == 200) {
				log.debug("|HTTPS|PostThirdPartyCall|ResponseBody|{}", responseBody);
				return responseBody;
			}
			else {
				log.error("|HTTPS|PostThirdPartyCall|Failed|Status: {} | Body: {}", statusCode, responseBody);
				throw new IOException("HTTP POST failed with status code: " + statusCode);
			}

		}
		catch (IOException ex) {
			log.error("|HTTPS|PostThirdPartyCall|Exception|Failed to POST to URL: {} | {}", url, ex.getMessage(), ex);
			return null;
		}
	}

	@Override
	public String httpGetThirdPartyCall(String url, Map<String, String> headerParams, Map<String, String> queryParams, int connectionTimeout,
			int readTimeout) {
		return null;
	}

	@Override
	public String httpPostThirdPartyCall(String url, Map<String, String> headerParams, Map<String, String> requestParams, int connectionTimeout,
			int readTimeout) {
		return null;
	}
}
