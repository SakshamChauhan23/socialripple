package com.social.ripple.external_ingestion.constants;

import java.util.Set;

public final class ConfigKeys {

	private ConfigKeys() {
	}

	public static final String PASSWORD_ENCODING_DECODING_SECRET = "PASSWORD_ENCODING_DECODING_SECRET";

	public static final String JWT_RESET_TOKEN_SECRET = "JWT_RESET_TOKEN_SECRET";
	public static final String JWT_RESET_TOKEN_EXPIRATION_MS = "JWT_RESET_TOKEN_EXPIRATION_MS";

	// X/Twitter OAuth Keys
	public static final String X_CONSUMER_KEY = "X_CONSUMER_KEY";
	public static final String X_CONSUMER_SECRET = "X_CONSUMER_SECRET";
	public static final String X_REQUEST_TOKEN_URL = "X_REQUEST_TOKEN_URL";
	public static final String X_ACCESS_TOKEN_URL = "X_ACCESS_TOKEN_URL";
	public static final String X_AUTHORIZE_URL = "X_AUTHORIZE_URL";
//	public static final String X_VERIFY_CREDENTIALS_URL = "X_VERIFY_CREDENTIALS_URL";
//	public static final String X_CALLBACK_URL = "X_CALLBACK_URL";

	public static final String TWITTER_ACCESS_TOKEN     = "1511026263192125442-brraFJEIdPdqWRylJPmV0XEoLpoS87";
	public static final String TWITTER_ACCESS_SECRET    = "oHFqQYQZpmPEOr2cmIBsyYO3DHARf0l1CdfN80TNL4ptf";

	public static final String REQUEST_TOKEN_URL        = "https://api.twitter.com/oauth/request_token";
	public static final String ACCESS_TOKEN_URL         = "https://api.twitter.com/oauth/access_token";
	public static final String AUTHORIZE_URL            = "https://api.twitter.com/oauth/authorize";
	public static final String VERIFY_CREDENTIALS_URL   = "https://api.twitter.com/1.1/account/verify_credentials.json";

	public static final String MEDIA_UPLOAD_URL         = "https://upload.twitter.com/1.1/media/upload.json";
	public static final String TWEET_POST_URL           = "https://api.twitter.com/2/tweets";

	public static final String MEDIA_LOCAL_FILE_PATH    = "D:/image/video check.mp4"; //D:\\image\\video check.mp4

//	Unused — actual callback URL is resolved from PublicUrlProperties at runtime

	public static final String JWT_SECRET = "JWT_SECRET";
	public static final String JWT_REFRESH_TOKEN_SECRET = "JWT_REFRESH_TOKEN_SECRET";
	public static final String JWT_REFRESH_TOKEN_EXPIRATION_MS = "JWT_REFRESH_TOKEN_EXPIRATION_MS";
	public static final String JWT_EXPIRATION_MS = "JWT_EXPIRATION_MS";

	public static final Set<String> REQUIRED_KEYS = Set.of(
//			TWITTER_ACCESS_TOKEN,
//			REQUEST_TOKEN_URL,
//			ACCESS_TOKEN_URL,
//			AUTHORIZE_URL,
//			VERIFY_CREDENTIALS_URL,
//			MEDIA_UPLOAD_URL,
//			TWEET_POST_URL,
//			MEDIA_LOCAL_FILE_PATH,
//			X_CALLBACK_URL
	);
}



//(MEDIA_UPLOAD_URL, TWEET_POST_URL, MEDIA_LOCAL_FILE_PATH,