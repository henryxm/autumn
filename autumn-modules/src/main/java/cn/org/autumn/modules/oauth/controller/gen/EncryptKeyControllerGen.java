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
import cn.org.autumn.modules.oauth.entity.EncryptKeyEntity;
import cn.org.autumn.modules.oauth.service.EncryptKeyService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;

/**
 * 加密秘钥
 *
 * @author User
 * @email henryxm@163.com
 * @date 2025-12
 */
public class EncryptKeyControllerGen {

    @Autowired
    protected EncryptKeyService encryptKeyService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("oauth:encryptkey:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = encryptKeyService.queryPage(params);
        return R.ok().put("page" , page);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    @RequiresPermissions("oauth:encryptkey:info")
    public R info(@PathVariable("id") Long id) {
        EncryptKeyEntity encryptKey = encryptKeyService.getById(id);
        return R.ok().put("encryptKey" , encryptKey);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @RequiresPermissions("oauth:encryptkey:save")
    public R save(@RequestBody EncryptKeyEntity encryptKey) {
        encryptKeyService.save(encryptKey);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @RequiresPermissions("oauth:encryptkey:update")
    public R update(@RequestBody EncryptKeyEntity encryptKey) {
        ValidatorUtils.validateEntity(encryptKey);
        encryptKeyService.updateById(encryptKey);//全部更新
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @RequiresPermissions("oauth:encryptkey:delete")
    public R delete(@RequestBody Long[] ids) {
        encryptKeyService.removeBatchByIds(Arrays.asList(ids));
        return R.ok();
    }
}
