package cn.org.autumn.modules.oauth.controller.gen;

import java.util.Arrays;
import java.util.Map;
import cn.org.autumn.validator.ValidatorUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import cn.org.autumn.modules.oauth.entity.ClientDetailsEntity;
import cn.org.autumn.modules.oauth.service.ClientDetailsService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;

/**
 * 客户端详情
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
public class ClientDetailsControllerGen {

    @Autowired
    protected ClientDetailsService clientDetailsService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("oauth:clientdetails:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = clientDetailsService.queryPage(params);
        return R.ok().put("page" , page);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    @RequiresPermissions("oauth:clientdetails:info")
    public R info(@PathVariable("id") Long id) {
        ClientDetailsEntity clientDetails = clientDetailsService.selectById(id);
        return R.ok().put("clientDetails" , clientDetails);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @RequiresPermissions("oauth:clientdetails:save")
    public R save(@RequestBody ClientDetailsEntity clientDetails) {
        clientDetailsService.insert(clientDetails);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @RequiresPermissions("oauth:clientdetails:update")
    public R update(@RequestBody ClientDetailsEntity clientDetails) {
        ValidatorUtils.validateEntity(clientDetails);
        clientDetailsService.updateAllColumnById(clientDetails);//全部更新
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @RequiresPermissions("oauth:clientdetails:delete")
    public R delete(@RequestBody Long[] ids) {
        clientDetailsService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }
}
