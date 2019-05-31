package winning.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.web.multipart.MultipartFile;

import winning.bean.BL_MBZDY;
import winning.bean.NemrMblb;
import winning.bean.NemrMbxx;
import winning.common.utils.FileUtils;
import winning.common.utils.JsonUtils;
import winning.dao.MbscDao;

import javax.annotation.Resource;

import javax.transaction.Transactional;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by 高阳 on 2019/4/2.
 */
@Service
public class MbscService {
    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Resource
    private MbscDao mbscDao;
    @Resource
    private CommonService commonService;
    @Value("${uploadPath}")
    private String uploadPath;

    @Transactional
    public Map getMblist(Map<String, String> paramMap) {
        Map map=new LinkedCaseInsensitiveMap();
        String mbmc="";
        if(!("").equals(paramMap.get("MBMC"))&&paramMap.get("MBMC")!=null){
            mbmc="%"+paramMap.get("MBMC")+"%";
        }
        List<Map> list= mbscDao.getMblist(paramMap.get("JGDM"),mbmc,paramMap.get("LCZT"));
        map.put("Mblist",list);
        return map;
    }

    @Transactional
    public Map addYwj(Map<String, String> paramMap) {
        Map map=new LinkedCaseInsensitiveMap();
        BL_MBZDY mbzdy=JsonUtils.jsonStrToBean(paramMap.get("BL_MBZDY"), BL_MBZDY.class);
        String path=uploadPath+mbscDao.getJgmc(mbzdy.getJgdm())+"\\"
        +mbscDao.getFlmc(mbzdy.getJgdm(),mbzdy.getMbid().substring(0,5))+"\\"
        +mbscDao.getFlmc(mbzdy.getJgdm(),mbzdy.getMbid().substring(0,9))+"\\";
        File file= FileUtils.getLocalFile(path,mbzdy.getMbid());
        if(file!=null){
            if (file.isFile()) {
                file.delete();
            }
        }
        int a=mbscDao.updateGlzt(mbzdy.getJgdm(),mbzdy.getMbid());
        if(a>0){
            FileUtils.UploadToPath(path,new String(mbzdy.getMbxx()),mbzdy.getMbid()+"-"+mbzdy.getMbmc());
            map.put("IsExist","关联成功");
        }
        return map;
    }
    @Transactional
    public Map getMbqx(Map<String, String> paras) {
        Map map=new LinkedCaseInsensitiveMap();
        List<Map> CityInfo =mbscDao.getMbqx(paras.get("JGDM"), String.valueOf('0'));
		if(CityInfo.size()>0){
			for(int u=0;u<CityInfo.size();u++){
				List<Map> QYInfo =mbscDao.getMbqx(paras.get("JGDM"),(String) CityInfo.get(u).get("value"));
				List<Map> childrenlist=new ArrayList<>();
                if(QYInfo.size()>0){
                	for(int i=0;i<QYInfo.size();i++){
						Map children=new LinkedCaseInsensitiveMap();
						children.put("label",QYInfo.get(i).get("label"));
						children.put("value",QYInfo.get(i).get("value"));
						childrenlist.add(children);
					}
				}
				CityInfo.get(u).put("children",childrenlist);
			}
		}
        map.put("mbdata", CityInfo);
        return map;
    }
    @Transactional
    public Map updateMbxx(Map<String, String> paramMap) {
        Map map=new LinkedCaseInsensitiveMap();
        NemrMblb nemrMblb= JsonUtils.jsonStrToBean(paramMap.get("BL_MBLB"),NemrMblb.class);
        NemrMbxx nemrMbxx= JsonUtils.jsonStrToBean(paramMap.get("BL_MBZD"),NemrMbxx.class);
        mbscDao.getBlMblb(nemrMblb).getFldm();
        String mbid = nemrMblb.getMbid();
        if(!mbscDao.getBlMblb(nemrMblb).getFldm().equals(nemrMblb.getFldm())){
            mbid=commonService.getBaseCode (paramMap.get("JGDM"), "BL_MBLB", paramMap.get("FLDM"), "4", "");
        }
        if(nemrMbxx.getMbxx()==null){
//            nemrMbxx.setMbxx(mbscDao.getBlMbzd(nemrMbxx).getMbxx());
        }else{
            String path=uploadPath+mbscDao.getJgmc(paramMap.get("JGDM"))+"\\"
                    +mbscDao.getFlmc(paramMap.get("JGDM"),mbid.substring(0,5))+"\\"
                    +mbscDao.getFlmc(paramMap.get("JGDM"),mbid.substring(0,9))+"\\";
            File file= FileUtils.getLocalFile(path,mbid);
            if (file.isFile()) {
                file.delete();
            }
            FileUtils.UploadToPath(path,new String(paramMap.get("YMBXX")),mbid+"-"+paramMap.get("YMBMC"));
        }
        nemrMbxx.setXgsj(getNowTime());
        if(mbid!=null&&!("").equals(mbid)){
            nemrMblb.setMbid(mbid);
            nemrMbxx.setMbid(mbid);
            int a=mbscDao.updateMblb(nemrMblb);
            int b=mbscDao.updateMbzd(nemrMbxx);
            if(a>0&&b>0){
                map.put("IsExist","修改成功");
            }else{
                map.put("IsExist","修改失败");
            }
        }
        return map;
    }
    @Transactional
    public Map addMbxx(Map<String, String> paramMap) {
        Map map=new LinkedCaseInsensitiveMap();
        NemrMbxx nemrMbxx= JsonUtils.jsonStrToBean(paramMap.get("BL_MBZD"),NemrMbxx.class);
        NemrMblb nemrMblb= JsonUtils.jsonStrToBean(paramMap.get("BL_MBLB"),NemrMblb.class);
        String mbid = commonService.getBaseCode (nemrMblb.getJgdm(), "BL_MBLB", nemrMblb.getFldm(), "4", "");
        nemrMbxx.setCjsj(getNowTime());
        if(mbid!=null&&!("").equals(mbid)){
            nemrMblb.setMbid(mbid);
            nemrMbxx.setMbid(mbid);
            int a=mbscDao.addMblb(nemrMblb);
            if(a>0){
                nemrMbxx.setPid(nemrMblb.getId());
                int b=mbscDao.addMbzd(nemrMbxx);
                if(b>0){
                    map.put("IsExist","新增成功");
                }else{
                    map.put("IsExist","新增失败");
                }
            }
        }
        return map;
    }
    @Transactional
    public Map deletembxx(Map<String, String> paramMap) {
        Map map=new LinkedCaseInsensitiveMap();
        int a=mbscDao.delMblb(paramMap.get("PID"));
        int b=mbscDao.delMbzd(paramMap.get("ID"));
//        int c=mbscDao.delMbzdy(paramMap.get("ID"));
        String path=uploadPath+mbscDao.getJgmc(paramMap.get("JGDM"))+"\\"
                +mbscDao.getFlmc(paramMap.get("JGDM"),paramMap.get("MBID").substring(0,5))+"\\"
                +mbscDao.getFlmc(paramMap.get("JGDM"),paramMap.get("MBID").substring(0,9))+"\\";
        File file= FileUtils.getLocalFile(path,paramMap.get("MBID"));
        if(file!=null){
            if (file.isFile()) {
                file.delete();
            }
        }
        if(a>0&b>0){
            map.put("IsExist","删除成功");
        }else{
            map.put("IsExist","删除失败");
        }
        return map;
    }
    @Transactional
    public Map getUpdateMbxx(Map<String, String> paramMap) {
        Map map=new LinkedCaseInsensitiveMap();
        String mbxx=mbscDao.getUpdateMbxx(paramMap.get("ID"));
        map.put("mbxx",mbxx);
        return map;
    }
    /**
     * file文件流转换为base64码
     *
     * @return
     */
    public Map getFileToBase64(MultipartFile file){
        Map map=new LinkedCaseInsensitiveMap();
        try {
            byte[] files = file.getBytes();
            map.put("base64NR",files);
        } catch (IOException e) {
            e.printStackTrace();
        }
             return map;
    }
    /**
     * 获取当前系统时间
     *
     * @return
     */
    private String getNowTime(){
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
        return df.format(new Date());
    }
  //获取科室列表
    @Transactional
    public Map getDeptList(Map<String, String> paramMap) {
        Map map=new LinkedCaseInsensitiveMap();
        List<Map> list= mbscDao.getDeptList(paramMap.get("JGDM"));
        map.put("KSList",list);
        return map;
    }
    //上传模板信息
    @Transactional
    public Map scMbxx(Map<String, String> paramMap) {
        Map map=new LinkedCaseInsensitiveMap();
        int a=mbscDao.updateLczt(paramMap.get("JGDM"),paramMap.get("MBID"));
        if(a>0){
            mbscDao.deleteCause(paramMap.get("JGDM"),paramMap.get("MBID"),paramMap.get("LCZT"));
            map.put("IsExist","上传成功");
        }else{
            map.put("IsExist","上传失败");
        }
        return map;
    }
    //修改模板名称
    @Transactional
    public Map editeMBMC(Map<String, String> paramMap) {
        Map map=new LinkedCaseInsensitiveMap();
        if(!(paramMap.get("MBMC").equals(mbscDao.getMBMC(paramMap.get("JGDM"),paramMap.get("MBID"))))){
            mbscDao.editeLBMBMC(paramMap.get("JGDM"),paramMap.get("MBID"),paramMap.get("MBMC"));
            mbscDao.editeZDMBMC(paramMap.get("JGDM"),paramMap.get("MBID"),paramMap.get("MBMC"));
        }
        return map;
    }
    //修改模板分类
    @Transactional
    public Map editeMBFL(Map<String, String> paramMap) {
        Map map=new LinkedCaseInsensitiveMap();
        if(!(paramMap.get("FLDM").equals(mbscDao.getFLDM(paramMap.get("JGDM"),paramMap.get("MBID"))))){
            String mbid = commonService.getBaseCode (paramMap.get("JGDM"), "BL_MBLB", paramMap.get("FLDM"), "4", "");
            mbscDao.editeLBMBFL(paramMap.get("JGDM"),paramMap.get("MBID"),mbid,paramMap.get("FLDM"));
            mbscDao.editeZDMBFL(paramMap.get("JGDM"),paramMap.get("MBID"),mbid);
        }
        return map;
    }
    //修改模板所属科室
    @Transactional
    public Map editeKSXX(Map<String, String> paramMap) {
        Map map=new LinkedCaseInsensitiveMap();
        if(!(paramMap.get("KSDM").equals(mbscDao.getKSDM(paramMap.get("JGDM"),paramMap.get("MBID"))))){
            mbscDao.editeLBKSXX(paramMap.get("JGDM"),paramMap.get("MBID"),paramMap.get("KSDM"),mbscDao.getKSMC(paramMap.get("KSDM"),paramMap.get("JGDM")));
        }
        return map;
    }

}
