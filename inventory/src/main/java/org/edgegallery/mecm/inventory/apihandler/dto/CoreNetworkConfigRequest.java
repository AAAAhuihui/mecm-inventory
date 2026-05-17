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

public class CoreNetworkConfigRequest {

    private String coreNetworkType;

    private String nefIp;

    private Integer nefPort;

    public String getCoreNetworkType() {
        return coreNetworkType;
    }

    public void setCoreNetworkType(String coreNetworkType) {
        this.coreNetworkType = coreNetworkType;
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
}
