package cn.org.autumn.modules.lan.service;

import cn.org.autumn.config.PostLoad;
import cn.org.autumn.config.PostLoadFactory;
import cn.org.autumn.modules.lan.entity.LanguageEntity;
import cn.org.autumn.modules.lan.service.gen.LanguageServiceGen;
import cn.org.autumn.table.utils.HumpConvert;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.util.*;

@Service
public class LanguageService extends LanguageServiceGen implements PostLoad {
    private Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    PostLoadFactory postLoadFactory;

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
        addLanguageColumnItem("sys_string_normal", "正常", "Normal");
        addLanguageColumnItem("sys_string_forbidden", "禁用", "Forbidden");
        addLanguageColumnItem("sys_string_status", "状态", "Status");
        addLanguageColumnItem("sys_string_immediate_execution", "立即执行", "Immediate execution");
        addLanguageColumnItem("sys_string_log_list", "日志列表", "Log list");
        addLanguageColumnItem("sys_string_config_management", "参数管理", "Config management");
        addLanguageColumnItem("sys_string_config_name", "参数名", "Config name");
        addLanguageColumnItem("sys_string_config_value", "参数值", "Config value");
        addLanguageColumnItem("sys_string_system_log", "系统日志", "System log");
        addLanguageColumnItem("sys_string_file_upload", "文件上传", "File upload");
        addLanguageColumnItem("sys_string_department_management", "部门管理", "Department management");
        addLanguageColumnItem("sys_string_select_department", "选择部门", "Select department");
        addLanguageColumnItem("sys_string_dictionary_management", "字典管理", "Dictionary management");
        addLanguageColumnItem("sys_string_code_generator", "代码生成", "Code generator");
        addLanguageColumnItem("sys_string_generate", "生成", "generate");
        addLanguageColumnItem("sys_string_generator_solution", "生成方案", "Generator solution");
        addLanguageColumnItem("sys_string_select_generator_solution", "选择生成方案", "Select generator solution");
        addLanguageColumnItem("sys_string_table_name", "表名", "Table name");
        addLanguageColumnItem("sys_string_engine", "引擎", "Engine");
        addLanguageColumnItem("sys_string_table_comment", "表备注", "Table comment");
        addLanguageColumnItem("sys_string_remark", "备注", "Remark");
        addLanguageColumnItem("sys_string_create_time", "创建时间", "Create time");
        addLanguageColumnItem("sys_string_query", "查询", "Query");
        addLanguageColumnItem("sys_string_select_language", "请选择语言", "Please select language");
        addLanguageColumnItem("sys_string_modify_password", "修改密码", "Modify password");
        addLanguageColumnItem("sys_string_navigation_menu", "导航菜单", "Navigation menu");
        addLanguageColumnItem("sys_string_select_menu", "选择菜单", "Select menu");
        addLanguageColumnItem("sys_string_upper_menu", "上级菜单", "Upper menu");
        addLanguageColumnItem("sys_string_menu_url", "菜单URL", "Menu url");
        addLanguageColumnItem("sys_string_permissions", "授权标识", "Permissions");
        addLanguageColumnItem("sys_string_permissions_ex", "多个用逗号分隔，如：user:list,user:create", "Comma split，ex：user:list,user:create");
        addLanguageColumnItem("sys_string_root_menu", "一级菜单", "Root menu");
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
        addLanguageColumnItem("sys_string_cancel", "取消", "Cancel");
        addLanguageColumnItem("sys_string_delete", "删除", "Delete");
        addLanguageColumnItem("sys_string_back", "返回", "Back");
        addLanguageColumnItem("sys_string_success", "成功", "Success");
        addLanguageColumnItem("sys_string_fail", "失败", "Fail");
        addLanguageColumnItem("sys_string_fail_message", "失败信息", "Fail message");
        addLanguageColumnItem("sys_string_successful", "操作成功", "Successful");
        addLanguageColumnItem("sys_string_are_sure_to_delete", "确定要删除选中的记录", "Are you sure to delete the selected record");
        addLanguageColumnItem("sys_string_are_sure_to_pause", "确定要暂停选中的记录", "Are you sure to pause the selected record");
        addLanguageColumnItem("sys_string_are_sure_to_resume", "确定要恢复选中的记录", "Are you sure to resume the selected record");
        addLanguageColumnItem("sys_string_are_sure_to_execute", "确定要立即执行选中的记录", "Are you sure to execute the selected record");
        addLanguageColumnItem("sys_string_please_select_record", "请选择一条记录", "Please select one record");
        addLanguageColumnItem("sys_string_department_id", "部门ID", "Department ID");
        addLanguageColumnItem("sys_string_department_name", "部门名称", "Department name");
        addLanguageColumnItem("sys_string_upper_department", "上级部门", "Upper department");
        addLanguageColumnItem("sys_string_order_number", "排序号", "Order number");
        addLanguageColumnItem("sys_string_icon", "图标", "Icon");
        addLanguageColumnItem("sys_string_menu_icon", "菜单图标", "Menu icon");
        addLanguageColumnItem("sys_string_find_icon", "获取图标", "Find icon");

