import axios from 'axios';
import { userApiBase } from './runtimeConfig';

const authClient = axios.create({
    baseURL: userApiBase,
    timeout: 30000,
    headers: {
        'Content-Type': 'application/json',
    },
});

const generateTraceId = () => {
    return 'trace-' + Math.random().toString(36).substring(2, 15);
};

// Request Interceptor for adding the headers
authClient.interceptors.request.use(
    (config) => {
        // const token = localStorage.getItem('authToken');
        // if (token) {
        //     config.headers.Authorization = `Bearer ${token}`;
        // }
        config.headers['x-trace-id'] = generateTraceId();
        config.headers['language-id'] = 'en';

        return config;
    },
    (error) => Promise.reject(error)
);

// Response Interceptor for handling errors
authClient.interceptors.response.use(
    (response) => response,
    (error) => {
        console.error('API Error:', error);
        return Promise.reject(error);
    }
);

export default authClient;
