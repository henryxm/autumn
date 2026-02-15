package cn.org.autumn.modules.spm.controller.gen;

import java.util.Arrays;
import java.util.Map;
import cn.org.autumn.validator.ValidatorUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import cn.org.autumn.modules.spm.entity.SuperPositionModelEntity;
import cn.org.autumn.modules.spm.service.SuperPositionModelService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;

/**
 * 超级位置模型
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-10
 */
public class SuperPositionModelControllerGen {

    @Autowired
    protected SuperPositionModelService superPositionModelService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("spm:superpositionmodel:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = superPositionModelService.queryPage(params);
        return R.ok().put("page" , page);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    @RequiresPermissions("spm:superpositionmodel:info")
    public R info(@PathVariable("id") Long id) {
        SuperPositionModelEntity superPositionModel = superPositionModelService.getById(id);
        return R.ok().put("superPositionModel" , superPositionModel);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @RequiresPermissions("spm:superpositionmodel:save")
    public R save(@RequestBody SuperPositionModelEntity superPositionModel) {
        superPositionModelService.save(superPositionModel);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @RequiresPermissions("spm:superpositionmodel:update")
    public R update(@RequestBody SuperPositionModelEntity superPositionModel) {
        ValidatorUtils.validateEntity(superPositionModel);
        superPositionModelService.updateById(superPositionModel);//全部更新
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @RequiresPermissions("spm:superpositionmodel:delete")
    public R delete(@RequestBody Long[] ids) {
        superPositionModelService.removeBatchByIds(Arrays.asList(ids));
        return R.ok();
    }
}
