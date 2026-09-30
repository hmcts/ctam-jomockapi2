package uk.gov.hmcts.ctam.jo.filters;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.ctam.jo.testsupport.RequestFixtures.servletRequest;

class RequestPathsTest {

    @Test
    void usesServletPathWhenPathInfoIsNull() {
        MockHttpServletRequest request = servletRequest("/api/v1/reference_data/x");

        assertThat(RequestPaths.path(request)).isEqualTo("/api/v1/reference_data/x");
    }

    @Test
    void appendsPathInfoWhenPresent() {
        MockHttpServletRequest request = servletRequest("/api/v1/reference_data/x", "/api");
        request.setPathInfo("/v1/reference_data/x");

        assertThat(RequestPaths.path(request)).isEqualTo("/api/v1/reference_data/x");
    }

    @Test
    void ignoresTheRawRequestUri() {
        MockHttpServletRequest request = servletRequest("/%61pi/v1/reference_data/x", "/api/v1/reference_data/x");

        assertThat(RequestPaths.path(request)).isEqualTo("/api/v1/reference_data/x");
    }
}
