import axios from 'axios';
import { platformApiBase } from './runtimeConfig';
import { toast } from 'react-toastify';
import { getStoredAuthToken, getStoredOrgId } from '../shared/authSession';

const platformClient = axios.create({
    baseURL: platformApiBase,
    // timeout: 10000,
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
platformClient.interceptors.request.use(
    (config) => {
        const token = getStoredAuthToken();
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        } else {
            delete config.headers.Authorization;
        }
        if (!(config.data instanceof FormData)) {
            config.headers['Content-Type'] = 'application/json';
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
platformClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      const { status, data } = error.response;
      switch (status) {
        case 400:
          console.error('Bad Request:', data.message || data);
          break;
        case 401:
          console.error('Unauthorized:', data.message || 'Login required');
          break;
        case 403:
          console.error('Forbidden:', data.message || 'Access denied');
          break;
        case 409:
          toast.error( data.message || 'Access denied');
          break;
        case 404:
          console.error('Not Found:', data.message || 'Resource not found');
          break;
        case 500:
          console.error('Server Error:', data.message || 'Internal server error');
          break;
        default:
          console.error(`Unhandled Error (${status}):`, data.message || data);
      }
    } else if (error.request) {
      console.error('No Response Received:', error.message);
    } else {
      console.error('Request Setup Error:', error.message);
    }

    return Promise.reject(error);
  }
)
export default platformClient;
