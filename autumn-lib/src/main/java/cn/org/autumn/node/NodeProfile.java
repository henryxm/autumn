package cn.org.autumn.node;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 本机节点画像读/写门面。消费方可注入本接口做薄封装扩展。
 */
public interface NodeProfile {

    Fingerprint.Snapshot lastSnapshot();

    String uuid();

    /**
     * 只读内存缓存中的 uuid；尚未 {@link #ensure()} 时返回 null（不落盘、不采指纹）。
     * 业务身份请在框架 Must / 建表 Init 之后使用 {@link #uuid()}。
     */
    String peekUuid();

    Profile profile();

    Profile get();

    Path home();

    Path file();

    List<String> roles();

    boolean has(String role);

    boolean hasAll(String... required);

    boolean hasAll(List<String> required);

    boolean adjusted();

    Map<String, String> labels();

    Profile ensure();

    Profile reload();

    Profile patch(Consumer<Profile> mutator);

    Profile patch(Map<String, Object> fields);

    Profile label(String key, String value);

    Profile labels(Map<String, String> labels);

    Profile roles(List<String> roles);

    Profile roles(String... roles);
}
