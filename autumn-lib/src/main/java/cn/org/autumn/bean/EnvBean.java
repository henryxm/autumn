package cn.org.autumn.bean;

import cn.org.autumn.annotation.EnvAware;
import cn.org.autumn.config.EnvHandler;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
public class EnvBean implements EnvHandler {

    @EnvAware("client.id")
    String clientId;

    @EnvAware("client.secret")
    String clientSecret;

    @EnvAware("logger.level")
    String loggerLevel;

    @EnvAware("supper.password")
    String supperPassword;

    @EnvAware("cluster.namespace")
    String clusterNamespace;

    @EnvAware("site.domain")
    String siteDomain;

    @EnvAware("system.username")
    String systemUsername;

    @EnvAware("system.password")
    String systemPassword;

    @EnvAware("site.ssl")
    boolean siteSsl;

    @EnvAware("root.domain")
    String rootDomain;

    @EnvAware("node.name")
    String nodeName;

    @EnvAware("node.tag")
    String nodeTag;

    /**
     * 与 {@code autumn.table.init} 一致；勿用 {@link EnvAware}+{@code Config#getEnv}，否则读不到 Spring 配置。
     */
    @Value("${autumn.table.init:true}")
    boolean tableInit = true;

    public String getNodeName() {
        if (null == nodeName)
            nodeName = "Name";
        return nodeName;
    }

    public String getNodeTag() {
        if (null == nodeTag)
            nodeTag = "Tag";
        return nodeTag;
    }
}