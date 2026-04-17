import inviteClient from "./invitaionApis";

export const Content_Generation = async (postData) => {
    try {
      const response = await inviteClient.post("content/generate", postData, {
        timeout: 60000,
      });
      return response.data;
    } catch (error) {
      const errorMessage =
        error.response?.data?.message || "Something went wrong!";
      throw new Error(errorMessage);
    }
  };