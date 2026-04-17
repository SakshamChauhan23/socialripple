//package com.social.ripple.usermanagement.service.implementation;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import org.springframework.stereotype.Service;
//
//import com.social.ripple.usermanagement.dao.model.Platform;
//import com.social.ripple.usermanagement.dto.PlatformShareResultDto;
//import com.social.ripple.usermanagement.service.IExternalShareHelper;
//
//@Service
//public class ExternalShareHelperImpl implements IExternalShareHelper {
//
//	@Override
//	public List<PlatformShareResultDto> shareToPlatform(Platform platform, Long postId, Long userId, String traceId) {
//		List<PlatformShareResultDto> results = new ArrayList<>();
//		switch (platform) {
//		case INSTAGRAM:
//			results.add(new PlatformShareResultDto());
//			break;
//		case FACEBOOK:
//			results.add(new PlatformShareResultDto());
//			break;
//		case LINKEDIN:
//			results.add(new PlatformShareResultDto());
//			break;
//		case X:
//			results.add(new PlatformShareResultDto());
//			break;
//		default:
//			results.add(new PlatformShareResultDto());
//		}
//		return results;
//	}
//}
