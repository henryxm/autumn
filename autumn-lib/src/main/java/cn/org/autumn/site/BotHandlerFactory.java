package cn.org.autumn.site;

import cn.org.autumn.config.BotDisplayNameRiskHandler;
import cn.org.autumn.config.BotHandlerOrder;
import cn.org.autumn.config.BotIconAuditHandler;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 机器人展示名硬拦 / 头像入审 Handler 聚合（对齐 {@link AccountFactory}）。
 * <p>共部署时 {@link BotHandlerOrder#LOCAL} 优先于 {@link BotHandlerOrder#REMOTE}，避免双检与双入审。
 */
@Component
public class BotHandlerFactory extends Factory {

    List<BotDisplayNameRiskHandler> displayNameHandlers;

    List<BotIconAuditHandler> iconAuditHandlers;

    /**
     * @return 拒文案；null 表示放行或无 Handler
     */
    public String displayNameRejectOrNull(String ownerUuid, String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }
        BotDisplayNameRiskHandler handler = primaryDisplayName();
        return handler == null ? null : handler.blockMessageOrNull(ownerUuid, name.trim());
    }

    /**
     * @return 审核单 uuid；null 表示未入审
     */
    public String submitIconPending(String user, String robotUuid, String iconId, String icon) throws Exception {
        BotIconAuditHandler.SubmitOutcome o = submitIcon(user, robotUuid, iconId, icon);
        return o == null ? null : o.uuid;
    }

    /**
     * @return 入审结果；null 表示无 Handler
     */
    public BotIconAuditHandler.SubmitOutcome submitIcon(String user, String robotUuid, String iconId, String icon)
            throws Exception {
        BotIconAuditHandler handler = primaryIconAudit();
        return handler == null ? null : handler.submit(user, robotUuid, iconId, icon);
    }

    public boolean hasIconAuditHandler() {
        return primaryIconAudit() != null;
    }

    private BotDisplayNameRiskHandler primaryDisplayName() {
        if (displayNameHandlers == null) {
            displayNameHandlers = getOrderList(BotDisplayNameRiskHandler.class);
        }
        return first(displayNameHandlers);
    }

    private BotIconAuditHandler primaryIconAudit() {
        if (iconAuditHandlers == null) {
            iconAuditHandlers = getOrderList(BotIconAuditHandler.class);
        }
        return first(iconAuditHandlers);
    }

    private static <T> T first(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }
}
