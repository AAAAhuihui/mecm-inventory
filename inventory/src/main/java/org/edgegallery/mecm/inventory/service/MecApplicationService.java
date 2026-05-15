package org.edgegallery.mecm.inventory.service;

import org.edgegallery.mecm.inventory.apihandler.dto.AppInstanceWithN6IpDto;

import java.util.List;

public interface MecApplicationService {
    // 获取所有appinstance_id
    List<String> getAllAppinstanceIds();

    // 在原有方法后新增
    List<AppInstanceWithN6IpDto> getAllAppinstanceIdsWithN6Ip();
}