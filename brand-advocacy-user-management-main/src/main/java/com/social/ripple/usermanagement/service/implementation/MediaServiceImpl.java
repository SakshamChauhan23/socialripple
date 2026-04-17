/**
 * <<<<<<< HEAD <<<<<<< HEAD Filename: MediaServiceImpl.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including
 * all intellectual property rights) in this software and any derivative works based upon or derived from this software belongs exclusively to
 * Quasarix. Access to this software is forbidden to anyone except current employees of Quasarix and its affiliated companies who have executed
 * non-disclosure agreements explicitly covering such access. While in the employment of Quasarix or its affiliate companies as the case may be,
 * employees may use this software internally, solely in the course of employment, for the sole purpose of developing new functionalities, features,
 * procedures, routines, customizations or derivative works, or for the purpose of providing maintenance or support for the software. Save as
 * expressly permitted above, no license or right thereto is hereby granted to anyone, either directly, by implication or otherwise. On the
 * termination of employment, the license granted to employee to access the software shall terminate and the software should be returned to the
 * employer, without retaining any copies. This software is (i) proprietary to Quasarix; (ii) is of significant value to it; (iii) contains trade
 * secrets of Quasarix; (iv) is not publicly available; and (v) constitutes the confidential information of Quasarix. Any use, reproduction,
 * modification, distribution, public performance or display of this software or through the use of this software without the prior, express written
 * consent of Quasarix is strictly prohibited and may be in violation of applicable laws.
 */
