INSERT INTO cnfg_config_parameters (parameter_name, parameter_value)
VALUES
  ('MEDIA_IMAGE_PROVIDER', 'BUNNY_STORAGE'),
  ('MEDIA_VIDEO_PROVIDER', 'BUNNY_STORAGE'),
  ('BUNNY_STORAGE_ZONE', 'social-ripple'),
  ('BUNNY_STORAGE_REGION', 'sg.storage.bunnycdn.com'),
  ('BUNNY_STORAGE_ACCESS_KEY', 'REPLACE_ME'),
  ('BUNNY_PULL_ZONE_BASE_URL', 'https://cdn.socialripple.ai')
ON DUPLICATE KEY UPDATE
  parameter_value = VALUES(parameter_value);
