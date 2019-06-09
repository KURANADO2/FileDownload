package com.kuranado.util;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * 并行流下载多个文件
 *
 * @Author: Xinling Jing
 * @Date: 2019-06-07 19:21
 */
public class ParallelStreamFileDownUtils {

    public static void downloadFromUrl(String url, String dir) {

        try {
            URL httpUrl = new URL(url);
            String fileName = getFileNameFromUrl(url);
            System.out.println(fileName);
            File file = new File(dir + fileName);
            FileUtils.copyURLToFile(httpUrl, file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getFileNameFromUrl(String url) {
        String name = System.currentTimeMillis() + ".X";
        int index = url.lastIndexOf("/");
        if (index > 0) {
            name = url.substring(index + 1);
            if (name.trim().length() > 0) {
                return name;
            }
        }
        return name;
    }

    public static void main(String[] args) {
        long beginTime = System.currentTimeMillis();
        // 此 List 用于存放下载 url
        List<String> urlList = new ArrayList<>();
        /// urlList.add(...);
        String dirname = String.valueOf(System.currentTimeMillis());
        urlList.parallelStream().forEach(url -> downloadFromUrl(url, "/Users/jing/Downloads/" + dirname + "/"));
        System.out.println("下载耗时:" + (double) (System.currentTimeMillis() - beginTime) / 1000 + " s");
    }

}
