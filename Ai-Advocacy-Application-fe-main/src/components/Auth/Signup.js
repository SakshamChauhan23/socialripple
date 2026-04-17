import React, { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import {
  Box,
  TextField,
  Button,
  Typography,
  Divider,
  IconButton,
  InputAdornment,
  Alert,
  CircularProgress,
} from "@mui/material";
import "../../../src/index.css";
import { ThemeProvider } from "@mui/material/styles";
import { Visibility, VisibilityOff } from "@mui/icons-material";
import avatarImg from "../../assets/loginandsignup/OBJECTS.png";
import logoImg from "../../assets/SocialRipplelogo_J.png";
import "./Signup.css";
import theme from "../../styles/theme";
import { Link } from "react-router-dom";
import authService from "../../services/authService";
import { formTypes } from "../../shared/constants";
import PhoneInput from "react-phone-number-input";
import "react-phone-number-input/style.css";
import { Auth } from "../../contexts/AuthContext";
import { GoogleLogin } from "@react-oauth/google";
import { useMsal } from "@azure/msal-react";
import { useGoogleReCaptcha } from "react-google-recaptcha-v3";
import { toast } from "react-toastify";

const SignUpPage = ({ formType }) => {
  const recaptchaSiteKey =
    process.env.REACT_APP_RECAPTCHA_SITE_KEY || "";
  const { setUser } = Auth();
  const navigate = useNavigate();
  const location = useLocation();
  const params = new URLSearchParams(location.search);
  const invitationToken = params.get("token");
  const isInviteSignup = formType === formTypes.SIGNUP && Boolean(invitationToken);
  const { executeRecaptcha } = useGoogleReCaptcha();
  const [loading, setLoading] = useState(false);
  const [, setInviteLoading] = useState(Boolean(invitationToken));
  const [inviteDataLoaded, setInviteDataLoaded] = useState(false);
  const [formData, setFormData] = useState({
    firstName: "",
    lastName: "",
    email: "",
    mobileNumber: "",
    password: "",
    confirmPassword: "",
  });

  const [errors, setErrors] = useState({});
  const [alert, setAlert] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [focusedField, setFocusedField] = useState(null);
  const [recaptchaState, setRecaptchaState] = useState(recaptchaSiteKey ? "loading" : "ready");
  const [recaptchaMessage, setRecaptchaMessage] = useState(
    recaptchaSiteKey ? "reCAPTCHA is still loading." : ""
  );
  const GOOGLE_PROVIDER_ID = 1;
  const MICROSOFT_PROVIDER_ID = 2;

  const togglePasswordVisibility = () => setShowPassword(!showPassword);

  // Pre-fill form from invitation data when signup link has a token
  useEffect(() => {
    if (invitationToken && isInviteSignup) {
      setInviteLoading(true);
      authService.getInviteInfo(invitationToken).then((response) => {
        if (response?.data?.status === true) {
          setFormData((prev) => ({
            ...prev,
            firstName: response.data.firstName || "",
            lastName: response.data.lastName || "",
            email: response.data.email || "",
            mobileNumber: response.data.phone || "",
          }));
          setInviteDataLoaded(true);
        }
      }).catch(() => {}).finally(() => setInviteLoading(false));
    }
  }, [invitationToken, isInviteSignup]);

  useEffect(() => {
    if (!recaptchaSiteKey) return;

    if (executeRecaptcha) {
      setRecaptchaState("ready");
      setRecaptchaMessage("");
      return;
    }

    let cancelled = false;
    const scriptId = "google-recaptcha-script";
    const existingScript = document.getElementById(scriptId);

    const markReady = () => {
      if (!cancelled && window.grecaptcha) {
        setRecaptchaState("ready");
        setRecaptchaMessage("");
      }
    };

    if (window.grecaptcha) {
      markReady();
      return;
    }

    const handleLoad = () => {
      if (!cancelled && window.grecaptcha) {
        window.grecaptcha.ready(markReady);
      }
    };

    const handleError = () => {
      if (!cancelled) {
        setRecaptchaState("script-error");
        setRecaptchaMessage("reCAPTCHA failed to initialize.");
      }
    };

    let script = existingScript;
    if (!script) {
      script = document.createElement("script");
      script.id = scriptId;
      script.src = `https://www.google.com/recaptcha/api.js?render=${recaptchaSiteKey}`;
      script.async = true;
      script.defer = true;
      document.head.appendChild(script);
    }

    script.addEventListener("load", handleLoad);
    script.addEventListener("error", handleError);

    const timeoutId = window.setTimeout(() => {
      if (!cancelled && !window.grecaptcha) {
        setRecaptchaState("script-timeout");
        setRecaptchaMessage("reCAPTCHA script did not load.");
      }
    }, 8000);

    return () => {
      cancelled = true;
      window.clearTimeout(timeoutId);
      script?.removeEventListener("load", handleLoad);
      script?.removeEventListener("error", handleError);
    };
  }, [executeRecaptcha, recaptchaSiteKey]);

  const getCaptchaToken = async () => {
    if (!recaptchaSiteKey) return "captcha-disabled";
    if (executeRecaptcha) {
      const token = await executeRecaptcha("submit");
      if (token) {
        setRecaptchaState("ready");
        setRecaptchaMessage("");
        return token;
      }
    }

    if (!window.grecaptcha) {
      throw new Error(
        recaptchaMessage || "reCAPTCHA script is not available in the browser."
      );
    }

    return new Promise((resolve, reject) => {
      window.grecaptcha.ready(() => {
        window.grecaptcha
          .execute(recaptchaSiteKey, { action: "submit" })
          .then((token) => {
            if (token) {
              setRecaptchaState("ready");
              setRecaptchaMessage("");
              resolve(token);
              return;
            }

            reject(new Error("reCAPTCHA token was not generated."));
          })
          .catch(() => {
            reject(new Error("reCAPTCHA failed to initialize."));
          });
      });
    });
  };

  const { instance } = useMsal();

  const handleSsoSuccess = async (providerId, ssoToken) => {
    const payload = {
      providerId,
      ssoToken,
    };

    if (isInviteSignup) {
      payload.invitationToken = invitationToken;
    }

    const backendResponse =
      isInviteSignup
        ? await authService.signupWithSso(payload)
        : formType === formTypes.SIGNUP
        ? await authService.registerWithSso(payload)
        : await authService.loginWithSso(payload);

    const session = authService.extractSession(backendResponse);
    const primaryRole = session?.role?.[0];

    if (!session?.token) {
      throw new Error(
        backendResponse?.data?.message || "SSO authentication did not return an access token."
      );
    }

    setUser(session);
    toast.success(
      formType === formTypes.SIGNUP
        ? "Welcome! You are now signed in with SSO."
        : "Welcome back! You are now signed in.",
      {
        style: {
          background: "#0047ab",
          color: "#fff",
        },
      }
    );

    if (!session.organizationId) {
      navigate("/organization-setup");
      return;
    }

    if (primaryRole === "ROLE_ADMIN") {
      navigate("/admin");
      return;
    }

    navigate("/employees/timeline");
  };

  const handleGoogleSuccess = async (credentialResponse) => {
    try {
      if (!credentialResponse?.credential) {
        throw new Error("Google did not return an ID token.");
      }

      await handleSsoSuccess(GOOGLE_PROVIDER_ID, credentialResponse.credential);
    } catch (error) {
      console.error("Google login failed", error);
      setAlert(
        error?.response?.data?.message ||
          error?.message ||
          "Google sign-in failed"
      );
    }
  };

  const handleMicrosoftLogin = async () => {
    try {
      const loginResponse = await instance.loginPopup({
        scopes: ["openid", "profile", "email", "User.Read"],
        prompt: "select_account",
      });

      if (!loginResponse?.idToken) {
        throw new Error("Microsoft did not return an ID token.");
      }

      instance.setActiveAccount(loginResponse.account || null);
      await handleSsoSuccess(MICROSOFT_PROVIDER_ID, loginResponse.idToken);
    } catch (error) {
      console.error("Microsoft login failed", error);
      setAlert(
        error?.response?.data?.message ||
          error?.message ||
          "Microsoft sign-in failed"
      );
    }
  };

  const toggleConfirmPasswordVisibility = () =>
    setShowConfirmPassword(!showConfirmPassword);
  const validateEmail = (email) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  const validatePhone = (phone) => /^\d{10}$/.test(phone);

  const handleChange = (e) => {
    const { name, value } = e.target;
    let errorMsg = "";

    if (!value.trim()) {
      errorMsg = `${name.replace(/([A-Z])/g, " $1")} is required`;
    } else if (name === "email" && !validateEmail(value)) {
      errorMsg = "Invalid email format";
    } else if (name === "mobileNumber" && !validatePhone(value)) {
      errorMsg = "Invalid phone number (10 digits required)";
    } else if (name === "password") {
      const strongPasswordRegex =
        /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*(),.?":{}|<>]).{8,}$/;

      if (!strongPasswordRegex.test(value) && formType !== "signIn") {
        errorMsg =
          "Password must be at least 8 characters long and include an uppercase letter, a lowercase letter, a number, and a special character";
      }
    } else if (name === "confirmPassword" && value !== formData.password) {
      errorMsg = "Passwords do not match";
    }

    setFormData({ ...formData, [name]: value });
    setErrors({ ...errors, [name]: errorMsg });
    setAlert("");
  };

  const validateForm = () => {
    let validationErrors = {};
    if (formType === formTypes.SIGNUP) {
      if (!formData.firstName) validationErrors.firstName = true;
      if (!formData.lastName) validationErrors.lastName = true;
      if (!formData.email) validationErrors.email = true;
      if (!formData.mobileNumber) validationErrors.mobileNumber = true;
      if (!formData.password) validationErrors.password = true;
      if (formData.password !== formData.confirmPassword || !formData.password)
        validationErrors.confirmPassword = true;
    } else if (formType === formTypes.SIGNIN) {
      if (!formData.email) validationErrors.email = true;
      if (!formData.password) validationErrors.password = true;
    } else if (formType === formTypes.PASSWORD_RECOVERY) {
      if (!formData.email) validationErrors.email = true;
    } else if (formType === formTypes.UPDATE_PASSWORD) {
      if (!formData.password) validationErrors.password = true;
      if (formData.password !== formData.confirmPassword || !formData.password)
        validationErrors.confirmPassword = true;
    }

    setErrors(validationErrors);

    if (Object.keys(validationErrors).length > 0) {
      setAlert("Please fill all fields to continue");
    }

    return Object.keys(validationErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validateForm()) return;
    setAlert("");
    setLoading(true);
    try {
      const captchaToken = await getCaptchaToken();
      let response;
      switch (formType) {
        case formTypes.SIGNUP:
          if (isInviteSignup) {
            const payload = {
              ...formData,
              inviteToken: invitationToken,
              captchaToken,
            };
            response = await authService.signUp(payload);
          } else {
            response = await authService.register({
              ...formData,
              captchaToken,
            });
          }
          if (response?.status === 200 || response?.status === 201) {
            toast.success(
              isInviteSignup
                ? "Welcome! You are now signed up. please login"
                : "Account created successfully. Sign in to continue organization setup."
            );
            navigate("/sign-in");
          }
          break;
        case formTypes.SIGNIN:
          response = await authService.signIn(
            formData.email,
            formData.password,
            captchaToken
          );
          if (response?.status === 200) {
            const session = authService.extractSession(response);
            if (!session.token) {
              throw new Error("Authentication token missing from login response");
            }
            setUser(session);
            toast.success("Welcome back! You are now signed in.", {
              style: {
                background: "#0047ab",
                color: "#fff",
              },
            });
            if (!session.organizationId) {
              navigate("/organization-setup");
            } else if (session?.role?.[0] === "ROLE_ADMIN") {
              navigate("/admin");
            } else {
              navigate("/employees/timeline");
            }
          }
          break;
        case formTypes.PASSWORD_RECOVERY:
          response = await authService.forgotPassword(formData.email);
          console.log(response, "response");
          break;
        case formTypes.UPDATE_PASSWORD:
          response = await authService.updatePassword({
            newPassword: formData.password,
            confirmNewPassword: formData.confirmPassword,
            resetToken: "",
            otp: "",
          });
          break;
        default:
          return;
      }
    } catch (error) {
      console.error(error);
      const errorMessage =
        error.response?.data?.message || error.message || "An error occurred";
      setAlert(errorMessage);
      if (errorMessage.toLowerCase().includes("recaptcha")) {
        toast.error(errorMessage);
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="root-container">
      <div className="logo-container">
        <img className="new_ico" src={logoImg} alt="Advocacy Logo" />
      </div>
      <Box
        display={"flex"}
        justifyContent={"space-between"}
        alignItems={"center"}
        flexDirection={{ xs: "column", md: "row" }}
        gap={{ xs: 3, md: 0 }}
        sx={{
          width: "100%",
          minHeight: { xs: "auto", md: "80vh" }, // desktop = tall layout, mobile = auto height
          px: {  md: 8 }, // padding responsive
          py: { xs: 4, md: 0 },
        }}
      >
        <Box
         sx={{
          width: { xs: "100%", sm: "80%", md: "50%", lg: "55%" },
          display: { xs: "none", md: "flex" },
          justifyContent: "center",
        }}
          className="image-container"
        >
          <img
            src={avatarImg}
            alt="Avatar"
            style={{
              width: "80%",
              maxWidth: "500px",
            }}
          />
        </Box>
        <Box
          sx={{
            width: { xs: "100%", md: "45%", lg: "40%" },
            display: "flex",
            justifyContent: { xs: "center", md: "flex-end" },
            alignItems: "center",
            mt: { xs: 2, md: 0 },
          }}
        >
          <Box className="form-container">
            <div className="headerBar" />
            <Typography className="form-header">
              {formType === formTypes.SIGNUP
                ? isInviteSignup
                  ? "Accept Invite"
                  : "Sign Up"
                : formType === formTypes.SIGNIN
                ? "Sign In"
                : formType === formTypes.PASSWORD_RECOVERY
                ? "Password Recovery"
                : "Update Password"}
            </Typography>
            {recaptchaState !== "ready" && (
              <Alert severity="warning" className="recaptcha-debug">
                {recaptchaMessage}
              </Alert>
            )}
            {alert && (
              <Alert
                severity="warning"
                style={{
                  marginBottom: "5px",
                  fontSize: "12px",
                  color: "#FFFFFF",
                  backgroundColor: "#DD3523",
                  border: "1px solid #DD3523",
                }}
              >
                {alert}
              </Alert>
            )}
            <ThemeProvider theme={theme}>
              {formType === formTypes.SIGNUP ? (
                <>
                  <TextField
                    placeholder="First Name"
                    name="firstName"
                    value={formData.firstName}
                    onChange={handleChange}
                    error={!!errors.firstName}
                    helperText={errors.firstName}
                    fullWidth
                    variant="outlined"
                    sx={{
                      "& .MuiInputBase-root": {
                        height: "40px",
                        // marginTop: "20px",
                      },
                      "& .MuiOutlinedInput-input":{
                        padding:"5px 14px !important"
                      }
                    }}
                  />
                  <TextField
                    placeholder="Last Name"
                    name="lastName"
                    value={formData.lastName}
                    onChange={handleChange}
                    error={!!errors.lastName}
                    helperText={errors.lastName}
                    fullWidth
                    variant="outlined"
                    sx={{
                      "& .MuiInputBase-root": {
                        height: "40px",
                        // marginTop: "20px",
                      },
                      "& .MuiOutlinedInput-input":{
                        padding:"5px 14px !important"
                      }
                    }}
                  />
                </>
              ) : null}
              {formType === formTypes.SIGNUP ||
              formType === formTypes.SIGNIN ||
              formType === formTypes.PASSWORD_RECOVERY ? (
                <>
                  <TextField
                    placeholder="Email"
                    name="email"
                    value={formData.email}
                    onChange={handleChange}
                    error={!!errors.email}
                    helperText={errors.email}
                    fullWidth
                    variant="outlined"
                    disabled={inviteDataLoaded}
                    type="email"
                    sx={{
                      "& .MuiInputBase-root": {
                        height: "40px",
                        // marginTop: "20px",
                      },
                      "& .MuiOutlinedInput-input":{
                        padding:"5px 14px !important"
                      }
                    }}
                  />
                </>
              ) : null}
              {formType === formTypes.SIGNUP ? (
                <>
                  <Box className="phoneInput">
                    <PhoneInput
                      name="mobileNumber"
                      error={!!errors.mobileNumber}
                      helperText={errors.mobileNumber}
                      international
                      defaultCountry="IN"
                      value={formData.mobileNumber}
                      placeholder="Mobile Number"
                      onChange={(value) => {
                        setFormData({ ...formData, mobileNumber: value });
                        setErrors({ ...errors, mobileNumber: false });
                        setAlert("");
                      }}
                      className="phone-input"
                      disableCountryCode={false}
                    />
                  </Box>
                </>
              ) : null}

              {formType === formTypes.SIGNIN ||
              formType === formTypes.SIGNUP ||
              formType === formTypes.UPDATE_PASSWORD ? (
                <>
                  <TextField
                    placeholder="Enter Password"
                    name="password"
                    value={formData.password}
                    onChange={handleChange}
                    error={!!errors.password}
                    helperText={errors.password}
                    fullWidth
                    variant="outlined"
                    type={showPassword ? "text" : "password"}
                    onFocus={() => setFocusedField('password')}
                    onBlur={() => setFocusedField(null)}
                    sx={{
                      "& .MuiInputBase-root": {
                        height: "40px",
                        // marginTop: "20px",
                      },
                      "& .MuiOutlinedInput-input":{
                        padding:"5px 14px !important"
                      }
                    }}
                    InputProps={{
                      endAdornment: focusedField === 'password' ? (
                        <InputAdornment position="end">
                          <IconButton
                            onClick={togglePasswordVisibility}
                            onMouseDown={(e) => e.preventDefault()}
                            edge="end"
                          >
                            {showPassword ? (
                              <Visibility
                                sx={{ color: "#95919D", fontSize: "14px" }}
                              />
                            ) : (
                              <VisibilityOff
                                sx={{ color: "#95919D", fontSize: "14px" }}
                              />
                            )}
                          </IconButton>
                        </InputAdornment>
                      ) : null,
                    }}
                  />
                  {formType === formTypes.SIGNIN && (
                    <Typography
                      variant="body2"
                      align="right"
                      style={{
                        marginTop: "0.5em",
                        fontSize: "12px",
                        cursor: "pointer",
                        color: "#2D76DC",
                      }}
                    >
                      <Link
                        to="/password-recovery"
                        className="navigation-texts"
                      >
                        Forgot password?
                      </Link>
                    </Typography>
                  )}
                </>
              ) : null}
              {formType === formTypes.SIGNUP ||
              formType === formTypes.UPDATE_PASSWORD ? (
                <>
                  <TextField
                    placeholder="Confirm Password"
                    name="confirmPassword"

                    value={formData.confirmPassword}
                    sx={{
                      "& .MuiInputBase-root": {
                        height: "40px",
                        // height: {lg:"40px",sm:"40px",xs:"40px"},
                        // marginTop: {lg:"20px",sm:"20px",xs:"20px"},
                      },
                      "& .MuiOutlinedInput-input":{
                        padding:"5px 14px !important"
                      }
                    }}
                    onChange={handleChange}
                    error={!!errors.confirmPassword}
                    helperText={errors.confirmPassword}
                    fullWidth
                    variant="outlined"
                    type={showConfirmPassword ? "text" : "password"}
                    onFocus={() => setFocusedField('confirmPassword')}
                    onBlur={() => setFocusedField(null)}
                    InputProps={{
                      endAdornment: focusedField === 'confirmPassword' ? (
                        <InputAdornment position="end">
                          <IconButton
                            onClick={toggleConfirmPasswordVisibility}
                            onMouseDown={(e) => e.preventDefault()}
                            edge="end"
                          >
                            {showConfirmPassword ? (
                              <Visibility
                                sx={{ color: "#95919D", fontSize: "14px" }}
                              />
                            ) : (
                              <VisibilityOff
                                sx={{ color: "#95919D", fontSize: "14px" }}
                              />
                            )}
                          </IconButton>
                        </InputAdornment>
                      ) : null,
                    }}
                  />
                </>
              ) : null}
            </ThemeProvider>
            {loading ? (
              <Box
                display={"flex"}
                justifyContent={"center"}
                alignItems="center"
              >
                <CircularProgress sx={{ ml: 2 }} />
              </Box>
            ) : (
              <Button
                variant="contained"
                color="primary"
                fullWidth
                sx={{
                  marginTop:{md:"1.5rem", xs:"1rem",lg:"1.5rem"},
                  textTransform: "none",
                  borderRadius: "6px",
                  fontWeight: "bold",
                  fontSize: "12px",
                  backgroundColor: "#0047AB",
                  height: "45px",
                }}
                onClick={handleSubmit}
              >
                {formType === formTypes.SIGNIN
                  ? "Sign In"
                  : formType === formTypes.SIGNUP
                  ? isInviteSignup
                    ? "Accept Invite"
                    : "Sign Up"
                  : formType === formTypes.PASSWORD_RECOVERY
                  ? "Reset"
                  : "Update"}
              </Button>
            )}
            {formType === formTypes.SIGNUP || formType === formTypes.SIGNIN ? (
              <>
                <Divider
                  style={{
                    margin: "10px 0",
                    color: "#4E4C59",
                    borderColor: "#9B9B9B",
                  }}
                >
                  or
                </Divider>
                <Box className="SocialLoginContainer">
                  <Box
                    sx={{
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      "& > div": {
                        lineHeight: 0,
                      },
                    }}
                  >
                    <GoogleLogin
                      onSuccess={handleGoogleSuccess}
                      onError={() => setAlert("Google sign-in failed")}
                      type="icon"
                      shape="circle"
                      theme="outline"
                      size="medium"
                      text="signin_with"
                      width="40"
                    />
                  </Box>
                  <IconButton onClick={handleMicrosoftLogin} disabled={loading}>
                    <svg
                      width="17"
                      height="17"
                      viewBox="0 0 17 17"
                      fill="none"
                      xmlns="http://www.w3.org/2000/svg"
                    >
                      <g clipPath="url(#clip0_602_37682)">
                        <path
                          d="M8.09573 0H-0.000976562V8.09483H8.09573V0Z"
                          fill="#F44336"
                        />
                        <path
                          d="M17.0003 0H8.9541V8.09483H17.0003V0Z"
                          fill="#4CAF50"
                        />
                        <path
                          d="M8.09573 8.9043H-0.000976562V16.9991H8.09573V8.9043Z"
                          fill="#2196F3"
                        />
                        <path
                          d="M17.0003 8.9043H8.9541V16.9991H17.0003V8.9043Z"
                          fill="#FFC107"
                        />
                      </g>
                      <defs>
                        <clipPath id="clip0_602_37682">
                          <rect width="17" height="17" rx="1" fill="white" />
                        </clipPath>
                      </defs>
                    </svg>
                  </IconButton>
                </Box>
              </>
            ) : null}
            <Typography
              variant="body2"
              align="center"
              style={{ marginTop: "0.5em", fontSize: "12px" }}
            >
              {formType === formTypes.SIGNIN ? (
                <>
                  Don't have an account?{" "}
                  <Link to="/sign-up" className="navigation-texts">
                    Sign Up
                  </Link>
                </>
              ) : formType === formTypes.SIGNUP ? (
                <>
                  Already have an account?{" "}
                  <Link to="/sign-in" className="navigation-texts">
                    Sign In
                  </Link>
                </>
              ) : formType === formTypes.PASSWORD_RECOVERY ? (
                <Typography variant="body2" align="center">
                  <Link to="/sign-in" className="navigation-texts typo">
                    Back to Sign In
                  </Link>
                </Typography>
              ) : null}
            </Typography>
          </Box>
        </Box>
      </Box>
    </div>
  );
};

export default SignUpPage;
