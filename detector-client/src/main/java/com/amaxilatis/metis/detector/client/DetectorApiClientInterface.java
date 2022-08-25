package com.amaxilatis.metis.detector.client;

import com.amaxilatis.metis.detector.client.dto.DataDTO;
import com.amaxilatis.metis.detector.client.dto.DataListDTO;
import com.amaxilatis.metis.detector.client.dto.DetectionRequestDTO;
import com.amaxilatis.metis.detector.client.dto.DetectionsDTO;
import com.amaxilatis.metis.detector.client.dto.DetectionsListDTO;
import com.amaxilatis.metis.detector.client.dto.ImageDetectionResultDTO;
import com.amaxilatis.metis.detector.client.dto.PingDataDTO;
import com.amaxilatis.metis.detector.client.interceptor.CompressingClientHttpRequestInterceptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

public interface DetectorApiClientInterface {
    
    String API_TILE = "/";
    String API_LIST = "/list";
    String API_IMAGE = "/image";
    String API_PING = "/ping";
    
    DetectionsDTO postData(final DataDTO data);
    
    DetectionsListDTO postData(final List<DataDTO> data);
    
    ImageDetectionResultDTO checkImageFile(final String image, final String mask);
    
    PingDataDTO getPingData();
}
