package cn.org.autumn.modules.opl.support;

import cn.org.autumn.modules.opl.entity.OpenAccountEntity;
import cn.org.autumn.modules.opl.entity.OpenAppEntity;
import cn.org.autumn.modules.opl.store.OplTokenContext;
import cn.org.autumn.opl.model.OpenAccountSnapshot;
import cn.org.autumn.opl.model.OpenAppSnapshot;
import cn.org.autumn.opl.model.OpenTokenSnapshot;
import java.util.ArrayList;
import java.util.List;

/** Entity / 运行时上下文与 lib Snapshot 之间的转换。 */
public final class OplSnapshots {

    private OplSnapshots() {
    }

    public static OpenAccountSnapshot toAccountSnapshot(OpenAccountEntity entity) {
        if (entity == null) {
            return null;
        }
        OpenAccountSnapshot snapshot = new OpenAccountSnapshot();
        snapshot.setUuid(entity.getUuid());
        snapshot.setUser(entity.getUser());
        snapshot.setName(entity.getName());
        snapshot.setStatus(entity.getStatus());
        snapshot.setCreate(entity.getCreate());
        snapshot.setUpdate(entity.getUpdate());
        return snapshot;
    }

    public static OpenAppSnapshot toAppSnapshot(OpenAppEntity entity) {
        if (entity == null) {
            return null;
        }
        OpenAppSnapshot snapshot = new OpenAppSnapshot();
        snapshot.setUuid(entity.getUuid());
        snapshot.setAccount(entity.getAccount());
        snapshot.setAppId(entity.getAppId());
        snapshot.setName(entity.getName());
        snapshot.setAppType(entity.getAppType());
        snapshot.setRedirectUri(entity.getRedirectUri());
        snapshot.setScope(entity.getScope());
        snapshot.setStatus(entity.getStatus());
        snapshot.setCreate(entity.getCreate());
        snapshot.setUpdate(entity.getUpdate());
        return snapshot;
    }

    public static List<OpenAppSnapshot> toAppSnapshots(List<OpenAppEntity> entities) {
        List<OpenAppSnapshot> snapshots = new ArrayList<>();
        if (entities == null) {
            return snapshots;
        }
        for (OpenAppEntity entity : entities) {
            snapshots.add(toAppSnapshot(entity));
        }
        return snapshots;
    }

    public static OpenTokenSnapshot toTokenSnapshot(OplTokenContext context) {
        if (context == null) {
            return null;
        }
        OpenTokenSnapshot snapshot = new OpenTokenSnapshot();
        snapshot.setAppId(context.getAppId());
        snapshot.setUser(context.getUser());
        snapshot.setOpenId(context.getOpenId());
        snapshot.setUnionId(context.getUnionId());
        snapshot.setAccessToken(context.getAccessToken());
        snapshot.setRefreshToken(context.getRefreshToken());
        snapshot.setAccessExpireIn(context.getExpireIn());
        snapshot.setScope(context.getGrantedScope());
        return snapshot;
    }
}
