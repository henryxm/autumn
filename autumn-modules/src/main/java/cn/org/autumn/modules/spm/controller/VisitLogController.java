package cn.org.autumn.modules.spm.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import cn.org.autumn.modules.spm.controller.gen.VisitLogControllerGen;

/**
 * 访问统计
 *
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-11
 */
@RestController
@RequestMapping("spm/visitlog")
public class VisitLogController extends VisitLogControllerGen {

}
