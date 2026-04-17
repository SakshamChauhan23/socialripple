UPDATE cnfg_config_parameters
SET parameter_value = 'LOCAL',
    description = 'Media image provider: LOCAL or BUNNY_STORAGE',
    updated_at = NOW()
WHERE parameter_name = 'MEDIA_IMAGE_PROVIDER';
INSERT INTO cnfg_config_parameters (parameter_name, parameter_value, description, created_at, updated_at)
SELECT 'MEDIA_IMAGE_PROVIDER', 'LOCAL', 'Media image provider: LOCAL or BUNNY_STORAGE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM cnfg_config_parameters WHERE parameter_name = 'MEDIA_IMAGE_PROVIDER');

UPDATE cnfg_config_parameters
SET parameter_value = 'LOCAL',
    description = 'Media video provider: LOCAL or BUNNY_STREAM',
    updated_at = NOW()
WHERE parameter_name = 'MEDIA_VIDEO_PROVIDER';
INSERT INTO cnfg_config_parameters (parameter_name, parameter_value, description, created_at, updated_at)
SELECT 'MEDIA_VIDEO_PROVIDER', 'LOCAL', 'Media video provider: LOCAL or BUNNY_STREAM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM cnfg_config_parameters WHERE parameter_name = 'MEDIA_VIDEO_PROVIDER');

UPDATE cnfg_config_parameters
SET parameter_value = 'REPLACE_ME',
    description = 'Bunny Storage zone name for image uploads',
    updated_at = NOW()
WHERE parameter_name = 'BUNNY_STORAGE_ZONE';
INSERT INTO cnfg_config_parameters (parameter_name, parameter_value, description, created_at, updated_at)
SELECT 'BUNNY_STORAGE_ZONE', 'REPLACE_ME', 'Bunny Storage zone name for image uploads', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM cnfg_config_parameters WHERE parameter_name = 'BUNNY_STORAGE_ZONE');

UPDATE cnfg_config_parameters
SET parameter_value = 'storage',
    description = 'Bunny Storage host prefix or full hostname',
    updated_at = NOW()
WHERE parameter_name = 'BUNNY_STORAGE_REGION';
INSERT INTO cnfg_config_parameters (parameter_name, parameter_value, description, created_at, updated_at)
SELECT 'BUNNY_STORAGE_REGION', 'storage', 'Bunny Storage host prefix or full hostname', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM cnfg_config_parameters WHERE parameter_name = 'BUNNY_STORAGE_REGION');

UPDATE cnfg_config_parameters
SET parameter_value = 'REPLACE_ME',
    description = 'Bunny Storage access key',
    updated_at = NOW()
WHERE parameter_name = 'BUNNY_STORAGE_ACCESS_KEY';
INSERT INTO cnfg_config_parameters (parameter_name, parameter_value, description, created_at, updated_at)
SELECT 'BUNNY_STORAGE_ACCESS_KEY', 'REPLACE_ME', 'Bunny Storage access key', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM cnfg_config_parameters WHERE parameter_name = 'BUNNY_STORAGE_ACCESS_KEY');

UPDATE cnfg_config_parameters
SET parameter_value = 'https://cdn.socialripple.ai',
    description = 'Bunny Pull Zone base URL for image delivery',
    updated_at = NOW()
WHERE parameter_name = 'BUNNY_PULL_ZONE_BASE_URL';
INSERT INTO cnfg_config_parameters (parameter_name, parameter_value, description, created_at, updated_at)
SELECT 'BUNNY_PULL_ZONE_BASE_URL', 'https://cdn.socialripple.ai', 'Bunny Pull Zone base URL for image delivery', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM cnfg_config_parameters WHERE parameter_name = 'BUNNY_PULL_ZONE_BASE_URL');

UPDATE cnfg_config_parameters
SET parameter_value = 'REPLACE_ME',
    description = 'Bunny Stream library ID for video uploads',
    updated_at = NOW()
WHERE parameter_name = 'BUNNY_STREAM_LIBRARY_ID';
INSERT INTO cnfg_config_parameters (parameter_name, parameter_value, description, created_at, updated_at)
SELECT 'BUNNY_STREAM_LIBRARY_ID', 'REPLACE_ME', 'Bunny Stream library ID for video uploads', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM cnfg_config_parameters WHERE parameter_name = 'BUNNY_STREAM_LIBRARY_ID');

UPDATE cnfg_config_parameters
SET parameter_value = 'REPLACE_ME',
    description = 'Bunny Stream API key',
    updated_at = NOW()
WHERE parameter_name = 'BUNNY_STREAM_API_KEY';
INSERT INTO cnfg_config_parameters (parameter_name, parameter_value, description, created_at, updated_at)
SELECT 'BUNNY_STREAM_API_KEY', 'REPLACE_ME', 'Bunny Stream API key', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM cnfg_config_parameters WHERE parameter_name = 'BUNNY_STREAM_API_KEY');

UPDATE cnfg_config_parameters
SET parameter_value = 'https://video.bunnycdn.com/play',
    description = 'Bunny Stream playback base URL',
    updated_at = NOW()
WHERE parameter_name = 'BUNNY_STREAM_PLAYBACK_BASE_URL';
INSERT INTO cnfg_config_parameters (parameter_name, parameter_value, description, created_at, updated_at)
SELECT 'BUNNY_STREAM_PLAYBACK_BASE_URL', 'https://video.bunnycdn.com/play', 'Bunny Stream playback base URL', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM cnfg_config_parameters WHERE parameter_name = 'BUNNY_STREAM_PLAYBACK_BASE_URL');

UPDATE cnfg_config_parameters
SET parameter_value = '',
    description = 'Optional Bunny Stream thumbnail base URL if using a custom hostname',
    updated_at = NOW()
WHERE parameter_name = 'BUNNY_STREAM_THUMBNAIL_BASE_URL';
INSERT INTO cnfg_config_parameters (parameter_name, parameter_value, description, created_at, updated_at)
SELECT 'BUNNY_STREAM_THUMBNAIL_BASE_URL', '', 'Optional Bunny Stream thumbnail base URL if using a custom hostname', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM cnfg_config_parameters WHERE parameter_name = 'BUNNY_STREAM_THUMBNAIL_BASE_URL');
