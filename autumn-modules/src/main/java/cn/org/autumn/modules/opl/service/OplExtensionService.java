package cn.org.autumn.modules.opl.service;

import cn.org.autumn.opl.OplConstants;
import cn.org.autumn.opl.model.OpenAppSnapshot;
import cn.org.autumn.opl.model.OpenAppType;
import cn.org.autumn.opl.model.OpenAuthorizationRequest;
import cn.org.autumn.opl.model.OpenIdentitySnapshot;
import cn.org.autumn.opl.model.OpenPlatformEvent;
import cn.org.autumn.opl.model.OpenTokenSnapshot;
import cn.org.autumn.opl.model.OpenUserInfoSnapshot;
import cn.org.autumn.opl.spi.OpenPlatformExtension;
import cn.org.autumn.opl.spi.OpenPlatformSubscriber;
import cn.org.autumn.site.Factory;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

/**
 * OPL 扩展链调度：按 {@link org.springframework.core.annotation.Order} 升序聚合 {@link OpenPlatformExtension} Bean。
 */
@Slf4j
@Service
public class OplExtensionService extends Factory {

    public void validateRegister(String accountUuid, String name, String redirectUri, String scope, OpenAppType appType) throws Exception {
        for (OpenPlatformExtension extension : extensions(appType)) {
            extension.validateRegister(accountUuid, name, redirectUri, scope, appType);
        }
    }

    public void validateRedirectUri(OpenAppSnapshot app, String redirectUri) throws Exception {
        for (OpenPlatformExtension extension : extensions(app == null ? null : app.getAppType())) {
            extension.validateRedirectUri(app, redirectUri);
        }
    }

    public boolean isRelaxedRedirectMatch(OpenAppSnapshot app) {
        for (OpenPlatformExtension extension : extensions(app == null ? null : app.getAppType())) {
            if (extension.relaxedRedirectMatch(app)) {
                return true;
            }
        }
        return false;
    }

    public void validateScope(OpenAppSnapshot app, String scope) throws Exception {
        for (OpenPlatformExtension extension : extensions(app == null ? null : app.getAppType())) {
            extension.validateScope(app, scope);
        }
    }

    public void beforeAuthorizePage(OpenAppSnapshot app, OpenAuthorizationRequest request) throws Exception {
        for (OpenPlatformExtension extension : extensions(app)) {
            extension.beforeAuthorizePage(app, request);
        }
    }

    public void beforeApprove(OpenAppSnapshot app, OpenAuthorizationRequest request) throws Exception {
        for (OpenPlatformExtension extension : extensions(app)) {
            extension.beforeApprove(app, request);
        }
    }

    public void afterCodeIssued(OpenAppSnapshot app, OpenAuthorizationRequest request, String code) {
        for (OpenPlatformExtension extension : extensions(app)) {
            extension.afterCodeIssued(app, request, code);
        }
        OpenPlatformEvent event = OpenPlatformEvent.of(OplConstants.Event.CODE_ISSUED);
        event.setAppId(app == null ? null : app.getAppId());
        event.setAccount(app == null ? null : app.getAccount());
        event.setUser(request == null ? null : request.getUserUuid());
        event.getPayload().put("codeIssued", true);
        publish(event);
    }

    public void afterTokenIssued(OpenAppSnapshot app, OpenTokenSnapshot token) {
        for (OpenPlatformExtension extension : extensions(app)) {
            extension.afterTokenIssued(app, token);
        }
        OpenPlatformEvent event = OpenPlatformEvent.of(OplConstants.Event.TOKEN_ISSUED);
        if (token != null) {
            event.setAppId(token.getAppId());
            event.setUser(token.getUser());
            event.setOpenId(token.getOpenId());
            event.setUnionId(token.getUnionId());
        }
        publish(event);
    }

    public void enrichUserInfo(OpenAppSnapshot app, OpenIdentitySnapshot identity, OpenUserInfoSnapshot userInfo) {
        for (OpenPlatformExtension extension : extensions(app)) {
            extension.enrichUserInfo(app, identity, userInfo);
        }
    }

    public String resolveOpenId(OpenAppSnapshot app, String userUuid, String defaultOpenId) {
        for (OpenPlatformExtension extension : getOrderList(OpenPlatformExtension.class)) {
            if (!extension.supports(app == null ? null : app.getAppType())) {
                continue;
            }
            String custom = extension.generateOpenId(app, userUuid);
            if (StringUtils.isNotBlank(custom)) {
                return custom;
            }
        }
        return defaultOpenId;
    }

    public String resolveUnionId(String accountUuid, String userUuid, String defaultUnionId) {
        for (OpenPlatformExtension extension : getOrderList(OpenPlatformExtension.class)) {
            String custom = extension.generateUnionId(accountUuid, userUuid);
            if (StringUtils.isNotBlank(custom)) {
                return custom;
            }
        }
        return defaultUnionId;
    }

    public void publish(OpenPlatformEvent event) {
        if (event == null || StringUtils.isBlank(event.getEvent())) {
            return;
        }
        List<OpenPlatformSubscriber> subscribers = getOrderList(OpenPlatformSubscriber.class, "onEvent", OpenPlatformEvent.class);
        for (OpenPlatformSubscriber subscriber : subscribers) {
            if (!matches(subscriber.events(), event.getEvent())) {
                continue;
            }
            try {
                subscriber.onEvent(event);
            } catch (Exception e) {
                log.error("OpenPlatformSubscriber failed event={} bean={}: {}", event.getEvent(), subscriber.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    private List<OpenPlatformExtension> extensions(OpenAppSnapshot app) {
        return extensions(app == null ? null : app.getAppType());
    }

    private List<OpenPlatformExtension> extensions(OpenAppType appType) {
        List<OpenPlatformExtension> matched = new ArrayList<>();
        for (OpenPlatformExtension extension : getOrderList(OpenPlatformExtension.class)) {
            if (extension.supports(appType)) {
                matched.add(extension);
            }
        }
        return matched;
    }

    private boolean matches(String pattern, String event) {
        if (StringUtils.isBlank(pattern)) {
            return false;
        }
        if ("*".equals(pattern.trim()) || OplConstants.Event.ALL.equals(pattern.trim())) {
            return true;
        }
        String[] parts = pattern.split(",");
        for (String part : parts) {
            if (event.equals(part.trim())) {
                return true;
            }
        }
        return false;
    }
}
