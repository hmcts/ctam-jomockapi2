package uk.gov.hmcts.ctam.jo.filters;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import uk.gov.hmcts.ctam.jo.exceptions.RepeatedQueryParameterException;
import uk.gov.hmcts.ctam.jo.exceptions.UndefinedQueryParameterException;

import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rejects a query parameter that the matched route doesn't declare (FR-015), or one given more than once (FR-016),
 * so no parameter is ever accepted and then silently ignored. A route declares its parameters with
 * {@link RequestParam}; this class names no route, so each paginated route only registers its path (research R5).
 * Repeats are checked first. Only the query string is read, never a form body. The log diagnostic names the offending
 * parameters, never their values. Only matched controller routes are
 * checked, so an unmatched path still gets the route's {@code 404}.
 */
@Component
public class DeclaredQueryParametersInterceptor implements HandlerInterceptor {

    private static final ParameterNameDiscoverer PARAMETER_NAMES = new DefaultParameterNameDiscoverer();

    private final Map<Method, Set<String>> declaredByMethod = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        String query = request.getQueryString();
        if (query == null || query.isEmpty()) {
            return true;
        }
        Map<String, Integer> counts = countNames(query);
        List<String> repeated = counts.entrySet().stream().filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey).sorted().toList();
        if (!repeated.isEmpty()) {
            throw new RepeatedQueryParameterException("repeated " + repeated);
        }
        Set<String> declared = declaredNames(method);
        List<String> undefined = counts.keySet().stream().filter(name -> !declared.contains(name)).sorted().toList();
        if (!undefined.isEmpty()) {
            throw new UndefinedQueryParameterException("undefined " + undefined);
        }
        return true;
    }

    /**
     * How often each parameter name appears in the query string; a name with an empty value ({@code a=}) or none
     * ({@code a}) is present.
     */
    private static Map<String, Integer> countNames(String query) {
        Map<String, Integer> counts = new HashMap<>();
        for (String pair : query.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int equals = pair.indexOf('=');
            String rawName = equals < 0 ? pair : pair.substring(0, equals);
            counts.merge(decode(rawName), 1, Integer::sum);
        }
        return counts;
    }

    /**
     * The decoded name; a malformed escape is kept as written, so it can only ever be an undeclared name.
     */
    private static String decode(String rawName) {
        try {
            return URLDecoder.decode(rawName, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException malformed) {
            return rawName;
        }
    }

    /**
     * The query parameters a route declares: each {@link RequestParam}'s name or value, or, when it names neither, the
     * Java parameter's name, as Spring binds it. Fixed for each method, so read once.
     */
    private Set<String> declaredNames(HandlerMethod method) {
        return declaredByMethod.computeIfAbsent(method.getMethod(), unused -> {
            Set<String> names = new HashSet<>();
            for (MethodParameter parameter : method.getMethodParameters()) {
                RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
                if (requestParam != null) {
                    names.add(nameOf(requestParam, parameter));
                }
            }
            return Set.copyOf(names);
        });
    }

    private static String nameOf(RequestParam requestParam, MethodParameter parameter) {
        if (!requestParam.name().isEmpty()) {
            return requestParam.name();
        }
        if (!requestParam.value().isEmpty()) {
            return requestParam.value();
        }
        parameter.initParameterNameDiscovery(PARAMETER_NAMES);
        return parameter.getParameterName();
    }
}
