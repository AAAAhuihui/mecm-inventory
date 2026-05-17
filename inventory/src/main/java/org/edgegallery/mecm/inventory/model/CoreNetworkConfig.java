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

import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "core_network_config")
public class CoreNetworkConfig {

    @Id
    @Column(name = "core_network_type")
    private String coreNetworkType;

    @Column(name = "core_network_name", nullable = false)
    private String coreNetworkName;

    @Column(name = "nef_ip", nullable = false)
    private String nefIp;

    @Column(name = "nef_port", nullable = false)
    private Integer nefPort;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;

    public CoreNetworkConfig() {
    }

    public CoreNetworkConfig(String coreNetworkType, String coreNetworkName, String nefIp,
            Integer nefPort) {
        this.coreNetworkType = coreNetworkType;
        this.coreNetworkName = coreNetworkName;
        this.nefIp = nefIp;
        this.nefPort = nefPort;
        this.createTime = new Timestamp(System.currentTimeMillis());
        this.updateTime = new Timestamp(System.currentTimeMillis());
    }

    public String getCoreNetworkType() {
        return coreNetworkType;
    }

    public void setCoreNetworkType(String coreNetworkType) {
        this.coreNetworkType = coreNetworkType;
    }

    public String getCoreNetworkName() {
        return coreNetworkName;
    }

    public void setCoreNetworkName(String coreNetworkName) {
        this.coreNetworkName = coreNetworkName;
    }

    public String getNefIp() {
        return nefIp;
    }

    public void setNefIp(String nefIp) {
        this.nefIp = nefIp;
    }

    public Integer getNefPort() {
        return nefPort;
    }

    public void setNefPort(Integer nefPort) {
        this.nefPort = nefPort;
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
