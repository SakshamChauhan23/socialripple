import apiClient from "./apiClient";

export const createNewTeam = async (Data) => {
  try {
    const response = await apiClient.post("admin/teams/create", Data);
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);

    throw new Error(errorMessage); // Throw meaningful error
  }
};
export const updateTeam = async (Data, id) => {
  try {
    const response = await apiClient.put(`admin/teams/update/${id}`, Data);
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);

    throw new Error(errorMessage); // Throw meaningful error
  }
};
export const addTeamMember = async (Data, id) => {
  try {
    const response = await apiClient.post(`admin/teams/${id}/members/add`, Data);
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);

    throw new Error(errorMessage); // Throw meaningful error
  }
};
export const deleteTeam = async (id) => {
  try {
    const response = await apiClient.delete(`admin/teams/delete/${id}`);
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);

    throw new Error(errorMessage); // Throw meaningful error
  }
};
export const deleteMemberFromTeam = async (data) => {
  try {
    const response = await apiClient.delete(`admin/teams/members/remove`, { data });
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);

    throw new Error(errorMessage); // Throw meaningful error
  }
};
export const getAllTeams = async () => {
  try {
    const response = await apiClient.get(`teams/list`);
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);

    throw new Error(errorMessage); // Throw meaningful error
  }
};
export const getTeamDetails = async (id) => {
  try {
    const response = await apiClient.get(`teams/details/${id}`);
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);

    throw new Error(errorMessage); // Throw meaningful error
  }
};
export const getAllLeaders = async (id) => {
  try {
    const response = await apiClient.get(`leaderboard/top`);
    return response.data;
  } catch (error) {
    const errorMessage =
      error.response?.data?.message || "Something went wrong!";
    console.error("Failed to create post:", errorMessage);
    throw new Error(errorMessage); // Throw meaningful error
  }
};
