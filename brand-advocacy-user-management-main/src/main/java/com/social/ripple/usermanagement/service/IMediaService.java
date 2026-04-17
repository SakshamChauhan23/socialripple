/**
 * Filename: IMediaService.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual property
 * rights) in this software and any derivative works based upon or derived from this software belongs exclusively to Quasarix. Access to this software
 * is forbidden to anyone except current employees of Quasarix and its affiliated companies who have executed non-disclosure agreements explicitly
 * covering such access. While in the employment of Quasarix or its affiliate companies as the case may be, employees may use this software
 * internally, solely in the course of employment, for the sole purpose of developing new functionalities, features, procedures, routines,
 * customizations or derivative works, or for the purpose of providing maintenance or support for the software. Save as expressly permitted above, no
 * license or right thereto is hereby granted to anyone, either directly, by implication or otherwise. On the termination of employment, the license
 * granted to employee to access the software shall terminate and the software should be returned to the employer, without retaining any copies. This
 * software is (i) proprietary to Quasarix; (ii) is of significant value to it; (iii) contains trade secrets of Quasarix; (iv) is not publicly
 * available; and (v) constitutes the confidential information of Quasarix. Any use, reproduction, modification, distribution, public performance or
 * display of this software or through the use of this software without the prior, express written consent of Quasarix is strictly prohibited and may
 * be in violation of applicable laws.
 */
package com.social.ripple.usermanagement.service;

import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.social.ripple.usermanagement.dto.request.CreatePostRequest;
import com.social.ripple.usermanagement.dto.request.UpdateMediaLibraryRequest;
import com.social.ripple.usermanagement.dto.request.UploadLibraryRequest;
import com.social.ripple.usermanagement.dto.request.ZipUploadRequest;
import com.social.ripple.usermanagement.dto.response.BaseResponse;
import com.social.ripple.usermanagement.security.service.UserDetailsImpl;

public interface IMediaService {

	public BaseResponse uploadMedia(String traceId, UserDetailsImpl userDetails, MultipartFile[] files,
			String mediaType);

	public BaseResponse postdMedia(String traceId, UserDetailsImpl userDetails, CreatePostRequest postRequest);

	public BaseResponse getMedia(String traceId, Long postId, UserDetailsImpl userDetails, Pageable pageable,
			Long tenantId, Long categoryId, String searchText, boolean leader);

	public BaseResponse unzipFile(String traceId, String tenantId, UserDetailsImpl userDetails,
			ZipUploadRequest zipRequest);

	public BaseResponse addMediaToLibrary(String traceId, String tenantId, UserDetailsImpl userDetails,
			UploadLibraryRequest libraryRequest);

	public BaseResponse updateMediaLibrary(String traceId, String tenantId, UserDetailsImpl userDetails,
			UpdateMediaLibraryRequest updateLibraryRequest);

	public BaseResponse deleteMediaLibrary(String traceId, String tenantId, UserDetailsImpl userDetails, Long libraryId);
	 // Employee + Admin → ACTIVE only
    BaseResponse fetchMediaLibrary(String traceId, String tenantId, UserDetailsImpl userDetails, String mediaType, Long categoryId, int page, int size);

    // Admin only → ACTIVE or ARCHIVED
    BaseResponse fetchMediaLibraryAdmin(String traceId, String tenantId, UserDetailsImpl userDetails, String mediaType, String status, Long categoryId, int page, int size);


}
