//package com.social.ripple.usermanagement.service.implementation;
//
//import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//
//import org.springframework.stereotype.Service;
//
//import com.social.ripple.usermanagement.dao.model.Platform;
//import com.social.ripple.usermanagement.dto.PlatformShareResultDto;
//import com.social.ripple.usermanagement.dto.request.ExternalShareRequest;
//import com.social.ripple.usermanagement.dto.response.ExternalShareResponse;
//import com.social.ripple.usermanagement.service.IExternalShareHelper;
//import com.social.ripple.usermanagement.service.IExternalShareService;
//import com.social.ripple.usermanagement.util.constants.ResponseCode;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class ExternalShareServiceImpl implements IExternalShareService {
//
//    private final IExternalShareHelper externalShareHelper;
//
//    @Override
//    public ExternalShareResponse sharePostExternally(Long userId, ExternalShareRequest request, String traceId) {
//        List<PlatformShareResultDto> allResults = new ArrayList<>();
//
//        try {
//            Long postId = request.getPostId();
//            Set<Platform> platforms = normalizePlatforms(request.getPlatforms());
//
//            if (platforms.isEmpty()) {
//                return buildErrorResponse("No valid platforms specified", ResponseCode.USMG_400);
//            }
//
//            for (Platform platform : platforms) {
//                allResults.addAll(externalShareHelper.shareToPlatform(platform, postId, userId, traceId));
//            }
//
//            return new ExternalShareResponse();
//
//        } catch (Exception e) {
//            log.error("[{}]|EXTERNAL_SHARE|ERROR|{}", traceId, e.getMessage(), e);
//            return buildErrorResponse("Error while sharing post", ResponseCode.USMG_500);
//        }
//    }
//
//    private Set<Platform> normalizePlatforms(List<String> selectedPlatforms) {
//        Set<Platform> finalPlatforms = new HashSet<>();
//        if (selectedPlatforms == null || selectedPlatforms.isEmpty()) return finalPlatforms;
//
//        for (String pStr : selectedPlatforms) {
//            try {
//                finalPlatforms.add(Platform.valueOf(pStr.toUpperCase()));
//            } catch (IllegalArgumentException ex) {
//                log.warn("Invalid platform: {}", pStr);
//            }
//        }
//
//        if (finalPlatforms.contains(Platform.ALL)) {
//            finalPlatforms.clear();
//            for (Platform p : Platform.values()) {
//                if (p != Platform.ALL) finalPlatforms.add(p);
//            }
//        }
//
//        return finalPlatforms;
//    }
//
//    private ExternalShareResponse buildErrorResponse(String message, String code) {
//        return new ExternalShareResponse();
//    }
//}
