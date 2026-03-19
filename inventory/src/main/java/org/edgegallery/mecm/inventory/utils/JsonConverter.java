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

package org.edgegallery.mecm.inventory.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JPA converter for converting between JSONB (PostgreSQL) and Map<String, String>.
 * This converter automatically handles JSON serialization/deserialization for entity fields.
 * 
 * <p>Usage example:
 * <pre>
 * {@code
 * @Column(name = "network_planes", columnDefinition = "jsonb")
 * @Convert(converter = JsonConverter.class)
 * private Map<String, String> networkPlanes;
 * }
 * </pre>
 */
@Converter
public class JsonConverter implements AttributeConverter<Map<String, String>, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonConverter.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Converts Map to JSON string for database storage.
     *
     * @param attribute the Map to convert (can be null)
     * @return JSON string representation, or null if input is null or empty
     */
    @Override
    public String convertToDatabaseColumn(Map<String, String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            LOGGER.error("Error converting Map to JSON string: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Converts JSON string from database to Map.
     *
     * @param dbData the JSON string from database (can be null)
     * @return Map representation, or empty Map if input is null or invalid
     */
    @Override
    public Map<String, String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return OBJECT_MAPPER.readValue(dbData, new TypeReference<Map<String, String>>() {});
        } catch (IOException e) {
            LOGGER.error("Error converting JSON string to Map: {}", e.getMessage());
            return new HashMap<>();
        }
    }
}
