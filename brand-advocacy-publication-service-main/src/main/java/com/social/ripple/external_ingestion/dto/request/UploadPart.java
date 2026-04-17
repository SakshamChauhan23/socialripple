package com.social.ripple.external_ingestion.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UploadPart {
    private String etag;
    private String range;

    public UploadPart(String etag, long startByte, long endByte) {
        this.etag = etag;
        this.range = "bytes " + startByte + "-" + endByte;
    }
}
