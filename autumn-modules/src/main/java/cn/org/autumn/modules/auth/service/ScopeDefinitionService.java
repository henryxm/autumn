package cn.org.autumn.modules.auth.service;

import cn.org.autumn.auth.scope.AuthField;
import cn.org.autumn.auth.scope.AuthScopeCatalog;
import cn.org.autumn.auth.scope.AuthScopeDef;
import cn.org.autumn.auth.scope.AuthScopeSensitivity;
import cn.org.autumn.auth.scope.AuthScopeSet;
import cn.org.autumn.auth.scope.AuthTrack;
import cn.org.autumn.base.ModuleService;
import cn.org.autumn.modules.auth.dao.ScopeDefinitionDao;
import cn.org.autumn.modules.auth.entity.ScopeDefinitionEntity;
import cn.org.autumn.utils.Uuid;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 授权范围定义：在 gen CRUD（{@code ModuleService}）之上扩展 catalog 同步与内置保护。
 */
@Service
public class ScopeDefinitionService extends ModuleService<ScopeDefinitionDao, ScopeDefinitionEntity> {

    @Autowired
    private AuthScopeCatalog authScopeCatalog;

    /** syncBuiltins 批量写入时抑制逐行 refresh。 */
    private final ThreadLocal<Boolean> suppressCatalogRefresh = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Override
    public String ico() {
        return "fa-key";
    }

    @Override
    public void init() {
        syncBuiltins();
        refreshCatalog();
        super.init();
    }

    // —— gen / MP 写路径：保证改库后刷新内存目录 ——

    @Override
    public boolean save(ScopeDefinitionEntity entity) {
        prepareNewCustom(entity);
        boolean ok = super.save(entity);
        if (ok) {
            touchCatalog();
        }
        return ok;
    }

    @Override
    public boolean updateById(ScopeDefinitionEntity entity) {
        boolean ok = super.updateById(entity);
        if (ok) {
            touchCatalog();
        }
        return ok;
    }

    @Override
    public int insert(ScopeDefinitionEntity entity) {
        int rows = super.insert(entity);
        if (rows > 0) {
            touchCatalog();
        }
        return rows;
    }

    @Override
    public boolean deleteById(Serializable id) {
        assertDeletable(id);
        boolean ok = super.deleteById(id);
        if (ok) {
            touchCatalog();
        }
        return ok;
    }

    @Override
    public int deleteBatchIds(List<?> idList) {
        List<?> allowed = filterDeletableIds(idList);
        if (allowed.isEmpty()) {
            return 0;
        }
        int rows = super.deleteBatchIds(allowed);
        if (rows > 0) {
            touchCatalog();
        }
        return rows;
    }

    @Override
    public boolean removeByIds(Collection<?> list) {
        List<?> allowed = filterDeletableIds(list);
        if (allowed.isEmpty()) {
            return false;
        }
        boolean ok = super.removeByIds(allowed);
        if (ok) {
            touchCatalog();
        }
        return ok;
    }

    public void refreshCatalog() {
        authScopeCatalog.refreshCustom(toCatalogDefs(recognizedEntities(selectByMap(null))));
    }

    public List<AuthScopeDef> listCatalog(AuthTrack track) {
        return authScopeCatalog.listDefinitions(track);
    }

    public List<ScopeDefinitionEntity> listForAdmin() {
        return recognizedEntities(selectByMap(null));
    }

    public ScopeDefinitionEntity getByUuid(String uuid) {
        if (StringUtils.isBlank(uuid)) {
            return null;
        }
        return baseMapper.getByUuid(uuid);
    }

    /**
     * 管理页新增/更新自定义 scope（在 gen 能力上约束：不可覆盖内置）。
     */
    public ScopeDefinitionEntity saveCustom(String code, String label, String tracks, String fields, String sensitivity, String requires) {
        if (StringUtils.isBlank(code) || StringUtils.isBlank(label)) {
            return null;
        }
        String normalized = AuthScopeSet.normalize(code);
        ScopeDefinitionEntity existing = baseMapper.getByCode(normalized);
        if (existing != null && existing.isBuiltin()) {
            throw new IllegalArgumentException("内置 scope 不可覆盖创建");
        }
        ScopeDefinitionEntity entity = existing == null ? new ScopeDefinitionEntity() : existing;
        if (StringUtils.isBlank(entity.getUuid())) {
            entity.setUuid(Uuid.uuid());
        }
        entity.setCode(normalized);
        entity.setLabel(label.trim());
        entity.setTracks(StringUtils.defaultIfBlank(tracks, AuthTrack.OAUTH.name().toLowerCase()));
        entity.setFields(StringUtils.defaultString(fields));
        entity.setSensitivity(StringUtils.defaultIfBlank(sensitivity, AuthScopeSensitivity.low.name()));
        entity.setRequires(StringUtils.defaultString(requires));
        entity.setEnabled(true);
        entity.setBuiltin(false);
        entity.setUpdated(new Date());
        insertOrUpdate(entity);
        return entity;
    }

