package uk.gov.hmcts.ctam.jo.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import uk.gov.hmcts.ctam.jo.util.LogSanitiser;

import java.io.IOException;

/**
 * The single place Spring MVC exceptions become HTTP errors (Principle IX). Every body is written
 * directly by {@link ErrorResponseFactory}, so it is always the shared JSON shape whatever the caller's
 * {@code Accept} header says.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ErrorResponseFactory errorResponseFactory;

    @ExceptionHandler({
        UnsupportedReferenceDataTypeException.class,
        InvalidReferenceIdException.class,
        UnsupportedQueryParameterException.class
    })
    public void handleBadRequest(ApiRequestException ex, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        reject(request, response, HttpStatus.BAD_REQUEST, ex.getMessage(), ex.getDiagnostic());
    }

    @ExceptionHandler(ReferenceDataNotFoundException.class)
    public void handleRecordNotFound(ReferenceDataNotFoundException ex, HttpServletRequest request,
                                     HttpServletResponse response) throws IOException {
        reject(request, response, HttpStatus.NOT_FOUND, ex.getMessage(), ex.getDiagnostic());
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public void handleNoRoute(Exception ex, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        reject(request, response, HttpStatus.NOT_FOUND, ErrorMessages.RESOURCE_NOT_FOUND, "no matching route");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public void handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest request,
                                       HttpServletResponse response) throws IOException {
        String[] supported = ex.getSupportedMethods();
        if (supported != null && supported.length > 0) {
            response.setHeader(HttpHeaders.ALLOW, StringUtils.arrayToCommaDelimitedString(supported));
        }
        reject(request, response, HttpStatus.METHOD_NOT_ALLOWED, ErrorMessages.METHOD_NOT_ALLOWED,
               "method not supported");
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public void handleNotAcceptable(HttpMediaTypeNotAcceptableException ex, HttpServletRequest request,
                                    HttpServletResponse response) throws IOException {
        reject(request, response, HttpStatus.NOT_ACCEPTABLE, ErrorMessages.NOT_ACCEPTABLE,
               "Accept excludes application/json");
    }

    @ExceptionHandler(Exception.class)
    public void handleUnexpected(Exception ex, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        log.error("Unhandled exception: status=500 method={} path={}",
                  LogSanitiser.sanitise(request.getMethod()), LogSanitiser.sanitise(request.getRequestURI()), ex);
        if (!response.isCommitted()) {
            errorResponseFactory.write(request, response, HttpStatus.INTERNAL_SERVER_ERROR,
                                       ErrorMessages.INTERNAL_SERVER_ERROR);
        }
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, HttpStatus status,
                        String message, String detail) throws IOException {
        log.warn("Request rejected: status={} error=\"{}\" method={} path={} detail={}",
                 status.value(), message, LogSanitiser.sanitise(request.getMethod()),
                 LogSanitiser.sanitise(request.getRequestURI()), detail);
        errorResponseFactory.write(request, response, status, message);
    }
}
