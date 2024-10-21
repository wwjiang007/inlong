/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sdk.dirtydata.sink;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Configure {

    private Map<String, String> data;

    public Configure(Map<String, String> data) {
        this.data = new ConcurrentHashMap<>();
        this.data.putAll(data);
    }

    public String get(String key, String defaultValue) {
        String value = data.get(key);
        if (value != null) {
            return value;
        }
        return defaultValue;
    }

    public String get(String key) {
        return data.get(key);
    }

    public Boolean getBoolean(String key, boolean defaultValue) {
        String value = data.get(key);
        if (value != null) {
            return Boolean.valueOf(value);
        }
        return defaultValue;
    }
}
