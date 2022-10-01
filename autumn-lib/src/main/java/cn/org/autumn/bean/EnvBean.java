package cn.org.autumn.bean;

import cn.org.autumn.annotation.EnvAware;
import cn.org.autumn.config.EnvHandler;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

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

    @EnvAware("table.init")
    boolean tableInit = true;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getLoggerLevel() {
        return loggerLevel;
    }

    public void setLoggerLevel(String loggerLevel) {
        this.loggerLevel = loggerLevel;
    }

    public String getSupperPassword() {
        return supperPassword;
    }

    public void setSupperPassword(String supperPassword) {
        this.supperPassword = supperPassword;
    }

    public String getClusterNamespace() {
        return clusterNamespace;
    }

    public void setClusterNamespace(String clusterNamespace) {
        this.clusterNamespace = clusterNamespace;
    }

    public String getSiteDomain() {
        return siteDomain;
    }

    public void setSiteDomain(String siteDomain) {
        this.siteDomain = siteDomain;
    }

    public String getSystemUsername() {
        return systemUsername;
    }

    public void setSystemUsername(String systemUsername) {
        this.systemUsername = systemUsername;
    }

    public String getSystemPassword() {
        return systemPassword;
    }

    public void setSystemPassword(String systemPassword) {
        this.systemPassword = systemPassword;
    }

    public boolean isSiteSsl() {
        return siteSsl;
    }

    public void setSiteSsl(boolean siteSsl) {
        this.siteSsl = siteSsl;
    }

    public String getRootDomain() {
        return rootDomain;
    }

    public void setRootDomain(String rootDomain) {
        this.rootDomain = rootDomain;
    }

    public boolean isTableInit() {
        return tableInit;
    }

    public void setTableInit(boolean tableInit) {
        this.tableInit = tableInit;
    }
}