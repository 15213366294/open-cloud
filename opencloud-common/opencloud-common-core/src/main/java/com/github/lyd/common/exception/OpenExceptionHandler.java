package com.github.lyd.common.exception;

import com.github.lyd.common.constants.ResultEnum;
import com.github.lyd.common.model.ResultBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.common.exceptions.*;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 统一异常管理
 *
 * @author LYD
 * @date 2017/7/3
 */
@ControllerAdvice
@ResponseBody
@Slf4j
public class OpenExceptionHandler {

    private static final String KEY = "x.servlet.exception.code";

    /**
     * 统一异常处理
     * AuthenticationException
     *
     * @param ex
     * @param request
     * @param response
     * @return
     */
    @ExceptionHandler({AuthenticationException.class})
    public static ResultBody authenticationException(Exception ex, HttpServletRequest request, HttpServletResponse response) {
        ResultEnum code = ResultEnum.ERROR;
        if (ex instanceof UsernameNotFoundException) {
            code = ResultEnum.USERNAME_NOT_FOUND;
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        } else if (ex instanceof BadCredentialsException) {
            code = ResultEnum.BAD_CREDENTIALS;
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        } else if (ex instanceof AccountExpiredException) {
            code = ResultEnum.ACCOUNT_EXPIRED;
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        } else if (ex instanceof LockedException) {
            code = ResultEnum.ACCOUNT_LOCKED;
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        } else if (ex instanceof DisabledException) {
            code = ResultEnum.ACCOUNT_DISABLED;
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        } else if (ex instanceof CredentialsExpiredException) {
            code = ResultEnum.CREDENTIALS_EXPIRED;
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        } else if (ex instanceof InsufficientAuthenticationException) {
            code = ResultEnum.UNAUTHORIZED;
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        }
        //放入请求域
        request.setAttribute(KEY, code);
        return buildBody(ex, request, response);
    }

    /**
     * OAuth2Exception
     *
     * @param ex
     * @param request
     * @param response
     * @return
     */
    @ExceptionHandler({OAuth2Exception.class, InvalidTokenException.class})
    public static ResultBody oauth2Exception(Exception ex, HttpServletRequest request, HttpServletResponse response) {
        ResultEnum code = ResultEnum.UNAUTHORIZED;
        if (ex instanceof InvalidClientException) {
            code = ResultEnum.INVALID_CLIENT;
        } else if (ex instanceof UnauthorizedClientException) {
            code = ResultEnum.UNAUTHORIZED_CLIENT;
        } else if (ex instanceof InvalidGrantException) {
            code = ResultEnum.INVALID_GRANT;
            if ("Bad credentials".equals(ex.getMessage())) {
                code = ResultEnum.BAD_CREDENTIALS;
            }
            if ("User is disabled".equals(ex.getMessage())) {
                code = ResultEnum.ACCOUNT_DISABLED;
            }
            if ("User account is locked".equals(ex.getMessage())) {
                code = ResultEnum.ACCOUNT_LOCKED;
            }
        } else if (ex instanceof InvalidScopeException) {
            code = ResultEnum.INVALID_SCOPE;
        } else if (ex instanceof InvalidTokenException) {
            code = ResultEnum.INVALID_TOKEN;
        } else if (ex instanceof InvalidRequestException) {
            code = ResultEnum.INVALID_REQUEST;
        } else if (ex instanceof RedirectMismatchException) {
            code = ResultEnum.REDIRECT_URI_MISMATCH;
        } else if (ex instanceof UnsupportedGrantTypeException) {
            code = ResultEnum.UNSUPPORTED_GRANT_TYPE;
        } else if (ex instanceof UnsupportedResponseTypeException) {
            code = ResultEnum.UNSUPPORTED_RESPONSE_TYPE;
        } else if (ex instanceof UserDeniedAuthorizationException) {
            code = ResultEnum.ACCESS_DENIED;
        } else {
            code = ResultEnum.INVALID_REQUEST;
        }
        if(code.equals(ResultEnum.ACCESS_DENIED)){
            response.setStatus(HttpStatus.FORBIDDEN.value());
        }else{
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        }
        //放入请求域
        request.setAttribute(KEY, code);
        return buildBody(ex, request, response);
    }

