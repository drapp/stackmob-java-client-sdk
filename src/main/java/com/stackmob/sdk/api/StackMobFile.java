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
    private String url;

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

    /**
     * create a StackMobFile that just wraps an url from S3
     * @param url the S3 url
     */
    public StackMobFile(String url) {
      this.url = url;
    }

    /**
     * get the url on S3 where this file has been uploaded
     * @return the S3 url
     */
    public String getS3Url() {
        return url;
    }

    /**
     * set the url for this file once uploaded
     * @param url the url where the file is located
     */
    public void setS3Url(String url) {
        this.value = null;
        this.url = url;
    }

    /**
     * get a binary string suitable for posting to StackMob
     * @return a binary representation of this file
     */
    public String getBinaryString() {
        return value;
    }
    
    @Override
    public String toString() {
        return value;
    }
    
}
