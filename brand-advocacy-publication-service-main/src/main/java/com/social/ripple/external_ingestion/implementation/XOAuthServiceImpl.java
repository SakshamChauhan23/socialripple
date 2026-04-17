//package com.social.ripple.external_ingestion.implementation;
//
//import com.social.ripple.external_ingestion.Constants.ConfigKeys;
//import com.social.ripple.external_ingestion.dao.model.Post;
//import com.social.ripple.external_ingestion.dao.model.UserAuthToken;
//import com.social.ripple.external_ingestion.dao.repository.PostRepository;
//import com.social.ripple.external_ingestion.dao.repository.UserAuthTokenRepository;
//import com.social.ripple.external_ingestion.dto.request.TweetRequest;
//import com.social.ripple.external_ingestion.dto.response.TweetResponse;
//import com.social.ripple.external_ingestion.service.IMediaStorageService;
//import com.social.ripple.external_ingestion.service.XOAuthService;
//import jakarta.servlet.http.HttpSession;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import oauth.signpost.OAuthConsumer;
//import oauth.signpost.OAuthProvider;
//import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
//import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
//import org.apache.http.HttpEntity;
//import org.apache.http.NameValuePair;
//import org.apache.http.client.entity.UrlEncodedFormEntity;
//import org.apache.http.client.methods.CloseableHttpResponse;
//import org.apache.http.client.methods.HttpGet;
//import org.apache.http.client.methods.HttpPost;
//import org.apache.http.entity.ContentType;
//import org.apache.http.entity.StringEntity;
//import org.apache.http.entity.mime.MultipartEntityBuilder;
//import org.apache.http.impl.client.CloseableHttpClient;
//import org.apache.http.impl.client.HttpClients;
//import org.apache.http.message.BasicNameValuePair;
//import org.apache.http.util.EntityUtils;
//import org.json.JSONArray;
//import org.json.JSONObject;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.File;
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Files;
//import java.time.LocalDateTime;
//import java.util.*;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class XOAuthServiceImpl implements XOAuthService {
//
//    private final UserAuthTokenRepository userAuthTokenRepository;
//    private final PostRepository postRepository;
//    private final IMediaStorageService mediaStorageService;
//
//    @Value("${twitter.consumer.key}")
//    private String consumerKey;
//
//    @Value("${twitter.consumer.secret}")
//    private String consumerSecret;
//
////    @Override
////    public String getAuthorizationUrl(String callbackUrl, HttpSession session) throws Exception {
////        OAuthConsumer consumer = new CommonsHttpOAuthConsumer(consumerKey, consumerSecret);
////        OAuthProvider provider = new CommonsHttpOAuthProvider(
////                ConfigKeys.REQUEST_TOKEN_URL,
////                ConfigKeys.ACCESS_TOKEN_URL,
////                ConfigKeys.AUTHORIZE_URL
////        );
////
////        session.setAttribute("consumer", consumer);
////        session.setAttribute("provider", provider);
////
////
////        return provider.retrieveRequestToken(consumer, callbackUrl);
////    }
////
////
////    @Override
////    public UserAuthToken handleCallback(String oauthVerifier, HttpSession session) throws Exception {
////        OAuthConsumer consumer = (OAuthConsumer) session.getAttribute("consumer");
////        OAuthProvider provider = (OAuthProvider) session.getAttribute("provider");
////
////        provider.retrieveAccessToken(consumer, oauthVerifier);
////
////        String accessToken = consumer.getToken();
////        String accessSecret = consumer.getTokenSecret();
////
////        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
////            HttpGet request = new HttpGet(ConfigKeys.VERIFY_CREDENTIALS_URL);
////            consumer.sign(request);
////
////            try (CloseableHttpResponse response = httpClient.execute(request)) {
////                String jsonResponse = EntityUtils.toString(response.getEntity());
////                JSONObject json = new JSONObject(jsonResponse);
////
////                String externalUserId = json.getString("id_str");
////                return saveUserTokens(externalUserId, accessToken, accessSecret);
////            }
////        }
////    }
////
////    @Override
////    public UserAuthToken saveUserTokens(String externalUserId, String accessToken, String accessSecret) {
////        Optional<UserAuthToken> existingTokenOpt =
////                userAuthTokenRepository.findByUserIdExternalAndPlatform(externalUserId, "X");
////
////        if (existingTokenOpt.isPresent()) {
////            UserAuthToken token = existingTokenOpt.get();
////            token.setAccessToken(accessToken);
////            token.setAccessSecret(accessSecret);
////            token.setClientId(consumerKey);
////            token.setCreatedAt(LocalDateTime.now());
////            return userAuthTokenRepository.save(token);
////        }
////
////        UserAuthToken token = UserAuthToken.builder()
////                .userIdExternal(externalUserId)
////                .platform("X")
////                .accessToken(accessToken)
////                .accessSecret(accessSecret)
////                .clientId(consumerKey)
////                .createdAt(LocalDateTime.now())
////                .build();
////
////        return userAuthTokenRepository.save(token);
////    }
//
//    @Override
//    public Post getPostById(Long postId) {
//        return postRepository.findById(postId)
//                .orElseThrow(() -> new RuntimeException("Post not found with id " + postId));
//    }
//
//    @Override
//    public TweetResponse postTweetWithUploadedFile(TweetRequest request) throws Exception {
//        Post post = getPostById(request.getPostId());
//
//        MultipartFile file = request.getFile();
//        String accessToken = request.getAccessToken();
//        String accessSecret = request.getAccessSecret();
//
//        File localFile = mediaStorageService.downloadFile(accessToken, accessSecret);
//        byte[] fileBytes = Files.readAllBytes(localFile.toPath());
//
//        long maxSize = 512L * 1024 * 1024; 
//        if (fileBytes.length > maxSize) {
//            throw new RuntimeException("File too large. Max allowed size is 512 MB for videos.");
//        }
//
//        OAuthConsumer consumer = new CommonsHttpOAuthConsumer(consumerKey, consumerSecret);
//        consumer.setTokenWithSecret(accessToken, accessSecret);
//
//        String mediaId = uploadMedia(localFile, fileBytes, consumer);
//
//        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
//            HttpPost tweetPost = new HttpPost(ConfigKeys.TWEET_POST_URL);
//            tweetPost.setHeader("Content-Type", "application/json");
//
//            JSONObject body = new JSONObject();
//            body.put("text", post.getContent());
//            body.put("media", new JSONObject().put("media_ids", new JSONArray().put(mediaId)));
//
//            tweetPost.setEntity(new StringEntity(body.toString(), StandardCharsets.UTF_8));
//            consumer.sign(tweetPost);
//
//            try (CloseableHttpResponse tweetResp = httpClient.execute(tweetPost)) {
//                String rawResponse = EntityUtils.toString(tweetResp.getEntity());
//                JSONObject data = new JSONObject(rawResponse).getJSONObject("data");
//
//                return TweetResponse.builder()
//                        .rawResponse(rawResponse)
//                        .build();
//            }
//        }
//    }
//
//    private String uploadMedia(File file, byte[] fileBytes, OAuthConsumer consumer) throws Exception {
//        String fileName = file.getName().toLowerCase();
//
//        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
//            if (fileName.endsWith(".mp4")) {
//                // INIT
//                HttpPost initPost = new HttpPost(ConfigKeys.MEDIA_UPLOAD_URL);
//                List<NameValuePair> initParams = new ArrayList<>();
//                initParams.add(new BasicNameValuePair("command", "INIT"));
//                initParams.add(new BasicNameValuePair("total_bytes", String.valueOf(fileBytes.length)));
//                initParams.add(new BasicNameValuePair("media_type", "video/mp4"));
//                initParams.add(new BasicNameValuePair("media_category", "tweet_video"));
//                initPost.setEntity(new UrlEncodedFormEntity(initParams, StandardCharsets.UTF_8));
//                consumer.sign(initPost);
//
//                String mediaId;
//                try (CloseableHttpResponse initResp = httpClient.execute(initPost)) {
//                    JSONObject initJson = new JSONObject(EntityUtils.toString(initResp.getEntity()));
//                    mediaId = initJson.getString("media_id_string");
//                }
//
//                int chunkSize = 4 * 1024 * 1024;
//                int segmentIndex = 0;
//                for (int offset = 0; offset < fileBytes.length; offset += chunkSize) {
//                    int end = Math.min(fileBytes.length, offset + chunkSize);
//                    byte[] chunk = Arrays.copyOfRange(fileBytes, offset, end);
//
//                    HttpPost appendPost = new HttpPost(ConfigKeys.MEDIA_UPLOAD_URL);
//                    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
//                    builder.addTextBody("command", "APPEND");
//                    builder.addTextBody("media_id", mediaId);
//                    builder.addTextBody("segment_index", String.valueOf(segmentIndex));
//                    builder.addBinaryBody("media", chunk, ContentType.DEFAULT_BINARY, file.getName());
//                    appendPost.setEntity(builder.build());
//                    consumer.sign(appendPost);
//
//                    try (CloseableHttpResponse appendResp = httpClient.execute(appendPost)) {
//                        EntityUtils.consume(appendResp.getEntity());
//                    }
//                    segmentIndex++;
//                }
//
//                HttpPost finalizePost = new HttpPost(ConfigKeys.MEDIA_UPLOAD_URL);
//                List<NameValuePair> finalizeParams = List.of(
//                        new BasicNameValuePair("command", "FINALIZE"),
//                        new BasicNameValuePair("media_id", mediaId)
//                );
//                finalizePost.setEntity(new UrlEncodedFormEntity(finalizeParams, StandardCharsets.UTF_8));
//                consumer.sign(finalizePost);
//
//                JSONObject finalizeJson;
//                try (CloseableHttpResponse finalizeResp = httpClient.execute(finalizePost)) {
//                    finalizeJson = new JSONObject(EntityUtils.toString(finalizeResp.getEntity()));
//                }
//
//                if (finalizeJson.has("processing_info")) {
//                    JSONObject pInfo = finalizeJson.getJSONObject("processing_info");
//                    int checkAfterSecs = pInfo.optInt("check_after_secs", 5);
//                    boolean processing = true;
//
//                    while (processing) {
//                        Thread.sleep(checkAfterSecs * 1000L);
//
//                        HttpGet statusGet = new HttpGet(
//                                ConfigKeys.MEDIA_UPLOAD_URL + "?command=STATUS&media_id=" + mediaId
//                        );
//                        consumer.sign(statusGet);
//
//                        try (CloseableHttpResponse statusResp = httpClient.execute(statusGet)) {
//                            JSONObject statusJson = new JSONObject(EntityUtils.toString(statusResp.getEntity()));
//                            if (statusJson.has("processing_info")) {
//                                JSONObject proc = statusJson.getJSONObject("processing_info");
//                                String state = proc.getString("state");
//
//                                if ("succeeded".equals(state)) {
//                                    processing = false;
//                                } else if ("failed".equals(state)) {
//                                    throw new RuntimeException("Video processing failed: " + statusJson);
//                                } else {
//                                    checkAfterSecs = proc.optInt("check_after_secs", 5);
//                                }
//                            } else {
//                                processing = false;
//                            }
//                        }
//                    }
//                }
//                return mediaId;
//
//            } else {
//                HttpPost uploadPost = new HttpPost(ConfigKeys.MEDIA_UPLOAD_URL);
//                HttpEntity uploadEntity = MultipartEntityBuilder.create()
//                        .addBinaryBody("media", fileBytes, ContentType.DEFAULT_BINARY, file.getName())
//                        .build();
//                uploadPost.setEntity(uploadEntity);
//                consumer.sign(uploadPost);
//
//                try (CloseableHttpResponse uploadResp = httpClient.execute(uploadPost)) {
//                    JSONObject json = new JSONObject(EntityUtils.toString(uploadResp.getEntity()));
//                    if (!json.has("media_id_string")) {
//                        throw new RuntimeException("Twitter upload failed: " + json);
//                    }
//                    return json.getString("media_id_string");
//                }
//            }
//        }
//    }
//}
