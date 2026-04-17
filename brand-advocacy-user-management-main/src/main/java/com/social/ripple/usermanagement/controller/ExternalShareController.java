//package com.social.ripple.usermanagement.controller;
//
//import java.util.Objects;
//
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestHeader;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import com.social.ripple.usermanagement.dto.request.ExternalShareRequest;
//import com.social.ripple.usermanagement.dto.response.ExternalShareResponse;
//import com.social.ripple.usermanagement.security.service.UserDetailsImpl;
//import com.social.ripple.usermanagement.service.IExternalShareService;
//import com.social.ripple.usermanagement.util.constants.ResponseCode;
//
//import lombok.extern.slf4j.Slf4j;
//
//@RestController
//@RequestMapping("{version}/external-share")
//@Slf4j
//public class ExternalShareController {
//
//    private final IExternalShareService externalShareService;
//
//    public ExternalShareController(IExternalShareService externalShareService) {
//        this.externalShareService = externalShareService;
//    }
//
//    @PostMapping("/{postId}")
//    public ResponseEntity<ExternalShareResponse> sharePost(
//            @PathVariable("version") String version,
//            @RequestHeader("x-trace-id") String traceId,
//            @RequestHeader("x-tenant-id") String tenantId,
//            @RequestHeader(name = "x-correlation-id", required = false) String correlationId,
//            @RequestHeader(name = "language-id", required = false) String languageId,
//            @RequestHeader(name = "authorization") String authorization,
//            @AuthenticationPrincipal UserDetailsImpl userDetails,
//            @RequestBody ExternalShareRequest request) {
//
//        log.info("[{}]|EXTERNAL_SHARE|REQUEST_RECEIVED|PostId={} Platforms={}",
//                traceId, request.getPostId(), request.getPlatforms());
//
//        // Validate user
//        if (!validateUser(tenantId, userDetails, traceId)) {
//            ExternalShareResponse errorResponse = new ExternalShareResponse();
//            errorResponse.setStatus(false);
//            errorResponse.setCode(ResponseCode.USMG_401);
//            errorResponse.setMessage("Unauthorized: User's organization does not match tenant ID");
//            errorResponse.setResults(null);
//            return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
//        }
//
//        try {
//            ExternalShareResponse serviceResponse = externalShareService.sharePostExternally(
//                    userDetails.getUserId(), request, traceId);
//
//            // Ensure success code
//            if (Boolean.TRUE.equals(serviceResponse.isStatus())) {
//                serviceResponse.setCode(ResponseCode.USMG_200);
//            }
//
//            return new ResponseEntity<>(serviceResponse, resolveHttpStatus(serviceResponse.getCode()));
//
//        } catch (Exception e) {
//            log.error("[{}]|EXTERNAL_SHARE|EXCEPTION|{}", traceId, e.getMessage(), e);
//
//            ExternalShareResponse errorResponse = new ExternalShareResponse();
//            errorResponse.setStatus(false);
//            errorResponse.setCode(ResponseCode.USMG_500);
//            errorResponse.setMessage("Internal server error: " + e.getMessage());
//            errorResponse.setResults(null);
//
//            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
//
//    private boolean validateUser(String tenantId, UserDetailsImpl userDetails, String traceId) {
//        if (userDetails == null || userDetails.getOrganization() == null) {
//            log.warn("[{}]|AUTH|USER_DETAILS_NULL", traceId);
//            return false;
//        }
//        String userOrgId = String.valueOf(userDetails.getOrganization().getId());
//        if (!Objects.equals(tenantId, userOrgId)) {
//            log.warn("[{}]|AUTH|TENANT_MISMATCH|TokenOrg={}, HeaderOrg={}", traceId, userOrgId, tenantId);
//            return false;
//        }
//        return true;
//    }
//
//    private HttpStatus resolveHttpStatus(String code) {
//        return switch (code) {
//            case ResponseCode.USMG_400 -> HttpStatus.BAD_REQUEST;
//            case ResponseCode.USMG_401 -> HttpStatus.UNAUTHORIZED;
//            case ResponseCode.USMG_403 -> HttpStatus.FORBIDDEN;
//            case ResponseCode.USMG_404 -> HttpStatus.NOT_FOUND;
//            case ResponseCode.USMG_409 -> HttpStatus.CONFLICT;
//            case ResponseCode.USMG_413 -> HttpStatus.PAYLOAD_TOO_LARGE;
//            case ResponseCode.USMG_422 -> HttpStatus.UNPROCESSABLE_ENTITY;
//            case ResponseCode.USMG_423 -> HttpStatus.LOCKED;
//            case ResponseCode.USMG_429 -> HttpStatus.TOO_MANY_REQUESTS;
//            case ResponseCode.USMG_500 -> HttpStatus.INTERNAL_SERVER_ERROR;
//            case ResponseCode.USMG_503 -> HttpStatus.SERVICE_UNAVAILABLE;
//            case ResponseCode.USMG_504 -> HttpStatus.GATEWAY_TIMEOUT;
//            default -> HttpStatus.OK;
//        };
//    }
//}
