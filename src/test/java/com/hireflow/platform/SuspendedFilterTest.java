package com.hireflow.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class SuspendedFilterTest {

    private final SuspendedCompanies suspended = mock(SuspendedCompanies.class);
    private final SuspendedFilter filter = new SuspendedFilter(suspended);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(Long companyId, String role) {
        var auth = new UsernamePasswordAuthenticationToken("x@x.com", null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        auth.setDetails(companyId);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private MockFilterChain run(MockHttpServletResponse response) throws Exception {
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(new MockHttpServletRequest(), response, chain);
        return chain;
    }

    @Test
    void suspendedCompanyIsBlocked() throws Exception {
        when(suspended.contains(5L)).thenReturn(true);
        loginAs(5L, "ADMIN");
        MockHttpServletResponse response = new MockHttpServletResponse();

        MockFilterChain chain = run(response);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("suspended");
        assertThat(chain.getRequest()).isNull();   // the request never reached the app
    }

    @Test
    void activeCompanyPasses() throws Exception {
        when(suspended.contains(5L)).thenReturn(false);
        loginAs(5L, "ADMIN");

        MockFilterChain chain = run(new MockHttpServletResponse());

        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void platformOwnerIsNeverBlocked() throws Exception {
        when(suspended.contains(5L)).thenReturn(true);
        loginAs(5L, "PLATFORM_ADMIN");

        MockFilterChain chain = run(new MockHttpServletResponse());

        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void requestWithoutLoginPasses() throws Exception {
        MockFilterChain chain = run(new MockHttpServletResponse());

        assertThat(chain.getRequest()).isNotNull();
    }
}