package uk.gov.hmcts.ctam.jo.filters;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;
import uk.gov.hmcts.ctam.jo.exceptions.RepeatedQueryParameterException;
import uk.gov.hmcts.ctam.jo.exceptions.UndefinedQueryParameterException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * FR-015 and FR-016 for any route: undeclared and repeated query parameters are rejected, repeats first.
 */
class DeclaredQueryParametersInterceptorTest {

    private final DeclaredQueryParametersInterceptor interceptor = new DeclaredQueryParametersInterceptor();

    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @SuppressWarnings("unused")
    static class TestController {

        String declaring(@RequestParam(name = "a", required = false) String a, @RequestParam("b") String b,
                         String notAQueryParameter) {
            return a + b + notAQueryParameter;
        }

        String declaringNone() {
            return "";
        }

        String declaringByJavaName(@RequestParam(required = false) String page) {
            return page;
        }
    }

    private static HandlerMethod method(String name) throws NoSuchMethodException {
        TestController controller = new TestController();
        return name.equals("declaring")
                ? new HandlerMethod(controller, TestController.class.getDeclaredMethod(name, String.class, String.class,
                                                                                         String.class))
                : new HandlerMethod(controller, TestController.class.getDeclaredMethod(name));
    }

    private static MockHttpServletRequest request(String query) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.setQueryString(query);
        return request;
    }

    @ParameterizedTest
    @ValueSource(strings = {"a=1&b=2", "a=1", "a=", "b", "a=1&b=", "a=1&&b=2"})
    void declaredParametersPass(String query) throws Exception {
        assertThat(interceptor.preHandle(request(query), response, method("declaring"))).isTrue();
    }

    @Test
    void noQueryPasses() throws Exception {
        assertThat(interceptor.preHandle(request(null), response, method("declaring"))).isTrue();
        assertThat(interceptor.preHandle(request(""), response, method("declaringNone"))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"c=1", "a=1&c=", "notAQueryParameter=1", "%zz=1", "a%3D1=2"})
    void anUndeclaredParameterIsRejected(String query) throws Exception {
        assertThatThrownBy(() -> interceptor.preHandle(request(query), response, method("declaring")))
                .isInstanceOf(UndefinedQueryParameterException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"a=1&a=2", "a=1&b=2&a=3", "c=1&c=2", "a=&a=", "a=1&%61=2"})
    void repeatedParameterIsRejectedFirst(String query) throws Exception {
        assertThatThrownBy(() -> interceptor.preHandle(request(query), response, method("declaring")))
                .isInstanceOf(RepeatedQueryParameterException.class);
    }

    // A @RequestParam naming neither name nor value is bound by its Java parameter name, so that name is declared.
    @Test
    void parameterNamedOnlyByItsJavaNameIsDeclared() throws Exception {
        HandlerMethod method = new HandlerMethod(new TestController(),
                TestController.class.getDeclaredMethod("declaringByJavaName", String.class));

        assertThat(interceptor.preHandle(request("page=2"), response, method)).isTrue();
        assertThatThrownBy(() -> interceptor.preHandle(request("per_page=2"), response, method))
                .isInstanceOf(UndefinedQueryParameterException.class);
    }

    @Test
    void theLogDiagnosticNamesTheParametersButNeverTheirValues() throws Exception {
        assertThatThrownBy(() -> interceptor.preHandle(request("a=1&c=secret-value&d=other"), response,
                                                       method("declaring")))
                .isInstanceOfSatisfying(UndefinedQueryParameterException.class, ex -> assertThat(ex.getDiagnostic())
                        .isEqualTo("undefined [c, d]").doesNotContain("secret-value"));
        assertThatThrownBy(() -> interceptor.preHandle(request("a=secret-value&a=2"), response, method("declaring")))
                .isInstanceOfSatisfying(RepeatedQueryParameterException.class, ex -> assertThat(ex.getDiagnostic())
                        .isEqualTo("repeated [a]").doesNotContain("secret-value"));
    }

    @Test
    void anyParameterOnARouteDeclaringNoneIsRejected() throws Exception {
        assertThatThrownBy(() -> interceptor.preHandle(request("a=1"), response, method("declaringNone")))
                .isInstanceOf(UndefinedQueryParameterException.class);
    }

    @Test
    void formBodyIsNotConsidered() throws Exception {
        MockHttpServletRequest request = request("a=1");
        request.addParameter("a", "1");
        request.addParameter("c", "2");

        assertThat(interceptor.preHandle(request, response, method("declaring"))).isTrue();
    }

    @Test
    void handlerThatIsNotAControllerMethodPassesUntouched() throws Exception {
        assertThat(interceptor.preHandle(request("c=1&c=2"), response, new Object())).isTrue();
    }
}