    public ScopeDefinitionEntity updateEnabled(String uuid, boolean enabled) {
        ScopeDefinitionEntity entity = getByUuid(uuid);
        if (entity == null) {
            return null;
        }
        entity.setEnabled(enabled);
        entity.setUpdated(new Date());
        updateById(entity);
        return entity;
    }

    public void deleteCustom(String uuid) {
        ScopeDefinitionEntity entity = getByUuid(uuid);
        if (entity == null || entity.isBuiltin()) {
            return;
        }
        deleteById(entity.getId());
    }

    private void syncBuiltins() {
        Map<String, BuiltinMerge> merged = new LinkedHashMap<>();
        for (AuthTrack track : AuthTrack.values()) {
            for (AuthScopeDef builtin : authScopeCatalog.listDefinitions(track)) {
                if (builtin == null || builtin.isAlias() || StringUtils.isBlank(builtin.getCode())) {
                    continue;
                }
                String code = builtin.getCode();
                BuiltinMerge merge = merged.get(code);
                if (merge == null) {
                    merge = new BuiltinMerge(builtin);
                    merged.put(code, merge);
                }
                merge.tracks.add(track);
                if (builtin.getFields() != null) {
                    merge.fields.addAll(builtin.getFields());
                }
                if (StringUtils.isNotBlank(builtin.getLabel())) {
                    merge.label = builtin.getLabel();
                }
                if (builtin.getSensitivity() != null) {
                    merge.sensitivity = builtin.getSensitivity();
                }
                if (builtin.getRequires() != null && !builtin.getRequires().isEmpty()) {
                    merge.requires = new ArrayList<>(builtin.getRequires());
                }
                merge.enabled = merge.enabled && builtin.isEnabled();
            }
        }
        suppressCatalogRefresh.set(Boolean.TRUE);
        try {
            for (Map.Entry<String, BuiltinMerge> entry : merged.entrySet()) {
                BuiltinMerge merge = entry.getValue();
                ScopeDefinitionEntity entity = baseMapper.getByCode(entry.getKey());
                if (entity == null) {
                    entity = new ScopeDefinitionEntity();
                    entity.setUuid(Uuid.uuid());
                    entity.setCode(entry.getKey());
                    applyBuiltinMerge(entity, merge);
                    entity.setBuiltin(true);
                    insert(entity);
                } else if (entity.isBuiltin()) {
                    applyBuiltinMerge(entity, merge);
                    updateById(entity);
                }
            }
        } finally {
            suppressCatalogRefresh.remove();
        }
    }

    private static void applyBuiltinMerge(ScopeDefinitionEntity entity, BuiltinMerge merge) {
        entity.setLabel(merge.label);
        entity.setTracks(joinTracks(merge.tracks));
        entity.setFields(joinFields(merge.fields));
        entity.setSensitivity(merge.sensitivity == null ? AuthScopeSensitivity.low.name() : merge.sensitivity.name());
        entity.setRequires(joinList(merge.requires));
        entity.setEnabled(merge.enabled);
        entity.setUpdated(new Date());
    }

    private void prepareNewCustom(ScopeDefinitionEntity entity) {
        if (entity == null) {
            return;
        }
        if (StringUtils.isBlank(entity.getUuid())) {
            entity.setUuid(Uuid.uuid());
        }
        if (StringUtils.isNotBlank(entity.getCode())) {
            String code = AuthScopeSet.normalize(entity.getCode());
            entity.setCode(code);
            ScopeDefinitionEntity existing = baseMapper.getByCode(code);
            if (existing != null && existing.isBuiltin()) {
                throw new IllegalArgumentException("内置 scope 不可覆盖创建");
            }
        }
        if (entity.getUpdated() == null) {
            entity.setUpdated(new Date());
        }
    }

    private void assertDeletable(Serializable id) {
        if (id == null) {
            return;
        }
        ScopeDefinitionEntity entity = getById(id);
        if (entity != null && entity.isBuiltin()) {
            throw new IllegalArgumentException("内置 scope 不可删除");
        }
    }

    private List<?> filterDeletableIds(Collection<?> idList) {
        List<Object> allowed = new ArrayList<>();
        if (idList == null) {
            return allowed;
        }
        for (Object id : idList) {
            if (id == null) {
                continue;
            }
            ScopeDefinitionEntity entity = getById((Serializable) id);
            if (entity != null && !entity.isBuiltin()) {
                allowed.add(id);
            }
        }
        return allowed;
    }

    private void touchCatalog() {
        if (Boolean.TRUE.equals(suppressCatalogRefresh.get())) {
            return;
        }
        refreshCatalog();
    }

