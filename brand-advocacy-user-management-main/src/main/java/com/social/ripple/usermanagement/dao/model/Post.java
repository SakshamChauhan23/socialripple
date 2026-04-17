/**
 * Filename: Post.java
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
package com.social.ripple.usermanagement.dao.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "posts")
@Getter
@Setter
public class Post {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "organization_id")
	private Long organizationId;

	@Column(name = "created_by")
	private Long createdBy;

	@Column(name = "title")
	private String title;

	@Column(name = "content", columnDefinition = "TEXT")
	private String content;

	@Column(name = "x_generated_content", columnDefinition = "TEXT")
	private String xGeneratedContent;

	@Column(name = "x_hashtags", columnDefinition = "TEXT")
	private String xHashtags;

	@Column(name = "type")
	private String type;

	@Column(name = "status")
	private String status;

	@Column(name = "source_type")
	private String sourceType;

	@Column(name = "scheduled_at")
	private LocalDateTime scheduledAt;

	@Column(name = "created_at")
	private LocalDateTime createdAt = LocalDateTime.now();

	@Column(name = "platform_unique_id", length = 255)
	private String platformUniqueId;

	@Column(name = "platform_created_at", nullable = true)
	private LocalDateTime platformCreatedAt;

	@Column(name = "updated_at", nullable = true)
	private LocalDateTime updatedAt;

	@Column(name = "source_name")
	private String sourceName;

	@Column(name = "source_username")
	private String sourceUsername;

	@Column(name = "source_avatar_url", length = 2000)
	private String sourceAvatarUrl;

	@Column(name = "is_editable", nullable = false)
	private Boolean isEditable = true;

	@Column(name = "is_featured", nullable = false)
	private Boolean isFeatured = false;

	@Column(name = "featured_until")
	private LocalDateTime featuredUntil;

	@Column(name = "featured_by")
	private Long featuredBy;

	@Column(name = "category_id")
	private Long categoryId;
}
