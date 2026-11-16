package cn.org.autumn.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import cn.org.autumn.utils.IPUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "IP地址")
public class IP implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(name = "IP地址")
    private String ip;

    @Schema(name = "国家")
    private String country;

    @Schema(name = "省份")
    private String region;

    @Schema(name = "城市")
    private String city;

    @Schema(name = "区县")
    private String district;

    @Schema(name = "运营商")
    private String isp;

    @Schema(name = "运营商")
    private String zip;

    @Schema(name = "区号")
    private String zone;

    @Schema(name = "网络类型")
    private String tag;

    public IP(String ip) {
        this.ip = ip;
    }

    public static List<String> getIps(HttpServletRequest request) {
        List<String> ips = new ArrayList<>();
        String ip = request.getHeader("remoteip");
        if (StringUtils.isNotEmpty(ip))
            ips.add(ip);
        ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.isEmpty(ip)) {
            ip = getIp(request);
            if (StringUtils.isNotEmpty(ip))
                ips.add(ip);
        } else {
            String[] ipa = ip.split(",");
            for (int index = 0; index < ipa.length; index++) {
                String strIp = ipa[index];
                if (!("unknown".equalsIgnoreCase(strIp))) {
                    ips.add(strIp);
                }
            }
        }
        return ips;
    }

    /**
     * 与 {@link IPUtils#getIp} 统一：多跳 XFF 取首个有效 hop 并 trim。
     */
    public static String getIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ip = IPUtils.getIp(request);
        return ip == null || ip.isEmpty() ? null : ip;
    }
}
