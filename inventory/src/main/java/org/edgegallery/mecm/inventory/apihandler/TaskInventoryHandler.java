/*
 *  Copyright 2026 Huawei Technologies Co., Ltd.
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
import io.swagger.annotations.ApiParam;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import org.apache.servicecomb.provider.rest.common.RestSchema;
import org.edgegallery.mecm.inventory.apihandler.dto.TaskDto;
import org.edgegallery.mecm.inventory.model.TaskRecord;
import org.edgegallery.mecm.inventory.service.repository.TaskRepository;
import org.edgegallery.mecm.inventory.utils.Constants;
import org.edgegallery.mecm.inventory.utils.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Task inventory API handler.
 */
@RestSchema(schemaId = "inventory-task")
@Api(value = "Inventory task api system")
@Validated
@RequestMapping("/inventory/v1")
@Controller
public class TaskInventoryHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskInventoryHandler.class);
    private static final String TENANT_ID = "tenant_id";
    private static final String TASK_ID = "task_id";

    @Autowired
    private TaskRepository repository;

    /**
     * Create task record.
     *
     * @param tenantId tenant identifier
     * @param taskDto task payload
     * @return save status
     */
    @ApiOperation(value = "Create task record", response = Status.class)
    @PostMapping(path = "/tenants/{tenant_id}/tasks", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Status> addTaskRecord(
            @ApiParam(value = "tenant identifier") @PathVariable(TENANT_ID)
            @Pattern(regexp = Constants.TENANT_ID_REGEX) @Size(max = 64) String tenantId,
            @Valid @ApiParam(value = "task information") @RequestBody TaskDto taskDto) {
        TaskRecord task = new TaskRecord();
        task.setTaskId(UUID.randomUUID().toString());
        task.setTenantId(tenantId);
        task.setTaskName(taskDto.getTaskName());
        task.setAppId(taskDto.getAppId());
        task.setAppName(taskDto.getAppName());
        task.setTaskType(taskDto.getTaskType());
        task.setUserName(Constants.ADMIN_USER);
        task.setUserRole(Constants.ROLE_TENANT);
        repository.save(task);
        LOGGER.info("Task created, tenant {}, taskId {}", tenantId, task.getTaskId());
        return new ResponseEntity<>(new Status("Saved"), HttpStatus.OK);
    }

    /**
     * Get all tasks by tenant.
     *
     * @param tenantId tenant identifier
     * @return task list
     */
    @ApiOperation(value = "Get all tasks", response = List.class)
    @GetMapping(path = "/tenants/{tenant_id}/tasks", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TaskDto>> getTaskRecords(
            @ApiParam(value = "tenant identifier") @PathVariable(TENANT_ID)
            @Pattern(regexp = Constants.TENANT_ID_REGEX) @Size(max = 64) String tenantId) {
        List<TaskDto> taskDtos = repository.findByTenantIdOrderByCreatedTimeDesc(tenantId).stream().map(task -> {
            TaskDto dto = new TaskDto();
            dto.setTaskId(task.getTaskId());
            dto.setTaskName(task.getTaskName());
            dto.setAppId(task.getAppId());
            dto.setAppName(task.getAppName());
            dto.setTaskType(task.getTaskType());
            dto.setCreatedTime(task.getCreatedTime() == null ? "" : task.getCreatedTime().toString());
            return dto;
        }).collect(Collectors.toList());
        return new ResponseEntity<>(taskDtos, HttpStatus.OK);
    }

    /**
     * Delete task by id.
     *
     * @param tenantId tenant identifier
     * @param taskId task identifier
     * @return delete status
     */
    @ApiOperation(value = "Delete task record", response = Status.class)
    @DeleteMapping(path = "/tenants/{tenant_id}/tasks/{task_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Status> deleteTaskRecord(
            @ApiParam(value = "tenant identifier") @PathVariable(TENANT_ID)
            @Pattern(regexp = Constants.TENANT_ID_REGEX) @Size(max = 64) String tenantId,
            @ApiParam(value = "task identifier") @PathVariable(TASK_ID)
            @Size(max = 64) String taskId) {
        TaskRecord record = repository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException(Constants.RECORD_NOT_FOUND_ERROR));
        if (!tenantId.equals(record.getTenantId())) {
            throw new IllegalArgumentException("tenant id does not match task owner");
        }
        repository.deleteById(taskId);
        LOGGER.info("Task deleted, tenant {}, taskId {}", tenantId, taskId);
        return new ResponseEntity<>(new Status("Deleted"), HttpStatus.OK);
    }
}
