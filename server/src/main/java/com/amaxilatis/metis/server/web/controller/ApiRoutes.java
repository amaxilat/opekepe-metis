package com.amaxilatis.metis.server.web.controller;

public interface ApiRoutes {
    
    String API_V1 = "/v1";
    
    String IMAGE_DIR_HASH = "imageDirectoryHash";
    String IMAGE_HASH = "imageHash";
    String REPORT_ID = "reportId";
    
    String API_BASE = API_V1 + "/api";
    String API_POOL = API_BASE + "/pool";
    String API_BACKUP = API_BASE + "/backup";
    String API_SCAN_IMAGES = API_BASE + "/imageScan";
    String API_IMAGE = API_BASE + "/image";
    String API_THUMBNAIL = API_BASE + "/thumbnail";
    String API_HISTOGRAM = API_BASE + "/histogram";
    String API_COLOR_BALANCE = API_BASE + "/colorbalance";
    String API_NIR = API_BASE + "/nir";
    String API_NDWI = API_BASE + "/ndwi";
    String API_BSI = API_BASE + "/bsi";
    String API_WATER = API_BASE + "/water";
    String API_CLOUD_COVER = API_BASE + "/cloudcover";
    String API_IMAGE_DIRECTORY = API_IMAGE + "/{" + IMAGE_DIR_HASH + "}";
    String API_THUMBNAIL_DIRECTORY = API_THUMBNAIL + "/{" + IMAGE_DIR_HASH + "}";
    String API_HISTOGRAM_DIRECTORY = API_HISTOGRAM + "/{" + IMAGE_DIR_HASH + "}";
    String API_COLOR_BALANCE_DIRECTORY = API_COLOR_BALANCE + "/{" + IMAGE_DIR_HASH + "}";
    String API_NIR_DIRECTORY = API_NIR + "/{" + IMAGE_DIR_HASH + "}";
    String API_NDWI_DIRECTORY = API_NDWI + "/{" + IMAGE_DIR_HASH + "}";
    String API_BSI_DIRECTORY = API_BSI + "/{" + IMAGE_DIR_HASH + "}";
    String API_WATER_DIRECTORY = API_WATER + "/{" + IMAGE_DIR_HASH + "}";
    String API_CLOUD_COVER_DIRECTORY = API_CLOUD_COVER + "/{" + IMAGE_DIR_HASH + "}";
    String API_IMAGE_DIRECTORY_IMAGE = API_IMAGE_DIRECTORY + "/{" + IMAGE_HASH + "}";
    String API_THUMBNAIL_DIRECTORY_IMAGE = API_THUMBNAIL_DIRECTORY + "/{" + IMAGE_HASH + "}";
    String API_HISTOGRAM_DIRECTORY_IMAGE = API_HISTOGRAM_DIRECTORY + "/{" + IMAGE_HASH + "}";
    String API_COLOR_BALANCE_DIRECTORY_IMAGE = API_COLOR_BALANCE_DIRECTORY + "/{" + IMAGE_HASH + "}";
    String API_NIR_DIRECTORY_IMAGE = API_NIR_DIRECTORY + "/{" + IMAGE_HASH + "}";
    String API_NDWI_DIRECTORY_IMAGE = API_NDWI_DIRECTORY + "/{" + IMAGE_HASH + "}";
    String API_BSI_DIRECTORY_IMAGE = API_BSI_DIRECTORY + "/{" + IMAGE_HASH + "}";
    String API_WATER_DIRECTORY_IMAGE = API_WATER_DIRECTORY + "/{" + IMAGE_HASH + "}";
    
    String API_CLOUD_COVER_DIRECTORY_IMAGE = API_CLOUD_COVER_DIRECTORY + "/{" + IMAGE_HASH + "}";
    String API_REPORTS = API_BASE + "/reports";
    String API_REPORT = API_BASE + "/report";
    String API_REPORT_DOWNLOAD = API_REPORT + "/{" + REPORT_ID + "}/download";
    String API_REPORT_TASKS_CANCEL = API_REPORT + "/{" + REPORT_ID + "}/tasks/cancel";
    String API_DIRECTORY_REPORT_DOWNLOAD = API_IMAGE + "/{" + IMAGE_DIR_HASH + "}/report";
    String API_REPORT_DELETE = API_REPORT + "/{" + REPORT_ID + "}/delete";
    
    String VIEW_HOME = "/";
    
    String VIEW_USER = "/user";
    String VIEW_LOG = "/log";
    String VIEW_TASKS = "/tasks";
    String VIEW_SETTINGS = "/settings";
    String VIEW_IMAGE_DIRECTORY = "/view";
    
    String ACTION_RUN = "/run";
    String ACTION_CLEAN = "/clean";
    
    String VIEW_LOGIN = "/login";
    
    String ACTION_SAVE_CONFIGURATION = "/save/configuration";
}
