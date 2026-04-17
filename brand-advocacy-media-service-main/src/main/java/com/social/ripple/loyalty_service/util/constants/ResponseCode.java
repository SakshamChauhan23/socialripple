/**
 * Filename: ResponseCode.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual property
 * rights) in this software and any derivative works based upon or derived from this software belongs exclusively to Quasarix. Access to this software
 * is forbidden to anyone except current employees of Quasarix and its affiliated companies who have executed non-disclosure agreements explicitly
 * covering such access. While in the employment of Quasarix or its affiliate companies as the case may be, employees may use this software
 * internally, solely in the course of employment, for the sole purpose of developing new functionalities, features, procedures, routines,
 * customizations or derivative works, or for the purpose of providing maintenance or support for the software. Save as expressly permitted above, no
 * license or right thereto is hereby granted to anyone, either directly, by implication or otherwise. On the termination of employment, the license
 * granted to employee to access the software shall terminate and the software should be returned to the employer, without retaining any copies. This
 * software is (i) proprietary to Quasarix; (ii) is of significant value to it; (iii) contains trade secrets of Quasarix; (iv) is not publicly
 * available; and (v) constitutes the confidential information of Quasarix. Any use, reproduction, modification, distribution, public performance or
 * display of this software or through the use of this software without the prior, express written consent of Quasarix is strictly prohibited and may
 * be in violation of applicable laws.
 */
package com.social.ripple.loyalty_service.util.constants;

/**
 * Centralized standard error codes used across the Loyalty module.
 * Format: MODULE_HTTPSTATUS
 * Example: LYLT_401 → Loyalty module, Unauthorized
 */
public final class ResponseCode {

    private ResponseCode() {
        // Prevent instantiation
    }

    public static final String LYLT_200 = "LYLT_200";
    public static final String LYLT_201 = "LYLT_201";
    public static final String LYLT_202 = "LYLT_202";
    public static final String LYLT_204 = "LYLT_204";

    public static final String LYLT_400 = "LYLT_400";
    public static final String LYLT_401 = "LYLT_401";
    public static final String LYLT_403 = "LYLT_403";
    public static final String LYLT_404 = "LYLT_404";
    public static final String LYLT_409 = "LYLT_409";
    public static final String LYLT_422 = "LYLT_422";
    public static final String LYLT_423 = "LYLT_423";
    public static final String LYLT_429 = "LYLT_429";

    public static final String LYLT_500 = "LYLT_500";
    public static final String LYLT_503 = "LYLT_503";
    public static final String LYLT_504 = "LYLT_504";
}
