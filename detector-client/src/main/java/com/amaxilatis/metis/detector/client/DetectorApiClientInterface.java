package com.amaxilatis.metis.detector.client;

import com.amaxilatis.metis.detector.client.dto.DataDTO;
import com.amaxilatis.metis.detector.client.dto.DetectionsDTO;
import com.amaxilatis.metis.detector.client.dto.DetectionsListDTO;
import com.amaxilatis.metis.detector.client.dto.ImageDetectionResultDTO;
import com.amaxilatis.metis.detector.client.dto.PingDataDTO;

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
