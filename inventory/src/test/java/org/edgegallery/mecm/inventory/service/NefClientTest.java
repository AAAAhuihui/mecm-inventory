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

package org.edgegallery.mecm.inventory.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.edgegallery.mecm.inventory.config.NefConfig;
import org.edgegallery.mecm.inventory.model.CoreNetworkConfig;
import org.edgegallery.mecm.inventory.model.SignalingDetails;
import org.edgegallery.mecm.inventory.service.repository.CoreNetworkConfigRepository;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

public class NefClientTest {

    @Test
    public void testBuildQiantongTrafficInfluenceEndpoint() {
        NefClient nefClient = new NefClient();
        NefConfig nefConfig = new NefConfig();
        nefConfig.setNefEndpoint("https://192.168.254.154:8000/3gpp-traffic-influence/v1/af001/subscriptions");
        CoreNetworkConfigRepository repository = Mockito.mock(CoreNetworkConfigRepository.class);
        CoreNetworkConfig config = new CoreNetworkConfig("qiantong", "Commercial Core Network",
                "10.38.1.79", 8080);
        Mockito.when(repository.findById("qiantong")).thenReturn(Optional.of(config));
        ReflectionTestUtils.setField(nefClient, "nefConfig", nefConfig);
        ReflectionTestUtils.setField(nefClient, "coreNetworkConfigRepository", repository);

        String endpoint = ReflectionTestUtils.invokeMethod(nefClient,
                "buildTrafficInfluenceEndpoint", "qiantong");

        assertEquals("http://10.38.1.79:8080/3gpp-traffic-influence/v1/af-mec-001/subscriptions", endpoint);
    }

    @Test
    public void testBuildQiantongTrafficInfluencePostRequestHeaders() {
        NefClient nefClient = new NefClient();
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), "{}");

        Request request = ReflectionTestUtils.invokeMethod(nefClient,
                "buildTrafficInfluencePostRequest",
                "http://10.38.1.79:8080/3gpp-traffic-influence/v1/af-mec-001/subscriptions",
                "qiantong", body);

        assertEquals("POST", request.method());
        assertEquals("application/json", request.header("Content-Type"));
        assertEquals("Bearer selftest", request.header("Authorization"));
        assertEquals(null, request.header("Accept"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBuildQiantongTrafficInfluenceRequest() {
        NefClient nefClient = new NefClient();
        NefConfig nefConfig = new NefConfig();
        nefConfig.setAfServiceId("mec-video-service");
        ReflectionTestUtils.setField(nefClient, "nefConfig", nefConfig);
        ReflectionTestUtils.setField(nefClient, "objectMapper", new ObjectMapper());

        SignalingDetails details = new SignalingDetails();
        details.setId(1L);
        details.setCoreNetworkType("qiantong");
        details.setAppInstanceId("video-edge-app");
        details.setTargetIp("10.38.1.90");
        details.setRequestPayload("{\"dnn\":\"cmnet\",\"afTransId\":\"ti-selftest-0001\"}");

        Map<String, Object> request = (Map<String, Object>) ReflectionTestUtils.invokeMethod(nefClient,
                "build3gppTrafficInfluenceRequest", details);

        assertEquals(13, request.size());
        assertEquals("mec-video-service", request.get("afServiceId"));
        assertEquals("video-edge-app", request.get("afAppId"));
        assertEquals("ti-selftest-0001", request.get("afTransId"));
        assertEquals("cmnet", request.get("dnn"));
        assertEquals("upf-2", request.get("ulclUpfId"));
        assertEquals("upf-1", request.get("psa1UpfId"));
        assertEquals("permit out ip from any to assigned", request.get("psa1UpfSdf"));
        assertEquals("upf-2", request.get("psa2UpfId"));
        assertEquals("permit out ip from 10.38.1.90/32 to assigned", request.get("psa2UpfSdf"));
        assertEquals(0, request.get("smfLoc"));
        assertEquals(0, request.get("enbId"));
        assertEquals(false, request.get("singlePsa1Enable"));

        List<Map<String, Object>> trafficRoutes =
                (List<Map<String, Object>>) request.get("trafficRoutes");
        assertEquals(1, trafficRoutes.size());
        assertFalse(trafficRoutes.get(0).entrySet().iterator().hasNext());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBuildQiantongCloseTrafficInfluenceRequest() {
        NefClient nefClient = new NefClient();
        NefConfig nefConfig = new NefConfig();
        nefConfig.setAfServiceId("mec-video-service");
        ReflectionTestUtils.setField(nefClient, "nefConfig", nefConfig);
        ReflectionTestUtils.setField(nefClient, "objectMapper", new ObjectMapper());

        SignalingDetails details = new SignalingDetails();
        details.setId(1L);
        details.setCoreNetworkType("qiantong");
        details.setAppInstanceId("video-edge-app");
        details.setTargetIp("100.201.1.34");
        details.setRequestPayload("{\"dnn\":\"cmnet\",\"afTransId\":\"ti-inventory-0001\"}");

        Map<String, Object> createRequest = (Map<String, Object>) ReflectionTestUtils.invokeMethod(nefClient,
                "build3gppTrafficInfluenceRequest", details);
        Map<String, Object> closeRequest = (Map<String, Object>) ReflectionTestUtils.invokeMethod(nefClient,
                "buildQiantongCloseTrafficInfluenceRequest", details);

        assertEquals(createRequest.size() - 1, closeRequest.size());
        assertEquals("upf-2", closeRequest.get("ulclUpfId"));
        assertEquals(false, closeRequest.get("singlePsa1Enable"));
        assertEquals(null, closeRequest.get("psa2UpfSdf"));
        createRequest.remove("psa2UpfSdf");
        assertEquals(createRequest, closeRequest);
    }
}
