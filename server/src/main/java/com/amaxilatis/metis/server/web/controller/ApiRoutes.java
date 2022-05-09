package com.amaxilatis.metis.server.web.controller;

public interface ApiRoutes {
    
    String API_V1 = "/v1";
    
    String IMAGE_DIR_HASH = "imageDirectoryHash";
    String IMAGE_HASH = "imageHash";
    String REPORT_ID = "reportId";
    
    String API_BASE = API_V1 + "/api";
    String API_POOL = API_BASE + "/pool";
    String API_SCAN_IMAGES = API_BASE + "/imageScan";
    String API_IMAGE = API_BASE + "/image";
    String API_THUMBNAIL = API_BASE + "/thumbnail";
    String API_IMAGE_DIRECTORY = API_IMAGE + "/{" + IMAGE_DIR_HASH + "}";
    String API_THUMBNAIL_DIRECTORY = API_THUMBNAIL + "/{" + IMAGE_DIR_HASH + "}";
    String API_IMAGE_DIRECTORY_IMAGE = API_IMAGE_DIRECTORY + "/{" + IMAGE_HASH + "}";
    String API_THUMBNAIL_DIRECTORY_IMAGE = API_THUMBNAIL_DIRECTORY + "/{" + IMAGE_HASH + "}";
    String API_IMAGE_DIRECTORY_IMAGE_DOWNLOAD = API_IMAGE_DIRECTORY_IMAGE + "/download";
    String API_REPORTS = API_BASE + "/reports";
    String API_REPORT = API_BASE + "/report";
    String API_REPORT_DOWNLOAD = API_REPORT + "/{" + REPORT_ID + "}/download";
    String API_REPORT_DELETE = API_REPORT + "/{" + REPORT_ID + "}/delete";
    
    String VIEW_HOME = "/";
    String VIEW_SETTINGS = "/settings";
    String VIEW_IMAGE_DIRECTORY = "/view/{" + IMAGE_DIR_HASH + "}";
    
    String ACTION_RUN = "/run";
    String ACTION_CLEAN = "/clean";
    
    String VIEW_LOGIN = "/login";
}
