package com.concurrent.hlsijx;

import com.concurrent.hlsijx.constant.ResultType;
import com.concurrent.hlsijx.model.TaskResult;
import com.concurrent.hlsijx.service.ProcessJob;
import com.concurrent.hlsijx.tools.SleepTools;

import javax.annotation.Resource;
import java.util.List;
import java.util.Random;

/**
 * 类说明：模拟一个应用程序，提交工作和任务，并查询任务进度
 */
public class AppTest {

    public static void main(String[] args) {

        //自定义业务实现
        MyTask myTask = new MyTask();

        //获取线程池实例
        ProcessJob pool = ProcessJob.getInstance();

        //注册任务到线程池
        pool.registerJob(JOB_NAME, JOB_LENGTH, myTask, 5);

        //放入任务到线程池
        Random r = new Random();
        for (int i = 0; i < JOB_LENGTH; i++) {
            pool.putTask(JOB_NAME, r.nextInt(1000));
        }

        //开启查询进度线程
        new Thread(new QueryResult(pool)).start();
    }


    private static final String JOB_NAME = "计算数值";
    private static final int JOB_LENGTH = 1000;

    //查询任务进度的线程
    private static class QueryResult implements Runnable {

        private ProcessJob pool;

        private QueryResult(ProcessJob pool) {
            this.pool = pool;
        }

        @Override
        public void run() {
            int i = 0;
            while (i < 350) {
                List<TaskResult<String>> taskDetail = pool.getTaskResult(JOB_NAME);
                if (!taskDetail.isEmpty()) {
                    System.out.println(pool.getTaskProgress(JOB_NAME));
                    System.out.println(taskDetail);
                }
                SleepTools.ms(100);
                i++;
            }
        }

    }
}

//自定义任务，需实现ITaskProcesser接口
class MyTask implements ITaskProcesser<Integer, Integer> {

    @Override
    public TaskResult<Integer> execute(Integer data) {
        Random r = new Random();
        int flag = r.nextInt(500);
        SleepTools.ms(flag);
        if (flag <= 300) {

            //正常处理的情况
            Integer returnValue = data + flag;
            return new TaskResult<>(ResultType.SUCCESS, returnValue, "Success");

        } else if (flag > 301 && flag <= 400) {

            //处理失败的情况
            return new TaskResult<>(ResultType.FAILURE, -1, "Failure");

        } else {
            //发生异常的情况
            try {
                throw new RuntimeException("异常发生了！！");
            } catch (Exception e) {
                return new TaskResult<>(ResultType.EXCEPTION, -1, e.getMessage());
            }
        }
    }
}
