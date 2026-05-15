package org.edgegallery.mecm.inventory.service.impl;

import org.edgegallery.mecm.inventory.service.repository.MecApplicationRepository;
import org.edgegallery.mecm.inventory.service.MecApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

//新增
import org.edgegallery.mecm.inventory.apihandler.dto.AppInstanceWithN6IpDto;

import java.util.ArrayList;

@Service
public class MecApplicationServiceImpl implements MecApplicationService {
    @Autowired
    private MecApplicationRepository mecApplicationRepository;

    @Override
    public List<String> getAllAppinstanceIds() {
        // 调用Repository查询所有appinstance_id
        return mecApplicationRepository.findAllAppinstanceIdByOrderByAppinstanceIdAsc();
    }

    // 新增
    @Override
    public List<AppInstanceWithN6IpDto> getAllAppinstanceIdsWithN6Ip() {
        List<Object[]> results = mecApplicationRepository.findAllAppinstanceIdWithN6Ip();
        List<AppInstanceWithN6IpDto> dtoList = new ArrayList<>();
        for (Object[] result : results) {
            String appInstanceId = (String) result[0];
            String n6Ip = (String) result[1];
            String appName = result.length > 2 ? (String) result[2] : null;
            dtoList.add(new AppInstanceWithN6IpDto(appInstanceId, n6Ip, appName));
        }
        return dtoList;
    }
}