    private List<ScopeDefinitionEntity> recognizedEntities(List<ScopeDefinitionEntity> entities) {
        List<ScopeDefinitionEntity> result = new ArrayList<>();
        if (entities == null) {
            return result;
        }
        for (ScopeDefinitionEntity entity : entities) {
            if (isRecognized(entity)) {
                result.add(entity);
            }
        }
        return result;
    }

    private boolean isRecognized(ScopeDefinitionEntity entity) {
        if (entity == null || StringUtils.isBlank(entity.getCode())) {
            return false;
        }
        if (!entity.isBuiltin()) {
            return true;
        }
        return authScopeCatalog.isRegisteredBuiltin(entity.getCode());
    }

    private List<AuthScopeDef> toCatalogDefs(List<ScopeDefinitionEntity> entities) {
        List<AuthScopeDef> list = new ArrayList<>();
        if (entities == null) {
            return list;
        }
        for (ScopeDefinitionEntity entity : entities) {
            if (!isRecognized(entity)) {
                continue;
            }
            AuthScopeDef def = toCatalogDef(entity);
            if (def != null) {
                list.add(def);
            }
        }
        return list;
    }

    private AuthScopeDef toCatalogDef(ScopeDefinitionEntity entity) {
        if (entity == null || StringUtils.isBlank(entity.getCode())) {
            return null;
        }
        AuthScopeDef def = new AuthScopeDef();
        def.setCode(AuthScopeSet.normalize(entity.getCode()));
        def.setLabel(entity.getLabel());
        def.setTracks(parseTracks(entity.getTracks()));
        def.setFields(parseFields(entity.getFields()));
        def.setSensitivity(parseSensitivity(entity.getSensitivity()));
        def.setRequires(parseRequires(entity.getRequires()));
        def.setEnabled(entity.isEnabled());
        def.setBuiltin(entity.isBuiltin());
        return def;
    }

    private EnumSet<AuthTrack> parseTracks(String source) {
        EnumSet<AuthTrack> tracks = EnumSet.noneOf(AuthTrack.class);
        if (StringUtils.isBlank(source)) {
            return tracks;
        }
        for (String part : source.split("[;,；，\\s]+")) {
            if (StringUtils.isBlank(part)) {
                continue;
            }
            try {
                tracks.add(AuthTrack.valueOf(part.trim().toUpperCase()));
            } catch (Exception ignored) {
            }
        }
        return tracks;
    }

    private EnumSet<AuthField> parseFields(String source) {
        EnumSet<AuthField> fields = EnumSet.noneOf(AuthField.class);
        if (StringUtils.isBlank(source)) {
            return fields;
        }
        for (String part : source.split("[;,；，\\s]+")) {
            if (StringUtils.isBlank(part)) {
                continue;
            }
            try {
                fields.add(AuthField.valueOf(part.trim()));
            } catch (Exception ignored) {
            }
        }
        return fields;
    }

    private AuthScopeSensitivity parseSensitivity(String source) {
        if (StringUtils.isBlank(source)) {
            return AuthScopeSensitivity.low;
        }
        try {
            return AuthScopeSensitivity.valueOf(source.trim().toLowerCase());
        } catch (Exception e) {
            return AuthScopeSensitivity.low;
        }
    }

    private List<String> parseRequires(String source) {
        List<String> list = new ArrayList<>();
        if (StringUtils.isBlank(source)) {
            return list;
        }
        for (String part : source.split("[;,；，\\s]+")) {
            if (StringUtils.isNotBlank(part)) {
                list.add(part.trim().toLowerCase());
            }
        }
        return list;
    }

    private static String joinTracks(Set<AuthTrack> tracks) {
        if (tracks == null || tracks.isEmpty()) {
            return "";
        }
        List<String> list = new ArrayList<>();
        for (AuthTrack track : tracks) {
            list.add(track.name().toLowerCase());
        }
        return StringUtils.join(list, ",");
    }

    private static String joinFields(Set<AuthField> fields) {
        if (fields == null || fields.isEmpty()) {
            return "";
        }
        List<String> list = new ArrayList<>();
        for (AuthField field : fields) {
            list.add(field.name());
        }
        return StringUtils.join(list, ",");
    }

    private static String joinList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return StringUtils.join(values, ",");
    }

    private static final class BuiltinMerge {
        private String label;
        private final EnumSet<AuthTrack> tracks = EnumSet.noneOf(AuthTrack.class);
        private final EnumSet<AuthField> fields = EnumSet.noneOf(AuthField.class);
        private AuthScopeSensitivity sensitivity;
        private List<String> requires = new ArrayList<>();
        private boolean enabled = true;

        private BuiltinMerge(AuthScopeDef builtin) {
            this.label = builtin.getLabel();
            this.sensitivity = builtin.getSensitivity();
            this.enabled = builtin.isEnabled();
            if (builtin.getRequires() != null) {
                this.requires = new ArrayList<>(builtin.getRequires());
            }
        }
    }
}
