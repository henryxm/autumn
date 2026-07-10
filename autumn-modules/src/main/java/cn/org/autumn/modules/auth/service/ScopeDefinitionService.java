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
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ScopeDefinitionService extends ModuleService<ScopeDefinitionDao, ScopeDefinitionEntity> {

    @Autowired
    private AuthScopeCatalog authScopeCatalog;

    @Override
    public boolean insert(ScopeDefinitionEntity entity) {
        boolean ok = super.insert(entity);
        if (ok) {
            refreshCatalog();
        }
        return ok;
    }

    @Override
    public boolean updateById(ScopeDefinitionEntity entity) {
        boolean ok = super.updateById(entity);
        if (ok) {
            refreshCatalog();
        }
        return ok;
    }

    @Override
    public boolean deleteById(Serializable id) {
        boolean ok = super.deleteById(id);
        if (ok) {
            refreshCatalog();
        }
        return ok;
    }

    @Override
    public boolean deleteBatchIds(Collection<? extends Serializable> idList) {
        boolean ok = super.deleteBatchIds(idList);
        if (ok) {
            refreshCatalog();
        }
        return ok;
    }

    @Override
    public String ico() {
        return "fa-key";
    }

    public void init() {
        syncBuiltins();
        refreshCatalog();
        super.init();
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
        refreshCatalog();
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
        refreshCatalog();
        return entity;
    }

    public void deleteCustom(String uuid) {
        ScopeDefinitionEntity entity = getByUuid(uuid);
        if (entity == null || entity.isBuiltin()) {
            return;
        }
        deleteById(entity.getId());
        refreshCatalog();
    }

    public ScopeDefinitionEntity getByUuid(String uuid) {
        if (StringUtils.isBlank(uuid)) {
            return null;
        }
        return baseMapper.getByUuid(uuid);
    }

    private void syncBuiltins() {
        Map<String, BuiltinMerge> merged = new LinkedHashMap<String, BuiltinMerge>();
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
                    merge.requires = new ArrayList<String>(builtin.getRequires());
                }
            }
        }
        for (Map.Entry<String, BuiltinMerge> entry : merged.entrySet()) {
            BuiltinMerge merge = entry.getValue();
            ScopeDefinitionEntity entity = baseMapper.getByCode(entry.getKey());
            if (entity == null) {
                entity = new ScopeDefinitionEntity();
                entity.setUuid(Uuid.uuid());
                entity.setCode(entry.getKey());
                entity.setLabel(merge.label);
                entity.setTracks(joinTracks(merge.tracks));
                entity.setFields(joinFields(merge.fields));
                entity.setSensitivity(merge.sensitivity == null ? AuthScopeSensitivity.low.name() : merge.sensitivity.name());
                entity.setRequires(joinList(merge.requires));
                entity.setEnabled(true);
                entity.setBuiltin(true);
                entity.setUpdated(new Date());
                insert(entity);
            } else if (entity.isBuiltin()) {
                entity.setLabel(merge.label);
                entity.setTracks(joinTracks(merge.tracks));
                entity.setFields(joinFields(merge.fields));
                entity.setSensitivity(merge.sensitivity == null ? AuthScopeSensitivity.low.name() : merge.sensitivity.name());
                entity.setRequires(joinList(merge.requires));
                entity.setUpdated(new Date());
                updateById(entity);
            }
        }
    }

    private static final class BuiltinMerge {
        private String label;
        private EnumSet<AuthTrack> tracks = EnumSet.noneOf(AuthTrack.class);
        private EnumSet<AuthField> fields = EnumSet.noneOf(AuthField.class);
        private AuthScopeSensitivity sensitivity;
        private List<String> requires = new ArrayList<String>();

        private BuiltinMerge(AuthScopeDef builtin) {
            this.label = builtin.getLabel();
            this.sensitivity = builtin.getSensitivity();
            if (builtin.getRequires() != null) {
                this.requires = new ArrayList<String>(builtin.getRequires());
            }
        }
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
        EnumSet<AuthField> fieldSet = parseFields(entity.getFields());
        def.setFields(fieldSet);
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

    private String joinTracks(Set<AuthTrack> tracks) {
        if (tracks == null || tracks.isEmpty()) {
            return "";
        }
        List<String> list = new ArrayList<>();
        for (AuthTrack track : tracks) {
            list.add(track.name().toLowerCase());
        }
        return StringUtils.join(list, ",");
    }

    private String joinFields(Set<AuthField> fields) {
        if (fields == null || fields.isEmpty()) {
            return "";
        }
        List<String> list = new ArrayList<>();
        for (AuthField field : fields) {
            list.add(field.name());
        }
        return StringUtils.join(list, ",");
    }

    private String joinList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return StringUtils.join(values, ",");
    }
}