package com.social.ripple.usermanagement.service.implementation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.social.ripple.usermanagement.dto.response.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.social.ripple.usermanagement.dao.model.Category;
import com.social.ripple.usermanagement.dao.model.ConfigParameter;
import com.social.ripple.usermanagement.dao.model.MediaCategoryMap;
import com.social.ripple.usermanagement.dao.model.MediaFile;
import com.social.ripple.usermanagement.dao.model.MediaHashtag;
import com.social.ripple.usermanagement.dao.model.MediaLibrary;
import com.social.ripple.usermanagement.dao.model.Post;
import com.social.ripple.usermanagement.dao.model.PostCategoryMap;
import com.social.ripple.usermanagement.dao.model.PostHashtag;
import com.social.ripple.usermanagement.dao.model.PostMedia;
import com.social.ripple.usermanagement.dao.model.PostTag;
import com.social.ripple.usermanagement.dao.repository.CategoryRepository;
import com.social.ripple.usermanagement.dao.repository.MediaCategoryMapRepository;
import com.social.ripple.usermanagement.dao.repository.MediaFileRepository;
import com.social.ripple.usermanagement.dao.repository.MediaHashtagsRepository;
import com.social.ripple.usermanagement.dao.repository.MediaLibraryRepository;
import com.social.ripple.usermanagement.dao.repository.PostCategoryMapRepository;
import com.social.ripple.usermanagement.dao.repository.PostHashtagRepository;
import com.social.ripple.usermanagement.dao.repository.PostMediaRepository;
import com.social.ripple.usermanagement.dao.repository.PostRepository;
import com.social.ripple.usermanagement.dao.repository.PostTagRepository;
import com.social.ripple.usermanagement.dto.request.CreatePostRequest;
import com.social.ripple.usermanagement.dto.request.UpdateMediaLibraryRequest;
import com.social.ripple.usermanagement.dto.request.UploadLibraryRequest;
import com.social.ripple.usermanagement.dto.request.ZipUploadRequest;
import com.social.ripple.usermanagement.security.service.UserDetailsImpl;
import com.social.ripple.usermanagement.service.IMediaService;
import com.social.ripple.usermanagement.service.IMediaStorageService;
import com.social.ripple.usermanagement.util.AppCache;
import com.social.ripple.usermanagement.util.ConfigKeys;
import com.social.ripple.usermanagement.util.constants.ApplicationConstants;
import com.social.ripple.usermanagement.util.constants.ResponseCode;
import com.social.ripple.usermanagement.util.enumeration.MediaType;
import com.social.ripple.usermanagement.util.enumeration.PostStatus;
import com.social.ripple.usermanagement.util.enumeration.PostType;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MediaServiceImpl implements IMediaService {

	private final MediaFileRepository mediaFileRepository;
	private final IMediaStorageService mediaStorageService;
	private final PostRepository postRepository;
	private final PostMediaRepository postMediaRepository;
	private final PostTagRepository postTagRepository;
	private final PostHashtagRepository postHashtagRepository;
	private final PostCategoryMapRepository postCategoryMapRepository;
	private final CategoryRepository categoryRepository;
	private final MediaLibraryRepository mediaLibraryRepository;
	private final MediaCategoryMapRepository mediaCategoryMapRepository;
	private final MediaHashtagsRepository mediaHashtagsRepository;
	private final AppCache appCache;
	private final ManagedMediaStorageService managedMediaStorageService;

	public MediaServiceImpl(MediaFileRepository mediaFileRepository, @Qualifier("localStorageService")
	IMediaStorageService mediaStorageService, PostRepository postRepository, PostMediaRepository postMediaRepository,
			PostTagRepository postTagRepository, PostHashtagRepository postHashtagRepository, PostCategoryMapRepository postCategoryMapRepository,
			CategoryRepository categoryRepository, MediaLibraryRepository mediaLibraryRepository,
			MediaCategoryMapRepository mediaCategoryMapRepository, MediaHashtagsRepository mediaHashtagsRepository, AppCache appCache,
			ManagedMediaStorageService managedMediaStorageService) {
		this.mediaFileRepository = mediaFileRepository;
		this.mediaStorageService = mediaStorageService;
		this.postRepository = postRepository;
		this.postMediaRepository = postMediaRepository;
		this.postTagRepository = postTagRepository;
		this.postHashtagRepository = postHashtagRepository;
		this.postCategoryMapRepository = postCategoryMapRepository;
		this.categoryRepository = categoryRepository;
		this.mediaLibraryRepository = mediaLibraryRepository;
		this.mediaCategoryMapRepository = mediaCategoryMapRepository;
		this.mediaHashtagsRepository = mediaHashtagsRepository;
		this.appCache = appCache;
		this.managedMediaStorageService = managedMediaStorageService;
	}

	@Override
	public UploadMediaResponse uploadMedia(String traceId, UserDetailsImpl userDetails, MultipartFile[] files, String mediaType) {
		List<Long> imageIds = new ArrayList<>();
		List<Long> videoIds = new ArrayList<>();
		List<MediaDTO> mediaDTOs = new ArrayList<>();

		try {
			if (files == null || files.length == 0) {
				log.warn("[{}]|MEDIA|VALIDATION_FAILED|No files provided for upload", traceId);
				return new UploadMediaResponse(false, ResponseCode.USMG_400, "No files uploaded", "File array is empty", new Date(), null);
			}
			MediaType typeEnum = MediaType.fromString(mediaType);
			if (typeEnum == null) {
				log.error("[{}]|MEDIA|VALIDATION_FAILED|Unsupported mediaType: {}", traceId, mediaType);
				return new UploadMediaResponse(false, ResponseCode.USMG_400, "Invalid media type", "Unsupported mediaType: " + mediaType, new Date(),
						null);
			}

			for (MultipartFile file : files) {
				if (file == null || file.isEmpty()) {
					log.warn("[{}]|MEDIA|VALIDATION_FAILED|Skipping empty file", traceId);
					continue;
				}

				StoredMediaResult storedMedia = managedMediaStorageService.store(traceId, file, typeEnum, userDetails.getOrganization().getId());

				MediaFile media = new MediaFile();
				media.setOrganizationId(userDetails.getOrganization().getId());
				media.setFileName(storedMedia.getFileName());
				media.setFileUrl(storedMedia.getFileUrl());
				media.setFileType(mediaType);
				media.setUploadedBy(userDetails.getUserId());
				media.setArchived(false);
				media.setFileId(storedMedia.getFileId());
				media.setStorageProvider(storedMedia.getStorageProvider().name());
				media.setProviderAssetId(storedMedia.getProviderAssetId());
				media.setPlaybackUrl(storedMedia.getPlaybackUrl());
				media.setThumbnailUrl(storedMedia.getThumbnailUrl());
				media.setProcessingStatus(storedMedia.getProcessingStatus());

				media = mediaFileRepository.save(media);

				if (typeEnum == MediaType.IMAGE) {
					imageIds.add(media.getId());
					mediaDTOs.add(new MediaDTO(media.getId(), media.getFileUrl(), mediaType, media.getThumbnailUrl(), media.getProcessingStatus()));
				}
				else if (typeEnum == MediaType.VIDEO) {
					videoIds.add(media.getId());
					mediaDTOs.add(new MediaDTO(media.getId(), media.getFileUrl(), mediaType, media.getThumbnailUrl(), media.getProcessingStatus()));
				}

				log.info("[{}]|MEDIA|STORED|Media saved. ID={}, fileId={}, provider={}", traceId, media.getId(), media.getFileId(),
						media.getStorageProvider());
			}

			return new UploadMediaResponse(true, ResponseCode.USMG_200, "Files uploaded successfully", null, new Date(),
					new MediaUploadData(imageIds, videoIds,mediaDTOs, 0, 0, 0));

		}
		catch (IOException ex) {
			log.error("[{}]|MEDIA|UPLOAD_FAILED|IOException: {}", traceId, ex.getMessage(), ex);
			return new UploadMediaResponse(false, ResponseCode.USMG_500, "Cannot upload the media file.", ex.getMessage(), new Date(), null);
		}
		catch (Exception ex) {
			log.error("[{}]|MEDIA|UPLOAD_FAILED|Exception: {}", traceId, ex.getMessage(), ex);
			return new UploadMediaResponse(false, ResponseCode.USMG_500, "File upload failed", ex.getMessage(), new Date(), null);
		}
	}

	@Override
	@Transactional
	public BaseResponse postdMedia(String traceId, UserDetailsImpl userDetails, CreatePostRequest postRequest) {
		Date timestamp = new Date();
		log.info("[{}]|POST|INIT|Start processing post creation request", traceId);

		if (postRequest == null || userDetails == null) {
			log.warn("[{}]|POST|VALIDATION_FAILED|Request or user context is null", traceId);
			return buildErrorPostResponse(traceId, ResponseCode.USMG_400, "Invalid request", "/posts/media", timestamp,
					null);
		}

		boolean isContentBlank = postRequest.getContent() == null || postRequest.getContent().isBlank();
		boolean hasLocalMedia = postRequest.getMediaIds() != null && !postRequest.getMediaIds().isEmpty();
		boolean hasLibraryMedia = postRequest.getLibraryMediaIds() != null
				&& !postRequest.getLibraryMediaIds().isEmpty();

		if (isContentBlank && !hasLocalMedia && !hasLibraryMedia) {
			log.warn("[{}]|POST|VALIDATION|Both content and media are missing", traceId);
			return buildErrorPostResponse(traceId, ResponseCode.USMG_401, "Either content or media must be provided",
					"media/posts", timestamp, null);
		}

		try {
			Long userId = userDetails.getUserId();
			Long orgId = userDetails.getOrganization().getId();

			Post post = createAndSavePost(traceId, postRequest, userId, orgId);
			Long postId = post.getId();

			processTaggedUsers(traceId, postRequest, postId);
			processHashtags(traceId, postRequest, post);
			processCategories(traceId, postRequest, postId);

			if (hasLocalMedia) {
				BaseResponse localMediaResponse = processMedia(traceId, postRequest, orgId, postId, timestamp);
				if (localMediaResponse != null) {
					return localMediaResponse;
				}
			}

			if (hasLibraryMedia) {
				BaseResponse libraryMediaResponse = processLibraryMedia(traceId, postRequest, orgId, postId, timestamp);
				if (libraryMediaResponse != null) {
					return libraryMediaResponse;
				}
			}

			log.info("[{}]|POST|SUCCESS|Post created successfully with ID: {}", traceId, postId);
			return buildSuccessPostMediaResponse(traceId, postId);

		} catch (Exception ex) {
			log.error("[{}]|POST|EXCEPTION|Unhandled error during post creation: {}", traceId, ex.getMessage(), ex);
			return buildErrorPostResponse(traceId, ResponseCode.USMG_500, "Internal server error", "/posts/media", null,
					ex.getMessage());
		}
	}

	private PostMediaResponse buildSuccessPostMediaResponse(String traceId, Long postId) {
		PostMediaResponse response = new PostMediaResponse();
		try {
			PostResponse postResponse = new PostResponse(String.valueOf(postId));

			response.setStatus(true);
			response.setCode(ResponseCode.USMG_200);
			response.setMessage("Post created successfully");
			response.setDevMessage("Post and media processed without error");
			response.setPostMediaResponseData(postResponse);
		} catch (Exception ex) {
			log.error("[{}]|POST|EXCEPTION|Unhandled error during post creation: {}", traceId, ex.getMessage(), ex);
			return buildErrorPostResponse(traceId, ResponseCode.USMG_500, "Internal server error", "/posts/media", null,
					ex.getMessage());
		}

		return response;
	}

	private Post createAndSavePost(String traceId, CreatePostRequest postRequest, Long userId, Long orgId) {
		Post post = new Post();
		post.setContent(Optional.ofNullable( cleanUnicode(postRequest.getContent())).orElse("").trim());
		post.setXGeneratedContent(Optional.ofNullable(cleanUnicode(postRequest.getXGeneratedContent())).orElse("").trim());
		post.setXHashtags(Optional.ofNullable(cleanUnicode(postRequest.getXHashtags())).orElse("").trim());
		post.setCreatedBy(userId);
		post.setOrganizationId(orgId);
		post.setStatus(PostStatus.PUBLISHED.name());
		post.setCreatedAt(LocalDateTime.now());
		post.setUpdatedAt(LocalDateTime.now());
		post.setType(PostType.MANUAL.name());

		postRepository.save(post);
		log.info("[{}]|POST|DB|Post saved with ID: {}", traceId, post.getId());
		return post;
	}

	private void processTaggedUsers(String traceId, CreatePostRequest postRequest, Long postId) {
		Optional.ofNullable(postRequest.getTaggedUserIds()).orElse(Collections.emptyList()).stream()
				.filter(Objects::nonNull).forEach(taggedUserId -> {
					try {
						PostTag tag = new PostTag();
						tag.setPostId(postId);
						tag.setTaggedUserId(taggedUserId);
						postTagRepository.save(tag);
						log.debug("[{}]|POST|TAGGING|Tagged user ID: {}", traceId, taggedUserId);
					} catch (Exception ex) {
						log.error("[{}]|POST|TAGGING_ERROR|Failed to tag user ID {}: {}", traceId, taggedUserId,
								ex.getMessage(), ex);
					}
				});
	}

	private void processHashtags(String traceId, CreatePostRequest postRequest, Post post) {
		Optional.ofNullable(postRequest.getHashtags()).orElse(Collections.emptyList()).stream()
				.filter(tag -> tag != null && !tag.isBlank()).map(String::trim).forEach(tag -> {
					try {
						PostHashtag hashtag = new PostHashtag();
						hashtag.setPost(post);
						hashtag.setHashTag(tag);
						postHashtagRepository.save(hashtag);
						log.debug("[{}]|POST|HASHTAG|Added hashtag: {}", traceId, tag);
					} catch (Exception ex) {
						log.error("[{}]|POST|HASHTAG_ERROR|Failed to save hashtag '{}': {}", traceId, tag,
								ex.getMessage(), ex);
					}
				});
	}

	private void processCategories(String traceId, CreatePostRequest postRequest, Long postId) {
		Optional.ofNullable(postRequest.getCategories()).orElse(Collections.emptyList()).stream()
				.filter(Objects::nonNull).forEach(categoryId -> {
					try {
						PostCategoryMap categoryMap = new PostCategoryMap();
						categoryMap.setPostId(postId);
						categoryMap.setCategoryId(categoryId);
						postCategoryMapRepository.save(categoryMap);
						log.debug("[{}]|POST|CATEGORY|Mapped category ID: {}", traceId, categoryId);
					} catch (Exception ex) {
						log.error("[{}]|POST|CATEGORY_ERROR|Failed to map category ID {}: {}", traceId, categoryId,
								ex.getMessage(), ex);
					}
				});
	}

	private BaseResponse processMedia(String traceId, CreatePostRequest postRequest, Long orgId, Long postId,
									  Date timestamp) {
		List<Long> mediaIds = Optional.ofNullable(postRequest.getMediaIds()).orElse(Collections.emptyList());
		if (mediaIds.isEmpty())
			return null;

		log.info("[{}]|POST|MEDIA|Processing media attachments", traceId);

		List<MediaFile> mediaFiles;
		try {
			mediaFiles = mediaFileRepository.findAllById(mediaIds);
		} catch (Exception ex) {
			log.error("[{}]|POST|MEDIA_DB_ERROR|Failed to retrieve media files: {}", traceId, ex.getMessage(), ex);
			return buildErrorPostResponse(traceId, "MEDIA_001", "Failed to retrieve media files", "/posts/media",
					timestamp, ex.getMessage());
		}

		for (MediaFile media : mediaFiles) {
			if (!orgId.equals(media.getOrganizationId())) {
				log.warn("[{}]|POST|MEDIA|Organization mismatch for media ID: {}", traceId, media.getId());
				return buildErrorPostResponse(traceId, ResponseCode.USMG_400, "Media file organization mismatch",
						"/posts/media", timestamp, null);
			}

			BaseResponse response = handleMediaAttachment(traceId, media, postId, timestamp);
			if (response != null)
				return response;
		}
		return null;
	}

	private BaseResponse processLibraryMedia(String traceId, CreatePostRequest postRequest, Long orgId, Long postId,
											 Date timestamp) {
		List<Long> libraryMediaIds = Optional.ofNullable(postRequest.getLibraryMediaIds())
				.orElse(Collections.emptyList());
		if (libraryMediaIds.isEmpty())
			return null;

		log.info("[{}]|POST|MEDIA_LIBRARY|Processing library media attachments", traceId);

		List<MediaLibrary> mediaLibraryFiles;
		try {
			mediaLibraryFiles = mediaLibraryRepository.findAllById(libraryMediaIds);
		} catch (Exception ex) {
			log.error("[{}]|POST|MEDIA_LIBRARY_DB_ERROR|Failed to retrieve library media files: {}", traceId,
					ex.getMessage(), ex);
			return buildErrorPostResponse(traceId, "MEDIA_LIB_001", "Failed to retrieve library media files",
					"/posts/media/library", timestamp, ex.getMessage());
		}

		for (MediaLibrary media : mediaLibraryFiles) {
			if (!orgId.equals(media.getOrganizationId())) {
				log.warn("[{}]|POST|MEDIA_LIBRARY|Organization mismatch for library media ID: {}", traceId,
						media.getId());
				return buildErrorPostResponse(traceId, ResponseCode.USMG_400,
						"Library media file organization mismatch", "/posts/media/library", timestamp, null);
			}

			try {
				MediaType mediaType = MediaType.valueOf(media.getFileType().toUpperCase());

				PostMedia postMedia = new PostMedia();
				postMedia.setPostId(postId);
				postMedia.setFileUrl(media.getUrl());
				postMedia.setMediaType(mediaType.name());

				postMediaRepository.save(postMedia);
				log.debug("[{}]|POST|MEDIA_LIBRARY|Attached library media ID: {}", traceId, media.getId());
			} catch (IllegalArgumentException ex) {
				log.error("[{}]|POST|MEDIA_LIBRARY|Invalid media type for library media ID: {} - {}", traceId,
						media.getId(), media.getFileType());
				return buildErrorPostResponse(traceId, ResponseCode.USMG_400,
						"Invalid library media type: " + media.getFileType(), "/posts/media/library", timestamp,
						ex.getMessage());
			} catch (Exception ex) {
				log.error("[{}]|POST|MEDIA_LIBRARY|Failed to attach library media ID {}: {}", traceId, media.getId(),
						ex.getMessage(), ex);
				return buildErrorPostResponse(traceId, ResponseCode.USMG_500, "Failed to attach library media",
						"/posts/media/library", timestamp, ex.getMessage());
			}
		}
		return null;
	}

	private BaseResponse handleMediaAttachment(String traceId, MediaFile media, Long postId, Date timestamp) {
		try {
			MediaType mediaType = MediaType.valueOf(media.getFileType().toUpperCase());

			PostMedia postMedia = new PostMedia();
			postMedia.setPostId(postId);
			postMedia.setFileUrl(media.getFileUrl());
			postMedia.setMediaType(mediaType.name());

			postMediaRepository.save(postMedia);
			log.debug("[{}]|POST|MEDIA|Attached media ID: {}", traceId, media.getId());

		}
		catch (IllegalArgumentException ex) {
			log.error("[{}]|POST|MEDIA|Invalid media type for media ID: {} - {}", traceId, media.getId(), media.getFileType());
			return buildErrorPostResponse(traceId, ResponseCode.USMG_400, "Invalid media type: " + media.getFileType(), "/posts/media", timestamp,
					ex.getMessage());
		}
		catch (Exception ex) {
			log.error("[{}]|POST|MEDIA|Failed to attach media ID {}: {}", traceId, media.getId(), ex.getMessage(), ex);
			return buildErrorPostResponse(traceId, ResponseCode.USMG_500, "Failed to attach media", "/posts/media", timestamp, ex.getMessage());
		}
		return null;
	}

	private PostMediaResponse buildErrorPostResponse(String traceId, String code, String message, String path, Date timestamp, String devMessage) {
		ErrorObj error = new ErrorObj(path, code, "ERROR", message);
		List<ErrorObj> errors = List.of(error);

		PostMediaResponse response = new PostMediaResponse();
		response.setStatus(false);
		response.setCode(code);
		response.setMessage(message);
		response.setDevMessage(devMessage);
		response.setTimestamp(timestamp);
		response.setErrors(errors);
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

		Map<String, MediaFile> mediaFilesByUrl = new HashMap<>();
		for (MediaFile mediaFile : mediaFileRepository.findByOrganizationIdAndFileUrlIn(tenantId, fileUrls)) {
			if (StringUtils.hasText(mediaFile.getFileUrl()) && !mediaFilesByUrl.containsKey(mediaFile.getFileUrl())) {
				mediaFilesByUrl.put(mediaFile.getFileUrl(), mediaFile);
			}
		}
		return mediaFilesByUrl;
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

	private String getConfig(String key, String defaultValue) {
		return Optional.ofNullable(AppCache.configParameters.get(key))
				.map(ConfigParameter::getConfigValue)
				.filter(val -> !val.isEmpty())
				.orElse(defaultValue);
	}

	@Override
	public BaseResponse getMedia(String traceId, Long postId, UserDetailsImpl userDetails, Pageable pageable, Long tenantId, Long categoryId,
			String searchText, boolean leader) {

		Date timestamp = new Date();
		log.info("[{}]|POST|GET|Fetching media data for postId: {}", traceId, postId);
		Page<Post> postPage = null;

		try {
			List<Post> posts;

			if (postId == null) {
				if (leader) {
//					postPage = postRepository.findLeaderPosts(tenantId, pageable);
				}
				else if (categoryId != null || (searchText != null && !searchText.isBlank())) {
					postPage = postRepository.findFilteredPosts(userDetails.getUserId(), tenantId, categoryId, searchText, pageable);
				}
				else {
					postPage = postRepository.findByCreatedByAndOrganizationId(userDetails.getUserId(), tenantId, pageable);
				}
				posts = postPage.getContent();
			}
			else {
				Optional<Post> optionalPost = postRepository.findById(postId);
				if (optionalPost.isEmpty() || !optionalPost.get().getOrganizationId().equals(tenantId)
						|| (!leader && !optionalPost.get().getCreatedBy().equals(userDetails.getUserId()))) {
					return buildErrorPostResponse(traceId, ResponseCode.USMG_401, "Unauthorized or post not found", "/media/get", new Date(),
							"Access denied or mismatched tenant");
				}
				posts = List.of(optionalPost.get());
				postPage = new PageImpl<>(posts, pageable, 1);
			}

			if (posts.isEmpty()) {
				return buildErrorPostResponse(traceId, ResponseCode.USMG_404, "No posts found", "/media/get", new Date(), null);
			}

			List<Long> postIds = new ArrayList<>();
			for (Post post : posts) {
				postIds.add(post.getId());
			}

			List<PostHashtag> hashtags = postHashtagRepository.findByPostIds(postIds);
			List<PostMedia> mediaList = postMediaRepository.findByPostIds(postIds);
			List<PostTag> tags = postTagRepository.findByPostIds(postIds);
			List<PostCategoryMap> categoryMaps = postCategoryMapRepository.findByPostIds(postIds);

			Set<Long> categoryIds = new HashSet<>();
			for (PostCategoryMap map : categoryMaps) {
				categoryIds.add(map.getCategoryId());
			}

			List<Category> categories = categoryRepository.findByIds(new ArrayList<>(categoryIds));
			Map<Long, Category> categoryMap = new HashMap<>();
			for (Category category : categories) {
				categoryMap.put(category.getId(), category);
			}

			List<PostResponseDto> postDtoList = new ArrayList<>();
			List<PostHashtagsDto> hashtagDtos = new ArrayList<>();
			List<MediaPostDto> mediaDtos = new ArrayList<>();
			List<PostTagDto> tagDtos = new ArrayList<>();
			List<PostCategoryMapDto> categoryDtos = new ArrayList<>();

			for (Post post : posts) {
				PostResponseDto postDto = new PostResponseDto();
				postDto.setId(post.getId());
				postDto.setTitle(post.getTitle());
				postDto.setOrganizationId(post.getOrganizationId());
				postDto.setCreatedBy(post.getCreatedBy());
				postDto.setContent(post.getContent());
				postDto.setCreatedAt(post.getCreatedAt());
				postDtoList.add(postDto);
			}

			for (PostHashtag hash : hashtags) {
				PostHashtagsDto dto = new PostHashtagsDto();
				dto.setId(hash.getId());
				dto.setHashTag(hash.getHashTag());
				hashtagDtos.add(dto);
			}

			Map<String, MediaFile> mediaFilesByUrl = buildMediaFileMap(tenantId, mediaList);
			for (PostMedia media : mediaList) {
				mediaDtos.add(toMediaPostDto(media, mediaFilesByUrl.get(media.getFileUrl())));
			}

			for (PostTag taged : tags) {
				PostTagDto dto = new PostTagDto();
				dto.setId(taged.getId());
				dto.setPostId(taged.getPostId());
				dto.setTaggedUserId(taged.getTaggedUserId());
				tagDtos.add(dto);
			}

			for (PostCategoryMap category : categoryMaps) {
				Category c = categoryMap.get(category.getCategoryId());
				if (c != null) {
					PostCategoryMapDto dto = new PostCategoryMapDto();
					dto.setId(category.getId());
					dto.setPostId(category.getPostId());
					dto.setCategoryId(category.getCategoryId());
					dto.setCategoryName(c.getName());
					dto.setCategoryDescription(c.getDescription());
					categoryDtos.add(dto);
				}
			}

			MediaResponse response = new MediaResponse();
			response.setStatus(true);
			response.setCode(ResponseCode.USMG_200);
			response.setMessage("Post media data fetched successfully");
			response.setDevMessage("Post data retrieved with associations");
			response.setTimestamp(timestamp);
			response.setPostData(postDtoList);
			response.setHashTags(hashtagDtos);
			response.setMediaPostDto(mediaDtos);
			response.setPostTag(tagDtos);
			response.setPostCategory(categoryDtos);
			response.setTotalElements(postPage.getTotalElements());
			response.setTotalPages(postPage.getTotalPages());
			response.setPage(pageable.getPageNumber());

			return response;
		}
		catch (Exception ex) {
			log.error("[{}]|POST|GET_FAILED|Exception: {}", traceId, ex.getMessage(), ex);
			return buildErrorPostResponse(traceId, ResponseCode.USMG_500, "Internal server error", "/media/get", timestamp, ex.getMessage());

		}
	}

	@Override
	public BaseResponse unzipFile(String traceId, String tenantId, UserDetailsImpl userDetails, ZipUploadRequest zipRequest) {

		log.info("[{}]|LIBRARY|UNZIP_INIT|Start processing ZIP upload", traceId);

		if (tenantId == null || tenantId.trim().isEmpty() || userDetails.getOrganization() == null || userDetails.getOrganization().getId() == null
				|| !tenantId.equals(String.valueOf(userDetails.getOrganization().getId()))) {

			log.warn("[{}]|LIBRARY|UNZIP_VALIDATION_FAILED|Invalid tenant or organization", traceId);
			return buildErrorResponse(ResponseCode.USMG_401, "Unauthorized - Invalid Tenant ID");
		}

		if (zipRequest.getZipFile() == null || zipRequest.getZipFile().isEmpty()) {
			log.warn("[{}]|LIBRARY|UNZIP_VALIDATION_FAILED|ZIP file missing or empty", traceId);
			return buildErrorResponse(ResponseCode.USMG_400, "ZIP file is missing or empty");
		}

		String originalFilename = zipRequest.getZipFile().getOriginalFilename();
		if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".zip")) {
			log.warn("[{}]|LIBRARY|UNZIP_VALIDATION_FAILED|Unsupported file format: {}", traceId, originalFilename);
			return buildErrorResponse(ResponseCode.USMG_400, "Only .zip files are supported");
		}

		int maxFilesInZip;
		int maxFileSizeMB;

		try {
			String maxFilesStr = appCache.getConfigParameterValue(traceId, ConfigKeys.MAX_FILES_IN_ZIP);
			String maxFileSizeStr = appCache.getConfigParameterValue(traceId, ConfigKeys.MAX_FILE_SIZE_MB);

			if (maxFilesStr == null || maxFileSizeStr == null) {
				log.error("[{}]|LIBRARY|CONFIG_INVALID|Missing configuration: MAX_FILES_IN_ZIP or MAX_FILE_SIZE_MB", traceId);
				return buildErrorResponse(ResponseCode.USMG_500, "Server configuration invalid - missing MAX_FILES_IN_ZIP or MAX_FILE_SIZE_MB");
			}

			maxFilesInZip = Integer.parseInt(maxFilesStr);
			maxFileSizeMB = Integer.parseInt(maxFileSizeStr);

			log.info("[{}]|LIBRARY|CONFIG_VALUES|MAX_FILES_IN_ZIP={} MAX_FILE_SIZE_MB={}", traceId, maxFilesInZip, maxFileSizeMB);
		}
		catch (NumberFormatException e) {
			log.error("[{}]|LIBRARY|CONFIG_INVALID|Invalid config values", traceId, e);
			return buildErrorResponse(ResponseCode.USMG_500, "Server configuration invalid", e.getMessage());
		}

		List<MultipartFile> extractedFiles = new ArrayList<>();
		int totalFiles = 0, successCount = 0, failedCount = 0;

		try (ZipInputStream zis = new ZipInputStream(zipRequest.getZipFile().getInputStream())) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				if (entry.isDirectory())
					continue;

				totalFiles++;
				if (totalFiles > maxFilesInZip) {
					log.warn("[{}]|LIBRARY|UNZIP_VALIDATION_FAILED|Exceeded max files in ZIP: {}", traceId, totalFiles);
					return buildErrorResponse(ResponseCode.USMG_400, "Too many files in ZIP. Max allowed: " + maxFilesInZip);
				}

				String fileName = entry.getName();
				if (!isSupportedMedia(fileName, zipRequest.getMediaType())) {
					failedCount++;
					log.debug("[{}]|LIBRARY|UNZIP_SKIPPED|Unsupported media type: {}", traceId, fileName);
					continue;
				}

				MultipartFile file = convertEntryToMultipartFile(entry, zis, fileName);

				if (file.getSize() > maxFileSizeMB * 1024L * 1024L) {
					failedCount++;
					log.debug("[{}]|LIBRARY|UNZIP_SKIPPED|File too large: {} Size: {}MB", traceId, fileName, file.getSize() / (1024 * 1024));
					continue;
				}

				extractedFiles.add(file);
				successCount++;
			}
		}
		catch (IOException ex) {
			log.error("[{}]|LIBRARY|UNZIP_FAILED|Error unzipping file {}: {}", traceId, originalFilename, ex.getMessage(), ex);
			return buildErrorResponse(ResponseCode.USMG_500, "Failed to unzip file", ex.getMessage());
		}

		if (extractedFiles.isEmpty()) {
			log.warn("[{}]|LIBRARY|UNZIP_VALIDATION_FAILED|No valid media files found", traceId);
			return buildErrorResponse(ResponseCode.USMG_400, "No valid media files found in the ZIP");
		}

		log.info("[{}]|LIBRARY|UNZIP_SUCCESS|Total files: {}, Success: {}, Failed: {}", traceId, totalFiles, successCount, failedCount);

		// Upload extracted files
		UploadMediaResponse uploadResponse = uploadMedia(traceId, userDetails, extractedFiles.toArray(new MultipartFile[0]),
				zipRequest.getMediaType());

		if (uploadResponse.getMediaUploadData() == null) {
			uploadResponse.setMediaUploadData(new MediaUploadData());
		}
		uploadResponse.getMediaUploadData().setTotalFiles(totalFiles);
		uploadResponse.getMediaUploadData().setSuccessCount(successCount);
		uploadResponse.getMediaUploadData().setFailedCount(failedCount);

		return uploadResponse;
	}

	private MultipartFile convertEntryToMultipartFile(ZipEntry entry, ZipInputStream zis, String fileName) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buffer = new byte[4096];
		int len;

		while ((len = zis.read(buffer)) > 0) {
			baos.write(buffer, 0, len);
		}

		return new MultipartFile() {
			@Override
			public String getName() {
				return fileName;
			}

			@Override
			public String getOriginalFilename() {
				return fileName;
			}

			@Override
			public String getContentType() {
				return null;
			}

			@Override
			public boolean isEmpty() {
				return baos.size() == 0;
			}

			@Override
			public long getSize() {
				return baos.size();
			}

			@Override
			public byte[] getBytes() {
				return baos.toByteArray();
			}

			@Override
			public InputStream getInputStream() {
				return new ByteArrayInputStream(baos.toByteArray());
			}

			@Override
			public void transferTo(File dest) throws IOException {
				try (FileOutputStream fos = new FileOutputStream(dest)) {
					fos.write(baos.toByteArray());
				}
			}
		};
	}

	private boolean isSupportedMedia(String fileName, String mediaType) {
		if (fileName == null || mediaType == null) {
			return false;
		}

		fileName = fileName.toLowerCase();

		String[] allowedExtensions;
		if ("IMAGE".equalsIgnoreCase(mediaType)) {
			allowedExtensions = ApplicationConstants.ALLOWED_IMAGE_EXTENSIONS;
		}
		else if ("VIDEO".equalsIgnoreCase(mediaType)) {
			allowedExtensions = ApplicationConstants.ALLOWED_VIDEO_EXTENSIONS;
		}
		else {
			allowedExtensions = new String[0];
		}

		for (String extension : allowedExtensions) {
			if (fileName.endsWith(extension.toLowerCase())) {
				return true;
			}
		}
		return false;
	}

	private BaseResponse buildErrorResponse(String code, String message) {
		return buildErrorResponse(code, message, null);
	}

	private BaseResponse buildErrorResponse(String code, String message, String devMessage) {
		BaseResponse response = new BaseResponse();
		response.setStatus(false);
		response.setCode(code);
		response.setMessage(message);
		response.setDevMessage(devMessage);
		response.setTimestamp(new Date());
		return response;
	}

	@Override
	@Transactional
	public BaseResponse addMediaToLibrary(String traceId, String tenantId, UserDetailsImpl userDetails, UploadLibraryRequest libraryRequest) {
		Date timestamp = new Date();
		log.info("[{}]|LIBRARY|INIT|Start processing media library upload", traceId);

		if (libraryRequest == null || userDetails == null) {
			return buildErrorResponse(traceId, ResponseCode.USMG_400, "Invalid request", "/media-library/upload", timestamp, null);
		}

		try {
			Long userId = userDetails.getUserId();
			Long orgId = userDetails.getOrganization().getId();

			if (tenantId == null || !tenantId.equals(String.valueOf(orgId))) {
				return buildErrorResponse(traceId, ResponseCode.USMG_401, "Tenant/Organization mismatch", "/media-library/upload", timestamp, null);

			}

			if (libraryRequest.getMediaIds() != null && !libraryRequest.getMediaIds().isEmpty()) {
				List<MediaFile> mediaFiles = mediaFileRepository.findAllById(libraryRequest.getMediaIds());

				List<Long> createdLibraryIds = new ArrayList<>();

				for (MediaFile media : mediaFiles) {
					if (!orgId.equals(media.getOrganizationId())) {
						log.warn("[{}]|LIBRARY|MEDIA|Organization mismatch for media ID: {}", traceId, media.getId());
						return buildErrorResponse(traceId, ResponseCode.USMG_400, "Media file organization mismatch", "/media-library/upload",
								timestamp, null);
					}

					boolean exists = mediaLibraryRepository.existsByOrganizationIdAndUrl(orgId, media.getFileUrl());
					if (exists) {
						log.warn("[{}]|LIBRARY|DUPLICATE|Media already exists in library. Skipping mediaId={}", traceId, media.getId());
						continue;
					}

					MediaLibrary library = new MediaLibrary();
					library.setTitle(libraryRequest.getTitle());
					library.setOrganizationId(orgId);
					library.setCreatedBy(userId);
					library.setCreatedAt(LocalDateTime.now());
					library.setFileType(media.getFileType());
					library.setUrl(media.getFileUrl());

					mediaLibraryRepository.save(library);

					Long libraryId = library.getId();
					createdLibraryIds.add(libraryId);

					if (libraryRequest.getCategoryId() != null) {
						MediaCategoryMap category = new MediaCategoryMap();
						category.setLibraryId(libraryId);
						category.setCategoryId(libraryRequest.getCategoryId());
						mediaCategoryMapRepository.save(category);
					}

					if (libraryRequest.getHashtags() != null) {
						for (String tag : libraryRequest.getHashtags()) {
							if (tag != null && !tag.trim().isEmpty()) {
								MediaHashtag hashtag = new MediaHashtag();
								hashtag.setLibraryId(libraryId);
								hashtag.setHashtag(tag.trim());
								mediaHashtagsRepository.save(hashtag);
							}
						}
					}

					log.info("[{}]|LIBRARY|SAVED|Library ID: {}", traceId, libraryId);
				}

				if (createdLibraryIds.isEmpty()) {
					return buildErrorResponse(traceId, ResponseCode.USMG_409, "All media already exist in library", "/media-library/upload",
							timestamp, null);
				}

				Long firstLibraryId = createdLibraryIds.stream().findFirst().orElse(null);
				return buildSuccessResponse(traceId, firstLibraryId, timestamp);
			}

			log.info("[{}]|LIBRARY|NO_MEDIA|No media IDs provided in request", traceId);
			return buildErrorResponse(traceId, ResponseCode.USMG_400, "No media IDs provided", "/media-library/upload", timestamp, null);

		}
		catch (Exception ex) {
			log.error("[{}]|LIBRARY|EXCEPTION|Unhandled error during media library upload: {}", traceId, ex.getMessage(), ex);
			return buildErrorResponse(traceId, ResponseCode.USMG_500, "unhandled error during media library upload", "/media-library/upload",
					timestamp, ex.getMessage());
		}
	}

	private BaseResponse buildErrorResponse(String traceId, String code, String message, String path, Date timestamp, String devMessage) {
		BaseResponse response = new BaseResponse();
		response.setStatus(false);
		response.setCode(code);
		response.setMessage(message);
		response.setDevMessage(devMessage);
		response.setTimestamp(timestamp);
		return response;
	}

	private BaseResponse buildSuccessResponse(String traceId, Long libraryId, Date timestamp) {
		BaseResponse response = new BaseResponse();
		response.setStatus(true);
		response.setCode(ResponseCode.USMG_200);
		response.setMessage("Media library created successfully");
		response.setTimestamp(timestamp);
		return response;
	}

	// -----------------------------------------
	// Employee + Admin → ACTIVE only
	// -----------------------------------------
	@Override
	public BaseResponse fetchMediaLibrary(String traceId, String tenantId, UserDetailsImpl userDetails, String mediaType, Long categoryId, int page,
			int size) {
		Date timestamp = new Date();

		if (mediaType == null || mediaType.isBlank()) {
			log.warn("[{}]|LIBRARY|VALIDATION_FAILED|Media type missing", traceId);
			return buildErrorResponse(traceId, ResponseCode.USMG_400, "Invalid request", "Media type is required", timestamp, null);
		}
		mediaType = mediaType.toLowerCase();
		log.info("[{}]|LIBRARY|FETCH|Employee/Admin ACTIVE|Fetching {} for tenant={}", traceId, mediaType, tenantId);

		try {
			if (userDetails == null) {
				log.warn("[{}]|LIBRARY|VALIDATION_FAILED|User details missing", traceId);
				return buildErrorResponse(traceId, ResponseCode.USMG_400, "Invalid request", "User details missing", timestamp, null);
			}

			Long orgId = userDetails.getOrganization().getId();
			if (tenantId == null || !tenantId.equals(String.valueOf(orgId))) {
				log.warn("[{}]|LIBRARY|TENANT_MISMATCH|Tenant {} does not match org {}", traceId, tenantId, orgId);
				return buildErrorResponse(traceId, ResponseCode.USMG_401, "Tenant/Organization mismatch", null, timestamp, null);
			}

			Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
			boolean archived = false; // ACTIVE only

			Page<MediaLibrary> librariesPage;
			if (categoryId != null) {
				librariesPage = mediaLibraryRepository.findByOrgAndTypeAndCategoryAndArchived(orgId, mediaType, categoryId, archived, pageable);
			}
			else {
				librariesPage = mediaLibraryRepository.findByOrganizationIdAndFileTypeAndArchived(orgId, mediaType, archived, pageable);
			}

			List<LibraryDto> responseData = new ArrayList<>();
			for (MediaLibrary library : librariesPage.getContent()) {
				List<String> categoryNames = mediaCategoryMapRepository.findCategoryNamesByLibraryId(library.getId());
				List<String> hashtags = mediaHashtagsRepository.findHashtagsByLibraryId(library.getId());
				Optional<MediaFile> mediaFile = resolveLibraryMediaFile(traceId, orgId, library);

				responseData.add(buildLibraryDto(library, mediaFile, categoryNames, hashtags));

				log.debug("[{}]|LIBRARY|FETCHED|LibraryId={} with {} categories & {} hashtags", traceId, library.getId(), categoryNames.size(),
						hashtags.size());
			}

			log.info("[{}]|LIBRARY|SUCCESS|Fetched {} {} libraries (page {}/{})", traceId, responseData.size(), mediaType, page + 1,
					librariesPage.getTotalPages());

			LibraryResponse successResponse = new LibraryResponse();
			successResponse.setStatus(true);
			successResponse.setCode(ResponseCode.USMG_200);
			successResponse.setMessage(mediaType + " library (ACTIVE) fetched successfully");
			successResponse.setTimestamp(timestamp);
			successResponse.setMediaLibrary(responseData);
			successResponse.setPage(page);
			successResponse.setSize(size);
			successResponse.setTotalElements(librariesPage.getTotalElements());
			successResponse.setTotalPages(librariesPage.getTotalPages());

			return successResponse;

		}
		catch (Exception e) {
			log.error("[{}]|LIBRARY|ERROR|{}", traceId, e.getMessage(), e);
			return buildErrorResponse(traceId, ResponseCode.USMG_500, "Internal server error", e.getMessage(), timestamp, null);
		}
	}

	// -----------------------------------------
	// Admin only → ACTIVE or ARCHIVED (status param    ydydy  )
	// -----------------------------------------
	@Override
	public BaseResponse fetchMediaLibraryAdmin(String traceId, String tenantId, UserDetailsImpl userDetails, String mediaType, String status,
			Long categoryId, int page, int size) {
		Date timestamp = new Date();

		if (mediaType == null || mediaType.isBlank()) {
			log.warn("[{}]|LIBRARY|VALIDATION_FAILED|Media type missing", traceId);
			return buildErrorResponse(traceId, ResponseCode.USMG_400, "Invalid request", "Media type is required", timestamp, null);
		}
		mediaType = mediaType.toLowerCase();

		if (status == null || status.isBlank()) {
			status = "active";
		}
		status = status.toLowerCase();
		if (!status.equals("all") && !status.equals("active") && !status.equals("archived")) {
			log.warn("[{}]|LIBRARY|VALIDATION_FAILED|Invalid status {}", traceId, status);
			return buildErrorResponse(traceId, ResponseCode.USMG_400, "Invalid request", "Allowed status: active, archived", timestamp, null);
		}
		boolean archived = status.equals("archived");

		log.info("[{}]|LIBRARY|FETCH|Admin {}|Fetching {} for tenant={}", traceId, status.toUpperCase(), mediaType, tenantId);

		try {
			if (userDetails == null) {
				log.warn("[{}]|LIBRARY|VALIDATION_FAILED|User details missing", traceId);
				return buildErrorResponse(traceId, ResponseCode.USMG_400, "Invalid request", "User details missing", timestamp, null);
			}

			Long orgId = userDetails.getOrganization().getId();
			if (tenantId == null || !tenantId.equals(String.valueOf(orgId))) {
				log.warn("[{}]|LIBRARY|TENANT_MISMATCH|Tenant {} does not match org {}", traceId, tenantId, orgId);
				return buildErrorResponse(traceId, ResponseCode.USMG_401, "Tenant/Organization mismatch", null, timestamp, null);
			}

			Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

			Page<MediaLibrary> librariesPage;
			if (categoryId != null) {
				librariesPage = mediaLibraryRepository.findByOrgAndTypeAndCategoryAndArchived(orgId, mediaType, categoryId, archived, pageable);
				if(status.equals("all")){
					librariesPage = mediaLibraryRepository.findByOrgAndTypeAndCategory(orgId, mediaType, categoryId, pageable);
				}
			}
			else {
				librariesPage = mediaLibraryRepository.findByOrganizationIdAndFileTypeAndArchived(orgId, mediaType, archived, pageable);
				if(status.equals("all")){
					librariesPage = mediaLibraryRepository.findByOrganizationIdAndFileType(orgId, mediaType, pageable);
				}
			}

			List<LibraryDto> responseData = new ArrayList<>();
			for (MediaLibrary library : librariesPage.getContent()) {
				List<String> categoryNames = mediaCategoryMapRepository.findCategoryNamesByLibraryId(library.getId());
				List<String> hashtags = mediaHashtagsRepository.findHashtagsByLibraryId(library.getId());
				Optional<MediaFile> mediaFile = resolveLibraryMediaFile(traceId, orgId, library);

				responseData.add(buildLibraryDto(library, mediaFile, categoryNames, hashtags));

				log.debug("[{}]|LIBRARY|FETCHED|LibraryId={} with {} categories & {} hashtags", traceId, library.getId(), categoryNames.size(),
						hashtags.size());
			}

			log.info("[{}]|LIBRARY|SUCCESS|Fetched {} {} libraries (page {}/{})", traceId, responseData.size(), mediaType, page + 1,
					librariesPage.getTotalPages());

			LibraryResponse successResponse = new LibraryResponse();
			successResponse.setStatus(true);
			successResponse.setCode(ResponseCode.USMG_200);
			successResponse.setMessage(mediaType + " library (" + status.toUpperCase() + ") fetched successfully");
			successResponse.setTimestamp(timestamp);
			successResponse.setMediaLibrary(responseData);
			successResponse.setPage(page);
			successResponse.setSize(size);
			successResponse.setTotalElements(librariesPage.getTotalElements());
			successResponse.setTotalPages(librariesPage.getTotalPages());

			return successResponse;

		}
		catch (Exception e) {
			log.error("[{}]|LIBRARY|ERROR|{}", traceId, e.getMessage(), e);
			return buildErrorResponse(traceId, ResponseCode.USMG_500, "Internal server error", e.getMessage(), timestamp, null);
		}
	}

	@Override
	@Transactional
	public BaseResponse updateMediaLibrary(String traceId, String tenantId, UserDetailsImpl userDetails, UpdateMediaLibraryRequest updateRequest) {
		Date timestamp = new Date();
		log.info("[{}]|LIBRARY|INIT|Start processing media library update", traceId);

		if (updateRequest == null || userDetails == null || updateRequest.getLibraryId() == null) {
			return buildErrorResponse(traceId, ResponseCode.USMG_400, "Invalid request", "/media-library/update", timestamp, null);
		}

		try {
			Long userId = userDetails.getUserId();
			Long orgId = userDetails.getOrganization().getId();

			if (tenantId == null || !tenantId.equals(String.valueOf(orgId))) {
				return buildErrorResponse(traceId, ResponseCode.USMG_401, "Tenant/Organization mismatch", "/media-library/update", timestamp, null);
			}

			MediaLibrary library = mediaLibraryRepository.findById(updateRequest.getLibraryId()).orElse(null);
			if (library == null || !orgId.equals(library.getOrganizationId())) {
				log.warn("[{}]|LIBRARY|NOT_FOUND|Library ID: {}", traceId, updateRequest.getLibraryId());
				return buildErrorResponse(traceId, ResponseCode.USMG_404, "Media library not found", "/media-library/update", timestamp, null);
			}

			library.setUpdatedAt(LocalDateTime.now());
			library.setUpdatedBy(userId);
			mediaLibraryRepository.save(library);

			Long libraryId = library.getId();

			if (updateRequest.getCategoryId() != null) {
				MediaCategoryMap category = mediaCategoryMapRepository.findByLibraryId(libraryId);
				if (category != null) {
					category.setCategoryId(updateRequest.getCategoryId());
					mediaCategoryMapRepository.save(category);
				}
				else {
					category = new MediaCategoryMap();
					category.setLibraryId(libraryId);
					category.setCategoryId(updateRequest.getCategoryId());
					mediaCategoryMapRepository.save(category);
				}
			}

			if (updateRequest.getHashTags() != null) {
				for (String tag : updateRequest.getHashTags()) {
					if (tag != null && !tag.trim().isEmpty()) {
						String trimmedTag = tag.trim();

						MediaHashtag existing = mediaHashtagsRepository.findByLibraryIdAndHashtag(libraryId, trimmedTag);

						if (existing != null) {

							existing.setUpdatedAt(LocalDateTime.now());
							mediaHashtagsRepository.save(existing);
						}
						else {

							MediaHashtag newHashtag = new MediaHashtag();
							newHashtag.setLibraryId(libraryId);
							newHashtag.setHashtag(trimmedTag);
							mediaHashtagsRepository.save(newHashtag);
						}
					}
				}
			}

			log.info("[{}]|LIBRARY|UPDATED|Library ID: {}", traceId, libraryId);

			LibraryResponse successResponse = new LibraryResponse();
			successResponse.setStatus(true);
			successResponse.setCode("USMG_200");
			successResponse.setMessage("Media library updated successfully");
			successResponse.setTimestamp(timestamp);

			return successResponse;

		}
		catch (Exception ex) {
			log.error("[{}]|LIBRARY|EXCEPTION|Unhandled error during media library update: {}", traceId, ex.getMessage(), ex);
			return buildErrorResponse(traceId, ResponseCode.USMG_500, "Unhandled error during media library update", "/media-library/update",
					timestamp, ex.getMessage());
		}
	}

	@Override
	@Transactional
	public BaseResponse deleteMediaLibrary(String traceId, String tenantId, UserDetailsImpl userDetails, Long libraryId) {
		Date timestamp = new Date();
		log.info("[{}]|LIBRARY|DELETE_INIT|Deleting library ID: {}", traceId, libraryId);

		if (libraryId == null || userDetails == null) {
			return buildErrorResponse(traceId, "USMG_400", "Invalid request", "/media-library/delete", timestamp,
					"Library ID or user details missing");
		}

		Long orgId = userDetails.getOrganization().getId();
		if (tenantId == null || !tenantId.equals(String.valueOf(orgId))) {
			return buildErrorResponse(traceId, "USMG_401", "Tenant/Organization mismatch", "/media-library/delete", timestamp,
					"Tenant ID does not match organization ID");
		}

		MediaLibrary library = mediaLibraryRepository.findById(libraryId).orElse(null);
		if (library == null || !orgId.equals(library.getOrganizationId())) {
			log.warn("[{}]|LIBRARY|NOT_FOUND|Library ID: {}", traceId, libraryId);
			return buildErrorResponse(traceId, "USMG_404", "Media library not found", "/media-library/delete", timestamp,
					"No library found for given ID and tenant");
		}

		try {

			mediaCategoryMapRepository.deleteByLibraryId(libraryId);
			log.info("[{}]|LIBRARY|CATEGORIES_DELETED|Library ID: {}", traceId, libraryId);

			mediaHashtagsRepository.deleteByLibraryId(libraryId);
			log.info("[{}]|LIBRARY|HASHTAGS_DELETED|Library ID: {}", traceId, libraryId);

			mediaLibraryRepository.deleteById(libraryId);
			log.info("[{}]|LIBRARY|DELETED|Library ID: {}", traceId, libraryId);

			BaseResponse response = new BaseResponse();
			response.setStatus(true);
			response.setCode("USMG_200");
			response.setMessage("Media library deleted successfully");
			response.setTimestamp(timestamp);
			response.setDevMessage(null);
			return response;

		}
		catch (Exception ex) {
			log.error("[{}]|LIBRARY|DELETE_FAILED|Error deleting library ID {}: {}", traceId, libraryId, ex.getMessage(), ex);
			return buildErrorResponse(traceId, "USMG_500", "Error deleting media library", "/media-library/delete", timestamp, ex.getMessage());
		}
	}

	public static String cleanUnicode(String input) {
		if (input == null) return null;
		// Remove or replace problematic characters
		return input.replaceAll("[^\\x00-\\x7F]", "");
		// Or keep only basic printable ASCII
	}

	private Optional<MediaFile> resolveLibraryMediaFile(String traceId, Long orgId, MediaLibrary library) {
		return mediaFileRepository.findByOrganizationIdAndFileUrl(orgId, library.getUrl())
				.map(mediaFile -> refreshVideoMediaFileIfNeeded(traceId, mediaFile));
	}

	private MediaFile refreshVideoMediaFileIfNeeded(String traceId, MediaFile mediaFile) {
		if (!Objects.equals("video", mediaFile.getFileType())) {
			return mediaFile;
		}

		boolean needsRefresh = !hasValue(mediaFile.getThumbnailUrl())
				|| !hasValue(mediaFile.getPlaybackUrl())
				|| (Objects.equals("BUNNY_STREAM", mediaFile.getStorageProvider())
						&& !Objects.equals("READY", mediaFile.getProcessingStatus()));
		if (!needsRefresh) {
			return mediaFile;
		}

		Optional<StoredMediaResult> refreshedState;
		if (Objects.equals("BUNNY_STREAM", mediaFile.getStorageProvider())) {
			refreshedState = managedMediaStorageService.fetchBunnyVideoStatus(traceId, mediaFile);
		}
		else if (Objects.equals("LOCAL", mediaFile.getStorageProvider())) {
			refreshedState = managedMediaStorageService.fetchLocalVideoMetadata(traceId, mediaFile);
		}
		else {
			return mediaFile;
		}
		if (refreshedState.isEmpty()) {
			return mediaFile;
		}

		StoredMediaResult refreshed = refreshedState.get();
		boolean dirty = false;

		if (!Objects.equals(mediaFile.getProcessingStatus(), refreshed.getProcessingStatus())) {
			mediaFile.setProcessingStatus(refreshed.getProcessingStatus());
			dirty = true;
		}
		if (hasChanged(mediaFile.getPlaybackUrl(), refreshed.getPlaybackUrl())) {
			mediaFile.setPlaybackUrl(refreshed.getPlaybackUrl());
			dirty = true;
		}
		if (hasChanged(mediaFile.getThumbnailUrl(), refreshed.getThumbnailUrl())) {
			mediaFile.setThumbnailUrl(refreshed.getThumbnailUrl());
			dirty = true;
		}
		if (hasChanged(mediaFile.getProviderAssetId(), refreshed.getProviderAssetId())) {
			mediaFile.setProviderAssetId(refreshed.getProviderAssetId());
			dirty = true;
		}
		if (hasChanged(mediaFile.getFileUrl(), refreshed.getFileUrl())) {
			mediaFile.setFileUrl(refreshed.getFileUrl());
			dirty = true;
		}

		if (!dirty) {
			return mediaFile;
		}

		log.info("[{}]|LIBRARY|VIDEO_METADATA_UPDATED|mediaId={} provider={} status={}", traceId, mediaFile.getId(), mediaFile.getStorageProvider(),
				mediaFile.getProcessingStatus());
		return mediaFileRepository.save(mediaFile);
	}

	private LibraryDto buildLibraryDto(MediaLibrary library, Optional<MediaFile> mediaFile, List<String> categoryNames, List<String> hashtags) {
		return new LibraryDto(library.getId(), library.getTitle(), library.getFileType(), library.getUrl(),
				mediaFile.map(MediaFile::getStorageProvider).orElse(null),
				mediaFile.map(MediaFile::getPlaybackUrl).orElse(library.getUrl()),
				mediaFile.map(MediaFile::getThumbnailUrl).orElse(null),
				mediaFile.map(MediaFile::getProcessingStatus).orElse(null),
				mediaFile.map(MediaFile::getProviderAssetId).orElse(null),
				categoryNames, hashtags, library.getCreatedBy(), library.getCreatedAt(), library.getArchived());
	}

	private boolean hasChanged(String currentValue, String newValue) {
		return newValue != null && !Objects.equals(currentValue, newValue);
	}

	private boolean hasValue(String value) {
		return value != null && !value.isBlank();
	}
}
