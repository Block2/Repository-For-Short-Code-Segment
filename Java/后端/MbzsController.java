package winning.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import winning.common.utils.RequestUtils;
import winning.service.MbzsService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * Created by 高阳 on 2019/4/2.
 */
@RestController
@RequestMapping("/mbzs")
public class MbzsController {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    @Resource
    private MbzsService bzmbService;

    @RequestMapping("/getBZCD")
    public Map getBZCD(HttpServletRequest request){
        Map<String,String> paramMap= RequestUtils.getRequestParamMap(request);
        return bzmbService.getBZCD(paramMap);
    }

    @RequestMapping("/getBzTreelList")
    public List getBzTreelList(HttpServletRequest request){
        Map<String,String> paramMap= RequestUtils.getRequestParamMap(request);
        return bzmbService.getBzTreelList(paramMap);
    }

    @RequestMapping("/getBzMbxx")
    public Map getBzMbxx(HttpServletRequest request){
        Map<String,String> paramMap= RequestUtils.getRequestParamMap(request);
        return bzmbService.getBzMbxx(paramMap);
    }

    @RequestMapping("/getBzmbList")
    public List<Map>  getBzmbList(HttpServletRequest request){
        Map<String,String> paramMap= RequestUtils.getRequestParamMap(request);
        return bzmbService.getBzmbList(paramMap);
    }

    @RequestMapping("/getKsmbList")
    public List<Map>  getKsmbList(HttpServletRequest request){
        Map<String,String> paramMap= RequestUtils.getRequestParamMap(request);
        return bzmbService.getKsmbList(paramMap);
    }
    
    @RequestMapping("/getYymbList")
    public List<Map>  getYymbList(HttpServletRequest request){
        Map<String,String> paramMap= RequestUtils.getRequestParamMap(request);
        return bzmbService.getYymbList(paramMap);
    }

    @RequestMapping("syncMultiMbs")
    public Map syncMultiMbs(HttpServletRequest request){
        Map<String,String> paramMap= RequestUtils.getRequestParamMap(request);
        return bzmbService.syncMultiMbs(paramMap);
    }

}
