/**
 * Filename: GenerateContentServiceImpl.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all
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
package com.social.ripple.usermanagement.service.implementation;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.social.ripple.usermanagement.dto.response.ContentGenerateResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.social.ripple.usermanagement.dao.model.Post;
import com.social.ripple.usermanagement.dao.repository.PostRepository;
import com.social.ripple.usermanagement.dto.request.GenerateContentRequestDTO;
import com.social.ripple.usermanagement.dto.response.ErrorObj;
import com.social.ripple.usermanagement.dto.response.GenerateContentResponse;
import com.social.ripple.usermanagement.dto.response.GenerateContentResponseDTO;
import com.social.ripple.usermanagement.service.GeminiClient;
import com.social.ripple.usermanagement.service.IGenerateContentService;
import com.social.ripple.usermanagement.util.constants.ResponseCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GenerateContentServiceImpl implements IGenerateContentService {

    private static final String PROMPT_PREFIX = "Prepare a social media posting summary for the following: ";

    private final GeminiClient geminiClient;
    private final PostRepository postRepository;

    @Autowired
    public GenerateContentServiceImpl(GeminiClient geminiClient, PostRepository postRepository) {
        this.geminiClient = geminiClient;
        this.postRepository = postRepository;
    }

    @Override
    public GenerateContentResponse generateSummary(String traceId, String tenantId, GenerateContentRequestDTO requestDTO) {
        log.info("[{}]|GENERATE_CONTENT|Service|Start summary generation for postId: {}", traceId, requestDTO.getPostId());

        GenerateContentResponse response = new GenerateContentResponse();
        response.setTimestamp(new Date());

        try {
            // 1. Validation
            if (requestDTO.getPostId() == null) {
                log.warn("[{}]|GENERATE_CONTENT|Service|Invalid request: postId is null", traceId);

                response.setStatus(false);
                response.setCode(ResponseCode.USMG_400);
                response.setMessage("Invalid request");
                response.setDevMessage("Post ID cannot be null");
                response.setErrors(List.of(new ErrorObj("/generate-content", ResponseCode.USMG_400, "Validation Error", "Post ID cannot be null")));
                return response;
            }

            // 2. Fetch post
            Optional<Post> optionalPost = postRepository.findById(requestDTO.getPostId());
            if (optionalPost.isEmpty()) {
                log.warn("[{}]|GENERATE_CONTENT|Service|Post not found: {}", traceId, requestDTO.getPostId());

                response.setStatus(false);
                response.setCode(ResponseCode.USMG_404);
                response.setMessage("Post not found");
                response.setDevMessage("No post found for ID " + requestDTO.getPostId());
                response.setErrors(
                        List.of(new ErrorObj("/generate-content", ResponseCode.USMG_404, "Not Found", "Post with given ID does not exist")));
                return response;
            }

            Post post = optionalPost.get();

            // 3. Check content
            if (post.getContent() == null || post.getContent().isBlank()) {
                log.warn("[{}]|GENERATE_CONTENT|Service|Post content is empty for postId {}", traceId, post.getId());

                response.setStatus(false);
                response.setCode(ResponseCode.USMG_404);
                response.setMessage("Post content is empty");
                response.setDevMessage("No content available for post ID " + post.getId());
                response.setErrors(List.of(new ErrorObj("/generate-content", ResponseCode.USMG_404, "No Content", "Post content is empty")));
                return response;
            }

            // 4. Prepare prompt (using constant prefix)
//            String prompt = PROMPT_PREFIX + post.getContent();

//            String prompt = String.format("Act as a professional LinkedIn content creator. Generate a concise, text-only share summary about %s.\n" +
//                    "\n Considering the comment: %s \n" +
//                    "Tone: Professional yet conversational and approachable.\n" +
//                    "Formatting: Use line breaks to improve readability. Do not use emojis. Do not use hashtags. Make it under 150 letters.",post.getContent(),requestDTO.getUserText());

            String prompt = String.format("Role: You are a LinkedIn Thought Leader and Content Curator. You excel at synthesizing complex information and resharing it with added value for your network.\n" +
                    "\n" +
                    "Task: Create a \"Reshare\" LinkedIn post based on the [ORIGINAL TEXT] provided below.\n" +
                    "\n" +
                    "Inputs:\n" +
                    "\n" +
//                    "Original Author: [INSERT AUTHOR NAME]\n" +
                    "\n" +
                    "Original Text/Content: %s \n" +
                    "\n" +
                    "My Perspective (Optional): %s \n" +
                    "\n" +
                    "Instructions:\n" +
                    "\n" +
                    "The Intro (The Setup): Start by praising the original insight or stating why this caught your eye. Use a phrase like \"I just read a powerful perspective from...\" or \"This is the best explanation of [Topic] I've seen.\"\n" +
                    "\n" +
                    "The Summary (The Value): Condense the original text into 3-4 punchy bullet points. What are the \"Must-Know\" takeaways?\n" +
                    "\n" +
                    "The Insight (The \"You\" Factor): Add a distinct paragraph explaining why this matters right now or how it applies to your industry. (Use the \"My Perspective\" input here if provided).\n" +
                    "\n" +
//                    "The Credit: Ensure you clearly credit the original author. Use a placeholder like \"@[Author Name]\" so I can tag them later.\n" +
                    "\n" +
                    "The Formatting: Keep sentences short and punchy.\n" +
                    "\n" +
                    "The CTA: End with a question that bridges the original content to the reader's experience.\n" +
                    "\n" +
                    "Constraint: Do not simply copy the text. Rewrite the summary in your own voice. Do not use emojis. Use only UTF-8 characters. Do not include the introduction. Do not use bold and italic." +
                    "Prepare separate content for common and X. X content should be max " + resolveXMaxCharacters(requestDTO) + " characters long (the remaining characters are reserved for a business page handle/link and hashtags that will be appended automatically). Response needed in two fields as \"common\" and \"xOnly\"", post.getContent(),requestDTO.getUserText());


            log.info("[{}]|GENERATE_CONTENT|Service|Prepared prompt for Gemini: {}", traceId, prompt);

            // 5. Call Gemini AI
            ContentGenerateResponseDTO contentGenerateResponseDTO = geminiClient.generateText(traceId, prompt);

            // 6. Build DTO
            GenerateContentResponseDTO dto = new GenerateContentResponseDTO(post.getId(), post.getContent(), contentGenerateResponseDTO.getGeneratedText(),contentGenerateResponseDTO.getXGeneratedContent());

            // 7. Build success response
            response.setStatus(true);
            response.setCode(ResponseCode.USMG_200);
            response.setMessage("Summary generated successfully");
            response.setDevMessage("Summary successfully generated using Gemini AI");
            response.setGenerateContentResponseDTO(dto);

            log.info("[{}]|GENERATE_CONTENT|Service|Summary generated successfully for postId {}", traceId, post.getId());

        }
        catch (Exception e) {
            log.error("[{}]|GENERATE_CONTENT|Service|Exception: {}", traceId, e.getMessage(), e);

            response.setStatus(false);
            response.setCode(ResponseCode.USMG_500);
            response.setMessage("Failed to generate summary");
            response.setDevMessage(e.getMessage());
            response.setErrors(List.of(new ErrorObj("/generate-content", ResponseCode.USMG_500, "Internal Server Error",
                    "Unexpected error occurred while generating summary")));
        }

        return response;
    }

    private int resolveXMaxCharacters(GenerateContentRequestDTO requestDTO) {
        if (requestDTO.getXMaxCharacters() != null && requestDTO.getXMaxCharacters() > 0 && requestDTO.getXMaxCharacters() <= 280) {
            return requestDTO.getXMaxCharacters();
        }
        return 200; // safe default if frontend doesn't send it
    }
}
