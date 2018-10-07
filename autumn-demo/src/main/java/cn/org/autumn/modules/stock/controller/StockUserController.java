package cn.org.autumn.modules.stock.controller;

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

import cn.org.autumn.modules.stock.entity.StockUserEntity;
import cn.org.autumn.modules.stock.service.StockUserService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;



/**
 * 股票用户
 *
 * @author Shaohua
 * @email henryxm@163.com
 * @date 2018-10
 */
@RestController
@RequestMapping("stock/stockuser")
public class StockUserController {
    @Autowired
    private StockUserService stockUserService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("stock:stockuser:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = stockUserService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    @RequiresPermissions("stock:stockuser:info")
    public R info(@PathVariable("id") Long id){
        StockUserEntity stockUser = stockUserService.selectById(id);

        return R.ok().put("stockUser", stockUser);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @RequiresPermissions("stock:stockuser:save")
    public R save(@RequestBody StockUserEntity stockUser){
        stockUserService.insert(stockUser);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @RequiresPermissions("stock:stockuser:update")
    public R update(@RequestBody StockUserEntity stockUser){
        ValidatorUtils.validateEntity(stockUser);
        stockUserService.updateAllColumnById(stockUser);//全部更新
        
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @RequiresPermissions("stock:stockuser:delete")
    public R delete(@RequestBody Long[] ids){
        stockUserService.deleteBatchIds(Arrays.asList(ids));

        return R.ok();
    }

}
