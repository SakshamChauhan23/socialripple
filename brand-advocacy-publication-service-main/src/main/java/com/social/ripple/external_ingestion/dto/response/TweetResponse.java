package com.social.ripple.external_ingestion.dto.response;


import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TweetResponse extends  BaseResponse{
    private String rawResponse;
    private List<SchedulePostDTO> schedulePostList;
}

