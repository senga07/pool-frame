package com.concurrent.hlsijx.model;

import com.concurrent.hlsijx.ITaskProcesser;
import com.concurrent.hlsijx.constant.ResultType;
import com.concurrent.hlsijx.service.QueryJob;
import lombok.Data;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 类说明：提交给框架执行的工作实体类,
 * 工作：表示本批次需要处理的同性质任务(Task)的一个集合
 * @author hlsijx
 */
@Data
public class JobInfo<R> {

    /**
     * 工作名，用以区分框架中唯一的工作
     */
    private final String jobName;

    /**
     * 工作任务的长度
     */
    private final int jobLength;

    /**
     * 任务的抽象
     */
    private final ITaskProcesser<?, ?> taskProcesser;

    /**
     * 任务的成功次数
     */
    private AtomicInteger successCount;

    /**
     * 当前总进度
     */
    private AtomicInteger taskProcessCount;

    /**
     * 有界阻塞队列，存放每个任务的处理结果，供查询用
     */
    private Deque<TaskResult<R>> taskDetailQueues;

    /**
     * 保留的工作的结果信息供查询的时长
     */
    private final long expireTime;

    /**
     * 提供查询进度
     */
    private static QueryJob queryJob = QueryJob.getInstance();

    public JobInfo(String jobName, int jobLength, ITaskProcesser<?, ?> taskProcesser, long expireTime) {
        this.jobName = jobName;
        this.jobLength = jobLength;
        this.taskProcesser = taskProcesser;
        this.expireTime = expireTime;
        this.successCount = new AtomicInteger(0);
        this.taskProcessCount = new AtomicInteger(0);
        this.taskDetailQueues = new LinkedBlockingDeque<>(jobLength);
    }


    /**
     * 提供工作的整体进度信息
     */
    public String getTotalProgress() {
        return "Success[" + successCount.get() + "]/Current[" + taskProcessCount.get() + "] Total[" + jobLength + "]";
    }

    /**
     * 提供工作中每个任务的处理结果
     */
    public List<TaskResult<R>> getTaskResult() {
        List<TaskResult<R>> taskResultList = new LinkedList<>();
        while (!taskDetailQueues.isEmpty()) {
            taskResultList.add(taskDetailQueues.pollFirst());
        }
        return taskResultList;
    }

    /**
     * 每个任务处理完成后，记录任务的处理结果，因为从业务应用的角度来说，
     * 对查询任务进度数据的一致性要不高
     * 我们保证最终一致性即可，无需对整个方法加锁
     */
    public void addTaskResult(TaskResult<R> taskResult) {
        //判断执行结果，成功就将successCount++
        if (ResultType.SUCCESS.equals(taskResult.getResultType())) {
            successCount.incrementAndGet();
        }
        taskProcessCount.incrementAndGet();
        taskDetailQueues.addLast(taskResult);

        //任务完成时，放入查询map
        if (taskProcessCount.get() == jobLength) {
            queryJob.putJob(jobName, expireTime);
        }
    }
}
