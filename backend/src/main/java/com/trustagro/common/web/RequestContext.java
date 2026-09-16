package com.trustagro.common.web;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Lightweight client IP extraction from the current HTTP request when present.
 */
public final class RequestContext {

    private RequestContext() {}

    public static String currentIpAddress() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            HttpServletRequest request = attrs.getRequest();
            if (request == null) {
                return null;
            }
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                String first = forwarded.split(",")[0].trim();
                return truncate(first, 64);
            }
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                return truncate(realIp.trim(), 64);
            }
            return truncate(request.getRemoteAddr(), 64);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
