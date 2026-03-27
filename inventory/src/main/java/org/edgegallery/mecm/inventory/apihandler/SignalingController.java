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

package org.edgegallery.mecm.inventory.apihandler;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.servicecomb.provider.rest.common.RestSchema;
import org.edgegallery.mecm.inventory.apihandler.dto.SignalingPolicyRequest;
import org.edgegallery.mecm.inventory.service.SignalingService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestSchema(schemaId = "inventory-signaling")
@Api(value = "Inventory Signaling API system")
@Validated
@RequestMapping("/inventory/v1")
@RestController
public class SignalingController {

    private static final Logger logger = LoggerFactory.getLogger(SignalingController.class);

    @Autowired
    private SignalingService signalingService;

    /**
     * 创建信令策略
     */
    @ApiOperation(value = "Creates signaling policy", response = String.class)
    @PostMapping(path = "/signaling/policies", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> createSignalingPolicy(
            @Valid @RequestBody SignalingPolicyRequest request) {

        logger.info("Creating signaling policy for app: {}", request.getAppId());
        Map<String, Object> result = signalingService.createSignalingPolicy(request);

        // Check the response code from the service layer
        Integer responseCode = (Integer) result.get("code");
        HttpStatus status = (responseCode != null && responseCode == 200) ? HttpStatus.OK
                : HttpStatus.INTERNAL_SERVER_ERROR;

        return new ResponseEntity<>(result, status);
    }

    /**
     * Get all signaling policies
     */
    @ApiOperation(value = "Gets all signaling policies", response = String.class)
    @GetMapping(path = "/signaling/show", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getAllSignalingPolicies() {

        logger.info("Retrieving all signaling policies");
        Map<String, Object> result = signalingService.getAllSignalingPolicies();

        // Since the service now returns code in the response body, we can check it
        Integer responseCode = (Integer) result.get("code");
        HttpStatus status = (responseCode != null && responseCode == 200) ? HttpStatus.OK
                : HttpStatus.INTERNAL_SERVER_ERROR;

        return new ResponseEntity<>(result, status);
    }

    /**
     * 删除信令策略
     */
    @ApiOperation(value = "Deletes signaling policy", response = String.class)
    @DeleteMapping(path = "/signaling/delete", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> deleteSignalingPolicy(
            @RequestBody Map<String, Long> request) {

        Long policyId = request.get("id");
        logger.info("Deleting signaling policy with ID: {}", policyId);

        if (policyId == null) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("code", 400);
            errorResult.put("data", new HashMap<>());
            errorResult.put("msg", "Policy ID is required");
            return new ResponseEntity<>(errorResult, HttpStatus.BAD_REQUEST);
        }

        Map<String, Object> result = signalingService.deleteSignalingPolicy(policyId);

        // Check the response code from the service layer
        Integer responseCode = (Integer) result.get("code");
        HttpStatus status;
        if (responseCode != null) {
            if (responseCode == 200) {
                status = HttpStatus.OK;
            } else if (responseCode == 400) {
                status = HttpStatus.BAD_REQUEST;
            } else if (responseCode == 502) {
                status = HttpStatus.BAD_GATEWAY;
            } else {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
            }
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return new ResponseEntity<>(result, status);
    }

    /**
     * 删除失败信令策略（仅删除数据库记录）
     */
    @ApiOperation(value = "Deletes failed signaling policy", response = String.class)
    @DeleteMapping(path = "/signaling/delete-failed", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> deleteFailedSignalingPolicy(
            @RequestBody Map<String, Long> request) {

        Long policyId = request.get("id");
        logger.info("Deleting failed signaling policy with ID: {}", policyId);

        if (policyId == null) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("code", 400);
            errorResult.put("data", new HashMap<>());
            errorResult.put("msg", "Policy ID is required");
            return new ResponseEntity<>(errorResult, HttpStatus.BAD_REQUEST);
        }

        Map<String, Object> result = signalingService.deleteFailedSignalingPolicy(policyId);

        // Check the response code from the service layer
        Integer responseCode = (Integer) result.get("code");
        HttpStatus status = (responseCode != null && responseCode == 200) ? HttpStatus.OK
                : HttpStatus.INTERNAL_SERVER_ERROR;

        return new ResponseEntity<>(result, status);
    }
}