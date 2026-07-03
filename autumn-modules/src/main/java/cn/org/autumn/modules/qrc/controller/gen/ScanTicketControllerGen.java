package cn.org.autumn.modules.qrc.controller.gen;

import java.util.Arrays;
import java.util.Map;
import cn.org.autumn.validator.ValidatorUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import cn.org.autumn.modules.qrc.entity.ScanTicketEntity;
import cn.org.autumn.modules.qrc.service.ScanTicketService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;

public class ScanTicketControllerGen {

    @Autowired
    protected ScanTicketService scanTicketService;

    @RequestMapping("/list")
    @RequiresPermissions("qrc:scanticket:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = scanTicketService.queryPage(params);
        return R.ok().put("page" , page);
    }

    @RequestMapping("/info/{id}")
    @RequiresPermissions("qrc:scanticket:info")
    public R info(@PathVariable("id") Long id) {
        ScanTicketEntity scanTicket = scanTicketService.selectById(id);
        return R.ok().put("scanTicket" , scanTicket);
    }

    @RequestMapping("/save")
    @RequiresPermissions("qrc:scanticket:save")
    public R save(@RequestBody ScanTicketEntity scanTicket) {
        scanTicketService.insert(scanTicket);
        return R.ok();
    }

    @RequestMapping("/update")
    @RequiresPermissions("qrc:scanticket:update")
    public R update(@RequestBody ScanTicketEntity scanTicket) {
        ValidatorUtils.validateEntity(scanTicket);
        scanTicketService.updateAllColumnById(scanTicket);
        return R.ok();
    }

    @RequestMapping("/delete")
    @RequiresPermissions("qrc:scanticket:delete")
    public R delete(@RequestBody Long[] ids) {
        scanTicketService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }
}
