package cn.org.autumn.modules.wall.site;

import cn.org.autumn.config.WallHandler;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
@Order(0)
public class WallDefault implements WallHandler, Serializable {

    boolean open = true;

    boolean ipWhiteEnable = true;

    boolean ipBlackEnable = true;

    boolean hostEnable = true;

    boolean urlBlackEnable = true;

    boolean visitEnable = true;

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public boolean isIpWhiteEnable() {
        return ipWhiteEnable;
    }

    @Override
    public boolean isIpBlackEnable() {
        return ipBlackEnable;
    }

    @Override
    public boolean isHostEnable() {
        return hostEnable;
    }

    @Override
    public boolean isUrlBlackEnable() {
        return urlBlackEnable;
    }

    @Override
    public boolean isVisitEnable() {
        return visitEnable;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public void setIpWhiteEnable(boolean ipWhiteEnable) {
        this.ipWhiteEnable = ipWhiteEnable;
    }

    public void setIpBlackEnable(boolean ipBlackEnable) {
        this.ipBlackEnable = ipBlackEnable;
    }

    public void setHostEnable(boolean hostEnable) {
        this.hostEnable = hostEnable;
    }

    public void setUrlBlackEnable(boolean urlBlackEnable) {
        this.urlBlackEnable = urlBlackEnable;
    }

    public void setVisitEnable(boolean visitEnable) {
        this.visitEnable = visitEnable;
    }
}
