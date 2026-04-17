package com.social.ripple.external_ingestion.dto.request;
import org.springframework.web.multipart.MultipartFile;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TweetRequest {
    private Long postId;
    private String content;
    private String type;
    private MultipartFile file;
    private String accessToken;
    private String accessSecret;
}
