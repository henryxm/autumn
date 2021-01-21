package cn.org.autumn.modules.job.service.gen;

import cn.org.autumn.site.InitFactory;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.Query;
import cn.org.autumn.modules.job.service.JobMenu;
import cn.org.autumn.modules.job.dao.ScheduleJobLogDao;
import cn.org.autumn.modules.job.entity.ScheduleJobLogEntity;
import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import cn.org.autumn.modules.sys.service.SysMenuService;
import cn.org.autumn.modules.lan.service.Language;
import cn.org.autumn.modules.lan.service.LanguageService;

/**
 * 任务日志控制器
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2021-01
 */
public class ScheduleJobLogServiceGen extends ServiceImpl<ScheduleJobLogDao, ScheduleJobLogEntity> implements InitFactory.Init {

    @Autowired
    protected JobMenu jobMenu;

    @Autowired
    protected SysMenuService sysMenuService;

    @Autowired
    protected Language language;

    @Autowired
    protected LanguageService languageService;

    public PageUtils queryPage(Map<String, Object> params) {
        Page<ScheduleJobLogEntity> _page = new Query<ScheduleJobLogEntity>(params).getPage();
        EntityWrapper<ScheduleJobLogEntity> entityEntityWrapper = new EntityWrapper<>();
        Map<String,Object> condition = new HashMap<>();
        if(params.containsKey("logId") && null != params.get("logId") && StringUtils.isNotEmpty(params.get("logId").toString())) {
            condition.put("log_id", params.get("logId"));
        }
        if(params.containsKey("jobId") && null != params.get("jobId") && StringUtils.isNotEmpty(params.get("jobId").toString())) {
            condition.put("job_id", params.get("jobId"));
        }
        if(params.containsKey("beanName") && null != params.get("beanName") && StringUtils.isNotEmpty(params.get("beanName").toString())) {
            condition.put("bean_name", params.get("beanName"));
        }
        if(params.containsKey("methodName") && null != params.get("methodName") && StringUtils.isNotEmpty(params.get("methodName").toString())) {
            condition.put("method_name", params.get("methodName"));
        }
        if(params.containsKey("params") && null != params.get("params") && StringUtils.isNotEmpty(params.get("params").toString())) {
            condition.put("params", params.get("params"));
        }
        if(params.containsKey("status") && null != params.get("status") && StringUtils.isNotEmpty(params.get("status").toString())) {
            condition.put("status", params.get("status"));
        }
        if(params.containsKey("error") && null != params.get("error") && StringUtils.isNotEmpty(params.get("error").toString())) {
            condition.put("error", params.get("error"));
        }
        if(params.containsKey("times") && null != params.get("times") && StringUtils.isNotEmpty(params.get("times").toString())) {
            condition.put("times", params.get("times"));
        }
        if(params.containsKey("createTime") && null != params.get("createTime") && StringUtils.isNotEmpty(params.get("createTime").toString())) {
            condition.put("create_time", params.get("createTime"));
        }
        _page.setCondition(condition);
        Page<ScheduleJobLogEntity> page = this.selectPage(_page, entityEntityWrapper);
        page.setTotal(baseMapper.selectCount(entityEntityWrapper));
        return new PageUtils(page);
    }

    /**
    * need implement it in the subclass.
    * @return
    */
    public int menuOrder(){
        return 0;
    }

    /**
    * need implement it in the subclass.
    * @return
    */
    public String parentMenu(){
        jobMenu.init();
        SysMenuEntity sysMenuEntity = sysMenuService.getByMenuKey(jobMenu.getMenu());
        if(null != sysMenuEntity)
            return sysMenuEntity.getMenuKey();
        return "";
    }

    public String menu() {
        String menu = SysMenuService.getMenuKey("Job", "ScheduleJobLog");
        return menu;
    }

    public String button(String button) {
        String menu = menu() + button;
        return menu;
    }

    public String ico(){
        return "fa-file-code-o";
    }

    protected String order(){
        return String.valueOf(menuOrder());
    }

    public void init() {
        sysMenuService.put(getMenuItemsInternal(), getMenuItems(), getMenuList());
        language.put(getLanguageItemsInternal(), getLanguageItems(), getLanguageList());
    }

    public List<String[]> getLanguageList() {
        return null;
    }

    public String[][] getLanguageItems() {
        return null;
    }

    private String[][] getLanguageItemsInternal() {
        String[][] items = new String[][]{
                {"job_schedulejoblog_table_comment", "任务日志"},
                {"job_schedulejoblog_column_log_id", "任务日志id"},
                {"job_schedulejoblog_column_job_id", "任务id"},
                {"job_schedulejoblog_column_bean_name", "BeanName"},
                {"job_schedulejoblog_column_method_name", "方法名"},
                {"job_schedulejoblog_column_params", "参数"},
                {"job_schedulejoblog_column_status", "任务状态,0:成功,1:失败"},
                {"job_schedulejoblog_column_error", "失败信息"},
                {"job_schedulejoblog_column_times", "耗时(单位：毫秒)"},
                {"job_schedulejoblog_column_create_time", "创建时间"},
        };
        return items;
    }

    public List<String[]> getMenuList() {
        return null;
    }

    public String[][] getMenuItems() {
        return null;
    }

    private String[][] getMenuItemsInternal() {
        String[][] menus = new String[][]{
                //{0:菜单名字,1:URL,2:权限,3:菜单类型,4:ICON,5:排序,6:MenuKey,7:ParentKey,8:Language}
                {"任务日志", "modules/job/schedulejoblog", "job:schedulejoblog:list,job:schedulejoblog:info,job:schedulejoblog:save,job:schedulejoblog:update,job:schedulejoblog:delete", "1", "fa " + ico(), order(), menu(), parentMenu(), "job_schedulejoblog_table_comment"},
                {"查看", null, "job:schedulejoblog:list,job:schedulejoblog:info", "2", null, order(), button("List"), menu(), "sys_string_lookup"},
                {"新增", null, "job:schedulejoblog:save", "2", null, order(), button("Save"), menu(), "sys_string_add"},
                {"修改", null, "job:schedulejoblog:update", "2", null, order(), button("Update"), menu(), "sys_string_change"},
                {"删除", null, "job:schedulejoblog:delete", "2", null, order(), button("Delete"), menu(), "sys_string_delete"},
        };
        return menus;
    }
}
