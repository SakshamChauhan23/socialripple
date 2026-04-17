import axios from 'axios';
import { getStoredAuthToken, getStoredOrgId } from '../shared/authSession';
import { userApiBase } from './runtimeConfig';

const apiClient = axios.create({
    baseURL: userApiBase,
    timeout: 30000,
    headers: {
        'Content-Type': 'application/json',
    },
});

const generateTraceId = () => {
    return 'trace-' + Math.random().toString(36).substring(2, 15);
};
const generateCorrelationId = () => {
    return 'corr-' + Math.random().toString(36).substring(2, 15);
};

// Request Interceptor for adding the headers
apiClient.interceptors.request.use(
    (config) => {
        const token = getStoredAuthToken();
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        } else {
            delete config.headers.Authorization;
        }
        config.headers['x-trace-id'] = generateTraceId();
        config.headers['language-id'] = 'en';
        const orgId = getStoredOrgId();
        if (orgId && orgId !== "null" && orgId !== "undefined") {
          config.headers['x-tenant-id'] = orgId;
        } else {
          delete config.headers['x-tenant-id'];
        }
        config.headers['x-correlation-id'] = generateCorrelationId();

        return config;
    },
    (error) => Promise.reject(error)
);

// Response Interceptor for handling errors
apiClient.interceptors.response.use(
    (response) => response,
    (error) => {
        console.error('API Error:', error);
        return Promise.reject(error);
    }
);

export default apiClient;
