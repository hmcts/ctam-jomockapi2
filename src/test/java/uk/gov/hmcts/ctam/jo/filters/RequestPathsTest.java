package uk.gov.hmcts.ctam.jo.filters;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class RequestPathsTest {

    @Test
    void usesServletPathWhenPathInfoIsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/reference_data/x");

        assertThat(RequestPaths.path(request)).isEqualTo("/api/v1/reference_data/x");
    }

    @Test
    void appendsPathInfoWhenPresent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api");
        request.setPathInfo("/v1/reference_data/x");

        assertThat(RequestPaths.path(request)).isEqualTo("/api/v1/reference_data/x");
    }

    @Test
    void ignoresTheRawRequestUri() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/%61pi/v1/reference_data/x");
        request.setServletPath("/api/v1/reference_data/x");

        assertThat(RequestPaths.path(request)).isEqualTo("/api/v1/reference_data/x");
    }
}
