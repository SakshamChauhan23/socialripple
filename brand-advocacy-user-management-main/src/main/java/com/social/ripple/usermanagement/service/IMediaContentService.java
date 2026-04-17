package com.social.ripple.usermanagement.service;

import com.social.ripple.usermanagement.dto.response.MediaContentResponse;

public interface IMediaContentService {

    /**
     * Fetch a media file from the server folder by its file name.
     *
     * @param fileName the name of the file to fetch
     * @return MediaContentResponse containing the file resource, or null if not found
     */
    MediaContentResponse getMediaFileByName(String fileName);

}
