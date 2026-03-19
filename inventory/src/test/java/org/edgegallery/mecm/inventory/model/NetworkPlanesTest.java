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

package org.edgegallery.mecm.inventory.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;
import org.edgegallery.mecm.inventory.apihandler.dto.MecHostDto;
import org.edgegallery.mecm.inventory.utils.InventoryUtilities;
import org.edgegallery.mecm.inventory.utils.JsonConverter;
import org.junit.Test;

/**
 * Network planes feature test.
 */
public class NetworkPlanesTest {

    @Test
    public void testJsonConverter() {
        JsonConverter converter = new JsonConverter();
        
        // 测试 Map -> JSON
        Map<String, String> planes = new HashMap<>();
        planes.put("eth1", "n6-net-1");
        planes.put("eth2", "n6-net-2");
        
        String json = converter.convertToDatabaseColumn(planes);
        assertNotNull(json);
        System.out.println("Map -> JSON: " + json);
        
        // 测试 JSON -> Map
        Map<String, String> restored = converter.convertToEntityAttribute(json);
        assertNotNull(restored);
        assertEquals(2, restored.size());
        assertEquals("n6-net-1", restored.get("eth1"));
        assertEquals("n6-net-2", restored.get("eth2"));
        System.out.println("JSON -> Map: " + restored);
    }
    
    @Test
    public void testEntityToDtoMapping() {
        // 创建 MecHost Entity
        MecHost host = new MecHost();
        host.setMechostId("host-001");
        host.setMechostIp("192.168.1.100");
        host.setMechostName("test-host");
        host.setCity("Beijing");
        host.setAddress("Test Address");
        host.setTenantId("tenant-001");
        host.setMepmIp("192.168.1.1");
        host.setCoordinates("39.9042,116.4074");
        
        Map<String, String> networkPlanes = new HashMap<>();
        networkPlanes.put("eth1", "n6-net-1");
        networkPlanes.put("eth2", "n6-net-2");
        networkPlanes.put("eth3", "n3-net-1");
        host.setNetworkPlanes(networkPlanes);
        
        // 使用 ModelMapper 转换为 DTO
        MecHostDto dto = InventoryUtilities.getModelMapper().map(host, MecHostDto.class);
        
        // 验证映射结果
        assertNotNull(dto);
        assertEquals("192.168.1.100", dto.getMechostIp());
        assertEquals("test-host", dto.getMechostName());
        assertNotNull(dto.getNetworkPlanes());
        assertEquals(3, dto.getNetworkPlanes().size());
        assertEquals("n6-net-1", dto.getNetworkPlanes().get("eth1"));
        assertEquals("n6-net-2", dto.getNetworkPlanes().get("eth2"));
        assertEquals("n3-net-1", dto.getNetworkPlanes().get("eth3"));
        
        System.out.println("✅ Entity -> DTO 映射成功!");
        System.out.println("Network Planes: " + dto.getNetworkPlanes());
    }
    
    @Test
    public void testDtoToEntityMapping() {
        // 创建 DTO
        MecHostDto dto = new MecHostDto();
        dto.setMechostIp("192.168.1.101");
        dto.setMechostName("test-host-2");
        dto.setCity("Shanghai");
        dto.setAddress("Test Address 2");
        dto.setCoordinates("31.2304,121.4737");
        
        Map<String, String> networkPlanes = new HashMap<>();
        networkPlanes.put("eth0", "n6-net-3");
        networkPlanes.put("eth1", "n3-net-2");
        dto.setNetworkPlanes(networkPlanes);
        
        // 转换为 Entity
        MecHost host = InventoryUtilities.getModelMapper().map(dto, MecHost.class);
        
        // 验证
        assertNotNull(host);
        assertEquals("192.168.1.101", host.getMechostIp());
        assertNotNull(host.getNetworkPlanes());
        assertEquals(2, host.getNetworkPlanes().size());
        assertEquals("n6-net-3", host.getNetworkPlanes().get("eth0"));
        assertEquals("n3-net-2", host.getNetworkPlanes().get("eth1"));
        
        System.out.println("✅ DTO -> Entity 映射成功!");
        System.out.println("Network Planes: " + host.getNetworkPlanes());
    }
}
