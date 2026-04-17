package com.social.ripple.external_ingestion.dto.response;


import lombok.*;

import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class ImpressionsResponse extends  BaseResponse{
    private Integer totalImpressions;
    private Integer totalEngagements;
    private Integer totalReach;
    private Double clickThroughRate;
    private Map<String,Integer> platformImpression;
    private Map<String,Integer> platformEngagement;
    private Map<String,Integer> platformReach;
    private Map<String,Double> platformClickThroughRate;
    private Map<String,Integer> platformClickCount;

    // Engagement breakdowns
    private Integer totalLikes;
    private Integer totalComments;
    private Integer totalShares;
    private Integer totalSaves;
    private Integer totalBookmarks;
    private Integer totalVideoViews;
    private Map<String,Integer> platformLikes;
    private Map<String,Integer> platformComments;
    private Map<String,Integer> platformShares;
    private Map<String,Integer> platformSaves;
    private Map<String,Integer> platformBookmarks;
    private Map<String,Integer> platformRetweets;
    private Map<String,Integer> platformReplies;
    private Map<String,Integer> platformQuotes;
    private Map<String,Integer> platformVideoViews;
}

