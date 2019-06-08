package com.kuranado.util;

import lombok.Data;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CountDownLatch;

/**
 * 多线程分块下载单个文件，最后合并为一个文件
 *
 * @Author: Xinling Jing
 * @Date: 2019-06-08 12:44
 */
@Data
public class DownFile {

    /**
     * 文件下载路径
     */
    private URL fileUrl;
    /**
     * 文件下载的线程数
     */
    private int threadCount;
    /**
     * 每个线程下载文件的开始位置
     */
    private int startPos;
    /**
     * 每个线程需下载文件的长度
     */
    private int size;
    /**
     * 下载的文件路径(包含文件名)
     */
    private String pathName;
    /**
     * 线程数组
     */
    private DownThread[] downThreads;
    private static CountDownLatch countDownLatch;

    private DownFile(URL fileUrl, int threadCount, String pathName) throws IOException {
        this.fileUrl = fileUrl;
        this.threadCount = threadCount;
        this.pathName = pathName;
        init();
    }

    /**
     * 初始化
     */
    private void init() throws IOException {
        downThreads = new DownThread[threadCount];
        countDownLatch = new CountDownLatch(threadCount);
        HttpURLConnection conn = (HttpURLConnection) fileUrl.openConnection();
        conn.setConnectTimeout(5000);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("connection", "keep-alive");
        // 文件总长度
        int fileLength = conn.getContentLength();
        System.out.println("文件总长度:" + fileLength);
        size = fileLength / threadCount;
        System.out.println("每个线程大约需下载的文件长度:" + size + " Byte");
        // 断开链接
        conn.disconnect();
    }

    /**
     * 开始下载
     */
    private void startDown() {
        for (int i = 0; i < threadCount; i++) {
            try {
                RandomAccessFile randomAccessFile = new RandomAccessFile(pathName, "rw");
                downThreads[i] = new DownThread(i * size, randomAccessFile, i);
                downThreads[i].start();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 下载线程类
     *
     * @author luweicheng
     */
    class DownThread extends Thread {

        /**
         * 开始的位置
         */
        private int startPos;
        private InputStream inputStream;
        /**
         * RandomAccessFile 可以从文件任意位置操作文件，此处 Random 表示"任意"的意思，而不是"随机"
         */
        private RandomAccessFile randomAccessFile;
        /**
         * 下载的文件长度
         */
        private int length;
        /**
         * 线程标志
         */
        private int flag;

        DownThread(int startPos, RandomAccessFile randomAccessFile, int flag) {
            this.startPos = startPos;
            this.randomAccessFile = randomAccessFile;
            this.flag = flag;
        }

        @Override
        public void run() {
            try {
                HttpURLConnection connection = (HttpURLConnection) fileUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("connection", "keep-alive");
                connection.setConnectTimeout(5000);
                inputStream = connection.getInputStream();
                // 跳过 startPos 个 Byte
                inputStream.skip(startPos);
                // 指针移到 startPos 处
                randomAccessFile.seek(startPos);
                // 设定缓冲区为 8 KB
                byte[] buf = new byte[8 * 1024];
                // 读出的字节数
                int hasRead;
                while (length < size && (hasRead = inputStream.read(buf)) != -1) {
                    randomAccessFile.write(buf, 0, hasRead);
                    length += hasRead;
                }
                System.out.println("线程-" + flag + "-下载完成,下载 " + length + " Byte");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                // 数值减一
                countDownLatch.countDown();
                try {
                    inputStream.close();
                    randomAccessFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        long beginTime = System.currentTimeMillis();
        new DownFile(new URL("https://download.jetbrains.8686c.com/go/goland-2019.1.3.dmg"), 3, "/Users/jing" +
            "/Downloads/golang.dmg").startDown();
        // 阻塞当前线程，直到 countDownLatch 中数值为 0 后继续执行当前线程
        countDownLatch.await();
        System.out.println("下载耗时:" + (double) (System.currentTimeMillis() - beginTime) / 1000 + " s");
    }
}