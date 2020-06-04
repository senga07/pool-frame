package com.concurrent.hlsijx.service;

import com.concurrent.hlsijx.model.DelayData;
import com.concurrent.hlsijx.model.JobInfo;

import java.util.Map;
import java.util.concurrent.DelayQueue;

/**
 * 类说明：任务完成后,在一定的时间供查询结果，
 * 之后为释放资源节约内存，需要定期处理过期的任务
 * @author hlsijx
 */
public class QueryJob {

    /**
     * 延时队列，当任务完成后放入任务名称并设置过期时间
     */
    private static DelayQueue<DelayData<String>> delayQueue = new DelayQueue<>();

    static {
        //将过期清理的任务设置为守护进程
        Thread thread = new Thread(new ExpireJob());
        thread.setDaemon(true);
        thread.start();
        System.out.println("开启过期检查的守护线程......");
    }

    /**
     * 处理队列中到期任务
     */
    private static class ExpireJob implements Runnable {

        private static DelayQueue<DelayData<String>> delayQueue = QueryJob.delayQueue;

        //缓存的工作信息
        private static Map<String, JobInfo<?>> jobInfoMap = ProcessJob.getMap();

        @Override
        public void run() {
            while (true) {
                try {
                    DelayData<String> item = delayQueue.take();
                    String jobName = item.getData();
                    jobInfoMap.remove(jobName);
                    System.out.println(jobName + "过期了，从缓存中清除");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 任务完成后，放入队列，经过expireTime时间后，会从整个框架中移除
     */
    public void putJob(String jobName, long expireTime) {
        DelayData<String> item = new DelayData<>(expireTime, jobName);
        if (delayQueue.offer(item)){
            System.out.println(jobName + "已经放入过期检查缓存，时长：" + expireTime);
        }
    }

    /**
     * 将该对象单例化
     */
    public static QueryJob getInstance() {
        return ProcesserHolder.processer;
    }
    private static class ProcesserHolder {
        private static QueryJob processer = new QueryJob();
    }
}
