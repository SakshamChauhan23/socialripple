ALTER TABLE media_files
    ADD COLUMN storage_provider VARCHAR(64) NULL AFTER file_id,
    ADD COLUMN provider_asset_id VARCHAR(255) NULL AFTER storage_provider,
    ADD COLUMN playback_url TEXT NULL AFTER provider_asset_id,
    ADD COLUMN thumbnail_url TEXT NULL AFTER playback_url,
    ADD COLUMN processing_status VARCHAR(64) NULL AFTER thumbnail_url;

UPDATE media_files
SET storage_provider = COALESCE(storage_provider, 'LOCAL'),
    processing_status = COALESCE(processing_status, 'READY')
WHERE storage_provider IS NULL
   OR processing_status IS NULL;
