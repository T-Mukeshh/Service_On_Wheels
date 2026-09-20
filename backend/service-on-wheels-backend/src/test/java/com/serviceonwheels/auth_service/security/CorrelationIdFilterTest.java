package com.serviceonwheels.auth_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void doFilter_withValidCorrelationIdHeader_usesHeaderValue() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(CorrelationIdContext.CORRELATION_ID_HEADER, "custom-trace-12345");

        AtomicReference<String> mdcValueDuringFilter = new AtomicReference<>();
        FilterChain chain = (req, res) -> {
            mdcValueDuringFilter.set(MDC.get(CorrelationIdContext.MDC_KEY));
        };

        filter.doFilter(request, response, chain);

        assertEquals("custom-trace-12345", mdcValueDuringFilter.get());
        assertEquals("custom-trace-12345", response.getHeader(CorrelationIdContext.CORRELATION_ID_HEADER));
        assertNull(MDC.get(CorrelationIdContext.MDC_KEY), "MDC must be cleared after filter completion");
    }

    @Test
    void doFilter_withoutCorrelationIdHeader_generatesUuid() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> mdcValueDuringFilter = new AtomicReference<>();
        FilterChain chain = (req, res) -> {
            mdcValueDuringFilter.set(MDC.get(CorrelationIdContext.MDC_KEY));
        };

        filter.doFilter(request, response, chain);

        String capturedId = mdcValueDuringFilter.get();
        assertNotNull(capturedId);
        assertFalse(capturedId.isBlank());
        assertEquals(capturedId, response.getHeader(CorrelationIdContext.CORRELATION_ID_HEADER));
        assertNull(MDC.get(CorrelationIdContext.MDC_KEY), "MDC must be cleared after filter completion");
    }

    @Test
    void doFilter_withInvalidCorrelationIdHeader_generatesSafeFallbackUuid() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        // Header contains special / dangerous characters that should be rejected
        request.addHeader(CorrelationIdContext.CORRELATION_ID_HEADER, "invalid id with spaces & <script>");

        AtomicReference<String> mdcValueDuringFilter = new AtomicReference<>();
        FilterChain chain = (req, res) -> {
            mdcValueDuringFilter.set(MDC.get(CorrelationIdContext.MDC_KEY));
        };

        filter.doFilter(request, response, chain);

        String capturedId = mdcValueDuringFilter.get();
        assertNotNull(capturedId);
        assertNotEquals("invalid id with spaces & <script>", capturedId);
        assertEquals(capturedId, response.getHeader(CorrelationIdContext.CORRELATION_ID_HEADER));
    }
}
