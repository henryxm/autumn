package cn.org.autumn.modules.sms.controller;

import java.util.Arrays;
import java.util.Map;

import cn.org.autumn.validator.ValidatorUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.org.autumn.modules.sms.entity.AliyunAccessKeysEntity;
import cn.org.autumn.modules.sms.service.AliyunAccessKeysService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;



/**
 * 阿里云授权码
 *
 * @author Shaohua
 * @email henryxm@163.com
 * @date 2018-10
 */
@RestController
@RequestMapping("sms/aliyunaccesskeys")
public class AliyunAccessKeysController {
    @Autowired
    private AliyunAccessKeysService aliyunAccessKeysService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("sms:aliyunaccesskeys:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = aliyunAccessKeysService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    @RequiresPermissions("sms:aliyunaccesskeys:info")
    public R info(@PathVariable("id") Long id){
        AliyunAccessKeysEntity aliyunAccessKeys = aliyunAccessKeysService.selectById(id);

        return R.ok().put("aliyunAccessKeys", aliyunAccessKeys);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @RequiresPermissions("sms:aliyunaccesskeys:save")
    public R save(@RequestBody AliyunAccessKeysEntity aliyunAccessKeys){
        aliyunAccessKeysService.insert(aliyunAccessKeys);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @RequiresPermissions("sms:aliyunaccesskeys:update")
    public R update(@RequestBody AliyunAccessKeysEntity aliyunAccessKeys){
        ValidatorUtils.validateEntity(aliyunAccessKeys);
        aliyunAccessKeysService.updateAllColumnById(aliyunAccessKeys);//全部更新
        
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @RequiresPermissions("sms:aliyunaccesskeys:delete")
    public R delete(@RequestBody Long[] ids){
        aliyunAccessKeysService.deleteBatchIds(Arrays.asList(ids));

        return R.ok();
    }

}
