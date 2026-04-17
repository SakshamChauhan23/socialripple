import inviteClient from "./invitaionApis";
import platformClient from "./platformIntegrations";

export const inviteUser = async (postData) => {
  try {
    const response = await inviteClient.post("admin/invite", postData);
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    throw new Error(errorMessage);
  }
};
export const reInviteUser = async (postData) => {
  try {
    const response = await inviteClient.post("admin/reinvite", postData);
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);
    throw new Error(errorMessage);
  }
};
export const userListing = async (org_id, page = 0, size = 100) => {
  try {
    const response = await inviteClient.get(
      `users/organization/${org_id}?page=${page}&size=${size}`
    );
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);
    throw new Error(errorMessage);
  }
};
export const userActivePlatform = async (org_id) => {
  try {
    const response = await inviteClient.get(`socialmedia/track/${org_id}`);
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);
    throw new Error(errorMessage);
  }
};
export const appointAsLeader = async (data) => {
  try {
    const response = await inviteClient.post(`teams/toggle/leader`, data);
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);
    throw new Error(errorMessage);
  }
};

export const updateUser = async (data) => {
  try {
    const response = await inviteClient.put(`admin/users/update`, data);
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to update user:", errorMessage);
    throw new Error(errorMessage);
  }
};

export const toggleAdminRole = async (data) => {
  try {
    const response = await inviteClient.put(`admin/users/toggle-admin-role`, data);
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to toggle admin role:", errorMessage);
    throw new Error(errorMessage);
  }
};

export const deleteUser = async (userId) => {
  try {
    const response = await inviteClient.delete(`users/${userId}`);
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to delete user:", errorMessage);
    throw new Error(errorMessage);
  }
};
export const employeeUserListing = async (org_id, page = 0, size = 10) => {
  try {
    const response = await inviteClient.get(
      `reference/users/${org_id}?page=${page}&size=${size}`
    );
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);
    throw new Error(errorMessage);
  }
};
export const employeeTrendingListing = async () => {
  try {
    const response = await inviteClient.get(`content/trending-hashtags`);
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);
    throw new Error(errorMessage);
  }
};
export const employeeTrendingTopics = async () => {
  try {
    const response = await inviteClient.get(`content/trending-topics`);
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);
    throw new Error(errorMessage);
  }
};
export const fetchOrgTrendingTopics = async () => {
  try {
    const response = await inviteClient.get(`content/trending-topics/preferences`);
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to fetch trending topics:", errorMessage);
    throw new Error(errorMessage);
  }
};

export const saveOrgTrendingTopics = async (topics) => {
  try {
    const response = await inviteClient.put(
      `content/trending-topics`,
      { topics }
    );
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to save trending topics:", errorMessage);
    throw new Error(errorMessage);
  }
};
export const getAllNotifications = async (page = 0, size = 10) => {
  try {
    const response = await inviteClient.get(
      `notifications?page=${page}&size=${size}`
    );
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);
    throw new Error(errorMessage);
  }
};

// dasboard related APIs
export const getAllEngagement = async () => {
  try {
    const response = await inviteClient.get(
      `api/dashboard/employee/engagement-metrics`
    );
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);
    throw new Error(errorMessage);
  }
};
export const getAllMonthlyParticipation = async () => {
  try {
    const response = await inviteClient.get(
      `api/dashboard/employee/monthly-participation`
    );
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);
    throw new Error(errorMessage);
  }
};
export const getAllTopContributors = async () => {
  try {
    const response = await inviteClient.get(
      `api/dashboard/employee/top-contributors`
    );
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);
    throw new Error(errorMessage);
  }
};
export const getMonthlyReach = async () => {
  try {
    const response = await platformClient.get(
      `external-ingestion/v1/api/dashboard/organization/monthly-reach`,
      { timeout: 55000 }
    );
    return response.data;
  } catch (error) {
    console.warn("Dashboard monthly reach unavailable:", error?.response?.data?.message || error.message);
    return null;
  }
};
export const getLeadConversation = async () => {
  try {
    const response = await platformClient.get(
      `external-ingestion/v1/api/dashboard/organization/lead-conversion`,
      { timeout: 55000 }
    );
    return response.data;
  } catch (error) {
    console.warn("Dashboard lead conversion unavailable:", error?.response?.data?.message || error.message);
    return null;
  }
};
