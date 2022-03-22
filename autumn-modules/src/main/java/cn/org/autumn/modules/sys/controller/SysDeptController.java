package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.modules.sys.entity.SysDeptEntity;
import cn.org.autumn.modules.sys.service.SysDeptService;
import cn.org.autumn.utils.R;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/sys/dept")
public class SysDeptController extends AbstractController {
    @Autowired
    @Lazy
    private SysDeptService sysDeptService;

    @Autowired
    @Lazy
    private SysUserService sysUserService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("sys:dept:list")
    public List<SysDeptEntity> list() {
        List<SysDeptEntity> deptList = sysDeptService.queryList(new HashMap<>());

        return deptList;
    }

    /**
     * 选择部门(添加、修改菜单)
     */
    @RequestMapping("/select")
    @RequiresPermissions("sys:dept:select")
    public R select() {
        List<SysDeptEntity> deptList = sysDeptService.queryList(new HashMap<>());

        //添加一级部门
        if (sysUserService.isSystemAdministrator(getUser())) {
            SysDeptEntity root = new SysDeptEntity();
            root.setDeptKey("0");
            root.setName("一级部门");
            root.setParentKey("");
            root.setOpen(true);
            deptList.add(root);
        }

        return R.ok().put("deptList", deptList);
    }

    /**
     * 上级部门Id(管理员则为0)
     */
    @RequestMapping("/info")
    @RequiresPermissions("sys:dept:list")
    public R info() {
        String deptKey = "";
        if (sysUserService.isSystemAdministrator(getUser())) {
            List<SysDeptEntity> deptList = sysDeptService.queryList(new HashMap<>());
            String parentId = null;
            for (SysDeptEntity sysDeptEntity : deptList) {
                if (parentId == null) {
                    parentId = sysDeptEntity.getParentKey();
                    continue;
                }
            }
            deptKey = parentId;
        }
        return R.ok().put("deptKey", deptKey);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{deptKey}")
    @RequiresPermissions("sys:dept:info")
    public R info(@PathVariable("deptKey") String deptKey) {
        SysDeptEntity dept = sysDeptService.getByDeptKey(deptKey);
        return R.ok().put("dept", dept);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @RequiresPermissions("sys:dept:save")
    public R save(@RequestBody SysDeptEntity dept) {
        sysDeptService.save(dept);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @RequiresPermissions("sys:dept:update")
    public R update(@RequestBody SysDeptEntity dept) {
        sysDeptService.save(dept);
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @RequiresPermissions("sys:dept:delete")
    public R delete(String deptKey) {
        //判断是否有子部门
        List<String> deptList = sysDeptService.getByParentKey(deptKey);
        if (deptList.size() > 0) {
            return R.error("请先删除子部门");
        }
        sysDeptService.deleteById(deptKey);
        return R.ok();
    }
}
