package com.amaxilatis.metis.server.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Getter
@Component
@RequiredArgsConstructor
@PropertySource({"classpath:git.properties"})
public class BuildVersionConfigurationProperties {
    @Value("${git.build.version:0.0.0}")
    private String buildVersion;
    @Value("${git.build.host:hostname}")
    private String buildHost;
    @Value("${git.build.time:none}")
    private String buildDate;
    @Value("${git.branch:none}")
    private String buildBranch;
    @Value("${git.commit.id:none}")
    private String buildCommit;
    @Value("${build.jenkinsHash:none}")
    private String jenkinsHash;
}
