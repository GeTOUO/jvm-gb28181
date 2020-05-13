package com.getouo.gb.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;

@Validated
@Component
@ConfigurationProperties(prefix = "gb.sip")
public class PlatConfiguration {

    @NotEmpty(message = "服务器id未配置")
    private String id;

    private Short port = 5060;

    private String ip = "0.0.0.0";

    @NotEmpty(message = "mediaServerIp 不能为空")
    private String mediaServerIp;

    @NotEmpty(message = "mediaServerPort 不能为空")
    private String mediaServerPort;


    @NotEmpty(message = "sipDeviceKey 不能为空")
    private String sipDeviceKey;

    private Integer mediaUdpTcpServerPort;

    public Integer getMediaUdpTcpServerPort() {
        return mediaUdpTcpServerPort;
    }

    public void setMediaUdpTcpServerPort(Integer mediaUdpTcpServerPort) {
        this.mediaUdpTcpServerPort = mediaUdpTcpServerPort;
    }

    public String getSipDeviceKey() {
        return sipDeviceKey;
    }

    public void setSipDeviceKey(String sipDeviceKey) {
        this.sipDeviceKey = sipDeviceKey;
    }

    public String getId() {
        return id;
    }

    public void setId(String serverId) {
        this.id = serverId;
    }

    public Integer getPort() {
        return Integer.valueOf(port);
    }

    public void setPort(Short serverPort) {
        this.port = serverPort;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String serverIp) {
        this.ip = serverIp;
    }

    public String getMediaServerIp() {
        return mediaServerIp;
    }

    public void setMediaServerIp(String mediaServerIp) {
        this.mediaServerIp = mediaServerIp;
    }

    public String getMediaServerPort() {
        return mediaServerPort;
    }

    public void setMediaServerPort(String mediaServerPort) {
        this.mediaServerPort = mediaServerPort;
    }
}
