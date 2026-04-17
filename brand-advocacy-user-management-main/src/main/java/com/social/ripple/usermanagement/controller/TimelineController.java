package com.social.ripple.usermanagement.controller;

import com.social.ripple.usermanagement.dao.model.Post;
import com.social.ripple.usermanagement.dao.repository.PostRepository;
import com.social.ripple.usermanagement.dto.response.BaseResponse;
import com.social.ripple.usermanagement.security.service.UserDetailsImpl;
import com.social.ripple.usermanagement.service.ITimelineService;
import com.social.ripple.usermanagement.util.constants.ResponseCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/{version}/timeline")
@RequiredArgsConstructor
public class TimelineController {

	private final ITimelineService timelineService;
	private final PostRepository postRepository;

	@GetMapping("/posts")
	@PreAuthorize("hasRole('ROLE_EMPLOYEE') or hasRole('ROLE_ADMIN')")
	public ResponseEntity<BaseResponse> getTimelinePosts(@PathVariable("version") String version,
			@RequestHeader(value = "x-trace-id", required = true) String traceId,
			@RequestHeader(value = "x-tenant-id", required = true) Long tenantId,
			@AuthenticationPrincipal UserDetailsImpl userDetails,
			@RequestParam(value = "postId", required = false) Long postId,
			@RequestParam(value = "categoryId", required = false) Long categoryId,
			@RequestParam(value = "platform", required = false) String platform,
			@RequestParam(value = "leader", required = false, defaultValue = "false") boolean leader,
			@RequestParam(value = "searchKey", required = false) String searchKey,
			@RequestParam(value = "userId", required = false) Long userId,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "10") int size) {

		log.info("[{}]|TIMELINE|GET|Fetching timeline posts for tenantId: {}", traceId, tenantId);

		try {

			// TODO remove after UI fix
			size = size<50?50:size;

			Pageable pageable = PageRequest.of(page, size);

			BaseResponse response = timelineService.getTimelinePosts(traceId, postId, tenantId, categoryId, platform,
					leader, searchKey, userId, pageable, userDetails);

			response.setTimestamp(new Date());

			if (ResponseCode.USMG_401.equals(response.getCode())) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
			}
			if (ResponseCode.USMG_404.equals(response.getCode())) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
			}

			return ResponseEntity.ok(response);

		} catch (Exception e) {
			log.error("[{}]|TIMELINE|GET_FAILED|Exception: {}", traceId, e.getMessage(), e);

			BaseResponse response = new BaseResponse();
			response.setStatus(false);
			response.setCode(ResponseCode.USMG_500);
			response.setMessage("Internal Server Error");
			response.setDevMessage(e.getMessage());
			response.setTimestamp(new Date());

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@GetMapping("/platforms/posts/{postId}")
	@PreAuthorize("hasRole('ROLE_EMPLOYEE') or hasRole('ROLE_ADMIN')")
	public ResponseEntity<BaseResponse> getPostForPlatformShare(@PathVariable("version") String version,
														 @RequestHeader(value = "x-trace-id", required = true) String traceId,
														 @RequestHeader(value = "x-tenant-id", required = true) Long tenantId,
														 @AuthenticationPrincipal UserDetailsImpl userDetails,
														 @PathVariable(value = "postId", required = false) Long postId,
														 @RequestParam(value = "isShare", required = false) Boolean isShare
	) {

		log.info("[{}]|TIMELINE|GET|Fetching timeline posts for tenantId: {}", traceId, tenantId);

		try {

			Pageable pageable = PageRequest.of(0, 1);

			BaseResponse response = timelineService.getPostForPlatformShare(traceId, postId, tenantId,  userDetails, isShare);

			response.setTimestamp(new Date());

			if (ResponseCode.USMG_401.equals(response.getCode())) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
			}
			if (ResponseCode.USMG_404.equals(response.getCode())) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
			}

			return ResponseEntity.ok(response);

		} catch (Exception e) {
			log.error("[{}]|TIMELINE|GET_FAILED|Exception: {}", traceId, e.getMessage(), e);

			BaseResponse response = new BaseResponse();
			response.setStatus(false);
			response.setCode(ResponseCode.USMG_500);
			response.setMessage("Internal Server Error");
			response.setDevMessage(e.getMessage());
			response.setTimestamp(new Date());

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	// Feature 7/8/9: Get active featured posts
	@GetMapping("/featured")
	@PreAuthorize("hasRole('ROLE_EMPLOYEE') or hasRole('ROLE_ADMIN')")
	public ResponseEntity<Map<String, Object>> getFeaturedPosts(@PathVariable("version") String version,
			@RequestHeader(value = "x-trace-id", required = true) String traceId,
			@RequestHeader(value = "x-tenant-id", required = true) Long tenantId,
			@AuthenticationPrincipal UserDetailsImpl userDetails) {
		try {
			java.util.List<Post> featured = postRepository.findActiveFeaturedPosts(tenantId);
			Map<String, Object> response = new HashMap<>();
			response.put("status", true);
			response.put("code", ResponseCode.USMG_200);
			response.put("message", "Featured posts fetched.");
			response.put("posts", featured);
			response.put("timestamp", new Date());
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			log.error("[{}]|TIMELINE|FEATURED|Exception: {}", traceId, e.getMessage(), e);
			Map<String, Object> response = new HashMap<>();
			response.put("status", false);
			response.put("code", ResponseCode.USMG_500);
			response.put("message", e.getMessage());
			response.put("timestamp", new Date());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	// Feature 7/9: Admin feature/unfeature a post with optional expiry
	@PutMapping("/admin/posts/{postId}/feature")
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public ResponseEntity<BaseResponse> featurePost(@PathVariable("version") String version,
			@RequestHeader(value = "x-trace-id", required = true) String traceId,
			@RequestHeader(value = "x-tenant-id", required = true) Long tenantId,
			@AuthenticationPrincipal UserDetailsImpl userDetails,
			@PathVariable("postId") Long postId,
			@RequestBody Map<String, Object> body) {
		BaseResponse response = new BaseResponse();
		try {
			Post post = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post not found"));
			boolean feature = Boolean.TRUE.equals(body.get("feature"));
			post.setIsFeatured(feature);
			post.setFeaturedBy(feature ? userDetails.getUserId() : null);
			if (feature && body.get("featuredUntil") != null) {
				post.setFeaturedUntil(LocalDateTime.parse(body.get("featuredUntil").toString()));
			} else if (!feature) {
				post.setFeaturedUntil(null);
			}
			postRepository.save(post);
			response.setStatus(true);
			response.setCode(ResponseCode.USMG_200);
			response.setMessage(feature ? "Post featured successfully." : "Post unfeatured.");
			response.setTimestamp(new Date());
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			log.error("[{}]|TIMELINE|FEATURE_POST|Exception: {}", traceId, e.getMessage(), e);
			response.setStatus(false);
			response.setCode(ResponseCode.USMG_500);
			response.setMessage(e.getMessage());
			response.setTimestamp(new Date());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	// Feature 4: Admin toggle editable flag on a post
	@PutMapping("/admin/posts/{postId}/editable")
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public ResponseEntity<BaseResponse> setPostEditable(@PathVariable("version") String version,
			@RequestHeader(value = "x-trace-id", required = true) String traceId,
			@RequestHeader(value = "x-tenant-id", required = true) Long tenantId,
			@AuthenticationPrincipal UserDetailsImpl userDetails,
			@PathVariable("postId") Long postId,
			@RequestBody Map<String, Object> body) {
		BaseResponse response = new BaseResponse();
		try {
			Post post = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post not found"));
			boolean editable = !Boolean.FALSE.equals(body.get("editable"));
			post.setIsEditable(editable);
			postRepository.save(post);
			response.setStatus(true);
			response.setCode(ResponseCode.USMG_200);
			response.setMessage(editable ? "Post is now editable." : "Post locked to quick-share only.");
			response.setTimestamp(new Date());
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			log.error("[{}]|TIMELINE|SET_EDITABLE|Exception: {}", traceId, e.getMessage(), e);
			response.setStatus(false);
			response.setCode(ResponseCode.USMG_500);
			response.setMessage(e.getMessage());
			response.setTimestamp(new Date());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@GetMapping("/detail/{postId}")
	public ResponseEntity<BaseResponse> getPostDetail(@PathVariable("version") String version,
																@RequestHeader(value = "x-trace-id", required = true) String traceId,
																@RequestHeader(value = "x-tenant-id", required = true) Long tenantId,
																@AuthenticationPrincipal UserDetailsImpl userDetails,
																@PathVariable(value = "postId", required = false) Long postId) {

		log.info("[{}]|TIMELINE|GET|Fetching timeline posts for tenantId: {}", traceId, tenantId);

		try {

			Pageable pageable = PageRequest.of(0, 1);

			BaseResponse response = timelineService.getPostDetailById(traceId, postId, tenantId,  pageable, userDetails);

			response.setTimestamp(new Date());

			if (ResponseCode.USMG_401.equals(response.getCode())) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
			}
			if (ResponseCode.USMG_404.equals(response.getCode())) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
			}

			return ResponseEntity.ok(response);

		} catch (Exception e) {
			log.error("[{}]|TIMELINE|GET_FAILED|Exception: {}", traceId, e.getMessage(), e);

			BaseResponse response = new BaseResponse();
			response.setStatus(false);
			response.setCode(ResponseCode.USMG_500);
			response.setMessage("Internal Server Error");
			response.setDevMessage(e.getMessage());
			response.setTimestamp(new Date());

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

}