        addLanguageColumnItem("sys_string_dict_name", "字典名称", "Dict name");
        addLanguageColumnItem("sys_string_dict_type", "字典类型", "Dict type");
        addLanguageColumnItem("sys_string_dict_code", "字典码", "Dict code");
        addLanguageColumnItem("sys_string_dict_value", "字典值", "Dict value");


        addLanguageColumnItem("sys_string_user_id", "用户ID", "User ID");
        addLanguageColumnItem("sys_string_username", "用户名", "Username");
        addLanguageColumnItem("sys_string_query_username", "用户名、用户操作", "ex: username");
        addLanguageColumnItem("sys_string_type", "类型", "Type");
        addLanguageColumnItem("sys_string_directory", "目录", "Directory");
        addLanguageColumnItem("sys_string_menu", "菜单", "Menu");
        addLanguageColumnItem("sys_string_button", "按钮", "Button");
        addLanguageColumnItem("sys_string_menu_name", "菜单名称", "Menu name");
        addLanguageColumnItem("sys_string_menu_id", "菜单ID", "Menu ID");
        addLanguageColumnItem("sys_string_menu_or_button_name", "菜单名称或按钮名称", "Menu name or button name");
        addLanguageColumnItem("sys_string_menu_key", "菜单标记", "Menu key");
        addLanguageColumnItem("sys_string_menu_name_cannot_be_empty", "菜单名称不能为空", "Menu name cannot by empty");
        addLanguageColumnItem("sys_string_menu_url_cannot_be_empty", "菜单URL不能为空", "Menu url cannot by empty");



        addLanguageColumnItem("sys_string_menu_key_format", "格式：{模块包名}_menu", "Format:(module)_menu");

        addLanguageColumnItem("sys_string_role", "角色", "Role");
        addLanguageColumnItem("sys_string_role_id", "角色ID", "Role ID");
        addLanguageColumnItem("sys_string_role_name", "角色名称", "Role name");
        addLanguageColumnItem("sys_string_own_department", "所属部门", "Own department");
        addLanguageColumnItem("sys_string_function_authority", "功能权限", "Function authority");
        addLanguageColumnItem("sys_string_data_authority", "数据权限", "Data authority");

        addLanguageColumnItem("sys_string_mobile", "手机", "Mobile");
        addLanguageColumnItem("sys_string_phone_number", "手机号", "Phone number");
        addLanguageColumnItem("sys_string_email", "邮箱", "Email");
        addLanguageColumnItem("sys_string_password", "密码", "Password");

        addLanguageColumnItem("sys_string_request_method", "请求方法", "Request method");
        addLanguageColumnItem("sys_string_request_parameter", "请求参数", "Request parameter");
        addLanguageColumnItem("sys_string_execute_duration", "执行时长(毫秒)", "Execute duration");
        addLanguageColumnItem("sys_string_ip_address", "IP地址", "IP Address");



        addLanguageColumnItem();
        postLoadFactory.register(this);
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
