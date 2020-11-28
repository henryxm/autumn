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
import cn.org.autumn.modules.oauth.entity.TokenStoreEntity;
import cn.org.autumn.modules.oauth.service.TokenStoreService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;

/**
 * 授权令牌
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
public class TokenStoreControllerGen {

    @Autowired
    protected TokenStoreService tokenStoreService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("oauth:tokenstore:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = tokenStoreService.queryPage(params);
        return R.ok().put("page" , page);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    @RequiresPermissions("oauth:tokenstore:info")
    public R info(@PathVariable("id") Long id) {
        TokenStoreEntity tokenStore = tokenStoreService.selectById(id);
        return R.ok().put("tokenStore" , tokenStore);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @RequiresPermissions("oauth:tokenstore:save")
    public R save(@RequestBody TokenStoreEntity tokenStore) {
        tokenStoreService.insert(tokenStore);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @RequiresPermissions("oauth:tokenstore:update")
    public R update(@RequestBody TokenStoreEntity tokenStore) {
        ValidatorUtils.validateEntity(tokenStore);
        tokenStoreService.updateAllColumnById(tokenStore);//全部更新
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @RequiresPermissions("oauth:tokenstore:delete")
    public R delete(@RequestBody Long[] ids) {
        tokenStoreService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }
}
