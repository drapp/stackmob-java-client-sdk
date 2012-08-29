/**
 * Copyright 2011 StackMob
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

package com.stackmob.sdk.api;

import com.stackmob.sdk.util.BinaryFieldFormatter;

/**
 * Represents binary data to be uploaded to StackMob and hosted on S3 as a file.
 */
public class StackMobFile {
    
    private String value;

    /**
     * create a StackMobFile with the information needed for the corresponding S3 file
     * @param contentType The content type of the file to be created
     * @param fileName The filename of the file to be created
     * @param bytes The bytes of the file to be created
     */
    public StackMobFile(String contentType, String fileName, byte[] bytes) {
        BinaryFieldFormatter formatter = new BinaryFieldFormatter(contentType, fileName, bytes);
        value = formatter.getJsonValue();
    }
    
    @Override
    public String toString() {
        return value;
    }
    
}
