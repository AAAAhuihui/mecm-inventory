package org.edgegallery.mecm.inventory.apihandler;

import org.edgegallery.mecm.inventory.exception.InventoryExceptionResponse;
import org.edgegallery.mecm.inventory.service.MecApplicationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;
import java.util.List;
//新增
import org.edgegallery.mecm.inventory.apihandler.dto.AppInstanceWithN6IpDto;

@RestController
@RequestMapping("/inventory/v1/mecapplication")
@Api(tags = "MEC应用实例接口")
public class MecApplicationInventoryHandler {
    @Autowired
    private MecApplicationService mecApplicationService;

    // 新增
    @GetMapping("/all-appinstance-ids-with-n6ip")
    @ApiOperation("查询所有应用实例ID（APPID）及对应的N6 IP")
    public InventoryExceptionResponse getAllAppinstanceIdsWithN6Ip() {
        try {
            List<AppInstanceWithN6IpDto> data = mecApplicationService.getAllAppinstanceIdsWithN6Ip();
            return new InventoryExceptionResponse(200, "查询成功", data);
        } catch (Exception e) {
            return new InventoryExceptionResponse(500, "查询失败: " + e.getMessage(), null);
        }
    }
}
