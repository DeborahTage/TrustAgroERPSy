package com.trustagro.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void usesIncomingRequestIdHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "client-req-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            assertEquals("client-req-123", MDC.get(CorrelationIdFilter.MDC_KEY));
        };

        filter.doFilter(request, response, chain);

        assertEquals("client-req-123", response.getHeader(CorrelationIdFilter.HEADER));
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    @Test
    void generatesUuidWhenHeaderMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        final String[] seen = new String[1];
        FilterChain chain = (req, res) -> {
            seen[0] = MDC.get(CorrelationIdFilter.MDC_KEY);
            assertNotNull(seen[0]);
            assertFalse(seen[0].isBlank());
        };

        filter.doFilter(request, response, chain);

        assertEquals(seen[0], response.getHeader(CorrelationIdFilter.HEADER));
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }
}
