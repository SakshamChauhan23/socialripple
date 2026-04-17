-- Move NewsAPI key out of source code into cnfg_config_parameters.
-- This ships the existing key value (already present in committed git history
-- as a hardcoded literal in ContentServiceImpl.java); the value is not being
-- rotated as part of this migration.

UPDATE cnfg_config_parameters
SET parameter_value = '8ab3628f5f9741a3bd55c77bc8d16447',
    description = 'NewsAPI.org API key used by the trending-topics feature',
    updated_at = NOW()
WHERE parameter_name = 'NEWSAPI_API_KEY';
INSERT INTO cnfg_config_parameters (parameter_name, parameter_value, description, created_at, updated_at)
SELECT 'NEWSAPI_API_KEY', '8ab3628f5f9741a3bd55c77bc8d16447', 'NewsAPI.org API key used by the trending-topics feature', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM cnfg_config_parameters WHERE parameter_name = 'NEWSAPI_API_KEY');

UPDATE cnfg_config_parameters
SET parameter_value = 'https://newsapi.org/v2/everything',
    description = 'NewsAPI.org base endpoint for trending topic fetches',
    updated_at = NOW()
WHERE parameter_name = 'NEWSAPI_BASE_URL';
INSERT INTO cnfg_config_parameters (parameter_name, parameter_value, description, created_at, updated_at)
SELECT 'NEWSAPI_BASE_URL', 'https://newsapi.org/v2/everything', 'NewsAPI.org base endpoint for trending topic fetches', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM cnfg_config_parameters WHERE parameter_name = 'NEWSAPI_BASE_URL');
