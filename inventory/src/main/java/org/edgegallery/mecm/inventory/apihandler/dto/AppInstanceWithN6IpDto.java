package org.edgegallery.mecm.inventory.apihandler.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 应用实例ID与N6IP关联返回DTO
 * 给前端封装APPID和对应N6IP的专用返参类
 */
@ApiModel(value = "AppInstanceWithN6IpDto", description = "应用实例ID(APPID)和N6网口IP地址关联返回对象")
public class AppInstanceWithN6IpDto {

    @ApiModelProperty(value = "应用实例ID，即APPID", required = true, example = "app-10001")
    private String appInstanceId;

    @ApiModelProperty(value = "边缘主机N6网口IP地址", example = "192.168.10.101")
    private String n6Ip;

    @ApiModelProperty(value = "应用名称", example = "Test App")
    private String appName;

    // 空参构造（框架序列化/反序列化需要）
    public AppInstanceWithN6IpDto() {
    }

    // 全参构造（Service层转换数据用）
    public AppInstanceWithN6IpDto(String appInstanceId, String n6Ip, String appName) {
        this.appInstanceId = appInstanceId;
        this.n6Ip = n6Ip;
        this.appName = appName;
    }

    // Getter & Setter（前端获取字段值/框架反射需要）
    public String getAppInstanceId() {
        return appInstanceId;
    }

    public void setAppInstanceId(String appInstanceId) {
        this.appInstanceId = appInstanceId;
    }

    public String getN6Ip() {
        return n6Ip;
    }

    public void setN6Ip(String n6Ip) {
        this.n6Ip = n6Ip;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }
}