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

package org.edgegallery.mecm.inventory.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NefConfig {

    @Value("${nef.endpoint:http://192.168.254.154:8000/3gpp-traffic-influence/v1/af001/subscriptions}")
    private String nefEndpoint;

    @Value("${nef.tls_enable:false}")
    private boolean tlsEnable;

    @Value("${nef.timeout_sec:15}")
    private int timeoutSeconds;

    @Value("${nef.afServiceId:Service1}")
    private String afServiceId;

    // Getters
    public String getNefEndpoint() {
        return nefEndpoint;
    }

    public boolean isTlsEnable() {
        return tlsEnable;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public String getAfServiceId() {
        return afServiceId;
    }

    // Setters
    public void setNefEndpoint(String nefEndpoint) {
        this.nefEndpoint = nefEndpoint;
    }

    public void setTlsEnable(boolean tlsEnable) {
        this.tlsEnable = tlsEnable;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public void setAfServiceId(String afServiceId) {
        this.afServiceId = afServiceId;
    }
}