package com.concurrent.hlsijx.model;

import com.concurrent.hlsijx.constant.ResultType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

/**
 * 类说明：任务处理后返回的结果实体类
 * @author hlsijx
 */
@AllArgsConstructor
@Data
@ToString
public class TaskResult<R> {

    /**
     * 方法执行结果
     */
	private final ResultType resultType;

    /**
     * 方法执行后的结果数据
     */
	private final R returnValue;

    /**
     * 如果方法失败，这里可以填充原因
     */
	private final String reason;
}
