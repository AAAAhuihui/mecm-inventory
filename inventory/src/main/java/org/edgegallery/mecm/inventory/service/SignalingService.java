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
import org.edgegallery.mecm.inventory.apihandler.dto.CoreNetworkConfigRequest;
import org.edgegallery.mecm.inventory.config.NefConfig;
import org.edgegallery.mecm.inventory.model.CoreNetworkConfig;
import org.edgegallery.mecm.inventory.model.MecApplication;
import org.edgegallery.mecm.inventory.model.SignalingDetails;
import org.edgegallery.mecm.inventory.apihandler.dto.SignalingPolicyRequest;
import org.edgegallery.mecm.inventory.service.repository.CoreNetworkConfigRepository;
import org.edgegallery.mecm.inventory.service.repository.SignalingDetailsRepository;
import org.edgegallery.mecm.inventory.service.repository.MecApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class SignalingService {

    private static final String DEFAULT_CORE_NETWORK = "default";
    private static final String QIANTONG_CORE_NETWORK = "qiantong";
    private static final String FALLBACK_NEF_IP = "192.168.254.154";
    private static final int FALLBACK_NEF_PORT = 8000;

    private Map<String, Object> createDataMap(String key, Object value) {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put(key, value);
        return dataMap;
    }

    private static final Logger logger = LoggerFactory.getLogger(SignalingService.class);

    @Autowired
    private NefClient nefClient;

    @Autowired
    private SignalingDetailsRepository signalingDetailsRepository;

    @Autowired
    private MecApplicationRepository mecApplicationRepository;

    @Autowired
    private CoreNetworkConfigRepository coreNetworkConfigRepository;

    @Autowired
    private NefConfig nefConfig;

    @Autowired
    private ObjectMapper objectMapper;

    public Map<String, Object> getCoreNetworkConfigs() {
        try {
            List<CoreNetworkConfig> savedConfigs = new ArrayList<>();
            try {
                savedConfigs = coreNetworkConfigRepository.findAll();
            } catch (RuntimeException e) {
                logger.warn("Failed to query core network config table, using default configs: {}", e.getMessage());
            }
            List<CoreNetworkConfig> configs = mergeCoreNetworkDefaults(savedConfigs);

            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("data", configs);
            result.put("msg", "查询成功");
            return result;
        } catch (Exception e) {
            logger.error("Failed to get core network configs: ", e);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("data", Arrays.asList(
                    createDefaultCoreNetworkConfig(DEFAULT_CORE_NETWORK),
                    createDefaultCoreNetworkConfig(QIANTONG_CORE_NETWORK)));
            result.put("msg", "查询成功");
            return result;
        }
    }

    public Map<String, Object> saveCoreNetworkConfig(CoreNetworkConfigRequest request) {
        Map<String, Object> result = new HashMap<>();
        if (request == null || request.getCoreNetworkType() == null || request.getCoreNetworkType().isEmpty()
                || request.getNefIp() == null || request.getNefIp().isEmpty()
                || request.getNefPort() == null) {
            result.put("code", 400);
            result.put("data", new HashMap<>());
            result.put("msg", "核心网类型、IP和端口不能为空");
            return result;
        }
        if (request.getNefPort() < 1 || request.getNefPort() > 65535) {
            result.put("code", 400);
            result.put("data", new HashMap<>());
            result.put("msg", "端口范围必须为1-65535");
            return result;
        }

        try {
            CoreNetworkConfig defaultConfig = createDefaultCoreNetworkConfig(request.getCoreNetworkType());
            CoreNetworkConfig config = coreNetworkConfigRepository.findById(request.getCoreNetworkType())
                    .orElse(defaultConfig);
            config.setCoreNetworkName(defaultConfig.getCoreNetworkName());
            config.setNefIp(request.getNefIp());
            config.setNefPort(request.getNefPort());
            if (config.getCreateTime() == null) {
                config.setCreateTime(new Timestamp(System.currentTimeMillis()));
            }
            config.setUpdateTime(new Timestamp(System.currentTimeMillis()));

            CoreNetworkConfig savedConfig = coreNetworkConfigRepository.save(config);

            result.put("code", 200);
            result.put("data", savedConfig);
            result.put("msg", "保存成功");
            return result;
        } catch (Exception e) {
            logger.error("Failed to save core network config: ", e);
            result.put("code", 500);
            result.put("data", new HashMap<>());
            result.put("msg", "保存失败：" + e.getMessage());
            return result;
        }
    }

    private List<CoreNetworkConfig> mergeCoreNetworkDefaults(List<CoreNetworkConfig> savedConfigs) {
        Map<String, CoreNetworkConfig> configMap = new HashMap<>();
        if (savedConfigs != null) {
            for (CoreNetworkConfig config : savedConfigs) {
                if (config != null && config.getCoreNetworkType() != null) {
                    configMap.put(config.getCoreNetworkType(), config);
                }
            }
        }

        List<CoreNetworkConfig> configs = new ArrayList<>();
        configs.add(configMap.containsKey(DEFAULT_CORE_NETWORK) ? configMap.get(DEFAULT_CORE_NETWORK)
                : createDefaultCoreNetworkConfig(DEFAULT_CORE_NETWORK));
        configs.add(configMap.containsKey(QIANTONG_CORE_NETWORK) ? configMap.get(QIANTONG_CORE_NETWORK)
                : createDefaultCoreNetworkConfig(QIANTONG_CORE_NETWORK));
        return configs;
    }

    private CoreNetworkConfig createDefaultCoreNetworkConfig(String coreNetworkType) {
        String normalizedType = normalizeCoreNetworkType(coreNetworkType);
        String name = QIANTONG_CORE_NETWORK.equals(normalizedType) ? "商业核心网-千通" : "默认核心网";
        EndpointParts endpointParts = parseNefEndpoint();
        return new CoreNetworkConfig(normalizedType, name, endpointParts.host, endpointParts.port);
    }

    private String normalizeCoreNetworkType(String coreNetworkType) {
        return QIANTONG_CORE_NETWORK.equals(coreNetworkType) ? QIANTONG_CORE_NETWORK : DEFAULT_CORE_NETWORK;
    }

    private EndpointParts parseNefEndpoint() {
        try {
            String endpoint = nefConfig.getNefEndpoint();
            if (endpoint == null || endpoint.isEmpty()) {
                return new EndpointParts(FALLBACK_NEF_IP, FALLBACK_NEF_PORT);
            }
            URI uri = new URI(endpoint);
            String host = uri.getHost() != null ? uri.getHost() : FALLBACK_NEF_IP;
            int port = uri.getPort() > 0 ? uri.getPort() : FALLBACK_NEF_PORT;
            return new EndpointParts(host, port);
        } catch (URISyntaxException | RuntimeException e) {
            logger.warn("Failed to parse nef.endpoint, using fallback NEF address: {}", e.getMessage());
            return new EndpointParts(FALLBACK_NEF_IP, FALLBACK_NEF_PORT);
        }
    }

    private static final class EndpointParts {
        private final String host;
        private final int port;

        private EndpointParts(String host, int port) {
            this.host = host;
            this.port = port;
        }
    }

    public Map<String, Object> createSignalingPolicy(SignalingPolicyRequest request) {
        String coreNetworkType = request.getCoreNetworkType() != null && !request.getCoreNetworkType().isEmpty()
                ? normalizeCoreNetworkType(request.getCoreNetworkType())
                : DEFAULT_CORE_NETWORK;
        boolean isQiantong = QIANTONG_CORE_NETWORK.equals(coreNetworkType);

        // Parameter validation
        if (request.getAppId() == null || request.getAppId().isEmpty() ||
                request.getTargetIp() == null || request.getTargetIp().isEmpty() ||
                request.getDnn() == null || request.getDnn().isEmpty() ||
                (!isQiantong && (request.getDnai() == null || request.getDnai().isEmpty()))) {
            logger.error("Missing required signaling parameters for core network type: {}", coreNetworkType);

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("code", 400);
            errorResult.put("data", new HashMap<>());
            errorResult.put("msg", isQiantong
                    ? "Missing required parameters: appId, targetIp, dnn"
                    : "Missing required parameters: appId, dnai, targetIp, dnn");
            return errorResult;
        }

        // UE type validation
        if (!isQiantong && (request.getUeType() == null || (!request.getUeType().equals("single")
                && !request.getUeType().equals("all")))) {
            logger.error("❌ Invalid UE type: {}", request.getUeType());

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("code", 400);
            errorResult.put("data", new HashMap<>());
            errorResult.put("msg", "UE type only supports 'single' or 'all'");
            return errorResult;
        }

        // Log request parameters
        logger.info("==================== Frontend request parameters ====================");
        logger.info("APPID: {}", request.getAppId());
        logger.info("DNAI: {}", request.getDnai());
        logger.info("N6IP: {}", request.getTargetIp());
        logger.info("UE Type: {}", request.getUeType());
        logger.info("UE IP: {}", request.getUeIp());
        logger.info("DNN: {}", request.getDnn());
        logger.info("SST: {}", request.getSst());
        logger.info("SD: {}", request.getSd());
        logger.info("Core Network Type: {}", request.getCoreNetworkType());
        logger.info("======================================================");

        try {
            String ueType = isQiantong ? "all"
                    : (request.getUeType() != null ? request.getUeType() : "all");
            Long signalingId = getNextSignalingId();

            // 1. Create request payload (including API version, AF ID, etc.)
            Map<String, Object> reqPayload = new HashMap<>();
            reqPayload.put("api_version", "1.0.0");
            reqPayload.put("af_id", "Service1"); // Default AF ID
            reqPayload.put("operation", "CREATE");
            reqPayload.put("create_time", new Timestamp(System.currentTimeMillis()).toString());

            // Add request parameters
            reqPayload.put("appId", request.getAppId());
            reqPayload.put("targetIp", request.getTargetIp());
            reqPayload.put("ueType", ueType);
            reqPayload.put("dnn", request.getDnn());
            reqPayload.put("coreNetworkType", coreNetworkType);
            if (isQiantong) {
                reqPayload.put("afTransId", String.format("ti-inventory-%04d", signalingId));
            } else {
                reqPayload.put("dnai", request.getDnai());
                reqPayload.put("ueIp", "all".equals(ueType) ? "" : request.getUeIp());
                reqPayload.put("sst", request.getSst());
                reqPayload.put("sd", request.getSd());
                reqPayload.put("networkSegment", request.getNetworkSegment());
                reqPayload.put("upf", request.getUpf());
                if (request.getRouteProfId() == null || request.getRouteProfId().isEmpty()) {
                    logger.info("routeProfId is empty, use default value: mec");
                    reqPayload.put("routeProfId", "mec");
                } else {
                    reqPayload.put("routeProfId", request.getRouteProfId());
                }
            }

            String requestPayload = objectMapper.writeValueAsString(reqPayload);

            // 3. Create signaling details record
            SignalingDetails signalingDetails = new SignalingDetails(
                    request.getAppId(),
                    request.getTargetIp(),
                    isQiantong ? "" : request.getDnai(),
                    ueType,
                    "all".equals(ueType) ? "" : request.getUeIp(),
                    request.getDnn(),
                    isQiantong ? null : request.getSst(),
                    isQiantong ? null : request.getSd(),
                    isQiantong ? null : request.getNetworkSegment(),
                    isQiantong ? null : request.getUpf(),
                    isQiantong ? null : request.getRouteProfId());
            signalingDetails.setId(signalingId);
            signalingDetails.setCoreNetworkType(coreNetworkType);

            // Set request payload
            signalingDetails.setRequestPayload(requestPayload);

            // Save to database (initial status is PENDING, updated later)
            signalingDetails = signalingDetailsRepository.save(signalingDetails);

            if ("single".equals(ueType)) {
                Map<String, Object> pfdResult = nefClient.sendPfdRequest(signalingDetails);
                if (!Boolean.TRUE.equals(pfdResult.get("success"))) {
                    signalingDetails.setStatus("FAILED");
                    signalingDetails.setResponseCode((Integer) pfdResult.get("statusCode"));
                    signalingDetails.setResponseBody((String) pfdResult.get("responseBody"));
                    signalingDetails.setTransactionId("pfd-fail-" + signalingDetails.getAppInstanceId() +
                            "-" + signalingDetails.getTargetDnai());
                    signalingDetails.setUpdateTime(new Timestamp(System.currentTimeMillis()));
                    signalingDetailsRepository.save(signalingDetails);

                    Map<String, Object> result = new HashMap<>();
                    result.put("code", 500);
                    result.put("data", new HashMap<>());
                    result.put("msg", "PFD failed - Traffic influence skipped");
                    return result;
                }
                signalingDetails.setPfdTransactionId((String) pfdResult.get("transactionId"));
                signalingDetailsRepository.save(signalingDetails);
            }

            // 3. Call NEF to create traffic influence subscription
            Map<String, Object> nefResult = nefClient.sendTrafficInfluenceRequest(signalingDetails);

            // 4. Update database record
            boolean isNefSuccess = (Boolean) nefResult.get("success");
            boolean isDbSuccess = true; // Current operation is DB operation, so success

            if (isNefSuccess) {
                signalingDetails.setStatus("SUCCESS");
                signalingDetails.setResponseCode((Integer) nefResult.get("statusCode"));
                signalingDetails.setResponseBody((String) nefResult.get("responseBody"));
                signalingDetails.setTransactionId((String) nefResult.get("transactionId"));
            } else {
                if ("single".equals(ueType) && signalingDetails.getPfdTransactionId() != null
                        && !signalingDetails.getPfdTransactionId().isEmpty()) {
                    logger.warn("Traffic influence request failed, rolling back PFD transaction: {}",
                            signalingDetails.getPfdTransactionId());
                    Map<String, Object> pfdRollbackResult =
                            nefClient.deletePfdRequest(signalingDetails.getPfdTransactionId(),
                                    signalingDetails.getCoreNetworkType());
                    boolean isPfdRollbackSuccess = Boolean.TRUE.equals(pfdRollbackResult.get("success"));
                    if (isPfdRollbackSuccess) {
                        logger.info("PFD rollback successful, transactionId: {}",
                                signalingDetails.getPfdTransactionId());
                        signalingDetails.setPfdTransactionId(null);
                    } else {
                        logger.error("PFD rollback failed, transactionId: {}, status: {}, response: {}",
                                signalingDetails.getPfdTransactionId(),
                                pfdRollbackResult.get("statusCode"),
                                pfdRollbackResult.get("responseBody"));
                    }
                }
                signalingDetails.setStatus("FAILED");
                signalingDetails.setResponseCode((Integer) nefResult.get("statusCode"));
                signalingDetails.setResponseBody((String) nefResult.get("responseBody"));
                signalingDetails.setTransactionId((String) nefResult.get("transactionId"));
            }

            signalingDetails.setUpdateTime(new Timestamp(System.currentTimeMillis()));

            // Save final status
            signalingDetailsRepository.save(signalingDetails);

            // 5. Determine final response based on NEF and DB results
            int resCode;
            String finalMsg;
            if (isNefSuccess && isDbSuccess) {
                finalMsg = "NEF success - Database write success";
                resCode = 200;
            } else if (isNefSuccess && !isDbSuccess) {
                finalMsg = "NEF success - Database write failed";
                resCode = 500;
            } else if (!isNefSuccess && isDbSuccess) {
                finalMsg = "NEF failed - Database write success";
                resCode = 502; // Bad Gateway
            } else {
                finalMsg = "NEF failed - Database write failed";
                resCode = 500;
            }

            logger.info("Signaling policy creation completed, ID: {}, NEF status: {}, DB status: {}, Final result: {}",
                    signalingDetails.getId(), isNefSuccess, isDbSuccess, finalMsg);

            // Prepare response according to frontend expectation
            Map<String, Object> result = new HashMap<>();
            if (isNefSuccess) {
                result.put("code", 200);
                result.put("data", createDataMap("dbId", signalingDetails.getId()));
                result.put("msg", finalMsg);
            } else {
                result.put("code", 500); // Use 500 for general error
                result.put("data", new HashMap<>()); // Empty data on error
                result.put("msg", finalMsg);
            }
            return result;

        } catch (Exception e) {
            logger.error("Failed to create signaling policy: ", e);

            // Create failed record
            SignalingDetails failedSignaling = null;
            try {
                String failedCoreNetworkType = request.getCoreNetworkType() != null
                        ? normalizeCoreNetworkType(request.getCoreNetworkType()) : DEFAULT_CORE_NETWORK;
                String failedUeType = QIANTONG_CORE_NETWORK.equals(failedCoreNetworkType) ? "all" : request.getUeType();
                failedSignaling = saveFailedSignalingRecord(request, failedCoreNetworkType, failedUeType,
                        "Signaling policy creation failed: " + e.getMessage());
            } catch (Exception dbEx) {
                logger.error("Failed to save failed signaling policy record: ", dbEx);
            }

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("code", 500);
            errorResult.put("data", failedSignaling != null ? createDataMap("dbId", failedSignaling.getId())
                    : new HashMap<>());
            errorResult.put("msg", "Signaling policy creation failed: " + e.getMessage());
            return errorResult;
        }
    }

    private SignalingDetails saveFailedSignalingRecord(SignalingPolicyRequest request, String coreNetworkType,
            String ueType, String failureMessage) throws Exception {
        boolean isQiantong = QIANTONG_CORE_NETWORK.equals(coreNetworkType);
        SignalingDetails failedSignaling = new SignalingDetails(
                request.getAppId(),
                request.getTargetIp(),
                isQiantong ? "" : request.getDnai(),
                ueType,
                "all".equals(ueType) ? "" : request.getUeIp(),
                request.getDnn(),
                isQiantong ? null : request.getSst(),
                isQiantong ? null : request.getSd(),
                isQiantong ? null : request.getNetworkSegment(),
                isQiantong ? null : request.getUpf(),
                isQiantong ? null : request.getRouteProfId());
        failedSignaling.setId(getNextSignalingId());
        failedSignaling.setCoreNetworkType(coreNetworkType);
        failedSignaling.setRequestPayload(buildFailedRequestPayload(request, coreNetworkType, ueType, failureMessage));
        failedSignaling.setStatus("FAILED");
        failedSignaling.setResponseCode(500);
        failedSignaling.setResponseBody(failureMessage);
        failedSignaling.setUpdateTime(new Timestamp(System.currentTimeMillis()));
        return signalingDetailsRepository.save(failedSignaling);
    }

    private String buildFailedRequestPayload(SignalingPolicyRequest request, String coreNetworkType, String ueType,
            String failureMessage) throws Exception {
        Map<String, Object> reqPayload = new HashMap<>();
        reqPayload.put("api_version", "1.0.0");
        reqPayload.put("af_id", "Service1");
        reqPayload.put("operation", "CREATE");
        reqPayload.put("create_time", new Timestamp(System.currentTimeMillis()).toString());
        reqPayload.put("appId", request.getAppId());
        reqPayload.put("dnai", request.getDnai());
        reqPayload.put("targetIp", request.getTargetIp());
        reqPayload.put("ueType", ueType);
        reqPayload.put("ueIp", "all".equals(ueType) ? "" : request.getUeIp());
        reqPayload.put("dnn", request.getDnn());
        reqPayload.put("sst", request.getSst());
        reqPayload.put("sd", request.getSd());
        reqPayload.put("networkSegment", request.getNetworkSegment());
        reqPayload.put("upf", request.getUpf());
        reqPayload.put("coreNetworkType", coreNetworkType);
        reqPayload.put("routeProfId", request.getRouteProfId() == null || request.getRouteProfId().isEmpty()
                ? "mec" : request.getRouteProfId());
        reqPayload.put("failureReason", failureMessage);
        return objectMapper.writeValueAsString(reqPayload);
    }

    private Long getNextSignalingId() {
        Long nextId = 1L;
        try {
            List<SignalingDetails> allPolicies = signalingDetailsRepository.findAll();
            if (!allPolicies.isEmpty()) {
                Long maxId = 0L;
                for (SignalingDetails policy : allPolicies) {
                    if (policy.getId() != null && policy.getId() > maxId) {
                        maxId = policy.getId();
                    }
                }
                nextId = maxId + 1;
            }
        } catch (Exception e) {
            logger.warn("Failed to get max ID, using default: {}", e.getMessage());
        }
        return nextId;
    }

    public Map<String, Object> deleteSignalingPolicy(Long policyId) {
        try {
            logger.info("Starting to cancel signaling, Policy ID: {}", policyId);

            // 1. Validate ID validity
            if (policyId == null || policyId <= 0) {
                logger.error("❌ Invalid signaling ID: {}", policyId);
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("code", 400); // Bad Request
                errorResult.put("data", new HashMap<>());
                errorResult.put("msg", "Invalid signaling ID: " + policyId);
                return errorResult;
            }

            // 2. Query signaling record by ID
            Optional<SignalingDetails> optionalSignaling = signalingDetailsRepository.findById(policyId);
            if (!optionalSignaling.isPresent()) {
                logger.error("❌ Failed to query record, ID={}", policyId);
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("code", 500);
                errorResult.put("data", new HashMap<>());
                errorResult.put("msg", "Query signaling record failed");
                return errorResult;
            }

            SignalingDetails signalingDetails = optionalSignaling.get();

            // 3. Execute NEF subscription cancellation
            logger.info("🔍 Starting to cancel NEF subscription, TransactionId: {}",
                    signalingDetails.getTransactionId());
            Map<String, Object> deleteResult = nefClient.cancelTrafficInfluenceRequest(signalingDetails);
            boolean isNefSuccess = (Boolean) deleteResult.get("success");
            boolean isSubscriptionNotFound = Boolean.TRUE.equals(deleteResult.get("subscriptionNotFound"));
            boolean isCancelFailed = Boolean.TRUE.equals(deleteResult.get("cancelFailed"));
            logger.info(isNefSuccess ? "✅ NEF subscription cancellation successful, ID={}"
                    : "❌ NEF subscription cancellation failed, ID={}", policyId);

            // 4. Execute database deletion based on NEF result
            boolean isDbSuccess = false;
            // Check if NEF returned 404 status code
            Integer statusCode = (Integer) deleteResult.get("statusCode");
            boolean is404Error = statusCode != null && statusCode == 404;
            boolean canDeletePfd = isNefSuccess || isSubscriptionNotFound || is404Error;
            boolean isPfdSuccess = false;
            if (canDeletePfd) {
                Map<String, Object> pfdDeleteResult = nefClient.deletePfdRequest(signalingDetails.getPfdTransactionId(),
                        signalingDetails.getCoreNetworkType());
                isPfdSuccess = Boolean.TRUE.equals(pfdDeleteResult.get("success"));
            } else {
                logger.info("Skipping PFD deletion because traffic influence cancellation failed, ID: {}", policyId);
            }

            if (canDeletePfd && isPfdSuccess) {
                try {
                    signalingDetailsRepository.deleteById(policyId);
                    logger.info("✅ Database record deletion successful, ID: {}", policyId);
                    isDbSuccess = true;
                } catch (Exception e) {
                    logger.error("❌ Database record deletion failed, ID: {}, Error: {}", policyId, e.getMessage());
                    isDbSuccess = false;
                }
            } else if (isCancelFailed) {
                logger.info("⚠️ NEF deletion failed (CANCEL_FAILED), skipping database deletion, ID: {}", policyId);
            } else {
                logger.info("⚠️ NEF cancellation failed, skipping database deletion, ID: {}", policyId);
            }

            // 5. Determine final response based on NEF and DB results
            int resCode;
            String finalMsg;
            if (isNefSuccess && isDbSuccess) {
                finalMsg = "NEF cancellation successful - Database deletion successful";
                resCode = 200;
            } else if ((isNefSuccess || isSubscriptionNotFound || is404Error) && !isPfdSuccess) {
                finalMsg = "PFD deletion failed - Database deletion skipped";
                resCode = 502;
            } else if (isNefSuccess && !isDbSuccess) {
                finalMsg = "NEF cancellation successful - Database deletion failed";
                resCode = 500;
            } else if (isSubscriptionNotFound && isDbSuccess) {
                finalMsg = "NEF subscription not found - Database deletion successful";
                resCode = 200;
            } else if (isSubscriptionNotFound && !isDbSuccess) {
                finalMsg = "NEF subscription not found - Database deletion failed";
                resCode = 500;
            } else if (is404Error && isDbSuccess) {
                finalMsg = "NEF returned 404 - Database deletion successful";
                resCode = 200;
            } else if (is404Error && !isDbSuccess) {
                finalMsg = "NEF returned 404 - Database deletion failed";
                resCode = 500;
            } else if (isCancelFailed) {
                finalMsg = "NEF deletion failed (CANCEL_FAILED) - Please contact administrator";
                resCode = 502;
            } else if (canDeletePfd && !isPfdSuccess) {
                finalMsg = "PFD deletion failed - Database deletion skipped";
                resCode = 502;
            } else {
                finalMsg = "NEF cancellation failed - Database deletion skipped";
                resCode = 502;
            }

            logger.info("==================== Cancel signaling result ====================");
            logger.info("Signaling ID: {}", policyId);
            logger.info("NEF status: {}", isNefSuccess);
            logger.info("PFD status: {}", isPfdSuccess);
            logger.info("Database status: {}", isDbSuccess);
            logger.info("Final result: {}", finalMsg);
            logger.info("======================================================");

            // Prepare response according to frontend expectation
            Map<String, Object> result = new HashMap<>();
            result.put("code", resCode);
            if ((isNefSuccess || isSubscriptionNotFound || is404Error) && isPfdSuccess && isDbSuccess) {
                result.put("data", createDataMap("id", policyId));
            } else {
                result.put("data", new HashMap<>()); // Empty data on error
            }
            result.put("msg", finalMsg);
            return result;

        } catch (Exception e) {
            logger.error("Failed to delete signaling policy: ", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("code", 500);
            errorResult.put("data", new HashMap<>());
            errorResult.put("msg", e.getMessage());
            return errorResult;
        }
    }

    public Map<String, Object> getAllSignalingPolicies(Integer page, Integer size, String coreNetworkType) {
        try {
            List<SignalingDetails> allPolicies = filterPoliciesByCoreNetwork(
                    signalingDetailsRepository.findAll(), coreNetworkType);
            int total = allPolicies.size();

            // 计算分页
            int start = (page - 1) * size;
            int end = Math.min(start + size, total);

            // 截取分页数据
            List<SignalingDetails> paginatedPolicies = new ArrayList<>();
            if (start < total) {
                paginatedPolicies = allPolicies.subList(start, end);
            }

            String msg = "查询成功，共" + total + "条数据";

            // Convert SignalingDetails to map with ueType and ueIp
            List<Map<String, Object>> dataWithUeInfo = new ArrayList<>();
            for (SignalingDetails policy : paginatedPolicies) {
                Map<String, Object> policyMap = new HashMap<>();
                policyMap.put("id", policy.getId());
                policyMap.put("transactionId", policy.getTransactionId());
                policyMap.put("appInstanceId", policy.getAppInstanceId());
                policyMap.put("targetIp", policy.getTargetIp());
                policyMap.put("targetDnai", policy.getTargetDnai());
                policyMap.put("ueType", policy.getUeType());
                policyMap.put("ueIp", policy.getUeIp());
                policyMap.put("dnn", policy.getDnn());
                policyMap.put("sst", policy.getSst());
                policyMap.put("sd", policy.getSd());
                policyMap.put("networkSegment", policy.getNetworkSegment());
                policyMap.put("upf", policy.getUpf());
                policyMap.put("routeProfId", policy.getRouteProfId());
                policyMap.put("coreNetworkType", policy.getCoreNetworkType());
                policyMap.put("requestPayload", policy.getRequestPayload());
                policyMap.put("responseCode", policy.getResponseCode());
                policyMap.put("responseBody", policy.getResponseBody());
                policyMap.put("status", policy.getStatus());
                policyMap.put("createTime", policy.getCreateTime());
                policyMap.put("updateTime", policy.getUpdateTime());

                // 查询app_name
                String appName = null;
                try {
                    appName = mecApplicationRepository.findAppNameByAppInstanceId(policy.getAppInstanceId());
                } catch (Exception e) {
                    logger.warn("Failed to get app name for appInstanceId: {}", policy.getAppInstanceId(), e);
                }
                policyMap.put("appName", appName);

                dataWithUeInfo.add(policyMap);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("data", dataWithUeInfo);
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);
            result.put("msg", msg);
            return result;

        } catch (Exception e) {
            logger.error("Failed to retrieve signaling policies: ", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("code", 500);
            errorResult.put("data", new ArrayList<>());
            errorResult.put("total", 0);
            errorResult.put("page", page);
            errorResult.put("size", size);
            errorResult.put("msg", "查询失败：" + e.getMessage());
            return errorResult;
        }
    }

    private List<SignalingDetails> filterPoliciesByCoreNetwork(List<SignalingDetails> policies, String coreNetworkType) {
        String targetCoreNetworkType = coreNetworkType != null && !coreNetworkType.isEmpty()
                ? normalizeCoreNetworkType(coreNetworkType) : DEFAULT_CORE_NETWORK;
        List<SignalingDetails> filteredPolicies = new ArrayList<>();
        for (SignalingDetails policy : policies) {
            String policyCoreNetworkType = policy.getCoreNetworkType() != null && !policy.getCoreNetworkType().isEmpty()
                    ? normalizeCoreNetworkType(policy.getCoreNetworkType()) : DEFAULT_CORE_NETWORK;
            if (targetCoreNetworkType.equals(policyCoreNetworkType)) {
                filteredPolicies.add(policy);
            }
        }
        return filteredPolicies;
    }

    public Map<String, Object> getSignalingProgressByTenant(String tenantId, String appInstanceIds) {
        try {
            List<MecApplication> tenantApps = mecApplicationRepository.findByTenantId(tenantId);
            Set<String> tenantAppIdSet = new HashSet<>();
            for (MecApplication app : tenantApps) {
                if (app != null && app.getAppInstanceId() != null && !app.getAppInstanceId().isEmpty()) {
                    tenantAppIdSet.add(app.getAppInstanceId());
                }
            }

            Set<String> requestAppIdSet = new HashSet<>();
            if (appInstanceIds != null && !appInstanceIds.trim().isEmpty()) {
                requestAppIdSet.addAll(Arrays.asList(appInstanceIds.split(",")));
            }

            Set<String> targetAppIdSet = new HashSet<>();
            if (requestAppIdSet.isEmpty()) {
                targetAppIdSet.addAll(tenantAppIdSet);
            } else {
                for (String appInstanceId : requestAppIdSet) {
                    if (appInstanceId != null) {
                        String normalized = appInstanceId.trim();
                        if (!normalized.isEmpty() && tenantAppIdSet.contains(normalized)) {
                            targetAppIdSet.add(normalized);
                        }
                    }
                }
            }

            List<Map<String, Object>> progressList = new ArrayList<>();
            if (targetAppIdSet.isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("code", 200);
                result.put("data", progressList);
                result.put("msg", "查询成功，共0条数据");
                return result;
            }

            List<String> targetAppIdList = new ArrayList<>(targetAppIdSet);
            List<SignalingDetails> signalingList = signalingDetailsRepository.findByAppInstanceIdIn(targetAppIdList);
            Map<String, SignalingDetails> latestByApp = new HashMap<>();
            for (SignalingDetails item : signalingList) {
                if (item == null || item.getAppInstanceId() == null) {
                    continue;
                }
                SignalingDetails current = latestByApp.get(item.getAppInstanceId());
                if (current == null || isNewer(item, current)) {
                    latestByApp.put(item.getAppInstanceId(), item);
                }
            }

            for (String appInstanceId : targetAppIdList) {
                SignalingDetails latest = latestByApp.get(appInstanceId);
                Map<String, Object> progress = new HashMap<>();
                progress.put("appInstanceId", appInstanceId);
                if (latest == null) {
                    progress.put("status", "NONE");
                    progress.put("signalingId", null);
                    progress.put("updateTime", null);
                } else {
                    progress.put("status", latest.getStatus());
                    progress.put("signalingId", latest.getId());
                    progress.put("updateTime", latest.getUpdateTime());
                }
                progressList.add(progress);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("data", progressList);
            result.put("msg", "查询成功，共" + progressList.size() + "条数据");
            return result;
        } catch (Exception e) {
            logger.error("Failed to retrieve signaling progress by tenant: {}", tenantId, e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("code", 500);
            errorResult.put("data", new ArrayList<>());
            errorResult.put("msg", "查询失败：" + e.getMessage());
            return errorResult;
        }
    }

    public void cleanupTaskSignaling(String tenantId, String appId, String appInstanceId) {
        Set<String> targetAppInstanceIds = new HashSet<>();
        if (appInstanceId != null && !appInstanceId.trim().isEmpty()) {
            targetAppInstanceIds.add(appInstanceId.trim());
        }

        if (targetAppInstanceIds.isEmpty() && appId != null && !appId.trim().isEmpty()) {
            List<MecApplication> tenantApps = mecApplicationRepository.findByTenantId(tenantId);
            for (MecApplication app : tenantApps) {
                if (app != null && appId.equals(app.getPackageId())
                        && app.getAppInstanceId() != null && !app.getAppInstanceId().isEmpty()) {
                    targetAppInstanceIds.add(app.getAppInstanceId());
                }
            }
        }

        if (targetAppInstanceIds.isEmpty()) {
            logger.info("No app instance matched for signaling cleanup, tenantId: {}, appId: {}, appInstanceId: {}",
                    tenantId, appId, appInstanceId);
            return;
        }

        List<SignalingDetails> signalings = signalingDetailsRepository
                .findByAppInstanceIdIn(new ArrayList<>(targetAppInstanceIds));
        for (SignalingDetails signaling : signalings) {
            try {
                if (signaling == null || signaling.getId() == null) {
                    continue;
                }

                String transactionId = signaling.getTransactionId();
                if ("FAILED".equals(signaling.getStatus()) || transactionId == null || transactionId.trim().isEmpty()
                        || transactionId.startsWith("nef-")) {
                    nefClient.deletePfdRequest(signaling.getPfdTransactionId(), signaling.getCoreNetworkType());
                    signalingDetailsRepository.deleteById(signaling.getId());
                    continue;
                }

                Map<String, Object> deleteResult = nefClient.cancelTrafficInfluenceRequest(signaling);
                boolean nefSuccess = Boolean.TRUE.equals(deleteResult.get("success"));
                Map<String, Object> pfdDeleteResult = nefClient.deletePfdRequest(signaling.getPfdTransactionId(),
                        signaling.getCoreNetworkType());
                boolean pfdSuccess = Boolean.TRUE.equals(pfdDeleteResult.get("success"));
                // Check if NEF returned 404 status code
                Object statusCodeObj = deleteResult.get("statusCode");
                Integer statusCode = null;
                if (statusCodeObj instanceof Number) {
                    statusCode = ((Number) statusCodeObj).intValue();
                }
                boolean is404Error = statusCode != null && statusCode == 404;

                if ((!nefSuccess && !is404Error) || !pfdSuccess) {
                    Object responseBodyObj = deleteResult.get("responseBody");
                    String responseBody = responseBodyObj == null
                            ? "NEF cancellation failed during task deletion"
                            : String.valueOf(responseBodyObj);
                    if (!pfdSuccess) {
                        responseBody = "PFD deletion failed during task deletion: "
                                + String.valueOf(pfdDeleteResult.get("responseBody"));
                    }

                    signaling.setStatus("CANCEL_FAILED");
                    signaling.setResponseCode(statusCode);
                    signaling.setResponseBody(responseBody);
                    signaling.setUpdateTime(new Timestamp(System.currentTimeMillis()));
                    signalingDetailsRepository.save(signaling);

                    logger.warn(
                            "NEF cancellation failed but task deletion will continue, signalingId: {}, statusCode: {}",
                            signaling.getId(), statusCode);
                    continue;
                }
                signalingDetailsRepository.deleteById(signaling.getId());
            } catch (Exception ex) {
                logger.warn("Failed to cleanup signaling during task deletion, signalingId: {}",
                        signaling == null ? null : signaling.getId(), ex);
            }
        }
    }

    private boolean isNewer(SignalingDetails candidate, SignalingDetails baseline) {
        Timestamp candidateTime = candidate.getUpdateTime() != null ? candidate.getUpdateTime()
                : candidate.getCreateTime();
        Timestamp baselineTime = baseline.getUpdateTime() != null ? baseline.getUpdateTime() : baseline.getCreateTime();
        if (candidateTime != null && baselineTime != null) {
            return candidateTime.after(baselineTime);
        }
        if (candidateTime != null) {
            return true;
        }
        if (baselineTime != null) {
            return false;
        }
        Long candidateId = candidate.getId();
        Long baselineId = baseline.getId();
        if (candidateId == null) {
            return false;
        }
        if (baselineId == null) {
            return true;
        }
        return candidateId > baselineId;
    }

    /**
     * 删除失败信令策略（仅删除数据库记录）
     */
    public Map<String, Object> deleteFailedSignalingPolicy(Long policyId) {
        try {
            logger.info("Deleting failed signaling policy, ID: {}", policyId);

            // 1. 验证ID有效性
            if (policyId == null || policyId <= 0) {
                logger.error("❌ Invalid signaling ID: {}", policyId);
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("code", 400);
                errorResult.put("data", new HashMap<>());
                errorResult.put("msg", "Invalid signaling ID: " + policyId);
                return errorResult;
            }

            // 2. 查询信令记录
            Optional<SignalingDetails> optionalSignaling = signalingDetailsRepository.findById(policyId);
            if (!optionalSignaling.isPresent()) {
                logger.error("❌ Signaling policy not found, ID: {}", policyId);
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("code", 404);
                errorResult.put("data", new HashMap<>());
                errorResult.put("msg", "Signaling policy not found");
                return errorResult;
            }

            SignalingDetails signalingDetails = optionalSignaling.get();

            // 3. 检查状态是否为FAILED
            if (!"FAILED".equals(signalingDetails.getStatus())) {
                logger.error("❌ Signaling policy status is not FAILED, ID: {}, status: {}", policyId,
                        signalingDetails.getStatus());
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("code", 400);
                errorResult.put("data", new HashMap<>());
                errorResult.put("msg", "Only FAILED signaling policies can be deleted");
                return errorResult;
            }

            // 4. 直接删除数据库记录
            nefClient.deletePfdRequest(signalingDetails.getPfdTransactionId(), signalingDetails.getCoreNetworkType());
            signalingDetailsRepository.deleteById(policyId);
            logger.info("✅ Failed signaling policy deleted successfully, ID: {}", policyId);

            // 5. 返回成功响应
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("data", createDataMap("id", policyId));
            result.put("msg", "Failed signaling policy deleted successfully");
            return result;

        } catch (Exception e) {
            logger.error("Failed to delete failed signaling policy: ", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("code", 500);
            errorResult.put("data", new HashMap<>());
            errorResult.put("msg", "Delete failed: " + e.getMessage());
            return errorResult;
        }
    }
}
