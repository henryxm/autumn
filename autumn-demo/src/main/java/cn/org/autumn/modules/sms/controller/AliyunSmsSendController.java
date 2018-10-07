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

import cn.org.autumn.modules.sms.entity.AliyunSmsSendEntity;
import cn.org.autumn.modules.sms.service.AliyunSmsSendService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;



/**
 * 发送验证码
 *
 * @author Shaohua
 * @email henryxm@163.com
 * @date 2018-10
 */
@RestController
@RequestMapping("sms/aliyunsmssend")
public class AliyunSmsSendController {
    @Autowired
    private AliyunSmsSendService aliyunSmsSendService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("sms:aliyunsmssend:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = aliyunSmsSendService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    @RequiresPermissions("sms:aliyunsmssend:info")
    public R info(@PathVariable("id") Long id){
        AliyunSmsSendEntity aliyunSmsSend = aliyunSmsSendService.selectById(id);

        return R.ok().put("aliyunSmsSend", aliyunSmsSend);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @RequiresPermissions("sms:aliyunsmssend:save")
    public R save(@RequestBody AliyunSmsSendEntity aliyunSmsSend){
        aliyunSmsSendService.insert(aliyunSmsSend);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @RequiresPermissions("sms:aliyunsmssend:update")
    public R update(@RequestBody AliyunSmsSendEntity aliyunSmsSend){
        ValidatorUtils.validateEntity(aliyunSmsSend);
        aliyunSmsSendService.updateAllColumnById(aliyunSmsSend);//全部更新
        
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @RequiresPermissions("sms:aliyunsmssend:delete")
    public R delete(@RequestBody Long[] ids){
        aliyunSmsSendService.deleteBatchIds(Arrays.asList(ids));

        return R.ok();
    }

}
