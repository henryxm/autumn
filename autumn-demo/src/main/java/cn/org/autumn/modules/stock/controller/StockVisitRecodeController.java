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

import cn.org.autumn.modules.stock.entity.StockVisitRecodeEntity;
import cn.org.autumn.modules.stock.service.StockVisitRecodeService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;



/**
 * 股票用户访问记录
 *
 * @author Shaohua
 * @email henryxm@163.com
 * @date 2018-10
 */
@RestController
@RequestMapping("stock/stockvisitrecode")
public class StockVisitRecodeController {
    @Autowired
    private StockVisitRecodeService stockVisitRecodeService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("stock:stockvisitrecode:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = stockVisitRecodeService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    @RequiresPermissions("stock:stockvisitrecode:info")
    public R info(@PathVariable("id") Long id){
        StockVisitRecodeEntity stockVisitRecode = stockVisitRecodeService.selectById(id);

        return R.ok().put("stockVisitRecode", stockVisitRecode);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @RequiresPermissions("stock:stockvisitrecode:save")
    public R save(@RequestBody StockVisitRecodeEntity stockVisitRecode){
        stockVisitRecodeService.insert(stockVisitRecode);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @RequiresPermissions("stock:stockvisitrecode:update")
    public R update(@RequestBody StockVisitRecodeEntity stockVisitRecode){
        ValidatorUtils.validateEntity(stockVisitRecode);
        stockVisitRecodeService.updateAllColumnById(stockVisitRecode);//全部更新
        
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @RequiresPermissions("stock:stockvisitrecode:delete")
    public R delete(@RequestBody Long[] ids){
        stockVisitRecodeService.deleteBatchIds(Arrays.asList(ids));

        return R.ok();
    }

}
