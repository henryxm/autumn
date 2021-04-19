package cn.org.autumn.modules.lan.controller;

import cn.org.autumn.modules.lan.entity.LanguageMetadata;
import cn.org.autumn.utils.R;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.*;

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

    /**
     * 语言资源注册接口，可以通过外部调用，增加多语言资源，集群时，各个子系统需要相互调用以注册多语言资源
     *
     * @param lang
     * @return
     */
    @RequestMapping(value = "/put", method = RequestMethod.POST)
    public R put(@RequestBody String[][] lang) {
        Object[] objects = new Object[1];
        objects[0] = lang;
        languageService.put(objects);
        return R.ok();
    }
}
