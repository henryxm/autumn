package cn.org.autumn.modules.lan.controller;

import cn.org.autumn.modules.lan.entity.LanguageMetadata;
import cn.org.autumn.utils.R;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.org.autumn.modules.lan.controller.gen.LanguageControllerGen;

import java.util.List;
import java.util.Map;


/**
 * 多语言
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-10
 */
@RestController
@RequestMapping("lan/language")
public class LanguageController extends LanguageControllerGen {

    /**
     * 列表
     */
    @RequestMapping("/supportedlist")
    @RequiresPermissions("lan:language:supportedlist")
    public R supportedlist(@RequestParam Map<String, Object> params) {
        List<LanguageMetadata> languageMetadataList = languageService.getLanguageMetadata();
        return R.ok().put("language", languageMetadataList);
    }

    @RequestMapping("/updatesupportedlanguage")
    @RequiresPermissions("lan:language:updatesupported")
    public R updatesupportedlanguage(@RequestBody List<LanguageMetadata> languageMetadata) {
        languageService.updateLanguageMetadata(languageMetadata);
        return R.ok();
    }
}
