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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.edgegallery.mecm.inventory.config.NefConfig;
import org.edgegallery.mecm.inventory.model.SignalingDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Component
public class NefClient {

    private static final Logger logger = LoggerFactory.getLogger(NefClient.class);

    @Autowired
    private NefConfig nefConfig;

    @Autowired
    private ObjectMapper objectMapper;

    public Map<String, Object> sendTrafficInfluenceRequest(SignalingDetails signalingDetails) {
        CloseableHttpClient httpClient = createHttpClient();
        Map<String, Object> result = new HashMap<>();

        try {
            // 构建3GPP流量影响请求
            Map<String, Object> requestPayload = build3gppTrafficInfluenceRequest(signalingDetails);

            HttpPost post = new HttpPost(nefConfig.getNefEndpoint());
            post.setHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8");
            post.setHeader(HttpHeaders.ACCEPT, "application/json");

            StringEntity entity = new StringEntity(objectMapper.writeValueAsString(requestPayload), "UTF-8");
            post.setEntity(entity);

            logger.info("Sending traffic influence request to NEF: {}", nefConfig.getNefEndpoint());
            HttpResponse response = httpClient.execute(post);
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity());

            // Get Location from response header (if 201 Created)
            String transactionId = response.getFirstHeader("Location") != null
                    ? response.getFirstHeader("Location").getValue()
                    : null;

            // Extract only the last number from transactionId
            if (transactionId != null && !transactionId.isEmpty()) {
                // Split by '/' and get the last part
                String[] parts = transactionId.split("/");
                if (parts.length > 0) {
                    transactionId = parts[parts.length - 1];
                }
            }

            result.put("statusCode", statusCode);
            result.put("responseBody", responseBody);
            result.put("success", statusCode == 200 || statusCode == 201);

            if ((Boolean) result.get("success")) {
                if (transactionId != null && !transactionId.isEmpty()) {
                    result.put("transactionId", transactionId);
                } else {
                    // Generate a temporary ID if no Location header in response
                    result.put("transactionId", "trans_" + System.currentTimeMillis());
                }
            } else {
                // Generate an identifier for tracking failed responses
                result.put("transactionId", "nef-fail-" + signalingDetails.getAppInstanceId() +
                        "-" + signalingDetails.getTargetDnai());
            }

            logger.info("NEF request completed, status: {}, response: {}", statusCode, responseBody);

        } catch (IOException e) {
            logger.error("Failed to send request to NEF: ", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("transactionId", "nef-fail-" + signalingDetails.getAppInstanceId() +
                    "-" + signalingDetails.getTargetDnai());
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                logger.error("Error closing HTTP client: ", e);
            }
        }

