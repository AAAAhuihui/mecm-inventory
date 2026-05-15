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

package org.edgegallery.mecm.inventory.service.repository;

import java.util.List;
import javax.transaction.Transactional;
import org.edgegallery.mecm.inventory.model.MecApplication;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * MEC application repository.
 */
public interface MecApplicationRepository extends CrudRepository<MecApplication, String>,
    BaseRepository<MecApplication> {

  @Transactional
  @Modifying
  @Query("delete from MecApplication m where m.tenantId=:tenantId")
  void deleteByTenantId(@Param("tenantId") String tenantId);

  @Query(value = "SELECT * FROM mecapplicationinventory m WHERE m.tenant_id=:tenantId", nativeQuery = true)
  List<MecApplication> findByTenantId(@Param("tenantId") String tenantId);

  @Query(value = "SELECT * FROM mecapplicationinventory m WHERE m.role=:role", nativeQuery = true)
  List<MecApplication> findByUserRole(@Param("role") String role);

  /**
   * 查询所有appinstance_id并按appinstance_id升序排序
   * 
   * @return appinstance_id列表
   */
  @Query(value = "SELECT appinstance_id FROM mecapplicationinventory ORDER BY appinstance_id ASC", nativeQuery = true)
  List<String> findAllAppinstanceIdByOrderByAppinstanceIdAsc();

  /**
   * 查询所有appinstance_id和对应的N6IP
   * 
   * @return 包含appinstance_id和N6IP的对象数组列表
   */
  @Query(value = "SELECT appinstance_id, app_ip as n6_ip, app_name FROM mecapplicationinventory", nativeQuery = true)
  List<Object[]> findAllAppinstanceIdWithN6Ip();

  /**
   * 通过appinstance_id查询app_name
   * 
   * @param appInstanceId app实例ID
   * @return app名称
   */
  @Query(value = "SELECT app_name FROM mecapplicationinventory WHERE appinstance_id=:appInstanceId", nativeQuery = true)
  String findAppNameByAppInstanceId(@Param("appInstanceId") String appInstanceId);

}