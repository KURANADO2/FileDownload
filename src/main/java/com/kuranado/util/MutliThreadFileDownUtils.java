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
 * 多线程分块下载单个文件，使用 RandomAccessFile 保存为一个文件
 *
 * @Author: Xinling Jing
 * @Date: 2019-06-08 12:44
 */
@Data
public class MutliThreadFileDownUtils {

    /**
     * 文件下载 url
     */
    private URL fileUrl;
    /**
     * 文件总字节数
     */
    private int fileLength;
    /**
     * 文件下载的线程数
     */
    private int threadCount;
    /**
     * 每个线程需下载文件块的字节数
     */
    private int blockSize;
    /**
     * 下载的文件路径(包含文件名)
     */
    private String pathName;
    /**
     * 线程数组
     */
    private DownThread[] downThreads;
    private static CountDownLatch countDownLatch;

    private MutliThreadFileDownUtils(URL fileUrl, int threadCount, String pathName) throws IOException {
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
        HttpURLConnection connection = (HttpURLConnection) fileUrl.openConnection();
        connection.setConnectTimeout(5000);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("connection", "keep-alive");
        System.out.println("线程-" + Thread.currentThread().getName() + "-response heade fields:" + connection.getHeaderFields());
        fileLength = connection.getContentLength();
        System.out.println("文件总字节数:" + fileLength);
        blockSize = fileLength / threadCount;
        System.out.println("每个线程大约需下载的文件块字节数:" + blockSize);
        // 断开链接
        connection.disconnect();
    }

    /**
     * 开始下载
     */
    private void startDown() {
        for (int i = 0; i < threadCount; i++) {
            try {
                RandomAccessFile randomAccessFile = new RandomAccessFile(pathName, "rw");
                if (i < threadCount - 1) {
                    downThreads[i] = new DownThread(blockSize * i, blockSize * (i + 1) - 1, randomAccessFile, i);
                } else {
                    downThreads[i] = new DownThread(blockSize * i, fileLength - 1, randomAccessFile, i);
                }
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
         * 当前线程下载文件开始位置
         */
        private int startPos;
        /**
         * 当前线程下载文件结束位置
         */
        private int endPos;
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

        DownThread(int startPos, int endPos, RandomAccessFile randomAccessFile, int flag) {
            this.startPos = startPos;
            this.endPos = endPos;
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
                connection.setRequestProperty("Range", "bytes=" + startPos + "-" + endPos);
                System.out.println("线程-" + flag + "-response heade fields:" + connection.getHeaderFields());
                inputStream = connection.getInputStream();
                // 指针移到 startPos 处
                randomAccessFile.seek(startPos);
                // 设定缓冲区为 8 KB
                byte[] buf = new byte[8 * 1024];
                // 读出的字节数
                int hasRead;
                while ((hasRead = inputStream.read(buf)) != -1) {
                    randomAccessFile.write(buf, 0, hasRead);
                    length += hasRead;
                }
                System.out.println("线程-" + flag + "-下载完成,下载字节数" + length);
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
        new MutliThreadFileDownUtils(new URL("https://download.jetbrains.8686c.com/go/goland-2019.1.3.dmg"), 4, "/Users/jing" +
            "/Downloads/golang.dmg").startDown();
        //new MutliThreadFileDownUtils(new URL("http://f.hiphotos.baidu.com/image/pic/item/b151f8198618367aa7f3cc7424738bd4b31ce525" +
        //    ".jpg"), 3, "/Users/jing/Downloads/test.jpg").startDown();

        // 阻塞当前线程，直到 countDownLatch 中数值为 0 后继续执行当前线程
        countDownLatch.await();
        System.out.println("下载耗时:" + (double) (System.currentTimeMillis() - beginTime) / 1000 + " s");
    }
}