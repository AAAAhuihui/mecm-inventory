/*
 *  Copyright 2020 Huawei Technologies Co., Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.edgegallery.mecm.inventory.apihandler.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class SignalingPolicyRequest {

    @NotBlank(message = "App ID is required")
    private String appId;

    @NotBlank(message = "DNAI is required")
    private String dnai;

    @NotBlank(message = "Target IP is required")
    private String targetIp;

    @NotNull(message = "UE Type is required")
    private String ueType; // "single" or "all"

    private String ueIp; // Only required when ueType is "single"

    private String dnn; // Data Network Name

    private String sst; // Service Slice Type

    private String sd; // Service Descriptor

    private String networkSegment; // Network segment for "all" UE type

    private String upf; // UPF

    private String routeProfId; // Route Profile ID

    // Constructors
    public SignalingPolicyRequest() {
    }

    // Getters and Setters
    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getDnai() {
        return dnai;
    }

    public void setDnai(String dnai) {
        this.dnai = dnai;
    }

    public String getTargetIp() {
        return targetIp;
    }

    public void setTargetIp(String targetIp) {
        this.targetIp = targetIp;
    }

    public String getUeType() {
        return ueType;
    }

    public void setUeType(String ueType) {
        this.ueType = ueType;
    }

    public String getUeIp() {
        return ueIp;
    }

    public void setUeIp(String ueIp) {
        this.ueIp = ueIp;
    }

    public String getDnn() {
        return dnn;
    }

    public void setDnn(String dnn) {
        this.dnn = dnn;
    }

    public String getSst() {
        return sst;
    }

    public void setSst(String sst) {
        this.sst = sst;
    }

    public String getSd() {
        return sd;
    }

    public void setSd(String sd) {
        this.sd = sd;
    }

    public String getNetworkSegment() {
        return networkSegment;
    }

    public void setNetworkSegment(String networkSegment) {
        this.networkSegment = networkSegment;
    }

    public String getUpf() {
        return upf;
    }

    public void setUpf(String upf) {
        this.upf = upf;
    }

    public String getRouteProfId() {
        return routeProfId;
    }

    public void setRouteProfId(String routeProfId) {
        this.routeProfId = routeProfId;
    }
}