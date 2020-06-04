package com.concurrent.hlsijx.model;

import lombok.Data;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * 类说明：存放的延时队列的元素
 * @author hlsijx
 */
@Data
public class DelayData<T> implements Delayed {

    /**
     * 到期时间,但传入的数值代表过期的时长，传入单位毫秒
     */
    private long activeTime;

    /**
     * 业务数据，泛型
     */
    private T data;

    /**
     * 传入过期时长,单位秒，内部转换
     */
    public DelayData(long expirationTime, T data) {
        this.activeTime = expirationTime * 1000 + System.currentTimeMillis();
        this.data = data;
    }

    /**
     * 这个方法返回到激活日期的剩余时间，时间单位由单位参数指定。
     */
    @Override
    public long getDelay(TimeUnit unit) {

        return unit.convert(this.activeTime - System.currentTimeMillis(), unit);
    }

    /**
     * Delayed接口继承了Comparable接口，按剩余时间排序，实际计算考虑精度为纳秒数
     */
    @Override
    public int compareTo(Delayed o) {
        long d = (getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS));
        if (d == 0) {
            return 0;
        }
        return d < 0 ? -1 : 1;
    }
}
