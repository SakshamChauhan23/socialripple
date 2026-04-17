/**
 * Filename: CategoryServiceImpl.java
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
package com.social.ripple.usermanagement.service.implementation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.social.ripple.usermanagement.dao.model.Category;
import com.social.ripple.usermanagement.dao.model.Post;
import com.social.ripple.usermanagement.dao.model.PostCategoryMap;
import com.social.ripple.usermanagement.dao.repository.CategoryRepository;
import com.social.ripple.usermanagement.dao.repository.PostCategoryMapRepository;
import com.social.ripple.usermanagement.dao.repository.PostRepository;
import com.social.ripple.usermanagement.dto.request.AssignCategoryRequest;
import com.social.ripple.usermanagement.dto.request.CategoryRequestDTO;
import com.social.ripple.usermanagement.dto.response.AssignCategoryResponse;
import com.social.ripple.usermanagement.dto.response.CategoryResponse;
import com.social.ripple.usermanagement.dto.response.CategoryResponseDTO;
import com.social.ripple.usermanagement.dto.response.ErrorObj;
import com.social.ripple.usermanagement.security.service.UserDetailsImpl;
import com.social.ripple.usermanagement.service.ICategoryService;
import com.social.ripple.usermanagement.util.AppCommonValidator;
import com.social.ripple.usermanagement.util.constants.ResponseCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CategoryServiceImpl implements ICategoryService {

	private final CategoryRepository categoryRepository;
	private final AppCommonValidator appCommonValidator;
	private final PostRepository postRepository;
	private final PostCategoryMapRepository postCategoryMapRepository;

	@Autowired
	public CategoryServiceImpl(CategoryRepository categoryRepository, AppCommonValidator appCommonValidator,
			PostRepository postRepository, PostCategoryMapRepository postCategoryMapRepository) {
		this.categoryRepository = categoryRepository;
		this.appCommonValidator = appCommonValidator;
		this.postRepository = postRepository;
		this.postCategoryMapRepository = postCategoryMapRepository;
	}

	@Override
	public CategoryResponse createCategory(String traceId, String correlationId, String tenantId, String languageId,
			CategoryRequestDTO request, UserDetailsImpl detailsImpl) {
		CategoryResponse response = new CategoryResponse();
		log.info("[{}]|CATEGORY|Service|createCategory initiated", traceId);

		try {
			if (!Long.valueOf(tenantId).equals(detailsImpl.getOrganization().getId())) {
				return buildErrorResponse(traceId, ResponseCode.USMG_401, "Unauthorized: Admin organization mismatch",
						null, response);
			}

			if (appCommonValidator.isNullOrEmpty(traceId, request.getName(), "name")) {
				return buildErrorResponse(traceId, ResponseCode.USMG_400, "Category name is required", "name",
						response);
			}

			if (appCommonValidator.isLengthInvalid(traceId, request.getName(), "name", 1, 150)
					|| appCommonValidator.hasSpecialCharacters(traceId, request.getName(), "name")) {
				return buildErrorResponse(traceId, ResponseCode.USMG_400, "Invalid category name format", "name",
						response);
			}

			if (categoryRepository.existsByNameAndOrganizationId(request.getName(), Long.valueOf(tenantId))) {
				return buildErrorResponse(traceId, ResponseCode.USMG_400, "Category name already exists", "name",
						response);
			}

			Category category = new Category();
			category.setOrganizationId(Long.valueOf(tenantId));
			category.setName(request.getName());
			category.setCreatedBy(detailsImpl.getUserId());
			category.setCreatedAt(LocalDateTime.now());

			categoryRepository.save(category);

			buildSuccessResponse(response, "Category created successfully");
			log.info("[{}]|CATEGORY|Service|createCategory success", traceId);

		} catch (Exception ex) {
			log.error("[{}]|CATEGORY|Service|createCategory failed: {}", traceId, ex.getMessage(), ex);
			buildInternalError(response, "Something went wrong while creating category");
		}

		return response;
	}

	@Override
	public CategoryResponse updateCategory(String traceId, String correlationId, String tenantId, String languageId,
			Long id, CategoryRequestDTO request, UserDetailsImpl detailsImpl) {
		CategoryResponse response = new CategoryResponse();
		log.info("[{}]|CATEGORY|Service|updateCategory initiated for id: {}", traceId, id);

		try {
			if (!Long.valueOf(tenantId).equals(detailsImpl.getOrganization().getId())) {
				return buildErrorResponse(traceId, ResponseCode.USMG_401, "Unauthorized: Admin organization mismatch",
						null, response);
			}

			if (id == null || id <= 0) {
				return buildErrorResponse(traceId, ResponseCode.USMG_400, "Invalid category ID", "id", response);
			}

			Optional<Category> optionalCategory = categoryRepository.findByIdAndOrganizationId(id,
					Long.valueOf(tenantId));
			if (optionalCategory.isEmpty()) {
				return buildErrorResponse(traceId, ResponseCode.USMG_404, "Category not found", "id", response);
			}

			Category category = optionalCategory.get();
			boolean isChanged = false;

			if (request.getName() != null) {
				if (appCommonValidator.isLengthInvalid(traceId, request.getName(), "name", 1, 150)
						|| appCommonValidator.hasSpecialCharacters(traceId, request.getName(), "name")) {
					return buildErrorResponse(traceId, ResponseCode.USMG_400, "Invalid category name format", "name",
							response);
				}

				if (!request.getName().equals(category.getName())) {
					if (categoryRepository.existsByNameAndOrganizationIdAndIdNot(request.getName(),
							Long.valueOf(tenantId), id)) {
						return buildErrorResponse(traceId, ResponseCode.USMG_400, "Category name already exists",
								"name", response);
					}
					category.setName(request.getName());
					isChanged = true;
				}
			}

			if (!isChanged) {
				return buildErrorResponse(traceId, ResponseCode.USMG_400, "No changes detected in update", null,
						response);
			}

			categoryRepository.save(category);
			buildSuccessResponse(response, "Category updated successfully");
			log.info("[{}]|CATEGORY|Service|updateCategory success", traceId);

		} catch (Exception ex) {
			log.error("[{}]|CATEGORY|Service|updateCategory failed: {}", traceId, ex.getMessage(), ex);
			buildInternalError(response, "Something went wrong while updating category");
		}

		return response;
	}

	@Override
	public CategoryResponse getAllCategories(String traceId, String correlationId, String tenantId, String languageId,
			Long id, UserDetailsImpl detailsImpl) {
		CategoryResponse response = new CategoryResponse();
		log.info("[{}]|CATEGORY|Service|getAllCategories initiated", traceId);

		try {
			if (!Long.valueOf(tenantId).equals(detailsImpl.getOrganization().getId())) {
				return buildErrorResponse(traceId, ResponseCode.USMG_401, "Unauthorized: Admin organization mismatch",
						null, response);
			}

			List<Category> categories;

			if (id != null) { // Get one
				Optional<Category> optionalCategory = categoryRepository.findByIdAndOrganizationId(id,
						Long.valueOf(tenantId));
				if (optionalCategory.isEmpty()) {
					return buildErrorResponse(traceId, ResponseCode.USMG_404, "Category not found", "id", response);
				}
				categories = List.of(optionalCategory.get());
			} else { // Get all
				categories = categoryRepository.findAllByOrganizationId(Long.valueOf(tenantId));
			}

			List<CategoryResponseDTO> data = categories.stream().map(CategoryResponseDTO::new)
					.collect(Collectors.toList());

			response.setCategories(data);
			buildSuccessResponse(response, "Categories fetched successfully");
			log.info("[{}]|CATEGORY|Service|getAllCategories success", traceId);

		} catch (Exception ex) {
			log.error("[{}]|CATEGORY|Service|getAllCategories failed: {}", traceId, ex.getMessage(), ex);
			buildInternalError(response, "Something went wrong while fetching categories");
		}
		return response;
	}

	@Override
	@Transactional
	public CategoryResponse deleteCategory(String traceId, String correlationId, String tenantId, String languageId,
			Long id, UserDetailsImpl detailsImpl) {
		CategoryResponse response = new CategoryResponse();
		log.info("[{}]|CATEGORY|Service|deleteCategory initiated", traceId);

		try {
			if (!Long.valueOf(tenantId).equals(detailsImpl.getOrganization().getId())) {
				return buildErrorResponse(traceId, ResponseCode.USMG_401, "Unauthorized: Admin organization mismatch",
						null, response);
			}

			if (id == null || id <= 0) {
				return buildErrorResponse(traceId, ResponseCode.USMG_400, "Invalid category ID", "id", response);
			}

			Optional<Category> optionalCategory = categoryRepository.findByIdAndOrganizationId(id,
					Long.valueOf(tenantId));
			if (optionalCategory.isEmpty()) {
				return buildErrorResponse(traceId, ResponseCode.USMG_404, "Category not found", "id", response);
			}

			postCategoryMapRepository.deleteByCategoryId(id);
			categoryRepository.delete(optionalCategory.get());
			buildSuccessResponse(response, "Category deleted successfully");
			log.info("[{}]|CATEGORY|Service|deleteCategory success", traceId);

		} catch (Exception ex) {
			log.error("[{}]|CATEGORY|Service|deleteCategory failed: {}", traceId, ex.getMessage(), ex);
			buildInternalError(response, "Something went wrong while deleting category");
		}

		return response;
	}

	@Override
	public AssignCategoryResponse assignCategoryToPost(String traceId, String correlationId, String tenantId,
			String languageId, Long postId, AssignCategoryRequest request, UserDetailsImpl detailsImpl) {

		AssignCategoryResponse response = new AssignCategoryResponse();
		log.info("[{}]|CATEGORY|Service|assignCategoryToPost initiated for postId: {} and categoryIds: {}", traceId,
				postId, request != null ? request.getCategoryIds() : null);

		try {

			if (detailsImpl == null || detailsImpl.getOrganization() == null
					|| detailsImpl.getOrganization().getId() == null) {
				return buildErrorResponse(response, ResponseCode.USMG_401,
						"Unauthorized: User details missing or invalid");
			}
			Long orgId = detailsImpl.getOrganization().getId();

			if (appCommonValidator.isNullOrEmpty(traceId, tenantId, "tenantId")) {
				return buildErrorResponse(response, ResponseCode.USMG_400, "TenantId cannot be null or empty");
			}
			if (!Long.valueOf(tenantId).equals(orgId)) {
				return buildErrorResponse(response, ResponseCode.USMG_401, "Unauthorized: Admin organization mismatch");
			}

			if (postId == null || postId <= 0) {
				log.warn("[{}]|CATEGORY|Validation failed: Invalid postId: {}", traceId, postId);
				return buildErrorResponse(response, ResponseCode.USMG_400, "PostId must be valid");
			}
			Optional<Post> postOpt = postRepository.findByIdAndOrganizationId(postId, orgId);
			if (postOpt.isEmpty()) {
				return buildErrorResponse(response, ResponseCode.USMG_404, "Post not found for this organization");
			}

			if (request == null || request.getCategoryIds() == null || request.getCategoryIds().isEmpty()) {
				return buildErrorResponse(response, ResponseCode.USMG_400, "CategoryIds cannot be null or empty");
			}

			List<Long> assignedCategories = new ArrayList<>();

			for (Long categoryId : request.getCategoryIds()) {
				boolean invalid = (categoryId == null || categoryId <= 0)
						|| categoryRepository.findByIdAndOrganizationId(categoryId, orgId).isEmpty()
						|| postCategoryMapRepository.existsByPostIdAndCategoryId(postId, categoryId);

				if (invalid) {
					continue;
				}

				PostCategoryMap mapping = new PostCategoryMap();
				mapping.setPostId(postId);
				mapping.setCategoryId(categoryId);
				postCategoryMapRepository.save(mapping);

				assignedCategories.add(categoryId);
			}

			response.setStatus(true);
			response.setCode(ResponseCode.USMG_200);
			response.setMessage("Categories assigned to post successfully");
			response.setPostId(postId);
			response.setCategoryIds(assignedCategories);
			response.setTimestamp(new Date());

			log.info("[{}]|CATEGORY|Service|assignCategoryToPost success. Assigned: {}", traceId, assignedCategories);

		} catch (Exception ex) {
			log.error("[{}]|CATEGORY|Service|assignCategoryToPost failed: {}", traceId, ex.getMessage(), ex);
			return buildErrorResponse(response, ResponseCode.USMG_500,
					"Something went wrong while assigning categories to post");
		}

		return response;
	}

	private void buildSuccessResponse(CategoryResponse response, String message) {
		response.setStatus(true);
		response.setCode(ResponseCode.USMG_200);
		response.setMessage(message);
		response.setTimestamp(new Date());
	}

	private CategoryResponse buildErrorResponse(String traceId, String code, String message, String path,
			CategoryResponse response) {
		response.setStatus(false);
		response.setCode(code);
		response.setMessage(message);
		response.setTimestamp(new Date());
		response.setErrors(List.of(new ErrorObj(path, code, "ERROR", message)));
		return response;
	}

	private void buildInternalError(CategoryResponse response, String message) {
		response.setStatus(false);
		response.setCode(ResponseCode.USMG_500);
		response.setMessage(message);
		response.setTimestamp(new Date());
	}

	private AssignCategoryResponse buildErrorResponse(AssignCategoryResponse response, String code, String message) {
		response.setStatus(false);
		response.setCode(code);
		response.setMessage(message);
		response.setTimestamp(new Date());
		return response;
	}
}
