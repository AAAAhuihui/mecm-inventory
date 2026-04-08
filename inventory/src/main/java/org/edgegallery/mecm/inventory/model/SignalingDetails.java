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

package org.edgegallery.mecm.inventory.model;

import javax.persistence.Column;
import javax.persistence.Entity;

import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table(name = "signaling_details")
public class SignalingDetails {

    @Id
    private Long id;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "app_instance_id", nullable = false)
    private String appInstanceId;

    @Column(name = "target_ip", nullable = false)
    private String targetIp;

    @Column(name = "target_dnai", nullable = false)
    private String targetDnai;

    @Column(name = "ue_type")
    private String ueType;

    @Column(name = "ue_ip")
    private String ueIp;

    @Column(name = "dnn")
    private String dnn;

    @Column(name = "sst")
    private String sst;

    @Column(name = "sd")
    private String sd;

    @Column(name = "network_segment")
    private String networkSegment;

    @Column(name = "upf")
    private String upf;

    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload;

    @Column(name = "response_code")
    private Integer responseCode;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "status", length = 20)
    private String status; // SUCCESS, FAILED, PENDING

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;

    // Constructors
    public SignalingDetails() {
    }

    public SignalingDetails(String appInstanceId, String targetIp, String targetDnai, String ueType, String ueIp,
            String dnn, String sst, String sd, String networkSegment, String upf) {
        this.appInstanceId = appInstanceId;
        this.targetIp = targetIp;
        this.targetDnai = targetDnai;
        this.ueType = ueType;
        this.ueIp = ueIp;
        this.dnn = dnn;
        this.sst = sst;
        this.sd = sd;
        this.networkSegment = networkSegment;
        this.upf = upf;
        this.status = "PENDING"; // 默认状态
        this.createTime = new Timestamp(System.currentTimeMillis());
        this.updateTime = new Timestamp(System.currentTimeMillis());
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getAppInstanceId() {
        return appInstanceId;
    }

    public void setAppInstanceId(String appInstanceId) {
        this.appInstanceId = appInstanceId;
    }

    public String getTargetIp() {
        return targetIp;
    }

    public void setTargetIp(String targetIp) {
        this.targetIp = targetIp;
    }

    public String getTargetDnai() {
        return targetDnai;
    }

    public void setTargetDnai(String targetDnai) {
        this.targetDnai = targetDnai;
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

    public String getRequestPayload() {
        return requestPayload;
    }

    public void setRequestPayload(String requestPayload) {
        this.requestPayload = requestPayload;
    }

    public Integer getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(Integer responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Timestamp createTime) {
        this.createTime = createTime;
    }

    public Timestamp getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Timestamp updateTime) {
        this.updateTime = updateTime;
    }
}