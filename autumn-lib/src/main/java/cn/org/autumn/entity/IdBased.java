package cn.org.autumn.entity;

import java.io.Serializable;

/**
 * 技术主键契约：自增 {@code Long id}，仅供后台代码生成 CRUD 与框架按主键更新/删除。
 * <p>
 * <strong>禁止</strong>在业务代码、对外 API、缓存键、消息体、日志对外标识或表间关联中使用 {@code id}；
 * 上述场景一律使用第二主键列 {@code uuid}（见 {@link UuidBased}、{@link SnowBased}）。
 * <p>
 * 实体须同时实现本接口与 {@link UuidBased}、{@link SnowBased} 或 {@link UserBased} 之一（按表语义选型），并配合
 * {@link cn.org.autumn.service.AutoIdService}（仅 {@code uuid} 型）在插入前自动填充。
 * 完整约定见 {@code docs/AI_DUAL_KEY.md}。
 */
public interface IdBased extends Serializable {
    Long getId();

    void setId(Long id);
}