    /**
     * 自定义异常
     *
     * @param ex
     * @param request
     * @param response
     * @return
     */
    @ExceptionHandler({OpenException.class})
    public static ResultBody openException(Exception ex, HttpServletRequest request, HttpServletResponse response) {
        ResultEnum code = ResultEnum.ERROR;
        if (ex instanceof OpenAlertException) {
            code = ResultEnum.ALERT;
        }
        if (ex instanceof OpenSignatureException) {
            code = ResultEnum.SIGNATURE_DENIED;
            response.setStatus(HttpStatus.BAD_REQUEST.value());
        } else {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        //放入请求域
        request.setAttribute(KEY, code);
        return buildBody(ex, request, response);
    }

    /**
     * 其他异常
     *
     * @param ex
     * @param request
     * @param response
     * @return
     */
    @ExceptionHandler({Exception.class})
    public static ResultBody exception(Exception ex, HttpServletRequest request, HttpServletResponse response) {
        ResultEnum code = ResultEnum.ERROR;
        if (ex instanceof HttpMessageNotReadableException || ex instanceof TypeMismatchException || ex instanceof MissingServletRequestParameterException) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            code = ResultEnum.BAD_REQUEST;
        } else if (ex instanceof NoHandlerFoundException) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            code = ResultEnum.NOT_FOUND;
        } else if (ex instanceof HttpRequestMethodNotSupportedException) {
            response.setStatus(HttpStatus.METHOD_NOT_ALLOWED.value());
            code = ResultEnum.METHOD_NOT_ALLOWED;
        } else if (ex instanceof HttpMediaTypeNotAcceptableException) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            code = ResultEnum.MEDIA_TYPE_NOT_ACCEPTABLE;
        } else if (ex instanceof MethodArgumentNotValidException) {
            BindingResult bindingResult = ((MethodArgumentNotValidException) ex).getBindingResult();
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            code = ResultEnum.ALERT;
            return ResultBody.failed(code.getCode(), bindingResult.getFieldError().getDefaultMessage());
        } else if (ex instanceof IllegalArgumentException) {
            //参数错误
            code = ResultEnum.ALERT;
            response.setStatus(HttpStatus.BAD_REQUEST.value());
        } else if (ex instanceof AccessDeniedException) {
            code = ResultEnum.ACCESS_DENIED;
            response.setStatus(HttpStatus.FORBIDDEN.value());
        }
        request.setAttribute(KEY, code);
        return buildBody(ex, request, response);
    }


    /**
     * 解析异常
     *
     * @param ex
     * @param request
     * @param response
     * @return
     */
    public static ResultBody resolveException(Exception ex, HttpServletRequest request, HttpServletResponse response) {
        ResultBody resultBody = null;
        if (ex instanceof AuthenticationException) {
            resultBody = authenticationException(ex, request, response);
        } else if (ex instanceof OAuth2Exception) {
            resultBody = oauth2Exception(ex, request, response);
        } else if (ex instanceof OpenException) {
            resultBody = openException(ex, request, response);
        } else {
            resultBody = exception(ex, request, response);
        }
        return resultBody;
    }

    private static ResultBody buildBody(Exception ex, HttpServletRequest request, HttpServletResponse response) {
        String exception = ex.getMessage();
        String path = request.getRequestURI();
        String method = request.getMethod();
        ResultEnum resultCode = (ResultEnum) request.getAttribute(KEY);
        if (resultCode == null) {
            resultCode = ResultEnum.ERROR;
        }
        //提示消息
        String message = ex.getMessage();
        log.error("错误解析:method={},path={},code={},message={},exception={}", method, path, resultCode.getCode(), message, (ex instanceof OpenException ? ex.getMessage() : ex));
        return ResultBody.failed(resultCode.getCode(), message).setPath(path);
    }


}
