package cn.org.autumn.modules.lan.service;

import cn.org.autumn.modules.lan.entity.LanguageEntity;
import cn.org.autumn.modules.lan.service.gen.LanguageServiceGen;
import cn.org.autumn.table.utils.HumpConvert;
import io.netty.util.internal.StringUtil;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.util.*;

@Service
public class LanguageService extends LanguageServiceGen {

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

    public int parentMenu() {
        return 1;
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
        if (baseMapper.hasKey(key) > 0)
            return false;
        LanguageEntity languageEntity = new LanguageEntity();
        languageEntity.setName(key);
        languageEntity.setZhCn(zhCn);
        languageEntity.setEnUs(enUs);
        insert(languageEntity);
        return true;
    }

    public boolean addLanguageColumnItem(String key, String zhCn) {
        return addLanguageColumnItem(key, zhCn, null);
    }

    @PostConstruct
    public void init() {
        super.init();
        addLanguageColumnItem("sys_string_management", "管理系统", "Manage system");
        addLanguageColumnItem("sys_string_system_management", "系统管理", "System management");
        addLanguageColumnItem("sys_string_manager_management", "管理员管理", "Manager management");
        addLanguageColumnItem("sys_string_role_management", "角色管理", "Role management");
        addLanguageColumnItem("sys_string_menu_management", "菜单管理", "Menu management");
        addLanguageColumnItem("sys_string_sql_monitor", "SQL监控", "SQL monitor");
        addLanguageColumnItem("sys_string_job_schedule", "定时任务", "Schedule job");
        addLanguageColumnItem("sys_string_lookup", "查看", "Look up");
        addLanguageColumnItem("sys_string_suspend", "暂停", "Suspend");
        addLanguageColumnItem("sys_string_resume", "恢复", "Resume");
        addLanguageColumnItem("sys_string_immediate_execution", "立即执行", "Immediate execution");
        addLanguageColumnItem("sys_string_log_list", "日志列表", "Log list");
        addLanguageColumnItem("sys_string_config_management", "参数管理", "Config management");
        addLanguageColumnItem("sys_string_system_log", "系统日志", "System log");
        addLanguageColumnItem("sys_string_file_upload", "文件上传", "File upload");
        addLanguageColumnItem("sys_string_department_management", "部门管理", "Department management");
        addLanguageColumnItem("sys_string_dictionary_management", "字典管理", "Dictionary management");

        addLanguageColumnItem("sys_string_code_generator", "代码生成", "Code generator");
        addLanguageColumnItem("sys_string_generate", "生成", "generate");
        addLanguageColumnItem("sys_string_generator_solution", "生成方案", "Generator solution");

        addLanguageColumnItem("sys_string_select_language", "请选择语言", "Please select language");
        addLanguageColumnItem("sys_string_modify_password", "修改密码", "Modify password");
        addLanguageColumnItem("sys_string_navigation_menu", "导航菜单", "Navigation menu");
        addLanguageColumnItem("sys_string_home", "首页", "Home");
        addLanguageColumnItem("sys_string_account", "账号", "Account");
        addLanguageColumnItem("sys_string_old_password", "原密码", "Old password");
        addLanguageColumnItem("sys_string_new_password", "新密码", "New password");
        addLanguageColumnItem("sys_string_welcome", "欢迎", "Welcome");
        addLanguageColumnItem("sys_string_login", "登录", "Login");
        addLanguageColumnItem("sys_string_logout", "退出系统", "Logout");
        addLanguageColumnItem("sys_string_add", "新增", "Add");
        addLanguageColumnItem("sys_string_change", "修改", "Change");
        addLanguageColumnItem("sys_string_confirm", "确定", "Confirm");
        addLanguageColumnItem("sys_string_delete", "删除", "Delete");
        addLanguageColumnItem("sys_string_back", "返回", "Back");
        addLanguageColumnItem("sys_string_successful", "操作成功", "Successful");
        addLanguageColumnItem("sys_string_are_sure_to_delete", "确定要删除选中的记录", "Are you sure to delete the selected record");
        addLanguageColumnItem();
        load();
    }

    public void addLanguageColumnItem() {
        addLanguageColumnItem("lan_language_table_comment", "国家语言", "Language");
        addLanguageColumnItem("lan_language_column_name", "标识", "Unique Name");
        addLanguageColumnItem("lan_language_column_zh_cn", "简体中文(中国)", "Chinese(China)");
        addLanguageColumnItem("lan_language_column_en_us", "英语(美国)");
        addLanguageColumnItem("lan_language_column_zh_hk", "繁体中文(香港)");
        addLanguageColumnItem("lan_language_column_ko_kr", "韩语(韩国)");
        addLanguageColumnItem("lan_language_column_ja_jp", "日语(日本)");
        addLanguageColumnItem("lan_language_column_tt_ru", "俄语(俄罗斯)");
        addLanguageColumnItem("lan_language_column_fr_fr", "法语(法国)");
        addLanguageColumnItem("lan_language_column_de_de", "德语(德国)");
        addLanguageColumnItem("lan_language_column_vi_vn", "越语(越南)");
        addLanguageColumnItem("lan_language_column_th_th", "泰语(泰国)");
        addLanguageColumnItem("lan_language_column_ms_my", "马来语(马来西亚)");
        addLanguageColumnItem("lan_language_column_id_id", "印尼语(印尼)");
        addLanguageColumnItem("lan_language_column_es_es", "西班牙语(西班牙)");
        addLanguageColumnItem("lan_language_column_tr_tr", "土耳其语(土耳其)");
        addLanguageColumnItem("lan_language_column_uk_uk", "乌克兰语(乌克兰)");
        addLanguageColumnItem("lan_language_column_pu_pt", "葡萄牙语(葡萄牙)");
        addLanguageColumnItem("lan_language_column_pl_pl", "波兰语(波兰)");
        addLanguageColumnItem("lan_language_column_mn_mn", "蒙古语(蒙古)");
        addLanguageColumnItem("lan_language_column_nb_no", "挪威语(挪威)");
        addLanguageColumnItem("lan_language_column_it_it", "意大利语(意大利)");
        addLanguageColumnItem("lan_language_column_he_il", "希伯来语(以色列)");
        addLanguageColumnItem("lan_language_column_el_gr", "希腊语(希腊)");
        addLanguageColumnItem("lan_language_column_fa_ir", "波斯语(伊朗)");
        addLanguageColumnItem("lan_language_column_ar_sa", "阿拉伯语(沙特阿拉伯)");
    }
}
