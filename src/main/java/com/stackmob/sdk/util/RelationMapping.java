/**
 * Copyright 2012 StackMob
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stackmob.sdk.util;

import java.util.*;

public class RelationMapping {
    private Map<String,String> relations = new HashMap<String, String>();
    private String basePath = "";


    public void add(String path, String schemaName) {
        String newPath = basePath + path.toLowerCase();
        relations.put(newPath, schemaName);
        basePath = newPath + ".";
    }

    public void leave() {
        basePath = basePath.substring(0,basePath.lastIndexOf(".", basePath.length() - 2) + 1);
    }

    public boolean isEmpty() {
        return relations.isEmpty();
    }


    public String toHeaderString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String,String> entry : relations.entrySet()) {
            if(!first) sb.append("&");
            first = false;
            sb.append(entry.getKey());
            sb.append("=");
            sb.append(entry.getValue());
        }
        return sb.toString();
    }
}
