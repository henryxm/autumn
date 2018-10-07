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

import cn.org.autumn.modules.sms.entity.AliyunSmsSignEntity;
import cn.org.autumn.modules.sms.service.AliyunSmsSignService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;



/**
 * 短信签名
 *
 * @author Shaohua
 * @email henryxm@163.com
 * @date 2018-10
 */
@RestController
@RequestMapping("sms/aliyunsmssign")
public class AliyunSmsSignController {
    @Autowired
    private AliyunSmsSignService aliyunSmsSignService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("sms:aliyunsmssign:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = aliyunSmsSignService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    @RequiresPermissions("sms:aliyunsmssign:info")
    public R info(@PathVariable("id") Long id){
        AliyunSmsSignEntity aliyunSmsSign = aliyunSmsSignService.selectById(id);

        return R.ok().put("aliyunSmsSign", aliyunSmsSign);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @RequiresPermissions("sms:aliyunsmssign:save")
    public R save(@RequestBody AliyunSmsSignEntity aliyunSmsSign){
        aliyunSmsSignService.insert(aliyunSmsSign);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @RequiresPermissions("sms:aliyunsmssign:update")
    public R update(@RequestBody AliyunSmsSignEntity aliyunSmsSign){
        ValidatorUtils.validateEntity(aliyunSmsSign);
        aliyunSmsSignService.updateAllColumnById(aliyunSmsSign);//全部更新
        
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @RequiresPermissions("sms:aliyunsmssign:delete")
    public R delete(@RequestBody Long[] ids){
        aliyunSmsSignService.deleteBatchIds(Arrays.asList(ids));

        return R.ok();
    }

}
