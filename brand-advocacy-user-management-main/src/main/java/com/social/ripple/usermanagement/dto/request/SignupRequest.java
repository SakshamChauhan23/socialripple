package com.social.ripple.usermanagement.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String mobileNumber;
    private String password;
    private String confirmPassword;
    private String inviteToken;
    //private String captchaToken;
}
