package com.example.secure.share.interceptor;

import java.io.IOException;
import java.util.Base64;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.ContentCachingResponseWrapper;

import com.example.secure.share.annotation.IgnoreEncryption;
import com.example.secure.share.dto.DecryptedServletRequest;
import com.example.secure.share.service.CryptoService;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(1)
public class SecurityFilter implements Filter {

	@Autowired
	@Qualifier("requestMappingHandlerMapping")
	private RequestMappingHandlerMapping requestMapper;

	@Autowired
	private CryptoService cryptoService;

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		boolean ignoreEncryption = true;
		try {
			HandlerExecutionChain handlerExeChain = requestMapper.getHandler((HttpServletRequest) request);
			if (Objects.nonNull(handlerExeChain)) {
				HandlerMethod handlerMethod = (HandlerMethod) handlerExeChain.getHandler();
				ignoreEncryption = handlerMethod.hasMethodAnnotation(IgnoreEncryption.class);
			}
		} catch (Exception e) {
			throw new ServletException(e);
		}

		if (ignoreEncryption) {
			chain.doFilter(request, response);
			return;
		}

		DecryptedServletRequest req = new DecryptedServletRequest((HttpServletRequest) request);
		ContentCachingResponseWrapper res = new ContentCachingResponseWrapper((HttpServletResponse) response);
		try {
			req.setInputStream(cryptoService.decrypt(req.getPublicKey(), Base64.getDecoder().decode(request.getInputStream().readAllBytes())));
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServletException(e);
		}
		chain.doFilter(req, res);
		try {
			byte[] encryptedResponse = cryptoService.encrypt(req.getPublicKey(), res.getContentAsByteArray());
			String encodedResponse = Base64.getEncoder().encodeToString(encryptedResponse);
			response.setContentLength(encodedResponse.length());
			response.getOutputStream().write(encodedResponse.getBytes());
			response.flushBuffer();
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServletException(e);
		}
	}

}
