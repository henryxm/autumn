package cn.org.autumn.site;

import cn.org.autumn.config.WallHandler;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WallFactory extends Factory {
    List<WallHandler> wallHandlers = null;

    WallHandler wallHandler = null;

    public boolean isOpen() {
        WallHandler handler = getWallHandler();
        if (null != handler)
            return handler.isOpen();
        return true;
    }

    public boolean isIpWhiteEnable() {
        WallHandler handler = getWallHandler();
        if (null != handler)
            return handler.isOpen() && handler.isIpWhiteEnable();
        return true;
    }

    public boolean isIpBlackEnable() {
        WallHandler handler = getWallHandler();
        if (null != handler)
            return handler.isOpen() && handler.isIpBlackEnable();
        return true;
    }

    public boolean isHostEnable() {
        WallHandler handler = getWallHandler();
        if (null != handler)
            return handler.isOpen() && handler.isHostEnable();
        return true;
    }

    public boolean isUrlBlack() {
        WallHandler handler = getWallHandler();
        if (null != handler)
            return handler.isOpen() && handler.isUrlBlackEnable();
        return true;
    }

    public boolean isVisitEnable() {
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