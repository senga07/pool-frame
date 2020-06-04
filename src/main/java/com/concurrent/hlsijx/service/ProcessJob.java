package com.concurrent.hlsijx.service;


import com.concurrent.hlsijx.ITaskProcesser;
import com.concurrent.hlsijx.constant.ResultType;
import com.concurrent.hlsijx.model.JobInfo;
import com.concurrent.hlsijx.model.TaskResult;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 类说明：框架的主体类，对使用者暴露的接口
 * @author hlsijx
 */
@Data
@NoArgsConstructor
@Component
public class ProcessJob {

    /**
     * 单例化
     */
    public static ProcessJob getInstance() {
        return JobPoolHolder.pool;
    }
    private static class JobPoolHolder {
        private static ProcessJob pool = new ProcessJob();
    }

    /**
     * 框架运行时的线程数，与机器的CPU数相同
     */
    private static final int THREAD_COUNTS = Runtime.getRuntime().availableProcessors();

    /**
     * 线程池，固定大小，有界队列
     */
    private static ExecutorService taskExecutor = new ThreadPoolExecutor(THREAD_COUNTS, THREAD_COUNTS,
            60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(5000));

    /**
     * 工作信息的存放容器(jobName, jobInfo)
     */
    private static ConcurrentHashMap<String, JobInfo<?>> jobInfoMap = new ConcurrentHashMap<>();
    static Map<String, JobInfo<?>> getMap() {
        return jobInfoMap;
    }

    /**
     * 对工作中的任务进行包装，提交给线程池使用，
     * 并将处理任务的结果，写入缓存以供查询
     */
    private static class ProcessTask<T, R> implements Runnable {

        private JobInfo<R> jobInfo;
        private T paramData;

        ProcessTask(JobInfo<R> jobInfo, T paramData) {
            this.jobInfo = jobInfo;
            this.paramData = paramData;
        }

        @Override
        public void run() {
            //默认值
            ResultType resultType = ResultType.EXCEPTION;
            String reason = "result is null";

            //取任务
            ITaskProcesser<T, R> taskProcesser = (ITaskProcesser<T, R>) jobInfo.getTaskProcesser();

            TaskResult<R> result = null;
            try {
                //执行任务
                result = taskProcesser.execute(paramData);
                if (result == null){
                    result = new TaskResult<>(resultType, null, reason);
                }
            } catch (Exception e) {
                result = new TaskResult<>(resultType, null, e.getMessage());
            } finally {
                //保存任务执行结果
                jobInfo.addTaskResult(result);
            }
        }
    }

    /**
     * 调用者注册工作，放入jobMap
     */
    public <R> void registerJob(String jobName, int jobLength, ITaskProcesser<?, ?> taskProcesser, long expireTime) {
        JobInfo<R> jobInfo = new JobInfo<>(jobName, jobLength, taskProcesser, expireTime);
        if (jobInfoMap.putIfAbsent(jobName, jobInfo) != null) {
            throw new RuntimeException(jobName + "已经注册！");
        }
    }

    /**
     * 提交任务到线程池执行
     */
    public <T, R> void putTask(String jobName, T t) {

        //根据名称找任务
        JobInfo<R> jobInfo = getJob(jobName);

        //将任务包装成线程
        ProcessTask<T, R> task = new ProcessTask<>(jobInfo, t);

        //放到线程池中运行
        taskExecutor.execute(task);
    }

    /**
     * 根据工作名称检索工作
     */
    @SuppressWarnings("unchecked")
    private <R> JobInfo<R> getJob(String jobName) {
        JobInfo<R> jobInfo = (JobInfo<R>) jobInfoMap.get(jobName);
        if (null == jobInfo){
            throw new RuntimeException(jobName + "是非法任务！");
        }
        return jobInfo;
    }

    /**
     * 获得工作的整体处理进度，for QueryJob
     */
    public <R> String getTaskProgress(String jobName) {
        JobInfo<R> jobInfo = getJob(jobName);
        return jobInfo.getTotalProgress();
    }

    /**
     * 获得每个任务的处理详情
     */
    public <R> List<TaskResult<R>> getTaskResult(String jobName) {
        JobInfo<R> jobInfo = getJob(jobName);
        return jobInfo.getTaskResult();
    }
}
