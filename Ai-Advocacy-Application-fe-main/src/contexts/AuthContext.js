import React, {
  useCallback,
  createContext,
  useReducer,
  useContext,
  useEffect,
  useRef,
  useState,
} from "react";
import { getBusinessPageLinks } from "../services/postService";
import {
  clearStoredSession,
  getStoredSession,
  hasStoredSessionToken,
  persistStoredSession,
} from "../shared/authSession";

const SET_USER = "SET_USER";
const LOGOUT_USER = "LOGOUT_USER";

const initialState = {
  token: null,
  refreshToken: null,
  organizationId: null,
  role: [],
  requiresOrganizationSetup: false,
  isAuthenticated: false,
};

const authReducer = (state, action) => {
  switch (action.type) {
    case SET_USER:
      return {
        ...state,
        token: action.payload.token,
        refreshToken: action.payload.refreshToken ?? null,
        organizationId: action.payload.organizationId ?? null,
        role: action.payload.role ?? [],
        requiresOrganizationSetup:
          action.payload.requiresOrganizationSetup ??
          (Boolean(action.payload.token) && action.payload.organizationId == null),
        isAuthenticated: Boolean(action.payload.token),
      };
    case LOGOUT_USER:
      return { ...initialState };
    default:
      return state;
  }
};

const AuthContext = createContext();

const buildBusinessPagesFromLinks = (storedLinks = {}) =>
  Object.entries(storedLinks).reduce((acc, [platform, url]) => {
    acc[platform] = {
      url: url || "",
      username: "",
      tagText: "",
    };
    return acc;
  }, {});

const getLegacyBusinessPages = () => {
  try {
    const storedLinks = JSON.parse(sessionStorage.getItem("businessPageLinks") || "{}");
    return buildBusinessPagesFromLinks(storedLinks);
  } catch (error) {
    return {};
  }
};

const getStoredBusinessPages = () => {
  try {
    const storedBusinessPages = JSON.parse(sessionStorage.getItem("businessPages") || "{}");
    if (storedBusinessPages && Object.keys(storedBusinessPages).length > 0) {
      return storedBusinessPages;
    }
  } catch (error) {
    return {};
  }
  return getLegacyBusinessPages();
};

const extractBusinessPageLinks = (businessPages = {}) =>
  Object.entries(businessPages).reduce((acc, [platform, info]) => {
    acc[platform] = info?.url || "";
    return acc;
  }, {});

export const AuthProvider = ({ children }) => {
  const [state, dispatch] = useReducer(authReducer, initialState);
  const [userProfile, setUserProfile] = useState(null);
  const [loyaltyPoints, setLoyaltyPoints] = useState(null);
  const [categoriesData, setCategoriesData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [categoriesDetails, setCategoriesDetails] = useState(null);
  const [searchValue, setSearchValue] = useState("");
  const [businessPages, setBusinessPages] = useState(() => getStoredBusinessPages());
  const [businessPagesLoading, setBusinessPagesLoading] = useState(false);
  const [businessPagesLoaded, setBusinessPagesLoaded] = useState(
    () => Object.keys(getStoredBusinessPages()).length > 0
  );
  const businessPagesRequestRef = useRef(null);
  useEffect(() => {
    const storedSession = getStoredSession();
    if (storedSession.token) {
      dispatch({
        type: SET_USER,
        payload: storedSession,
      });
    }
    setLoading(false); // finished checking for token
  }, []);

  const refreshBusinessPageLinks = useCallback(async () => {
    if (!hasStoredSessionToken()) {
      setBusinessPages({});
      setBusinessPagesLoaded(false);
      setBusinessPagesLoading(false);
      businessPagesRequestRef.current = null;
      sessionStorage.removeItem("businessPages");
      sessionStorage.removeItem("businessPageLinks");
      return {};
    }

    if (businessPagesRequestRef.current) {
      return businessPagesRequestRef.current;
    }

    setBusinessPagesLoading(true);

    const request = (async () => {
    try {
      const response = await getBusinessPageLinks();
      const normalizedBusinessPages =
        response?.businessPages ||
        buildBusinessPagesFromLinks(response?.businessPageLinks || {});
      setBusinessPages(normalizedBusinessPages);
      setBusinessPagesLoaded(true);
      sessionStorage.setItem("businessPages", JSON.stringify(normalizedBusinessPages));
      sessionStorage.setItem(
        "businessPageLinks",
        JSON.stringify(extractBusinessPageLinks(normalizedBusinessPages))
      );
      return normalizedBusinessPages;
    } catch (error) {
      console.error("Unable to refresh business page links", error);
      setBusinessPagesLoaded(true);
      return {};
    } finally {
      setBusinessPagesLoading(false);
      businessPagesRequestRef.current = null;
    }
    })();

    businessPagesRequestRef.current = request;
    return request;
  }, []);

  useEffect(() => {
    if (state.token) {
      refreshBusinessPageLinks();
    } else {
      setBusinessPages({});
      setBusinessPagesLoaded(false);
      setBusinessPagesLoading(false);
      businessPagesRequestRef.current = null;
      sessionStorage.removeItem("businessPages");
      sessionStorage.removeItem("businessPageLinks");
    }
  }, [state.token, refreshBusinessPageLinks]);

  const setUser = (sessionOrToken) => {
    persistStoredSession(sessionOrToken);
    if (typeof sessionOrToken === "string") {
      dispatch({ type: SET_USER, payload: { token: sessionOrToken } });
      return;
    }
    dispatch({ type: SET_USER, payload: sessionOrToken || {} });
  };

  const clearUser = () => {
    clearStoredSession();
    dispatch({ type: LOGOUT_USER });
  };

  if (loading) {
    return <div>Loading...</div>;
  }

  return (
    <AuthContext.Provider
      value={{
        state,
        loading,
        dispatch,
        setUser,
        clearUser,
        loyaltyPoints,
        setLoyaltyPoints,
        userProfile,
        setUserProfile,
        categoriesData,
        setCategoriesData,
        categoriesDetails,
        setCategoriesDetails,
        searchValue,
        setSearchValue,
        businessPages,
        businessPagesLoading,
        businessPagesLoaded,
        setBusinessPages,
        businessPageLinks: extractBusinessPageLinks(businessPages),
        refreshBusinessPageLinks,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const Auth = () => useContext(AuthContext);
