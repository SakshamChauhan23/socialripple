import apiClient from "./apiClient";
import platformClient from "./platformIntegrations";

const getSchedulerServiceErrorMessage = (error) => {
  const status = error.response?.status;
  if (
    status === 502 ||
    !error.response ||
    error.code === "ERR_NETWORK" ||
    error.message === "Network Error"
  ) {
    return "Scheduler service is temporarily unavailable. Please retry in a moment.";
  }
  return error.response?.data?.message || "Something went wrong!";
};

export const createPost = async (postData) => {
  try {
    const response = await apiClient.post("media/post", postData, {
      headers: {
        Accept: "application/json",
      },
    });
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);

    throw new Error(errorMessage); // Throw meaningful error
  }
};
export const getInsights = async (id) => {
  try {
    const response = await platformClient.get(
      `external-ingestion/v1/api/impressions/post/${id}`
    );
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);
    throw new Error(errorMessage);
  }
};
export const createXExternalPost = async (postData) => {
  try {
    const response = await platformClient.post(
      "external-ingestion/v1/api/x/post",
      postData
    );
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.devMessage ||
      error.response?.data?.message ||
      (error.request
        ? "No response received from the X publishing service."
        : "Something went wrong!");
    console.error("Failed to create post:", errorMessage);
    throw new Error(errorMessage);
  }
};
export const scheduleExternalPost = async (postData) => {
  try {
    const response = await platformClient.post(
      "external-ingestion/v1/api/schedule/post/create",
      postData
    );
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);
    throw new Error(errorMessage);
  }
};
export const getScheduleExternalPost = async () => {
  try {
    const response = await platformClient.get(
      "external-ingestion/v1/api/schedule/posts"
    );
    return response.data;
  } catch (error) {
    const errorMessage = getSchedulerServiceErrorMessage(error);
    console.error("Failed to fetch scheduled posts:", errorMessage);
    throw new Error(errorMessage);
  }
};
export const deleteScheduleExternalPost = async (id) => {
  try {
    const response = await platformClient.delete(
      `external-ingestion/v1/api/schedule/post/${id}`
    );
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to delete scheduled post:", errorMessage);
    throw new Error(errorMessage);
  }
};
export const updateScheduleExternalPost = async (postData) => {
  try {
    const response = await platformClient.put(
      "external-ingestion/v1/api/schedule/post/update",
      postData
    );
    return response.data;
  } catch (error) {
    const errorMessage = getSchedulerServiceErrorMessage(error);
    console.error("Failed to update scheduled post:", errorMessage);
    throw new Error(errorMessage);
  }
};
export const createFacebookExternalPost = async (postData) => {
  try {
    const response = await platformClient.post(
      "external-ingestion/v1/api/fb/post",
      postData
    );
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);
    throw new Error(errorMessage);
  }
};
export const createLinkedinExternalPost = async (postData) => {
  try {
    const response = await platformClient.post(
      "external-ingestion/v1/api/linkedin/post",
      postData
    );
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);
    throw new Error(errorMessage);
  }
};
export const createInstagramExternalPost = async (postData) => {
  try {
    const response = await platformClient.post(
      "external-ingestion/v1/api/instagram/post",
      postData
    );
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);
    throw new Error(errorMessage);
  }
};
export const getPosts = async (postData, page = 0, size = 10) => {
  try {
    const response = await apiClient.get(
      `timeline/posts?categoryId=${postData?.category || ""}&searchKey=${
        postData?.search || ""
      }&platform=${postData?.platform || ""}&page=${page}&size=${size}`,
      postData
    );
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);

    throw new Error(errorMessage); // Throw meaningful error
  }
};
export const preparePost = async (postData) => {
  try {
    const response = await apiClient.get(
      `timeline/platforms/posts/${postData}`,
      postData
    );
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);
    throw new Error(errorMessage); // Throw meaningful error
  }
};
export const postSummary = async (postData) => {
  try {
    const response = await apiClient.post(`content/summarize`, postData, { timeout: 60000 });
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);
    throw new Error(errorMessage); // Throw meaningful error
  }
};
export const assignCategoryToPost = async (postData) => {
  try {
    const response = await apiClient.put(`assign/category`, postData);
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);
    throw new Error(errorMessage); // Throw meaningful error
  }
};
export const postDetailsByPostId = async (postId) => {
  try {
    const response = await apiClient.get(`/timeline/detail/${postId}`);
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);
    throw new Error(errorMessage); // Throw meaningful error
  }
};
export const getSharedDetailsByPostId = async (postId) => {
  try {
    const response = await apiClient.get(
      `/user/activities/post/shared-users?postId=${postId}`
    );
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);
    throw new Error(errorMessage); // Throw meaningful error
  }
};

export const uploadMedia = async (postData) => {
  try {
    const response = await apiClient.post(`/media/upload`, postData, {
      headers: {
        "Content-Type": "multipart/form-data",
        Accept: "application/json",
      },
      timeout: 300000,
    });
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);

    throw new Error(errorMessage);
  }
};
export const uploadZipFile = async (postData) => {
  try {
    const response = await apiClient.post(`media/upload-zip`, postData, {
      headers: {
        "Content-Type": "multipart/form-data",
        Accept: "application/json",
      },
      timeout: 300000,
    });
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);

    throw new Error(errorMessage);
  }
};
export const AddMediaToLibrary = async (postData) => {
  try {
    const response = await apiClient.post(`media/uploadlibrary`, postData);
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);

    throw new Error(errorMessage);
  }
};

export const getAllMediaByType = async (data, page = 0, size = 20) => {
  try {
    const response = await apiClient.get(
      `media/library?mediaType=${data?.type}&categoryId=${
        data?.categoryId || ""
      }&searchKey=${data?.search || ""}&status=${
        data?.status
      }&page=${page}&size=${size}`
    );
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);

    throw new Error(errorMessage); // Throw meaningful error
  }
};
export const getAllAdminMedia = async (data, page = 0, size = 20) => {
  try {
    const response = await apiClient.get(
      `admin/media/library?mediaType=${data?.type}&categoryId=${
        data?.categoryId || ""
      }&status=${data?.status}&page=${page}&size=${size}`
    );
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);

    throw new Error(errorMessage); // Throw meaningful error
  }
};

export const deleteMediaById = async (id) => {
  try {
    const response = await apiClient.delete(`media/${id}`);
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);

    throw new Error(errorMessage); // Throw meaningful error
  }
};

export const changeMediaToArchive = async (data) => {
  try {
    const response = await apiClient.post(`media/${data?.id}/archive`);
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);
    throw new Error(errorMessage); // Throw meaningful error
  }
};

export const restoreMedia = async (data) => {
  try {
    const response = await apiClient.post(`media/${data?.id}/restore`);
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);
    throw new Error(errorMessage); // Throw meaningful error
  }
};

export const restoreArchive = async (data) => {
  try {
    const response = await apiClient.post(`media/${data?.id}/restore`);
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);

    throw new Error(errorMessage); // Throw meaningful error
  }
};
export const getAllPlatforms = async () => {
  try {
    const response = await apiClient.get(`socialmedia/track-self`);
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);

    throw new Error(errorMessage); // Throw meaningful error
  }
};
export const getBusinessPageLinks = async () => {
  try {
    const response = await apiClient.get(`socialmedia/business-pages`);
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to fetch business page links:", errorMessage);
    throw new Error(errorMessage);
  }
};
export const enablePlatforms = async (data) => {
  try {
    const response = await apiClient.post(`connections/toggle`, data);
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);

    throw new Error(errorMessage); // Throw meaningful error
  }
};
