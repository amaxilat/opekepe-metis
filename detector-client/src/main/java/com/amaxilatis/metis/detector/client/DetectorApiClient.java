package com.amaxilatis.metis.detector.client;

import com.amaxilatis.metis.detector.client.dto.*;
import com.amaxilatis.metis.detector.client.interceptor.CompressingClientHttpRequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
public class DetectorApiClient implements DetectorApiClientInterface {
    
    private final String baseUrl;
    final RestTemplate restTemplate;
    
    public DetectorApiClient() {
        this("http://localhost:5000");
    }
    
    public DetectorApiClient(final String baseUrl) {
        this.baseUrl = baseUrl;
        restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(new CompressingClientHttpRequestInterceptor());
    }
    
    
    public DetectionsDTO postData(final DataDTO data) {
        final HttpEntity<DataDTO> httpEntity = new HttpEntity<>(data);
        final ResponseEntity<DetectionsDTO> response = restTemplate.exchange(baseUrl + API_TILE, HttpMethod.POST, httpEntity, DetectionsDTO.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RestClientException(response.toString());
        }
        return response.getBody();
    }
    
    public DetectionsListDTO postData(final List<DataDTO> data) {
        final HttpEntity<DataListDTO> httpEntity = new HttpEntity<>(new DataListDTO(data));
        final ResponseEntity<DetectionsListDTO> response = restTemplate.exchange(baseUrl + API_LIST, HttpMethod.POST, httpEntity, DetectionsListDTO.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RestClientException(response.toString());
        }
        return response.getBody();
    }
    
    public ImageDetectionResultDTO checkImageFile(final String image, final String mask) {
        final HttpEntity<DetectionRequestDTO> httpEntity = new HttpEntity<>(new DetectionRequestDTO(image, mask));
        final ResponseEntity<ImageDetectionResultDTO> response = restTemplate.exchange(baseUrl + API_IMAGE, HttpMethod.POST, httpEntity, ImageDetectionResultDTO.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RestClientException(response.toString());
        }
        return response.getBody();
    }
    
    public PingDataDTO getPingData() {
        try {
            final ResponseEntity<PingDataDTO> response = restTemplate.getForEntity(baseUrl + API_PING, PingDataDTO.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RestClientException(response.toString());
            }
            return response.getBody();
        } catch (NullPointerException e) {
            log.error(e.getMessage());
            return new PingDataDTO();
        }
    }
}
