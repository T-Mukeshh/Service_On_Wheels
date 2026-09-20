package com.serviceonwheels.auth_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import org.mockito.Mock;

class RateLimitFilterTest {

    private RateLimitFilter rateLimitFilter;

    @Mock
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @Mock
    private org.springframework.data.redis.core.ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        org.mockito.MockitoAnnotations.openMocks(this);
        rateLimitFilter = new RateLimitFilter(redisTemplate);
        
        // Setup mock behavior to throw exception so fallback logic is used
        lenient().when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis unavailable"));
    }

    @Test
    void testRateLimitAllowsRequestsWithinLimit() throws ServletException, IOException {
        FilterChain filterChain = mock(FilterChain.class);
        
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/auth/login");
            request.setRemoteAddr("192.168.1.1");
            
            MockHttpServletResponse response = new MockHttpServletResponse();
            
            rateLimitFilter.doFilterInternal(request, response, filterChain);
            
            assertEquals(200, response.getStatus(), "Status should be 200 OK within the limit");
        }
        
        verify(filterChain, times(5)).doFilter(any(), any());
    }

    @Test
    void testRateLimitBlocksRequestsExceedingLimit() throws ServletException, IOException {
        FilterChain filterChain = mock(FilterChain.class);
        String ip = "192.168.1.2";
        
        // Consume 10 requests (the limit)
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/auth/login");
            request.setRemoteAddr(ip);
            MockHttpServletResponse response = new MockHttpServletResponse();
            rateLimitFilter.doFilterInternal(request, response, filterChain);
            assertEquals(200, response.getStatus());
        }
        
        // 11th request should be blocked
        MockHttpServletRequest blockedRequest = new MockHttpServletRequest();
        blockedRequest.setRequestURI("/api/auth/login");
        blockedRequest.setRemoteAddr(ip);
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        
        rateLimitFilter.doFilterInternal(blockedRequest, blockedResponse, filterChain);
        
        assertEquals(429, blockedResponse.getStatus(), "Status should be 429 Too Many Requests");
        assertTrue(blockedResponse.getContentAsString().contains("Too many requests"));
        
        verify(filterChain, times(10)).doFilter(any(), any()); // Only 10 made it through
    }

    @Test
    void testRateLimitIgnoresNonAuthEndpoints() throws ServletException, IOException {
        FilterChain filterChain = mock(FilterChain.class);
        
        for (int i = 0; i < 15; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/users/profile");
            request.setRemoteAddr("192.168.1.3");
            
            MockHttpServletResponse response = new MockHttpServletResponse();
            
            rateLimitFilter.doFilterInternal(request, response, filterChain);
            
            assertEquals(200, response.getStatus());
        }
        
        // The limit is 10, but since it's not /api/auth/, all 15 should pass
        verify(filterChain, times(15)).doFilter(any(), any());
    }
}
