package cn.org.autumn.site;

import cn.org.autumn.config.WallHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WallFactory extends Factory {
    List<WallHandler> wallHandlers = null;

    WallHandler wallHandler = null;

    @Value("${autumn.firewall.open:true}")
    private boolean firewallOpen;

    public boolean getFirewallOpen() {
        return firewallOpen;
    }

    public void setFirewallOpen(boolean open) {
        firewallOpen = open;
    }

    public boolean isOpen() {
        if (!firewallOpen)
            return false;
        WallHandler handler = getWallHandler();
        if (null != handler)
            return handler.isOpen();
        return true;
    }

    public boolean isIpWhiteEnable() {
        if (!firewallOpen)
            return false;
        WallHandler handler = getWallHandler();
        if (null != handler)
            return handler.isOpen() && handler.isIpWhiteEnable();
        return true;
    }

    public boolean isIpBlackEnable() {
        if (!firewallOpen)
            return false;
        WallHandler handler = getWallHandler();
        if (null != handler)
            return handler.isOpen() && handler.isIpBlackEnable();
        return true;
    }

    public boolean isHostEnable() {
        if (!firewallOpen)
            return false;
        WallHandler handler = getWallHandler();
        if (null != handler)
            return handler.isOpen() && handler.isHostEnable();
        return true;
    }

    public boolean isUrlBlack() {
        if (!firewallOpen)
            return false;
        WallHandler handler = getWallHandler();
        if (null != handler)
            return handler.isOpen() && handler.isUrlBlackEnable();
        return true;
    }

    public boolean isVisitEnable() {
        if (!firewallOpen)
            return false;
        WallHandler handler = getWallHandler();
        if (null != handler)
            return handler.isOpen() && handler.isVisitEnable();
        return true;
    }

    public WallHandler getWallHandler() {
        if (null == wallHandler) {
            if (null == wallHandlers)
                wallHandlers = getOrderList(WallHandler.class);
            if (null != wallHandlers && !wallHandlers.isEmpty()) {
                wallHandler = wallHandlers.get(wallHandlers.size() - 1);
            }
        }
        return wallHandler;
    }
}