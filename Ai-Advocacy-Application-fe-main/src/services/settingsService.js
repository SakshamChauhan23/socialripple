import platformClient from "./platformIntegrations";

export const getBusinessPages = async () => {
    const response = await platformClient.get("external-ingestion/v1/api/org/business-pages");
    return response.data;
};

export const saveBusinessPage = async (platform, payload) => {
    const response = await platformClient.put(`external-ingestion/v1/api/org/business-pages/${platform.toLowerCase()}`, payload);
    return response.data;
};

export const deleteBusinessPage = async (platform) => {
    const response = await platformClient.delete(`external-ingestion/v1/api/org/business-pages/${platform.toLowerCase()}`);
    return response.data;
};

export const connectBusinessPage = async (platform) => {
    const response = await platformClient.post(`external-ingestion/v1/api/org/business-pages/${platform.toLowerCase()}/connect`);
    return response.data;
};

export const saveBusinessPageManual = async (platform, payload) => {
    const response = await platformClient.post(
        `external-ingestion/v1/api/org/business-pages/${platform.toLowerCase()}/manual`,
        payload
    );
    return response.data;
};

export const getAvailableBusinessPages = async (platform, transactionId) => {
    const response = await platformClient.get(
        `external-ingestion/v1/api/org/business-pages/${platform.toLowerCase()}/available-pages`,
        { params: { transactionId } }
    );
    return response.data;
};

export const selectBusinessPage = async (platform, payload) => {
    const response = await platformClient.post(
        `external-ingestion/v1/api/org/business-pages/${platform.toLowerCase()}/select`,
        payload
    );
    return response.data;
};

export const getLeaderPages = async () => {
    const response = await platformClient.get("external-ingestion/v1/api/org/leader-pages");
    return response.data;
};
