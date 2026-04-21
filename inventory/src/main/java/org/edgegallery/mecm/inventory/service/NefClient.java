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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.edgegallery.mecm.inventory.config.NefConfig;
import org.edgegallery.mecm.inventory.model.SignalingDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Component
public class NefClient {

    private static final Logger logger = LoggerFactory.getLogger(NefClient.class);

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    @Autowired
    private NefConfig nefConfig;

    @Autowired
    private ObjectMapper objectMapper;

    private volatile OkHttpClient httpClient;

    public Map<String, Object> sendTrafficInfluenceRequest(SignalingDetails signalingDetails) {
        OkHttpClient client = getHttpClient();
        Map<String, Object> result = new HashMap<>();

        try {
            // 构建3GPP流量影响请求
            Map<String, Object> requestPayload = build3gppTrafficInfluenceRequest(signalingDetails);

            RequestBody requestBody = RequestBody.create(JSON_MEDIA_TYPE,
                    objectMapper.writeValueAsString(requestPayload));
            Request request = new Request.Builder()
                    .url(nefConfig.getNefEndpoint())
                    .addHeader("Content-Type", "application/json; charset=utf-8")
                    .addHeader("Accept", "application/json")
                    .post(requestBody)
                    .build();

            logger.info("Sending traffic influence request to NEF: {}", nefConfig.getNefEndpoint());
            try (Response response = client.newCall(request).execute()) {
                int statusCode = response.code();
                String responseBody = response.body() != null ? response.body().string() : "";

                // Get Location from response header (if 201 Created)
                String transactionId = response.header("Location");

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
            }

        } catch (IOException e) {
            logger.error("Failed to send request to NEF: ", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("transactionId", "nef-fail-" + signalingDetails.getAppInstanceId() +
                    "-" + signalingDetails.getTargetDnai());
        }

        return result;
    }

    public Map<String, Object> deleteTrafficInfluenceRequest(String transactionId) {
        OkHttpClient client = getHttpClient();
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

            Request request = new Request.Builder()
                    .url(deleteUrl)
                    .addHeader("Accept", "application/json")
                    .delete()
                    .build();

            logger.info("Sending delete request to NEF: {}", deleteUrl);
            try (Response response = client.newCall(request).execute()) {
                int statusCode = response.code();
                String responseBody = "";

                // Handle 204 No Content response
                if (statusCode != 204 && response.body() != null) {
                    try {
                        responseBody = response.body().string();
                    } catch (Exception e) {
                        logger.warn("Failed to read response body: {}", e.getMessage());
                    }
                }

                result.put("statusCode", statusCode);
                result.put("responseBody", responseBody);

                // Parse response body to extract title field for error handling
                String title = null;
                if (responseBody != null && !responseBody.isEmpty()) {
                    try {
                        Map<String, Object> responseJson = objectMapper.readValue(responseBody, Map.class);
                        title = (String) responseJson.get("title");
                        result.put("title", title);
                        logger.info("Extracted title from response: {}", title);
                    } catch (Exception e) {
                        logger.warn("Failed to parse response body as JSON: {}", e.getMessage());
                    }
                }

                // Determine success based on status code and title
                boolean success = statusCode == 200 || statusCode == 204;

                // Special handling for 404 with Data not found - treat as success
                if (statusCode == 404 && "Data not found".equals(title)) {
                    logger.info("NEF subscription not found (404 Data not found), treating as successful deletion");
                    success = true;
                    result.put("subscriptionNotFound", true);
                }

                // Special handling for 500 with CANCEL_FAILED - treat as failure with specific
                // error
                if (statusCode == 500 && "CANCEL_FAILED".equals(title)) {
                    logger.error("NEF deletion failed (500 CANCEL_FAILED), core network cannot delete subscription");
                    success = false;
                    result.put("cancelFailed", true);
                }

                result.put("success", success);

                logger.info("NEF delete request completed, status: {}, response: {}, success: {}", statusCode,
                        responseBody,
                        success);
            }

        } catch (IOException e) {
            logger.error("Failed to delete request from NEF: ", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("statusCode", 500); // Set status code for error
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
        String routeProfId = (String) requestParams.get("routeProfId");

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
        trafficRoute.put("routeProfId", routeProfId != null ? routeProfId : "mec");

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

    private OkHttpClient getHttpClient() {
        if (httpClient == null) {
            synchronized (this) {
                if (httpClient == null) {
                    httpClient = new OkHttpClient.Builder()
                            .connectTimeout(nefConfig.getTimeoutSeconds(), TimeUnit.SECONDS)
                            .readTimeout(nefConfig.getTimeoutSeconds(), TimeUnit.SECONDS)
                            .writeTimeout(nefConfig.getTimeoutSeconds(), TimeUnit.SECONDS)
                            .protocols(Collections.singletonList(Protocol.H2_PRIOR_KNOWLEDGE))
                            .build();
                }
            }
        }
        return httpClient;
    }
}