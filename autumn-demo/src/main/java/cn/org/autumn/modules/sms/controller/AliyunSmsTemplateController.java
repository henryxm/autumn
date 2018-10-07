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

import cn.org.autumn.modules.sms.entity.AliyunSmsTemplateEntity;
import cn.org.autumn.modules.sms.service.AliyunSmsTemplateService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;



/**
 * 阿里云短信模板
 *
 * @author Shaohua
 * @email henryxm@163.com
 * @date 2018-10
 */
@RestController
@RequestMapping("sms/aliyunsmstemplate")
public class AliyunSmsTemplateController {
    @Autowired
    private AliyunSmsTemplateService aliyunSmsTemplateService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("sms:aliyunsmstemplate:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = aliyunSmsTemplateService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    @RequiresPermissions("sms:aliyunsmstemplate:info")
    public R info(@PathVariable("id") Long id){
        AliyunSmsTemplateEntity aliyunSmsTemplate = aliyunSmsTemplateService.selectById(id);

        return R.ok().put("aliyunSmsTemplate", aliyunSmsTemplate);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @RequiresPermissions("sms:aliyunsmstemplate:save")
    public R save(@RequestBody AliyunSmsTemplateEntity aliyunSmsTemplate){
        aliyunSmsTemplateService.insert(aliyunSmsTemplate);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @RequiresPermissions("sms:aliyunsmstemplate:update")
    public R update(@RequestBody AliyunSmsTemplateEntity aliyunSmsTemplate){
        ValidatorUtils.validateEntity(aliyunSmsTemplate);
        aliyunSmsTemplateService.updateAllColumnById(aliyunSmsTemplate);//全部更新
        
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @RequiresPermissions("sms:aliyunsmstemplate:delete")
    public R delete(@RequestBody Long[] ids){
        aliyunSmsTemplateService.deleteBatchIds(Arrays.asList(ids));

        return R.ok();
    }

}
