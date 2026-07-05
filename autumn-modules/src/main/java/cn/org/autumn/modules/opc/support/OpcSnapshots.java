package cn.org.autumn.modules.opc.support;

import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.opc.model.ConnectAppSnapshot;
import java.util.ArrayList;
import java.util.List;

/** ConnectApp Entity 与 lib Snapshot 转换。 */
public final class OpcSnapshots {

    private OpcSnapshots() {
    }

    public static ConnectAppSnapshot toSnapshot(ConnectAppEntity entity) {
        if (entity == null) {
            return null;
        }
        ConnectAppSnapshot snapshot = new ConnectAppSnapshot();
        snapshot.setUuid(entity.getUuid());
        snapshot.setUser(entity.getUser());
        snapshot.setAppId(entity.getAppId());
        snapshot.setName(entity.getName());
        snapshot.setPlatformBaseUrl(entity.getPlatformBaseUrl());
        snapshot.setRedirectUri(entity.getRedirectUri());
        snapshot.setScope(entity.getScope());
        snapshot.setAuthorizeUri(entity.getAuthorizeUri());
        snapshot.setTokenUri(entity.getTokenUri());
        snapshot.setUserInfoUri(entity.getUserInfoUri());
        snapshot.setStatus(entity.getStatus());
        snapshot.setCreate(entity.getCreate());
        snapshot.setUpdate(entity.getUpdate());
        return snapshot;
    }

    public static List<ConnectAppSnapshot> toSnapshots(List<ConnectAppEntity> entities) {
        List<ConnectAppSnapshot> snapshots = new ArrayList<>();
        if (entities == null) {
            return snapshots;
        }
        for (ConnectAppEntity entity : entities) {
            snapshots.add(toSnapshot(entity));
        }
        return snapshots;
    }
}
