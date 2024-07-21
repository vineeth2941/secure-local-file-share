package com.example.secure.share.dto;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.util.StringUtils;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class DecryptedServletRequest extends HttpServletRequestWrapper {

	private final static String PUBLIC_KEY_HEADER = "public-key";

	private ServletInputStream body;

	public DecryptedServletRequest(HttpServletRequest request) {
		super(request);
	}

	public void setInputStream(InputStream stream) {
		this.body = new ServletInputStream() {
			public int read() throws IOException {
				return stream.read();
			}

			@Override
			public boolean isFinished() {
				try {
					return stream.available() == 0;
				} catch (IOException e) {
					e.printStackTrace();
				}
				return false;
			}

			@Override
			public boolean isReady() {
				try {
					return stream.available() > 0;
				} catch (IOException e) {
					e.printStackTrace();
				}
				return false;
			}

			@Override
			public void setReadListener(ReadListener listener) {				
			}
		};
	}

	public ServletInputStream getInputStream() {
		return this.body;
	}

	public String getPublicKey() {
		String pubKey = this.getHeader(PUBLIC_KEY_HEADER);
		if (!StringUtils.hasText(pubKey)) {
			throw new RuntimeException("Public Key header is missing"); // TODO
		}
		return pubKey;
	}

}
