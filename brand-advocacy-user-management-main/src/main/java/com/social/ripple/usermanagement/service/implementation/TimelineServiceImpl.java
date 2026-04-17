package com.social.ripple.usermanagement.service.implementation;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.social.ripple.usermanagement.dao.model.*;
import com.social.ripple.usermanagement.dao.repository.*;
import com.social.ripple.usermanagement.dto.response.*;
import com.social.ripple.usermanagement.security.service.UserDetailsImpl;
import com.social.ripple.usermanagement.service.ITimelineService;
import com.social.ripple.usermanagement.util.constants.ResponseCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimelineServiceImpl implements ITimelineService {

    private final PostRepository postRepository;
    private final PostHashtagRepository postHashtagRepository;
    private final PostMediaRepository postMediaRepository;
    private final PostTagRepository postTagRepository;
    private final PostCategoryMapRepository postCategoryMapRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final ExternalShareRepository externalShareRepository;
    private final MediaFileRepository mediaFileRepository;
    private final ExternalPlatformRepository externalPlatformRepository;
    private final UserAuthTokenRepository userAuthTokenRepository;

    @Override
    public BaseResponse getTimelinePosts(String traceId, Long postId, Long tenantId, Long categoryId, String platform,
                                         boolean leader, String searchText, Long userId, Pageable pageable, UserDetailsImpl userDetails) {

        Date timestamp = new Date();
        log.info("[{}]|TIMELINE|GET|User {} fetching posts for tenantId: {}",
                traceId, userDetails.getUsername(), tenantId);

        if (traceId == null || traceId.isEmpty()) {
            return buildErrorResponse(ResponseCode.USMG_400, "Missing required header: x-trace-id",
                    timestamp, "Header x-trace-id is required");
        }
        if (tenantId == null) {
            return buildErrorResponse(ResponseCode.USMG_400, "Missing required header: x-tenant-id",
                    timestamp, "Header x-tenant-id is required");
        }

        try {
            Page<Post> postPage;
            switch (determineFilterCase(postId, platform, categoryId, leader, searchText, userId)) {
                case 1:
                    Optional<Post> optPost = postRepository.findByIdAndOrganizationId(postId, tenantId);
                    if (optPost.isEmpty()) {
                        return buildErrorResponse(ResponseCode.USMG_401,
                                "Unauthorized or post not found", timestamp, "Access denied or mismatched tenant");
                    }
                    postPage = new PageImpl<>(Collections.singletonList(optPost.get()), pageable, 1);
                    break;

                case 2:

                    if(categoryId == null){
                        if(StringUtils.hasText(searchText)){
                            postPage = postRepository.findByOrganizationIdAndTypeAndContentContainingIgnoreCaseOrderByPlatformCreatedAtDescCreatedAtDesc(tenantId, platform, searchText, pageable);
                        }else{
                            postPage = postRepository.findByOrganizationIdAndTypeOrderByPlatformCreatedAtDescCreatedAtDesc(tenantId, platform, pageable);
                        }
                    }else {
                        if(StringUtils.hasText(searchText)){
                            postPage = postRepository.fetchByOrganizationIdAndTypeAndCategoryIdAndContentContainingOrderByPlatformCreatedAtDescCreatedAtDesc(tenantId, platform, categoryId,searchText, pageable);
                        }else{
                            postPage = postRepository.fetchByOrganizationIdAndTypeAndCategoryIdOrderByPlatformCreatedAtDescCreatedAtDesc(tenantId, platform, categoryId, pageable);
                        }
                    }

                    break;

                case 3:
                    postPage = postRepository.findByTenantAndCategory(tenantId, categoryId, pageable);
                    break;

                case 4:
                    if(categoryId == null){
                        if(StringUtils.hasText(searchText)) {
                            postPage = postRepository.findByTenantAndLeaderAndSearch(tenantId, searchText, pageable);
                        }else {
                            postPage = postRepository.findByTenantAndLeader(tenantId, pageable);
                        }
                    }else {
                        if(StringUtils.hasText(searchText)) {
                            postPage = postRepository.fetchByTenantAndLeaderAndCategoryAndSearch(tenantId, categoryId, searchText, pageable);
                        }else {
                            postPage = postRepository.fetchByTenantAndLeaderAndCategory(tenantId, categoryId, pageable);
                        }
                    }

                    break;

                case 5:
                    postPage = postRepository.findByTenantAndSearch(tenantId, searchText, pageable);
                    break;

                case 7: // New case: posts by a specific user under tenant
                    postPage = postRepository.findByCreatedByAndOrganizationId(userId, tenantId, pageable);
                    break;

                case 6:
                default:
                    postPage = postRepository.findByOrganizationIdOrderByPlatformCreatedAtDescCreatedAtDesc(tenantId, pageable);
            }

            List<Post> posts = postPage.getContent();
            if (posts.isEmpty()) {
                TimelinePostResponse response = new TimelinePostResponse();
                response.setStatus(true);
                response.setCode(ResponseCode.USMG_200);
                response.setMessage("Timeline posts fetched successfully");
                response.setDevMessage("Timeline data retrieved with associations");
                response.setTimestamp(timestamp);
                response.setPosts(Collections.emptyList());
                response.setTotalElements(postPage.getTotalElements());
                response.setTotalPages(postPage.getTotalPages());
                response.setPage(pageable.getPageNumber());
            }

            List<Long> postIds = posts.stream().map(Post::getId).toList();

            List<PostHashtag> hashtags = postHashtagRepository.findByPostIds(postIds);
            List<PostMedia> mediaList = postMediaRepository.findByPostIds(postIds);
            List<PostTag> tags = postTagRepository.findByPostIds(postIds);
            List<PostCategoryMap> categoryMaps = postCategoryMapRepository.findByPostIds(postIds);

            List<Long> categoryIds = categoryMaps.stream()
                    .map(PostCategoryMap::getCategoryId).distinct().toList();
            Map<Long, Category> categoryMap = categoryRepository.findByIds(categoryIds).stream()
                    .collect(Collectors.toMap(Category::getId, c -> c));

            Map<Long, List<PostHashtag>> hashtagsByPost = hashtags.stream()
                    .collect(Collectors.groupingBy(h -> h.getPost().getId()));

            Map<Long, List<PostMedia>> mediaByPost = mediaList.stream()
                    .collect(Collectors.groupingBy(PostMedia::getPostId));
            Map<String, MediaFile> mediaFilesByUrl = buildMediaFileMap(tenantId, mediaList);

            Map<Long, List<PostTag>> tagsByPost = tags.stream()
                    .collect(Collectors.groupingBy(PostTag::getPostId));

            List<Long> taggedUserIdsList = tags.stream().map(PostTag::getTaggedUserId).toList();
            Map<Long, User> taggedUserMap = userRepository.findAllById(taggedUserIdsList).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));

            Map<Long, List<PostCategoryMap>> categoriesByPost = categoryMaps.stream()
                    .collect(Collectors.groupingBy(PostCategoryMap::getPostId));

            List<Long> userIdsList = posts.stream().map(Post::getCreatedBy).toList();
            Map<Long, User> usersById = userRepository.findAllById(userIdsList).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));

            List<UserTimelineDto> timelineDtos = new ArrayList<>();
            for (Post post : posts) {
                UserTimelineDto dto = new UserTimelineDto();
                dto.setId(post.getId());
                dto.setOrganizationId(post.getOrganizationId());
                dto.setTitle(post.getTitle());
                dto.setContent(post.getContent());
                dto.setType(post.getType());
                dto.setSourceType(post.getSourceType());
                dto.setStatus(post.getStatus());
                dto.setScheduledAt(post.getScheduledAt());
                dto.setCreatedAt(post.getCreatedAt());
                dto.setPlatformCreatedAt(post.getPlatformCreatedAt());
                dto.setUpdatedAt(post.getUpdatedAt());

                PostCreatorDto creator = new PostCreatorDto();
                creator.setId(post.getCreatedBy());
                User user = usersById.get(post.getCreatedBy());
                if (user != null) {
                    creator.setName(user.getName());
                    creator.setProfileImageUrl(user.getProfilePictureUrl());
//                    creator.setTeamName(teamRepository.findByCreatedBy(user)
//                            .map(Team::getTeamName).orElse("Unknown Team"));
                } else {
                    creator.setName("Unknown User");
                    creator.setTeamName("Unknown Team");
                }
                dto.setCreatedBy(creator);

                // Source page info
                dto.setSourceName(post.getSourceName());
                dto.setSourceUsername(post.getSourceUsername());
                dto.setSourceAvatarUrl(post.getSourceAvatarUrl());

                List<PostMedia> postMedia = mediaByPost.getOrDefault(post.getId(), Collections.emptyList());
                dto.setMedia(postMedia.stream()
                        .map(m -> toMediaPostDto(m, mediaFilesByUrl.get(m.getFileUrl())))
                        .toList());

                dto.setHashtags(hashtagsByPost.getOrDefault(post.getId(), Collections.emptyList())
                        .stream().map(h -> {
                            PostHashtagsDto hdto = new PostHashtagsDto();
                            hdto.setId(h.getId());
                            hdto.setHashTag(h.getHashTag());
                            return hdto;
                        }).toList());

                dto.setTaggedUsers(tagsByPost.getOrDefault(post.getId(), Collections.emptyList())
                        .stream().map(t -> {
                            PostTagDto tdto = new PostTagDto();
                            tdto.setId(t.getId());
                            tdto.setPostId(t.getPostId());
                            tdto.setTaggedUserId(t.getTaggedUserId());
                            tdto.setUserName(taggedUserMap.get(t.getTaggedUserId()).getName());
                            tdto.setUserProfilePicture(taggedUserMap.get(t.getTaggedUserId()).getProfilePictureUrl());
                            return tdto;
                        }).toList());

                dto.setCategories(categoriesByPost.getOrDefault(post.getId(), Collections.emptyList())
                        .stream().map(cm -> {
                            Category c = categoryMap.get(cm.getCategoryId());
                            if (c == null) return null;
                            PostCategoryMapDto cdto = new PostCategoryMapDto();
                            cdto.setId(cm.getPostId());
                            cdto.setPostId(cm.getPostId());
                            cdto.setCategoryId(cm.getCategoryId());
                            cdto.setCategoryName(c.getName());
                            cdto.setCategoryDescription(c.getDescription());
                            return cdto;
                        }).filter(Objects::nonNull).toList());

                timelineDtos.add(dto);
            }

            TimelinePostResponse response = new TimelinePostResponse();
            response.setStatus(true);
            response.setCode(ResponseCode.USMG_200);
            response.setMessage("Timeline posts fetched successfully");
            response.setDevMessage("Timeline data retrieved with associations");
            response.setTimestamp(timestamp);
            response.setPosts(timelineDtos);
            response.setTotalElements(postPage.getTotalElements());
            response.setTotalPages(postPage.getTotalPages());
            response.setPage(pageable.getPageNumber());

            return response;

        } catch (Exception ex) {
            log.error("[{}]|TIMELINE|GET_FAILED|Exception: {}", traceId, ex.getMessage(), ex);
            return buildErrorResponse(ResponseCode.USMG_500, "Internal server error",
                    timestamp, ex.getMessage());
        }
    }

    private int determineFilterCase(Long postId, String platform, Long categoryId, boolean leader, String searchText, Long userId) {
        if (postId != null) return 1;
        if (platform != null && !platform.isEmpty() && !platform.equals("LEADERSHIP")) return 2;
        if (categoryId != null) return 3;
        if (leader || (platform != null && platform.equals("LEADERSHIP"))) return 4;
        if (searchText != null && !searchText.isEmpty()) return 5;
        if (userId != null) return 7;
        return 6;
    }

    private BaseResponse buildErrorResponse(String code, String message, Date timestamp, String devMessage) {
        BaseResponse response = new BaseResponse();
        response.setStatus(false);
        response.setCode(code);
        response.setMessage(message);
        response.setDevMessage(devMessage);
        response.setTimestamp(timestamp);
        return response;
    }

    private Map<String, MediaFile> buildMediaFileMap(Long tenantId, List<PostMedia> postMediaList) {
        if (tenantId == null || postMediaList == null || postMediaList.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> fileUrls = postMediaList.stream()
                .map(PostMedia::getFileUrl)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (fileUrls.isEmpty()) {
            return Collections.emptyMap();
        }

        return mediaFileRepository.findByOrganizationIdAndFileUrlIn(tenantId, fileUrls).stream()
                .filter(mediaFile -> StringUtils.hasText(mediaFile.getFileUrl()))
                .collect(Collectors.toMap(MediaFile::getFileUrl, Function.identity(), (first, ignored) -> first));
    }

    private MediaPostDto toMediaPostDto(PostMedia postMedia, MediaFile mediaFile) {
        MediaPostDto dto = new MediaPostDto();
        dto.setId(postMedia.getId());
        dto.setPostId(postMedia.getPostId());
        dto.setFileUrl(postMedia.getFileUrl());
        dto.setMediaType(postMedia.getMediaType());

        if (mediaFile == null) {
            if ("VIDEO".equalsIgnoreCase(postMedia.getMediaType())) {
                dto.setPlaybackUrl(postMedia.getFileUrl());
            } else if ("IMAGE".equalsIgnoreCase(postMedia.getMediaType())) {
                dto.setThumbnailUrl(postMedia.getFileUrl());
            }
            return dto;
        }

        dto.setStorageProvider(mediaFile.getStorageProvider());
        dto.setPlaybackUrl(StringUtils.hasText(mediaFile.getPlaybackUrl()) ? mediaFile.getPlaybackUrl() : mediaFile.getFileUrl());
        dto.setThumbnailUrl(StringUtils.hasText(mediaFile.getThumbnailUrl()) ? mediaFile.getThumbnailUrl()
                : ("IMAGE".equalsIgnoreCase(postMedia.getMediaType()) ? mediaFile.getFileUrl() : null));
        return dto;
    }

    @Override
    public BaseResponse getPostForPlatformShare(String traceId, Long postId, Long tenantId, UserDetailsImpl userDetails, Boolean isForShare) {

        Date timestamp = new Date();
        log.info("[{}]|TIMELINE|GET|User {} fetching posts for tenantId: {}",
                traceId, userDetails.getUsername(), tenantId);

        if (traceId == null || traceId.isEmpty()) {
            return buildErrorResponse(ResponseCode.USMG_400, "Missing required header: x-trace-id",
                    timestamp, "Header x-trace-id is required");
        }
        if (tenantId == null) {
            return buildErrorResponse(ResponseCode.USMG_400, "Missing required header: x-tenant-id",
                    timestamp, "Header x-tenant-id is required");
        }

        Pageable pageable = PageRequest.of(0, 1);

        try {
            Page<Post> postPage;

            Optional<Post> optPost = postRepository.findByIdAndOrganizationId(postId, tenantId);
            if (optPost.isEmpty()) {
                return buildErrorResponse(ResponseCode.USMG_401,
                        "Unauthorized or post not found", timestamp, "Access denied or mismatched tenant");
            }
            postPage = new PageImpl<>(Collections.singletonList(optPost.get()), pageable, 1);

            List<Post> posts = postPage.getContent();
            if (posts.isEmpty()) {
                return buildErrorResponse(ResponseCode.USMG_404, "No posts found", timestamp, null);
            }

            List<Long> postIds = posts.stream().map(Post::getId).toList();

            List<PostHashtag> hashtags = postHashtagRepository.findByPostIds(postIds);
            List<PostMedia> mediaList = postMediaRepository.findByPostIds(postIds);
            List<PostTag> tags = postTagRepository.findByPostIds(postIds);
            List<PostCategoryMap> categoryMaps = postCategoryMapRepository.findByPostIds(postIds);

            List<Long> categoryIds = categoryMaps.stream()
                    .map(PostCategoryMap::getCategoryId).distinct().toList();
            Map<Long, Category> categoryMap = categoryRepository.findByIds(categoryIds).stream()
                    .collect(Collectors.toMap(Category::getId, c -> c));

            Map<Long, List<PostHashtag>> hashtagsByPost = hashtags.stream()
                    .collect(Collectors.groupingBy(h -> h.getPost().getId()));

            Map<Long, List<PostMedia>> mediaByPost = mediaList.stream()
                    .collect(Collectors.groupingBy(PostMedia::getPostId));
            Map<String, MediaFile> mediaFilesByUrl = buildMediaFileMap(tenantId, mediaList);

            Map<Long, List<PostTag>> tagsByPost = tags.stream()
                    .collect(Collectors.groupingBy(PostTag::getPostId));

            List<Long> taggedUserIdsList = tags.stream().map(PostTag::getTaggedUserId).toList();
            Map<Long, User> taggedUserMap = userRepository.findAllById(taggedUserIdsList).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));

            Map<Long, List<PostCategoryMap>> categoriesByPost = categoryMaps.stream()
                    .collect(Collectors.groupingBy(PostCategoryMap::getPostId));

            List<Long> userIdsList = posts.stream().map(Post::getCreatedBy).toList();
            Map<Long, User> usersById = userRepository.findAllById(userIdsList).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));

            Post post = posts.getFirst();

                UserTimelineDto dto = new UserTimelineDto();
                dto.setId(post.getId());
                dto.setOrganizationId(post.getOrganizationId());
                dto.setTitle(post.getTitle());
                dto.setContent(post.getContent());
                dto.setType(post.getType());
                dto.setSourceType(post.getSourceType());
                dto.setStatus(post.getStatus());
                dto.setScheduledAt(post.getScheduledAt());
                dto.setCreatedAt(post.getCreatedAt());
                dto.setUpdatedAt(post.getUpdatedAt());

                PostCreatorDto creator = new PostCreatorDto();
                creator.setId(post.getCreatedBy());
                User user = usersById.get(post.getCreatedBy());
                if (user != null) {
                    creator.setName(user.getName());
                    creator.setProfileImageUrl(user.getProfilePictureUrl());
//                    creator.setTeamName(teamRepository.findByCreatedBy(user)
//                            .map(Team::getTeamName).orElse("Unknown Team"));
                } else {
                    creator.setName("Unknown User");
                    creator.setTeamName("Unknown Team");
                }
                dto.setCreatedBy(creator);

                // Source page info
                dto.setSourceName(post.getSourceName());
                dto.setSourceUsername(post.getSourceUsername());
                dto.setSourceAvatarUrl(post.getSourceAvatarUrl());

                List<PostMedia> postMedia = mediaByPost.getOrDefault(post.getId(), Collections.emptyList());
                dto.setMedia(postMedia.stream()
                        .map(m -> toMediaPostDto(m, mediaFilesByUrl.get(m.getFileUrl())))
                        .toList());

                dto.setHashtags(hashtagsByPost.getOrDefault(post.getId(), Collections.emptyList())
                        .stream().map(h -> {
                            PostHashtagsDto hdto = new PostHashtagsDto();
                            hdto.setId(h.getId());
                            hdto.setHashTag(h.getHashTag());
                            return hdto;
                        }).toList());

                dto.setTaggedUsers(tagsByPost.getOrDefault(post.getId(), Collections.emptyList())
                        .stream().map(t -> {
                            PostTagDto tdto = new PostTagDto();
                            tdto.setId(t.getId());
                            tdto.setPostId(t.getPostId());
                            tdto.setTaggedUserId(t.getTaggedUserId());
                            tdto.setUserName(taggedUserMap.get(t.getTaggedUserId()).getName());
                            tdto.setUserProfilePicture(taggedUserMap.get(t.getTaggedUserId()).getProfilePictureUrl());
                            return tdto;
                        }).toList());

                dto.setCategories(categoriesByPost.getOrDefault(post.getId(), Collections.emptyList())
                        .stream().map(cm -> {
                            Category c = categoryMap.get(cm.getCategoryId());
                            if (c == null) return null;
                            PostCategoryMapDto cdto = new PostCategoryMapDto();
                            cdto.setId(cm.getId());
                            cdto.setPostId(cm.getPostId());
                            cdto.setCategoryId(cm.getCategoryId());
                            cdto.setCategoryName(c.getName());
                            cdto.setCategoryDescription(c.getDescription());
                            return cdto;
                        }).filter(Objects::nonNull).toList());

            Map<String, String> platformContentMap = new HashMap<>();
            platformContentMap.put("X",StringUtils.hasText(post.getXGeneratedContent())? post.getXGeneratedContent():post.getContent().length() > 260?dto.getContent().substring(0,260):dto.getContent() );
            platformContentMap.put("INSTAGRAM",dto.getContent());
            platformContentMap.put("FACEBOOK",dto.getContent());
            platformContentMap.put("LINKEDIN",dto.getContent());

            TimelinePostResponse response = new TimelinePostResponse();
            response.setStatus(true);
            response.setCode(ResponseCode.USMG_200);
            response.setMessage("Timeline posts fetched successfully");
            response.setDevMessage("Timeline data retrieved with associations");
            response.setTimestamp(timestamp);
            response.setPreparedPost(dto);
            response.setPlatformContentMap(platformContentMap);
            response.setXHashtags(post.getXHashtags());

            fillEstimatedPoints(response, isForShare);

            return response;

        } catch (Exception ex) {
            log.error("[{}]|TIMELINE|GET_FAILED|Exception: {}", traceId, ex.getMessage(), ex);
            return buildErrorResponse(ResponseCode.USMG_500, "Internal server error",
                    timestamp, ex.getMessage());
        }
    }

    private void fillEstimatedPoints(TimelinePostResponse response, Boolean isForShare) {
        LoyaltyPointDTO loyaltyPointDTO = new LoyaltyPointDTO();

        // Get points

        int points = isForShare!=null && isForShare?10:5;


        loyaltyPointDTO.setFacebookPoints(points);
        loyaltyPointDTO.setInstagramPoints(points);
        loyaltyPointDTO.setLinkedinPoints(points);
        loyaltyPointDTO.setXPoints(points);
        loyaltyPointDTO.setTotal(points*4);

        response.setEstimatedLoyaltyPoints(loyaltyPointDTO);
    }

    @Override
    public BaseResponse getActivitiesPosts(String traceId, Long postId, Long tenantId,  Long userId, Pageable pageable, UserDetailsImpl userDetails) {

        Date timestamp = new Date();
        log.info("[{}]|TIMELINE|GET|User {} fetching posts for tenantId: {}",
                traceId, userDetails.getUsername(), tenantId);

        if (traceId == null || traceId.isEmpty()) {
            return buildErrorResponse(ResponseCode.USMG_400, "Missing required header: x-trace-id",
                    timestamp, "Header x-trace-id is required");
        }
        if (tenantId == null) {
            return buildErrorResponse(ResponseCode.USMG_400, "Missing required header: x-tenant-id",
                    timestamp, "Header x-tenant-id is required");
        }

        try {
            Page<Post> postPage = postRepository.findByOrganizationIdAndCreatedByAndSourceTypeIsNullOrderByCreatedAtDesc(tenantId, userId == null?userDetails.getUserId():userId, pageable);

            List<UserTimelineDto> timelineDtos = preparePostListWithDetail(pageable, postPage, timestamp, null);

            TimelinePostResponse response = new TimelinePostResponse();
            response.setStatus(true);
            response.setCode(ResponseCode.USMG_200);
            response.setMessage("Timeline posts fetched successfully");
            response.setDevMessage("Timeline data retrieved with associations");
            response.setTimestamp(timestamp);
            response.setPosts(timelineDtos);
            response.setTotalElements(postPage.getTotalElements());
            response.setTotalPages(postPage.getTotalPages());
            response.setPage(pageable.getPageNumber());

            return response;

        } catch (Exception ex) {
            log.error("[{}]|TIMELINE|GET_FAILED|Exception: {}", traceId, ex.getMessage(), ex);
            return buildErrorResponse(ResponseCode.USMG_500, "Internal server error",
                    timestamp, ex.getMessage());
        }
    }

    private List<UserTimelineDto> preparePostListWithDetail(Pageable pageable, Page<Post> postPage, Date timestamp, Map<Long, Object[]> postPointMap) {
        List<Post> posts = postPage.getContent();
        if (posts.isEmpty()) {
            TimelinePostResponse response = new TimelinePostResponse();
            response.setStatus(true);
            response.setCode(ResponseCode.USMG_200);
            response.setMessage("Timeline posts fetched successfully");
            response.setDevMessage("Timeline data retrieved with associations");
            response.setTimestamp(timestamp);
            response.setPosts(Collections.emptyList());
            response.setTotalElements(postPage.getTotalElements());
            response.setTotalPages(postPage.getTotalPages());
            response.setPage(pageable.getPageNumber());
        }

        List<Long> postIds = posts.stream().map(Post::getId).toList();

        List<PostHashtag> hashtags = postHashtagRepository.findByPostIds(postIds);
        List<PostMedia> mediaList = postMediaRepository.findByPostIds(postIds);
        List<PostTag> tags = postTagRepository.findByPostIds(postIds);
        List<PostCategoryMap> categoryMaps = postCategoryMapRepository.findByPostIds(postIds);
        List<Object[]> postShareCount = externalShareRepository.fetchShareCountByPostList(postIds);

        Map<Long, Object[]> postShareCountMap = new HashMap<>();

        if(!postShareCount.isEmpty()) {
            postShareCountMap = postShareCount.stream()
                    .collect(Collectors.toMap(
                            x->(Long)x[0],
                            x->x
                    ));
        }

        List<Long> categoryIds = categoryMaps.stream()
                .map(PostCategoryMap::getCategoryId).distinct().toList();
        Map<Long, Category> categoryMap = categoryRepository.findByIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, c -> c));

        Map<Long, List<PostHashtag>> hashtagsByPost = hashtags.stream()
                .collect(Collectors.groupingBy(h -> h.getPost().getId()));

        Map<Long, List<PostMedia>> mediaByPost = mediaList.stream()
                .collect(Collectors.groupingBy(PostMedia::getPostId));
        Map<String, MediaFile> mediaFilesByUrl = buildMediaFileMap(posts.isEmpty() ? null : posts.getFirst().getOrganizationId(), mediaList);

        Map<Long, List<PostTag>> tagsByPost = tags.stream()
                .collect(Collectors.groupingBy(PostTag::getPostId));

        List<Long> taggedUserIdsList = tags.stream().map(PostTag::getTaggedUserId).toList();
        Map<Long, User> taggedUserMap = userRepository.findAllById(taggedUserIdsList).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        Map<Long, List<PostCategoryMap>> categoriesByPost = categoryMaps.stream()
                .collect(Collectors.groupingBy(PostCategoryMap::getPostId));

        List<Long> userIdsList = posts.stream().map(Post::getCreatedBy).toList();
        Map<Long, User> usersById = userRepository.findAllById(userIdsList).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // Build fallback maps for posts missing source metadata
        boolean hasNullSource = posts.stream().anyMatch(p -> !StringUtils.hasText(p.getSourceName()));
        Map<String, ExternalPlatform> externalPlatformMap = new HashMap<>();
        Map<String, UserAuthToken> leaderTokenMap = new HashMap<>();
        if (hasNullSource) {
            List<Long> orgIds = posts.stream().map(Post::getOrganizationId).distinct().toList();
            for (Long orgId : orgIds) {
                externalPlatformRepository.findByOrganizationId(orgId)
                        .forEach(ep -> externalPlatformMap.put(orgId + ":" + ep.getPlatformName().toUpperCase(), ep));
            }
            userAuthTokenRepository.findByUserIdIn(userIdsList)
                    .forEach(t -> leaderTokenMap.put(t.getUserId() + ":" + t.getPlatform().toString(), t));
        }

        List<UserTimelineDto> timelineDtos = new ArrayList<>();
        for (Post post : posts) {
            UserTimelineDto dto = new UserTimelineDto();
            dto.setId(post.getId());
            dto.setOrganizationId(post.getOrganizationId());
            dto.setTitle(post.getTitle());
            dto.setContent(post.getContent());
            dto.setType(post.getType());
            dto.setSourceType(post.getSourceType());
            dto.setStatus(post.getStatus());
            dto.setScheduledAt(post.getScheduledAt());
            dto.setCreatedAt(post.getCreatedAt());
            dto.setPlatformCreatedAt(post.getPlatformCreatedAt());
            dto.setUpdatedAt(post.getUpdatedAt());

            PostCreatorDto creator = new PostCreatorDto();
            creator.setId(post.getCreatedBy());
            User user = usersById.get(post.getCreatedBy());
            if (user != null) {
                creator.setName(user.getName());
                creator.setProfileImageUrl(user.getProfilePictureUrl());
//                    creator.setTeamName(teamRepository.findByCreatedBy(user)
//                            .map(Team::getTeamName).orElse("Unknown Team"));
            } else {
                creator.setName("Unknown User");
                creator.setTeamName("Unknown Team");
            }
            dto.setCreatedBy(creator);

            // Source page info — stored in post during fetch
            dto.setSourceName(post.getSourceName());
            dto.setSourceUsername(post.getSourceUsername());
            dto.setSourceAvatarUrl(post.getSourceAvatarUrl());

            // Fallback: resolve from external_platforms / leader tokens if source info is incomplete
            if (!StringUtils.hasText(dto.getSourceName()) || !StringUtils.hasText(dto.getSourceAvatarUrl())) {
                resolveSourceInfo(dto, post, user, externalPlatformMap, leaderTokenMap);
            }

            List<PostMedia> postMedia = mediaByPost.getOrDefault(post.getId(), Collections.emptyList());
            dto.setMedia(postMedia.stream()
                    .map(m -> toMediaPostDto(m, mediaFilesByUrl.get(m.getFileUrl())))
                    .toList());

            dto.setHashtags(hashtagsByPost.getOrDefault(post.getId(), Collections.emptyList())
                    .stream().map(h -> {
                        PostHashtagsDto hdto = new PostHashtagsDto();
                        hdto.setId(h.getId());
                        hdto.setHashTag(h.getHashTag());
                        return hdto;
                    }).toList());

            dto.setTaggedUsers(tagsByPost.getOrDefault(post.getId(), Collections.emptyList())
                    .stream().map(t -> {
                        PostTagDto tdto = new PostTagDto();
                        tdto.setId(t.getId());
                        tdto.setPostId(t.getPostId());
                        tdto.setTaggedUserId(t.getTaggedUserId());
                        tdto.setUserName(taggedUserMap.get(t.getTaggedUserId()).getName());
                        tdto.setUserProfilePicture(taggedUserMap.get(t.getTaggedUserId()).getProfilePictureUrl());
                        return tdto;
                    }).toList());

            dto.setCategories(categoriesByPost.getOrDefault(post.getId(), Collections.emptyList())
                    .stream().map(cm -> {
                        Category c = categoryMap.get(cm.getCategoryId());
                        if (c == null) return null;
                        PostCategoryMapDto cdto = new PostCategoryMapDto();
                        cdto.setId(cm.getPostId());
                        cdto.setPostId(cm.getPostId());
                        cdto.setCategoryId(cm.getCategoryId());
                        cdto.setCategoryName(c.getName());
                        cdto.setCategoryDescription(c.getDescription());
                        return cdto;
                    }).filter(Objects::nonNull).toList());

            setPostShareCountData(post, postShareCountMap, dto);

            try {
                if (postPointMap != null && postPointMap.containsKey(post.getId()) && postPointMap.get(post.getId())[1] != null) {
                    dto.setEarnedLoyaltyPoint(((Long) postPointMap.get(post.getId())[1]).intValue());
                }
            } catch (Exception e) {
                log.error("e:{}",e.getMessage());
            }

            timelineDtos.add(dto);
        }
        return timelineDtos;
    }

    private void setPostShareCountData(Post post, Map<Long, Object[]> postShareCountMap, UserTimelineDto dto) {
        PostShareDataDTO postShareDataDTO = new PostShareDataDTO();
        Object[] postShareData = postShareCountMap.get(post.getId());
        if(postShareData !=null) {
            postShareDataDTO.setShareCount(((Long) postShareData[1]).intValue());

            postShareDataDTO.setHasFacebook(((BigDecimal) postShareData[2]).intValue() > 0);
            postShareDataDTO.setHasX(((BigDecimal) postShareData[3]).intValue() > 0);
            postShareDataDTO.setHasLinkedin(((BigDecimal) postShareData[4]).intValue() > 0);
            postShareDataDTO.setHasInstagram(((BigDecimal) postShareData[5]).intValue() > 0);
        }
        dto.setPostShareData(postShareDataDTO);
    }

    @Override
    public BaseResponse getActivitiesShares(String traceId, Long postId, Long tenantId, Long userId, Pageable pageable, UserDetailsImpl userDetails) {
        Date timestamp = new Date();
        log.info("[{}]|TIMELINE|GET|User {} fetching posts for tenantId: {}",
                traceId, userDetails.getUsername(), tenantId);

        if (traceId == null || traceId.isEmpty()) {
            return buildErrorResponse(ResponseCode.USMG_400, "Missing required header: x-trace-id",
                    timestamp, "Header x-trace-id is required");
        }
        if (tenantId == null) {
            return buildErrorResponse(ResponseCode.USMG_400, "Missing required header: x-tenant-id",
                    timestamp, "Header x-tenant-id is required");
        }

        try {
            Long actualUserId = userId == null? userDetails.getUserId(): userId;

//            List<Long> selectedPostIds = externalShareRepository.findPostIdsByUserId(actualUserId);
            List<Object[]> selectedPostIdsWithPoint = externalShareRepository.findPostIdsByUserIdWithPoint(actualUserId);

            Map<Long, Object[]> postPointMap = new HashMap<>();

            if(!selectedPostIdsWithPoint.isEmpty()) {
                postPointMap = selectedPostIdsWithPoint.stream()
                        .collect(Collectors.toMap(
                                x->(Long)x[0],
                                x->x
                        ));
            }

            Page<Post> postPage = postRepository.findByIdInAndOrganizationIdOrderByPlatformCreatedAtDescCreatedAtDesc(postPointMap.keySet(), tenantId, pageable);

            List<UserTimelineDto> timelineDtos = preparePostListWithDetail(pageable, postPage, timestamp, postPointMap);

            TimelinePostResponse response = new TimelinePostResponse();
            response.setStatus(true);
            response.setCode(ResponseCode.USMG_200);
            response.setMessage("Timeline posts fetched successfully");
            response.setDevMessage("Timeline data retrieved with associations");
            response.setTimestamp(timestamp);
            response.setPosts(timelineDtos);
            response.setTotalElements(postPage.getTotalElements());
            response.setTotalPages(postPage.getTotalPages());
            response.setPage(pageable.getPageNumber());

            return response;

        } catch (Exception ex) {
            log.error("[{}]|TIMELINE|GET_FAILED|Exception: {}", traceId, ex.getMessage(), ex);
            return buildErrorResponse(ResponseCode.USMG_500, "Internal server error",
                    timestamp, ex.getMessage());
        }
    }

    @Override
    public BaseResponse getActivitiesTags(String traceId, Long postId, Long tenantId, Long userId, Pageable pageable, UserDetailsImpl userDetails) {
        Date timestamp = new Date();
        log.info("[{}]|TIMELINE|GET|User {} fetching posts for tenantId: {}",
                traceId, userDetails.getUsername(), tenantId);

        if (traceId == null || traceId.isEmpty()) {
            return buildErrorResponse(ResponseCode.USMG_400, "Missing required header: x-trace-id",
                    timestamp, "Header x-trace-id is required");
        }
        if (tenantId == null) {
            return buildErrorResponse(ResponseCode.USMG_400, "Missing required header: x-tenant-id",
                    timestamp, "Header x-tenant-id is required");
        }

        try {
            Long actualUserId = userId == null? userDetails.getUserId(): userId;

            List<Long> selectedPostIds = postTagRepository.findPostIdsByTaggedUserId(actualUserId);

            Page<Post> postPage = postRepository.findByIdInAndOrganizationIdOrderByPlatformCreatedAtDescCreatedAtDesc(selectedPostIds, tenantId, pageable);

            List<UserTimelineDto> timelineDtos = preparePostListWithDetail(pageable, postPage, timestamp, null);

            TimelinePostResponse response = new TimelinePostResponse();
            response.setStatus(true);
            response.setCode(ResponseCode.USMG_200);
            response.setMessage("Timeline posts fetched successfully");
            response.setDevMessage("Timeline data retrieved with associations");
            response.setTimestamp(timestamp);
            response.setPosts(timelineDtos);
            response.setTotalElements(postPage.getTotalElements());
            response.setTotalPages(postPage.getTotalPages());
            response.setPage(pageable.getPageNumber());

            return response;

        } catch (Exception ex) {
            log.error("[{}]|TIMELINE|GET_FAILED|Exception: {}", traceId, ex.getMessage(), ex);
            return buildErrorResponse(ResponseCode.USMG_500, "Internal server error",
                    timestamp, ex.getMessage());
        }
    }

    @Override
    public BaseResponse getPostDetailById(String traceId, Long postId, Long tenantId, Pageable pageable, UserDetailsImpl userDetails) {
        Date timestamp = new Date();
        log.info("[{}]|TIMELINE|GET|User {} fetching posts for tenantId: {}",
                traceId, userDetails.getUsername(), tenantId);

        if (traceId == null || traceId.isEmpty()) {
            return buildErrorResponse(ResponseCode.USMG_400, "Missing required header: x-trace-id",
                    timestamp, "Header x-trace-id is required");
        }
        if (tenantId == null) {
            return buildErrorResponse(ResponseCode.USMG_400, "Missing required header: x-tenant-id",
                    timestamp, "Header x-tenant-id is required");
        }

        try {
            Optional<Post> optPost = postRepository.findByIdAndOrganizationId(postId,tenantId);

            if (optPost.isEmpty()) {
                return buildErrorResponse(ResponseCode.USMG_401,
                        "Unauthorized or post not found", timestamp, "Access denied or mismatched tenant");
            }
            Page<Post> postPage = new PageImpl<>(Collections.singletonList(optPost.get()), pageable, 1);

            List<UserTimelineDto> timelineDtos = preparePostListWithDetail(pageable, postPage, timestamp, null);

            TimelinePostResponse response = new TimelinePostResponse();
            response.setStatus(true);
            response.setCode(ResponseCode.USMG_200);
            response.setMessage("Timeline posts fetched successfully");
            response.setDevMessage("Timeline data retrieved with associations");
            response.setTimestamp(timestamp);
            response.setPost(timelineDtos.getFirst());
            response.setTotalElements(postPage.getTotalElements());
            response.setTotalPages(postPage.getTotalPages());
            response.setPage(pageable.getPageNumber());

            return response;

        } catch (Exception ex) {
            log.error("[{}]|TIMELINE|GET_FAILED|Exception: {}", traceId, ex.getMessage(), ex);
            return buildErrorResponse(ResponseCode.USMG_500, "Internal server error",
                    timestamp, ex.getMessage());
        }
    }

    @Override
    public BaseResponse getActivitiesPostSharedUsers(String traceId, Long postId, Long tenantId, Pageable pageable, UserDetailsImpl userDetails) {
        Date timestamp = new Date();
        log.info("[{}]|TIMELINE|GET|User {} fetching posts for tenantId: {}",
                traceId, userDetails.getUsername(), tenantId);

        if (traceId == null || traceId.isEmpty()) {
            return buildErrorResponse(ResponseCode.USMG_400, "Missing required header: x-trace-id",
                    timestamp, "Header x-trace-id is required");
        }
        if (tenantId == null) {
            return buildErrorResponse(ResponseCode.USMG_400, "Missing required header: x-tenant-id",
                    timestamp, "Header x-tenant-id is required");
        }

        try {

            Page<ExternalShare> externalSharesPage = externalShareRepository.findByPostIdOrderBySharedAtDesc(postId,pageable);

            List<Long> userIds = externalSharesPage.getContent()
                    .stream()
                    .map(ExternalShare::getUserId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            List<User> users = userRepository.findAllById(userIds);

            Map<Long, User> userMap = users.stream()
                    .collect(Collectors.toMap(User::getId, Function.identity()));


            List<ShareUserDTO> shareUserDTOs = new ArrayList<>();

            if(externalSharesPage.hasContent()){
                for (ExternalShare externalShare : externalSharesPage.getContent()) {
                    Optional<ShareUserDTO> shareUserDTOFetch = shareUserDTOs.stream().filter(o -> o.getUserId().equals(externalShare.getUserId())).findFirst();

                    ShareUserDTO shareUserDTO = null;

                    if(shareUserDTOFetch.isPresent()){
                        shareUserDTO = shareUserDTOFetch.get();
                    }else {
                        User user = userMap.get(externalShare.getUserId());
                        shareUserDTO = new ShareUserDTO();
                        shareUserDTO.setUserId(user.getId());
                        shareUserDTO.setUserName(user.getName());
                        shareUserDTO.setJobTitle(user.getJobTitle());
                        shareUserDTO.setProfilePicture(user.getProfilePictureUrl());

                        shareUserDTOs.add(shareUserDTO);
                    }

                    switch (externalShare.getPlatform()){
                        case "X":
                            shareUserDTO.setSharedInX(true);
                            break;
                        case "LINKEDIN":
                            shareUserDTO.setSharedInLinkedin(true);
                            break;
                        case "FACEBOOK":
                            shareUserDTO.setSharedInFacebook(true);
                            break;
                        case "INSTAGRAM":
                            shareUserDTO.setSharedInInstagram(true);
                            break;
                    }

                }

            }


            TimelinePostResponse response = new TimelinePostResponse();
            response.setStatus(true);
            response.setCode(ResponseCode.USMG_200);
            response.setMessage("Timeline posts fetched successfully");
            response.setDevMessage("Timeline data retrieved with associations");
            response.setTimestamp(timestamp);
            response.setShareUserDTOs(shareUserDTOs);
//            response.setTotalElements(postPage.getTotalElements());
//            response.setTotalPages(postPage.getTotalPages());
//            response.setPage(pageable.getPageNumber());

            return response;

        } catch (Exception ex) {
            log.error("[{}]|TIMELINE|GET_FAILED|Exception: {}", traceId, ex.getMessage(), ex);
            return buildErrorResponse(ResponseCode.USMG_500, "Internal server error",
                    timestamp, ex.getMessage());
        }
    }

    private void resolveSourceInfo(UserTimelineDto dto, Post post, User user,
                                   Map<String, ExternalPlatform> externalPlatformMap,
                                   Map<String, UserAuthToken> leaderTokenMap) {
        String platform = post.getType();
        if (!StringUtils.hasText(platform)) return;
        String normalizedPlatform = platform.toUpperCase();

        if ("BUSINESS_PAGE".equals(post.getSourceType())) {
            ExternalPlatform ep = externalPlatformMap.get(post.getOrganizationId() + ":" + normalizedPlatform);
            if (ep != null && StringUtils.hasText(ep.getCredentials())) {
                try {
                    com.fasterxml.jackson.databind.JsonNode creds = new com.fasterxml.jackson.databind.ObjectMapper().readTree(ep.getCredentials());
                    dto.setSourceName(firstNonBlank(jsonText(creds, "displayName"), jsonText(creds, "name"), jsonText(creds, "pageName")));
                    dto.setSourceUsername(jsonText(creds, "username"));
                    dto.setSourceAvatarUrl(firstNonBlank(jsonText(creds, "profileImageUrl"), jsonText(creds, "avatarUrl")));
                } catch (Exception ignored) {}
            }
        } else if ("LEADER_PROFILE".equals(post.getSourceType()) && user != null) {
            UserAuthToken token = leaderTokenMap.get(user.getId() + ":" + normalizedPlatform);
            String username = token != null ? token.getUserIdExternal() : null;
            dto.setSourceName(user.getName());
            if (StringUtils.hasText(username) && ("X".equals(normalizedPlatform) || "TWITTER".equals(normalizedPlatform))) {
                dto.setSourceUsername("@" + username);
            }
            dto.setSourceAvatarUrl(user.getProfilePictureUrl());
        }
    }

    private static String jsonText(com.fasterxml.jackson.databind.JsonNode node, String field) {
        if (node == null || node.isNull()) return null;
        com.fasterxml.jackson.databind.JsonNode f = node.get(field);
        return f != null && !f.isNull() && StringUtils.hasText(f.asText()) ? f.asText().trim() : null;
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (StringUtils.hasText(v)) return v;
        }
        return null;
    }
}
