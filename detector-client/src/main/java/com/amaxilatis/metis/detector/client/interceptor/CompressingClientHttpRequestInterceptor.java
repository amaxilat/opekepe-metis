package com.amaxilatis.metis.detector.client.interceptor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

public class CompressingClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {
    private static final String COMPRESSION = "gzip";
    
    public static byte[] compress(byte[] body) throws IOException {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (final GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
            gzipOutputStream.write(body);
        }
        return byteArrayOutputStream.toByteArray();
    }
    
    public ClientHttpResponse intercept(HttpRequest req, byte[] body, ClientHttpRequestExecution exec) throws IOException {
        HttpHeaders httpHeaders = req.getHeaders();
        httpHeaders.add(HttpHeaders.CONTENT_ENCODING, COMPRESSION);
        httpHeaders.add(HttpHeaders.ACCEPT_ENCODING, COMPRESSION);
        return exec.execute(req, compress(body));
    }
}
