package uk.gov.hmcts.ctam.jo.filters;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import uk.gov.hmcts.ctam.jo.exceptions.UnsupportedQueryParameterException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NoQueryParametersInterceptorTest {

    private final NoQueryParametersInterceptor interceptor = new NoQueryParametersInterceptor();

    private final MockHttpServletResponse response = new MockHttpServletResponse();

    private final HandlerMethod handlerMethod = handlerMethod();

    @ParameterizedTest
    @ValueSource(strings = {"page=2", "name=", "a"})
    void rejectsAnyQueryStringOnAControllerRoute(String query) {
        MockHttpServletRequest request = request(query);

        assertThatThrownBy(() -> interceptor.preHandle(request, response, handlerMethod))
                .isInstanceOf(UnsupportedQueryParameterException.class)
                .hasMessage("Query parameters are not supported on this endpoint.");
    }

    @Test
    void keepsASanitisedQueryStringForLogs() {
        MockHttpServletRequest request = request("a=1\r\nforged");

        assertThatThrownBy(() -> interceptor.preHandle(request, response, handlerMethod))
                .isInstanceOfSatisfying(UnsupportedQueryParameterException.class,
                    ex -> assertThat(ex.getDiagnostic()).isEqualTo("a=1\\r\\nforged"));
    }

    @Test
    void allowsAControllerRouteWithoutAQueryString() throws Exception {
        assertThat(interceptor.preHandle(request(null), response, handlerMethod)).isTrue();
        assertThat(interceptor.preHandle(request(""), response, handlerMethod)).isTrue();
    }

    @Test
    void ignoresHandlersThatAreNotControllerMethods() throws Exception {
        // An unmatched path falls through to the static-resource handler; route checks come first (404, not 400).
        assertThat(interceptor.preHandle(request("page=2"), response, new ResourceHttpRequestHandler())).isTrue();
    }

    private static MockHttpServletRequest request(String query) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/reference_data/x");
        request.setQueryString(query);
        return request;
    }

    private static HandlerMethod handlerMethod() {
        try {
            return new HandlerMethod(new Object(), Object.class.getMethod("toString"));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }
}