        return result;
    }

    public Map<String, Object> deleteTrafficInfluenceRequest(String transactionId) {
        CloseableHttpClient httpClient = createHttpClient();
        Map<String, Object> result = new HashMap<>();

        // Skip invalid subscriptions (records where NEF creation failed, no need to
        // call delete interface)
        if (transactionId == null || transactionId.isEmpty() || transactionId.startsWith("nef-")) {
            logger.info("Skipping invalid NEF subscription deletion, TransactionId: {}", transactionId);
            result.put("statusCode", 200);
            result.put("responseBody", "Skipped invalid subscription");
            result.put("success", true);
            return result;
        }

        try {
            // Build correct DELETE URL: base endpoint + transactionId (last number)
            String deleteUrl;
            if (transactionId.startsWith("http")) {
                // If transactionId is already a full URL, use it directly
                deleteUrl = transactionId;
            } else {
                // If transactionId is just a number, append it to the base endpoint
                String baseEndpoint = nefConfig.getNefEndpoint();
                // Ensure baseEndpoint ends with /
                if (!baseEndpoint.endsWith("/")) {
                    baseEndpoint += "/";
                }
                // Append transactionId
                deleteUrl = baseEndpoint + transactionId;
            }

            HttpDelete delete = new HttpDelete(deleteUrl);
            delete.setHeader(HttpHeaders.ACCEPT, "application/json");

            logger.info("Sending delete request to NEF: {}", deleteUrl);
            HttpResponse response = httpClient.execute(delete);
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = "";

            // Handle 204 No Content response
            if (statusCode != 204) {
                try {
                    responseBody = EntityUtils.toString(response.getEntity());
                } catch (Exception e) {
                    logger.warn("Failed to read response body: {}", e.getMessage());
                }
            }

            result.put("statusCode", statusCode);
            result.put("responseBody", responseBody);
            // 3GPP standard: 200 / 204 both indicate successful deletion
            result.put("success", statusCode == 200 || statusCode == 204);

            logger.info("NEF delete request completed, status: {}, response: {}", statusCode, responseBody);

        } catch (IOException e) {
            logger.error("Failed to delete request from NEF: ", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("statusCode", 500); // Set status code for error
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                logger.error("Error closing HTTP client: ", e);
            }
        }

        return result;
    }

    private Map<String, Object> build3gppTrafficInfluenceRequest(SignalingDetails signalingDetails) {
        // Extract parameters from request payload
        Map<String, Object> requestParams = extractRequestParams(signalingDetails);

        String appId = (String) requestParams.get("appId");
        String dnai = (String) requestParams.get("dnai");
        String targetIp = (String) requestParams.get("targetIp");
        String ueType = (String) requestParams.get("ueType");
        String ueIp = (String) requestParams.get("ueIp");
        String dnn = (String) requestParams.get("dnn");
        String sst = (String) requestParams.get("sst");
        String sd = (String) requestParams.get("sd");
        String networkSegment = (String) requestParams.get("networkSegment");

        // Build SNSSAI
        Map<String, Object> snssai = new HashMap<>();
        try {
            snssai.put("sst", Integer.parseInt(sst != null ? sst : "1")); // Default to 1
        } catch (NumberFormatException e) {
            snssai.put("sst", 1); // Default value
        }
        snssai.put("sd", sd != null ? sd : "010203"); // Default value

        // Build traffic route
        Map<String, Object> trafficRoute = new HashMap<>();
        trafficRoute.put("dnai", dnai);

        // Build final request based on UE type
        Map<String, Object> request = new HashMap<>();
        request.put("afServiceId", nefConfig.getAfServiceId());
        request.put("dnn", dnn != null ? dnn : "default-dnn"); // Default DNN
        request.put("snssai", snssai);
        request.put("notificationDestination", "http://af:8000/test123"); // Fixed notification address
        request.put("trafficRoutes", Arrays.asList(trafficRoute));

        if ("single".equals(ueType)) {
            // For single UE, use the format provided by user
            request.put("ipv4Addr", ueIp != null ? ueIp : "");
            request.put("AfAppId", appId);
            request.put("suppFeat", "01"); // Fixed value
        } else {
            // For all UE, use the standard format provided by user
            // Build traffic rule
            String targetNetwork = networkSegment != null && !networkSegment.isEmpty() ? networkSegment
                    : "10.60.0.0/16";
            String flowRule = String.format("permit out ip from %s to %s", targetIp, targetNetwork);

            // Build traffic filter
            Map<String, Object> trafficFilter = new HashMap<>();
            trafficFilter.put("flowId", 1);
            trafficFilter.put("flowDescriptions", Arrays.asList(flowRule));

            request.put("anyUeInd", true); // For all UE
            request.put("trafficFilters", Arrays.asList(trafficFilter));
        }

        return request;
    }

    private Map<String, Object> extractRequestParams(SignalingDetails signalingDetails) {
        Map<String, Object> params = new HashMap<>();

        // Extract basic information from SignalingDetails
        params.put("appId", signalingDetails.getAppInstanceId());
        params.put("dnai", signalingDetails.getTargetDnai());
        params.put("targetIp", signalingDetails.getTargetIp());

        // Parse additional parameters from request payload
        if (signalingDetails.getRequestPayload() != null) {
            try {
                Map<String, Object> payload = objectMapper.readValue(signalingDetails.getRequestPayload(), Map.class);
                params.put("ueType", (String) payload.get("ueType"));
                params.put("ueIp", (String) payload.get("ueIp"));
                params.put("dnn", (String) payload.get("dnn"));
                params.put("sst", (String) payload.get("sst"));
                params.put("sd", (String) payload.get("sd"));
                params.put("networkSegment", (String) payload.get("networkSegment"));
            } catch (Exception e) {
                logger.warn("Could not parse request payload for additional parameters: ", e);
                // Set default values
                params.put("ueType", "all");
                params.put("ueIp", "");
                params.put("dnn", "default-dnn");
                params.put("sst", "1");
                params.put("sd", "010203");
            }
        } else {
            // If no request payload, use default values
            params.put("ueType", "all");
            params.put("ueIp", "");
            params.put("dnn", "default-dnn");
            params.put("sst", "1");
            params.put("sd", "010203");
        }

        return params;
    }

    private CloseableHttpClient createHttpClient() {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(nefConfig.getTimeoutSeconds() * 1000)
                .setSocketTimeout(nefConfig.getTimeoutSeconds() * 1000)
                .build();

        return HttpClients.custom()
                .setDefaultRequestConfig(config)
                .build();
    }
}