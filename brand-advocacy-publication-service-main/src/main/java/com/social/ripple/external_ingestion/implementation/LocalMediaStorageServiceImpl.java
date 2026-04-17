///**
// * Filename: LocalMediaStorageServiceImpl.java
// *
// * © Copyright 2024 Quasarix. ALL RIGHTS RESERVED.
//
// * All rights, title and interest (including all intellectual property rights) in this software and any derivative works based upon or derived from
// * this software belongs exclusively to Quasarix.
//
// * Access to this software is forbidden to anyone except current employees of Quasarix and its affiliated companies who have executed non-disclosure
// * agreements explicitly covering such access. While in the employment of Quasarix or its affiliate companies as the case may be, employees may use
// * this software internally, solely in the course of employment, for the sole purpose of developing new functionalities, features, procedures,
// * routines, customizations or derivative works, or for the purpose of providing maintenance or support for the software. Save as expressly permitted
// * above, no license or right thereto is hereby granted to anyone, either directly, by implication or otherwise. On the termination of employment,
// * the license granted to employee to access the software shall terminate and the software should be returned to the employer, without retaining any
// * copies.
//
// * This software is (i) proprietary to Quasarix; (ii) is of significant value to it; (iii) contains trade secrets of Quasarix; (iv) is not publicly
// * available; and (v) constitutes the confidential information of Quasarix.
//
// * Any use, reproduction, modification, distribution, public performance or display of this software or through the use of this software without the
// * prior, express written consent of Quasarix is strictly prohibited and may be in violation of applicable laws.
// *
// */
//package com.social.ripple.external_ingestion.implementation;
//
//import java.io.File;
//import org.springframework.stereotype.Service;
//import com.social.ripple.external_ingestion.Constants.ConfigKeys;
//import com.social.ripple.external_ingestion.service.IMediaStorageService;
//import lombok.extern.slf4j.Slf4j;
//
//@Service("localStorageService")
//@Slf4j
//public class LocalMediaStorageServiceImpl implements IMediaStorageService { 
//
//    @Override
//    public File downloadFile(String traceId, String uuid) {
//     //   String localFilePath = ConfigKeys.MEDIA_LOCAL_FILE_PATH;
//
//        log.info("[{}]|MEDIA|LOCAL_FILE_DOWNLOAD|Requested file UUID: {}", traceId, uuid);
//        log.debug("[{}]|MEDIA|LOCAL_FILE_PATH|Resolved local file path: {}", traceId, localFilePath);
//
//        try {
//            File file = new File(localFilePath);
//            if (!file.exists() || !file.canRead()) {
//                String errorType = !file.exists() ? "LOCAL_FILE_NOT_FOUND" : "LOCAL_FILE_UNREADABLE";
//                String errorMsg = !file.exists() ? "File not found at path" : "Cannot read file at path";
//                log.error("[{}]|MEDIA|{}|{}: {}", traceId, errorType, errorMsg, localFilePath);
//                return null;
//            }
//
//
//            String extension = localFilePath.substring(localFilePath.lastIndexOf(".") + 1).toLowerCase();
//            if (extension.equals("mp4")) {
//                log.info("[{}]|MEDIA|TYPE_DETECTED|Video file detected", traceId);
//            } else if (extension.equals("jpg") || extension.equals("jpeg") || extension.equals("png")) {
//                log.info("[{}]|MEDIA|TYPE_DETECTED|Image file detected", traceId);
//            } else {
//                log.warn("[{}]|MEDIA|TYPE_DETECTED|Unknown file type: {}", traceId, extension);
//            }
//
//            log.info("[{}]|MEDIA|LOCAL_FILE_FOUND|File successfully located: {}", traceId, file.getAbsolutePath());
//            return file;
//
//        } catch (Exception ex) {
//            log.error("[{}]|MEDIA|LOCAL_FILE_ERROR|Unexpected error occurred while accessing file at path: {} | Error: {}",
//                    traceId, localFilePath, ex.getMessage(), ex);
//            return null;
//        }
//    }
//}
