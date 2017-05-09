/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.spring.initializr.web.support;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.client.RestTemplate;

import io.spring.initializr.metadata.DefaultMetadataElement;

/**
 * Reads metadata from the main spring.io website. This is a stateful service: create a new instance whenever you need to refresh the content.
 *
 * @author Stephane Nicoll
 */
public class SpringBootMetadataReader {

    private final JSONObject content;

    private static final String DEFAULT_ENCODING = "UTF-8";
    private static final int PROTECTED_LENGTH = 51200;

    /**
     * InputStream转字符串
     * 
     * @param input
     * @return
     * @throws Exception
     */
    private String readInfoStream(InputStream input) throws Exception {
        if (input == null) {
            throw new Exception("输入流为null");
        }
        // 字节数组
        byte[] bcache = new byte[2048];
        int readSize = 0;// 每次读取的字节长度
        int totalSize = 0;// 总字节长度
        ByteArrayOutputStream infoStream = new ByteArrayOutputStream();
        try {
            // 一次性读取2048字节
            while ((readSize = input.read(bcache)) > 0) {
                totalSize += readSize;
                if (totalSize > PROTECTED_LENGTH) {
                    throw new Exception("输入流超出50K大小限制");
                }
                // 将bcache中读取的input数据写入infoStream
                infoStream.write(bcache, 0, readSize);
            }
        } catch (IOException e1) {
            throw new Exception("输入流读取异常");
        } finally {
            try {
                // 输入流关闭
                input.close();
            } catch (IOException e) {
                throw new Exception("输入流关闭异常");
            }
        }

        try {
            return infoStream.toString(DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new Exception("输出异常");
        }
    }

    /**
     * Parse the content of the metadata at the specified url
     * 
     * @throws FileNotFoundException
     * @throws JSONException
     */
    public SpringBootMetadataReader(RestTemplate restTemplate, String url) {

        String result = null;

        try {
            //流转字符串
            result = readInfoStream(this.getClass().getClassLoader().getResourceAsStream("metadata.json"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        /**
         * 从配置文件中读取metadata
         */
        this.content = new JSONObject(result);

        /**
         * 从 https://spring.io/project_metadata/spring-boot 读取metadata
         */
        // this.content = new JSONObject(restTemplate.getForObject(url, String.class));
    }

    /**
     * Return the boot versions parsed by this instance.
     */
    public List<DefaultMetadataElement> getBootVersions() {
        JSONArray array = content.getJSONArray("projectReleases");
        List<DefaultMetadataElement> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject it = array.getJSONObject(i);
            DefaultMetadataElement version = new DefaultMetadataElement();
            version.setId(it.getString("version"));
            String name = it.getString("versionDisplayName");
            version.setName(it.getBoolean("snapshot") ? name + " (SNAPSHOT)" : name);
            version.setDefault(it.getBoolean("current"));
            list.add(version);
        }
        return list;
    }

}
