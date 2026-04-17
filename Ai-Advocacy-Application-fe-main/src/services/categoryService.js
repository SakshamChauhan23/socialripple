import apiClient from "./apiClient";

const UserService = {
  getAllCategories: async () => {
    try {
      const response = await apiClient.get("/categories");
      return response.data;
    } catch (error) {
      console.error("Error fetching categories:", error);
      throw error;
    }
  },
  addNewCategories: async (payload) => {
    try {
      const response = await apiClient.post("admin/categories", payload);
      return response.data;
    } catch (error) {
      console.error("Error fetching categories:", error);
      throw error;
    }
  },
  updateCategory: async (id, payload) => {
    try {
      const response = await apiClient.put(`admin/categories/${id}`, payload);
      return response.data;
    } catch (error) {
      console.error("Error updating category:", error);
      throw error;
    }
  },
  deleteCategory: async (id) => {
    try {
      const response = await apiClient.delete(`admin/categories/${id}`);
      return response.data;
    } catch (error) {
      console.error("Error deleting category:", error);
      throw error;
    }
  },
  updateProfile: async (payload) => {
    try {
      const response = await apiClient.put("user/profile", payload);
      return response.data;
    } catch (error) {
      console.error("Error fetching categories:", error);
      throw error;
    }
  },
  getProfile: async (id) => {
    try {
      const response = await apiClient.get(`user/profile?userId=${id || ''}`);
      return response.data;
    } catch (error) {
      console.error("Error fetching categories:", error);
      throw error;
    }
  },
  getCounting: async (id) => {
    try {
      const response = await apiClient.get(
        `user/activities/platforms/share/count?userId=${id || ''}`
      );
      return response.data;
    } catch (error) {
      console.error("Error fetching categories:", error);
      throw error;
    }
  },
  getActivities: async (id) => {
    try {
      const response = await apiClient.get(
        `user/activities/posts?userId=${id || ''}`
      );
      return response.data;
    } catch (error) {
      console.error("Error fetching categories:", error);
      throw error;
    }
  },
  getShares: async (id) => {
    try {
      const response = await apiClient.get(
        `user/activities/shares?userId=${id || ''}`
      );
      return response.data;
    } catch (error) {
      console.error("Error fetching categories:", error);
      throw error;
    }
  },
  getTags: async (id) => {
    try {
      const response = await apiClient.get(`user/activities/tags?userId=${id || ''}`);
      return response.data;
    } catch (error) {
      console.error("Error fetching categories:", error);
      throw error;
    }
  },
  getLoyaltyPoints: async (id) => {
    try {
      const response = await apiClient.get(`loyaltyPoints?userId=${id || ''}`);
      return response.data;
    } catch (error) {
      console.error("Error fetching categories:", error);
      throw error;
    }
  },
  getUserActivitiesShares: async () => {
    try {
      const response = await apiClient.get("user/activities/shares");
      return response.data;
    } catch (error) {
      console.error("Error fetching categories:", error);
      throw error;
    }
  },
  getUserActivitiesPosts: async () => {
    try {
      const response = await apiClient.get("user/activities/posts");
      return response.data;
    } catch (error) {
      console.error("Error fetching categories:", error);
      throw error;
    }
  },
};

export default UserService;
