package cn.org.autumn.model;

import lombok.Getter;
import lombok.Setter;

/**
 * 兼容请求包装：
 * 统一将请求体（对象/数组/基础类型）承载到 data 字段中，
 * 由参数解析器负责做加密与明文场景下的数据归一化。
 *
 * @param <T> 业务数据类型
 */
@Getter
@Setter
public class CompatibleRequest<T> extends Request<T> {
}
