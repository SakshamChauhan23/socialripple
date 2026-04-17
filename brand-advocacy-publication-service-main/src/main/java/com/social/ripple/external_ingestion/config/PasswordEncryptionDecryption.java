/**
 * Filename: PasswordEncryptionDecryption.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all
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
package com.social.ripple.external_ingestion.config;


import com.social.ripple.external_ingestion.constants.ApplicationConstants;
import com.social.ripple.external_ingestion.constants.ConfigKeys;
import com.social.ripple.external_ingestion.util.AppCommonValidator;

import com.social.ripple.external_ingestion.util.enumeration.AppCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@Slf4j
@DependsOn("initialConfigurationLoader")
@Primary
public class PasswordEncryptionDecryption implements PasswordEncoder {

	private final AppCommonValidator appCommonValidator;
	private final AppCache configCache;
	private static final PasswordEncoder bcryptEncoder = new BCryptPasswordEncoder();
	private static final int GCM_TAG_LENGTH = 128;
	private static final int GCM_IV_LENGTH = 12;

	public PasswordEncryptionDecryption(AppCache configCache, AppCommonValidator appCommonValidator) {
		this.configCache = configCache;
		this.appCommonValidator = appCommonValidator;
	}

	@Override
	public String encode(CharSequence rawPassword) {
		String traceId = appCommonValidator.getOrGenerateRequestId((String) null);

		try {
			log.info("[{}]|PASSWORD|ENCODE|Starting password encryption", traceId);

			String hashedPassword = bcryptEncoder.encode(rawPassword.toString());
			log.debug("[{}]|PASSWORD|ENCODE|Password hashed using BCrypt", traceId);
			
			byte[] vector = new byte[GCM_IV_LENGTH];
			Cipher cipher = initCipher(traceId, Cipher.ENCRYPT_MODE, vector);
			byte[] encryptedBytes = cipher.doFinal(hashedPassword.getBytes(StandardCharsets.UTF_8));
			ByteBuffer byteBuffer = ByteBuffer.allocate(vector.length + encryptedBytes.length);
			byteBuffer.put(vector);
			byteBuffer.put(encryptedBytes);
			String encryptedBase64 = Base64.getEncoder().encodeToString(byteBuffer.array());
			log.info("[{}]|PASSWORD|ENCODE|Password encrypted and Base64-encoded successfully", traceId);
			return encryptedBase64;

		}
		catch (Exception ex) {
			log.error("[{}]|PASSWORD|ENCODE|Error encrypting password: {}", traceId, ex.getMessage(), ex);
			return null;
		}
	}

	@Override
	public boolean matches(CharSequence rawPassword, String encodedPassword) {
		String traceId = appCommonValidator.getOrGenerateRequestId((String) null);

		try {
			log.info("[{}]|PASSWORD|MATCH|Starting password verification", traceId);

			byte[] decoded = Base64.getDecoder().decode(encodedPassword);
			ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);

			byte[] vector = new byte[GCM_IV_LENGTH];
			byteBuffer.get(vector);
			byte[] encryptedHash = new byte[byteBuffer.remaining()];
			byteBuffer.get(encryptedHash);

			Cipher cipher = initCipher(traceId, Cipher.DECRYPT_MODE, vector);
			String decryptedHashedPassword = new String(cipher.doFinal(encryptedHash), StandardCharsets.UTF_8);

			boolean isMatch = bcryptEncoder.matches(rawPassword, decryptedHashedPassword);

			log.info("[{}]|PASSWORD|MATCH|Password match result: {}", traceId, isMatch ? "SUCCESS" : "FAILURE");
			return isMatch;

		}
		catch (Exception ex) {
			log.error("[{}]|PASSWORD|MATCH|Error verifying password: {}", traceId, ex.getMessage(), ex);
			return false;
		}
	}

	private Cipher initCipher(String traceId, int mode, byte[] iv) throws Exception {
		String secretKey = configCache.getConfigParameterValue(traceId, ConfigKeys.PASSWORD_ENCODING_DECODING_SECRET);

		SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");
		GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

		Cipher cipher = Cipher.getInstance(ApplicationConstants.PASSWORD_ENCODING_ALGORITHM);
		cipher.init(mode, keySpec, gcmSpec);

		log.debug("[{}]|PASSWORD|CIPHER|Initialized AES-GCM cipher in {} mode", traceId, mode == Cipher.ENCRYPT_MODE ? "ENCRYPT" : "DECRYPT");

		return cipher;
	}
}