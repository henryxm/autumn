package cn.org.autumn.modules.lan.service;

import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.lan.entity.LanguageEntity;
import cn.org.autumn.modules.lan.service.gen.LanguageServiceGen;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.site.LoadFactory;
import cn.org.autumn.table.utils.HumpConvert;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.*;

@Service
public class LanguageService extends LanguageServiceGen implements LoadFactory.Load, LoopJob.Job {
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    SysConfigService sysConfigService;

    private static Map<String, Map<String, String>> languages;

    static {
        languages = new LinkedHashMap<>();
    }

    @Override
    public int menuOrder() {
        return 6;
    }

    @Override
    public String ico() {
        return "fa-language";
    }

    public String parentMenu() {
        return sysMenuService.getSystemMenuKey("SystemManagement");
    }

    public static void f(LanguageEntity languageEntity) {
        if (null == languageEntity)
            return;
        if (StringUtil.isNullOrEmpty(languageEntity.getName()))
            return;
        Field[] fields = LanguageEntity.class.getDeclaredFields();
        for (Field field : fields) {
            String name = field.getName();
            if ("serialVersionUID".equalsIgnoreCase(name) || "id".equalsIgnoreCase(name) || "name".equalsIgnoreCase(name))
                continue;
            name = HumpConvert.HumpToUnderline(name);
            Map<String, String> map = null;
            if (!languages.containsKey(name)) {
                map = new LinkedHashMap<>();
                languages.put(name, map);
                if (name.contains("_")) {
                    String l = name.split("_")[0];
                    if (!languages.containsKey(l))
                        languages.put(l, map);
                }
            } else {
                map = languages.get(name);
            }
            field.setAccessible(true);
            Object v = null;
            try {
                v = field.get(languageEntity);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            if (null == v)
                map.put(languageEntity.getName(), languageEntity.getZhCn());
            else
                map.put(languageEntity.getName(), v.toString());
        }
    }

    public void load() {
        List<LanguageEntity> languageEntityList = baseMapper.selectByMap(new HashMap<>());
        for (LanguageEntity languageEntity : languageEntityList) {
            f(languageEntity);
        }
    }

    public Map<String, String> getLanguage(Locale locale) {
        String t = locale.toLanguageTag();
        t = t.replace("-", "_").toLowerCase();
        return languages.get(t);
    }

    /**
     * key定义规范：{模块包名}_{类型}_{单词或缩写}
     *
     * @param key
     * @param zhCn
     * @return
     */
    public boolean addLanguageColumnItem(String key, String zhCn, String enUs) {
        if (baseMapper.hasKey(key) > 0) {
            logger.debug("Duplicate Key: " + key);
            return false;
        }
        LanguageEntity languageEntity = new LanguageEntity();
        languageEntity.setName(key);
        languageEntity.setZhCn(zhCn);
        languageEntity.setEnUs(enUs);
        insert(languageEntity);
        return true;
    }

    private static Field getEntityField(Field[] fields, String key) {
        Field r = null;
        for (Field field : fields) {
            if (field.getName().toLowerCase().contains(key)) {
                r = field;
            }
        }
        return r;
    }


    public static Map<String, LanguageEntity> from(List<String[]> ls) {
        Map<String, LanguageEntity> map = new LinkedHashMap<>();
        for (String[] l : ls) {
            LanguageEntity languageEntity = from(l);
            map.put(languageEntity.getName(), languageEntity);
        }
        return map;
    }

    public static Map<String, LanguageEntity> from(String[][] ls) {
        Map<String, LanguageEntity> map = new LinkedHashMap<>();
        for (String[] l : ls) {
            LanguageEntity languageEntity = from(l);
            map.put(languageEntity.getName(), languageEntity);
        }
        return map;
    }

    public static LanguageEntity from(String[] languages) {
        Field[] fields = LanguageEntity.class.getDeclaredFields();
        LanguageEntity languageEntity = new LanguageEntity();
        for (int i = 0; i < languages.length; i++) {
            try {
                String lang = languages[i];
                if (lang.contains(":")) {
                    String[] sp = lang.split(":");
                    String lk = sp[0].trim().toLowerCase();
                    String lv = sp[1];
                    Field field = getEntityField(fields, lk);
                    if (null != field) {
                        field.setAccessible(true);
                        field.set(languageEntity, lv);
                    } else {
                        if (i < fields.length - 1) {
                            field = fields[i + 2];
                            field.setAccessible(true);
                            field.set(languageEntity, lang);
                        }
                    }
                } else {
                    if (i < fields.length - 1) {
                        Field field = fields[i + 2];
                        field.setAccessible(true);
                        field.set(languageEntity, lang);
                    }
                }
            } catch (IllegalAccessException e) {
            }
        }
        return languageEntity;
    }

    private Map<String, LanguageEntity> merge(Object... objects) {
        Map<String, LanguageEntity> map = new LinkedHashMap<>();
        for (Object o : objects) {
            if (o instanceof List) {
                List<String[]> l = (List<String[]>) o;
                Map<String, LanguageEntity> t = from(l);
                map.putAll(t);
            }
            if (o instanceof String[][]) {
                String[][] l = (String[][]) o;
                Map<String, LanguageEntity> t = from(l);
                map.putAll(t);
            }
        }
        return map;
    }

    public void put(boolean update, Object... objects) {
        Map<String, LanguageEntity> map = merge(objects);
        put(update, map.values());
    }

    public void put(Object... objects) {
        Map<String, LanguageEntity> map = merge(objects);
        put(map.values());
    }

    public void put(boolean update, Collection<LanguageEntity> languageEntities) {
        for (LanguageEntity languageEntity : languageEntities) {
            put(update, languageEntity);
        }
    }

    public void put(Collection<LanguageEntity> languageEntities) {
        boolean update = sysConfigService.isUpdateLanguage();
        put(update, languageEntities);
    }

    public void put(LanguageEntity languageEntity) {
        boolean update = sysConfigService.isUpdateLanguage();
        put(update, languageEntity);
    }

    public void put(boolean update, LanguageEntity languageEntity) {
        if (null == languageEntity)
            return;
        LanguageEntity existed = baseMapper.getByKey(languageEntity.getName());
        if (null == existed) {
            insert(languageEntity);
        } else {
            if (update && existed.hashCode() != languageEntity.hashCode()) {
                existed.merge(languageEntity);
                updateById(existed);
            }
        }
        return;
    }

    public boolean addLanguageColumnItem(String key, String zhCn) {
        return addLanguageColumnItem(key, zhCn, null);
    }

    public void init() {
        LoopJob.onTenMinute(this);
        super.init();
    }

    public String[][] getLanguageItems() {
        String[][] items = new String[][]{

                {"lan_language_table_comment", "国家语言", "Language"},
                {"lan_language_column_name", "标识", "Unique Name"},
                {"lan_language_column_zh_cn", "简体中文(中国)", "Chinese(China)"},
                {"lan_language_column_en_us", "英语(美国)"},
                {"lan_language_column_zh_hk", "繁体中文(香港)"},
                {"lan_language_column_ko_kr", "韩语(韩国)"},
                {"lan_language_column_ja_jp", "日语(日本)"},
                {"lan_language_column_tt_ru", "俄语(俄罗斯)"},
                {"lan_language_column_fr_fr", "法语(法国)"},
                {"lan_language_column_de_de", "德语(德国)"},
                {"lan_language_column_vi_vn", "越语(越南)"},
                {"lan_language_column_th_th", "泰语(泰国)"},
                {"lan_language_column_ms_my", "马来语(马来西亚)"},
                {"lan_language_column_id_id", "印尼语(印尼)"},
                {"lan_language_column_es_es", "西班牙语(西班牙)"},
                {"lan_language_column_tr_tr", "土耳其语(土耳其)"},
                {"lan_language_column_uk_uk", "乌克兰语(乌克兰)"},
                {"lan_language_column_pu_pt", "葡萄牙语(葡萄牙)"},
                {"lan_language_column_pl_pl", "波兰语(波兰)"},
                {"lan_language_column_mn_mn", "蒙古语(蒙古)"},
                {"lan_language_column_nb_no", "挪威语(挪威)"},
                {"lan_language_column_it_it", "意大利语(意大利)"},
                {"lan_language_column_he_il", "希伯来语(以色列)"},
                {"lan_language_column_el_gr", "希腊语(希腊)"},
                {"lan_language_column_fa_ir", "波斯语(伊朗)"},
                {"lan_language_column_ar_sa", "阿拉伯语(沙特阿拉伯)"},

                {"sys_string_management", "管理系统", "Manage system"},
                {"sys_string_system_management", "系统管理", "System management"},
                {"sys_string_manager_management", "管理员管理", "Manager management"},
                {"sys_string_role_management", "角色管理", "Role management"},
                {"sys_string_menu_management", "菜单管理", "Menu management"},
                {"sys_string_sql_monitor", "SQL监控", "SQL monitor"},
                {"sys_string_job_schedule", "定时任务", "Schedule job"},
                {"sys_string_lookup", "查看", "Look up"},
                {"sys_string_suspend", "暂停", "Suspend"},
                {"sys_string_resume", "恢复", "Resume"},
                {"sys_string_normal", "正常", "Normal"},
                {"sys_string_forbidden", "禁用", "Forbidden"},
                {"sys_string_status", "状态", "Status"},
                {"sys_string_immediate_execution", "立即执行", "Immediate execution"},
                {"sys_string_log_list", "日志列表", "Log list"},
                {"sys_string_config_management", "参数管理", "Config management"},
                {"sys_string_config_name", "参数名", "Config name"},
                {"sys_string_config_value", "参数值", "Config value"},
                {"sys_string_system_log", "系统日志", "System log"},
                {"sys_string_file_upload", "文件上传", "File upload"},
                {"sys_string_department_management", "部门管理", "Department management"},
                {"sys_string_select_department", "选择部门", "Select department"},
                {"sys_string_dictionary_management", "字典管理", "Dictionary management"},
                {"sys_string_code_generator", "代码生成", "Code generator"},
                {"sys_string_generate", "生成", "generate"},
                {"sys_string_generator_solution", "生成方案", "Generator solution"},
                {"sys_string_select_generator_solution", "选择生成方案", "Select generator solution"},
                {"sys_string_table_name", "表名", "Table name"},
                {"sys_string_engine", "引擎", "Engine"},
                {"sys_string_table_comment", "表备注", "Table comment"},
                {"sys_string_remark", "备注", "Remark"},
                {"sys_string_create_time", "创建时间", "Create time"},
                {"sys_string_query", "查询", "Query"},
                {"sys_string_select_language", "请选择语言", "Please select language"},
                {"sys_string_modify_password", "修改密码", "Modify password"},
                {"sys_string_navigation_menu", "导航菜单", "Navigation menu"},
                {"sys_string_select_menu", "选择菜单", "Select menu"},
                {"sys_string_upper_menu", "上级菜单", "Upper menu"},
                {"sys_string_menu_url", "菜单URL", "Menu url"},
                {"sys_string_permissions", "授权标识", "Permissions"},
                {"sys_string_permissions_ex", "多个用逗号分隔，如：user:list,user:create", "Comma split，ex：user:list,user:create"},
                {"sys_string_root_menu", "一级菜单", "Root menu"},
                {"sys_string_home", "首页", "Home"},
                {"sys_string_account", "账号", "Account"},
                {"sys_string_old_password", "原密码", "Old password"},
                {"sys_string_new_password", "新密码", "New password"},
                {"sys_string_welcome", "欢迎", "Welcome"},
                {"sys_string_login", "登录", "Login"},
                {"sys_string_logout", "退出系统", "Logout"},
                {"sys_string_add", "新增", "Add"},
                {"sys_string_copy", "复制", "Copy"},
                {"sys_string_change", "修改", "Change"},
                {"sys_string_confirm", "确定", "Confirm"},
                {"sys_string_cancel", "取消", "Cancel"},
                {"sys_string_delete", "删除", "Delete"},
                {"sys_string_back", "返回", "Back"},
                {"sys_string_success", "成功", "Success"},
                {"sys_string_fail", "失败", "Fail"},
                {"sys_string_fail_message", "失败信息", "Fail message"},
                {"sys_string_successful", "操作成功", "Successful"},
                {"sys_string_are_sure_to_copy", "确定要复制选中的记录", "Are you sure to copy the selected record"},
                {"sys_string_are_sure_to_delete", "确定要删除选中的记录", "Are you sure to delete the selected record"},
                {"sys_string_are_sure_to_pause", "确定要暂停选中的记录", "Are you sure to pause the selected record"},
                {"sys_string_are_sure_to_resume", "确定要恢复选中的记录", "Are you sure to resume the selected record"},
                {"sys_string_are_sure_to_execute", "确定要立即执行选中的记录", "Are you sure to execute the selected record"},
                {"sys_string_please_select_record", "请选择一条记录", "Please select one record"},
                {"sys_string_department_id", "部门ID", "Department ID"},
                {"sys_string_department_name", "部门名称", "Department name"},
                {"sys_string_department_key", "部门标识", "Department key"},
                {"sys_string_upper_department", "上级部门", "Upper department"},
                {"sys_string_upper_department_key", "上级标识", "Upper key"},
                {"sys_string_order_number", "排序号", "Order number"},
                {"sys_string_icon", "图标", "Icon"},
                {"sys_string_menu_icon", "菜单图标", "Menu icon"},
                {"sys_string_find_icon", "获取图标", "Find icon"},
                {"sys_string_dict_name", "字典名称", "Dict name"},
                {"sys_string_dict_type", "字典类型", "Dict type"},
                {"sys_string_dict_code", "字典码", "Dict code"},
                {"sys_string_dict_value", "字典值", "Dict value"},
                {"sys_string_user_id", "用户ID", "User ID"},
                {"sys_string_username", "用户名", "Username"},
                {"sys_string_query_username", "用户名、用户操作", "ex: username"},
                {"sys_string_type", "类型", "Type"},
                {"sys_string_directory", "目录", "Directory"},
                {"sys_string_menu", "菜单", "Menu"},
                {"sys_string_button", "按钮", "Button"},
                {"sys_string_menu_name", "菜单名称", "Menu name"},
                {"sys_string_menu_id", "菜单ID", "Menu ID"},
                {"sys_string_menu_or_button_name", "菜单名称或按钮名称", "Menu name or button name"},
                {"sys_string_menu_key", "菜单标记", "Menu key"},
                {"sys_string_menu_name_cannot_be_empty", "菜单名称不能为空", "Menu name cannot by empty"},
                {"sys_string_menu_url_cannot_be_empty", "菜单URL不能为空", "Menu url cannot by empty"},
                {"sys_string_menu_key_format", "格式：Menu:{模块包名}:{模块包名}Menu", "Format:Menu:{module}:{module}_menu"},
                {"sys_string_role", "角色", "Role"},
                {"sys_string_role_id", "角色ID", "Role ID"},
                {"sys_string_role_name", "角色名称", "Role name"},
                {"sys_string_role_key", "角色标识", "Role Key"},
                {"sys_string_own_department", "所属部门", "Own department"},
                {"sys_string_function_authority", "功能权限", "Function authority"},
                {"sys_string_data_authority", "数据权限", "Data authority"},
                {"sys_string_mobile", "手机", "Mobile"},
                {"sys_string_phone_number", "手机号", "Phone number"},
                {"sys_string_email", "邮箱", "Email"},
                {"sys_string_password", "密码", "Password"},
                {"sys_string_request_method", "请求方法", "Request method"},
                {"sys_string_request_parameter", "请求参数", "Request parameter"},
                {"sys_string_execute_duration", "执行时长(毫秒)", "Execute duration"},
                {"sys_string_ip_address", "IP地址", "IP Address"},
                {"sys_string_yes", "是", "Yes"},
                {"sys_string_no", "否", "No"},
        };
        return items;
    }

    @Override
    public void runJob() {
        load();
    }
}
