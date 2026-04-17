package com.social.ripple.external_ingestion.implementation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.social.ripple.external_ingestion.dao.model.*;
import com.social.ripple.external_ingestion.dao.repository.*;
import com.social.ripple.external_ingestion.service.ILeaderPostFetcherService;
import com.social.ripple.external_ingestion.service.IPostDeletionCheckerService;
import com.social.ripple.external_ingestion.util.enumeration.AppCache;
import com.social.ripple.external_ingestion.util.enumeration.Platform;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostDeletionCheckerServiceImpl implements IPostDeletionCheckerService {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Value("${twitter.consumer.key}")
    private String consumerKey;

    @Value("${twitter.consumer.secret}")
    private String consumerSecret;

    @Autowired
    private UserAuthTokenRepository userAuthTokenRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PostMediaRepository postMediaRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public void performDeletionCheckForLinkedIn(Platform platform) {

        // Collect Platform ids by last sync date

        List<String> postExternalIds = postRepository.findPostIdsForSync(
                LocalDateTime.now().minusDays(1), platform.toString()
        );

        List externalIdsToDelete = new ArrayList<>();
        // process each item by calling API
        if(!CollectionUtils.isEmpty(postExternalIds)){
            postExternalIds.forEach(x->{
                if(!isPresentInPlatform(x)){
                    externalIdsToDelete.add(x);
                }
            });
        }

        // Update all with new dates and save

        postRepository.updateSyncedAtForPlatformIds(postExternalIds, LocalDateTime.now());

        // remove by deleted ids
        if(!CollectionUtils.isEmpty(externalIdsToDelete)) {
            postRepository.deleteByPlatformUniqueIdIn(externalIdsToDelete);
        }

    }

    private boolean isPresentInPlatform(String postId){
        boolean isPresent = true;



        return isPresent;
    }

    private boolean executeGetRequestLinkedinVideo(CloseableHttpClient client, String url, String token) throws IOException {
        boolean isPresent = true;

        HttpGet request = new HttpGet(url);
        request.setHeader("Authorization", "Bearer " + token);
        request.setHeader("X-Restli-Protocol-Version", "2.0.0");
        request.setHeader("LinkedIn-Version", "202509");

        try (CloseableHttpResponse response = client.execute(request)) {
            HttpEntity entity = response.getEntity();
            String responseBody = EntityUtils.toString(entity);

            log.info("response: {}",responseBody);

            if (response.getStatusLine().getStatusCode() == 404) {
                isPresent = false;
            }

            return isPresent;
        }
    }
}



