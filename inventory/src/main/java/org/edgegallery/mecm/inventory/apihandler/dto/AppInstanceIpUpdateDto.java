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

package org.edgegallery.mecm.inventory.apihandler.dto;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.edgegallery.mecm.inventory.utils.Constants;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public final class AppInstanceIpUpdateDto {

    @NotEmpty(message = "app ip is empty")
    @Pattern(regexp = Constants.IP_REGEX, message = "app ip is invalid")
    @Size(max = 15)
    private String appIp;

    @Size(max = 64)
    private String source;
}
