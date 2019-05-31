package winning.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import winning.bean.NemrMblb;
import winning.bean.NemrMbxx;

import java.util.List;
import java.util.Map;

/**
 * Created by 高阳 on 2019/4/2.
 */
@Mapper
public interface MbzsDao {

    List<Map> getBZCD(@Param("jgdm")String jgdm, @Param("flmc")String ywmc, @Param("qx") int moz);
    List<Map> getKSCD(@Param("jgdm") String jgdm, @Param("ksmc") String ywmc);
    List<Map> getBzTreelList(Map param);
    Map getBzMbxx(@Param("jgdm")String jgdm, @Param("mbid")String mbid);
    List<Map> getBzmbList(@Param("jgdm")String jgdm, @Param("mbid")String mbid);
    List<Map> getKsmbList(@Param("ksdm")String ksdm);
    List<Map> getKsTreelList(@Param("ksdm")String ksdm);
    List getKsTree(@Param("ksdm")String fldm);


    List<Map> getMBCD( @Param("jgmc") String ywmc, @Param("jglb") String jglb);
    int getTotalMB(@Param("jgdm") String jgdm);
    List<Map> getMbsc(Map<String, String> paramMap);
    List<Map> getYYTreelList(@Param("jgdm") String jgdm);
    List<Map> getYymbList(@Param("jgdm")String jgdm);

    List<NemrMblb> getMbList(@Param("jgdm")String jgdm, @Param("mbid")String mbid);

    List<NemrMbxx> getMbxxList(@Param("jgdm")String jgdm, @Param("mbid")String mbid);
}
