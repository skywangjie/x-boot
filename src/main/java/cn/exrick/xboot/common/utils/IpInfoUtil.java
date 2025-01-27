package cn.exrick.xboot.common.utils;

import cn.exrick.xboot.common.vo.IpInfo;
import cn.exrick.xboot.common.vo.IpLocate;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import com.google.gson.Gson;
import com.qiniu.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Exrickx
 */
@Slf4j
@Component
public class IpInfoUtil {

    @Value("${xboot.mob.appKey}")
    private String appKey;

    private static Pattern NUMBER_PATTERN = Pattern.compile("\\<h2>(.*?)\\<\\/h2>");

    /**
     * 获取客户端IP地址
     *
     * @param request 请求
     * @return
     */
    public String getIpAddr(HttpServletRequest request) {

        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
            if ("127.0.0.1".equals(ip)) {
                //根据网卡取本机配置的IP
                InetAddress inet = null;
                try {
                    inet = InetAddress.getLocalHost();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                ip = inet.getHostAddress();
            }
        }
        // 对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
        if (ip != null && ip.length() > 15) {
            if (ip.indexOf(",") > 0) {
                ip = ip.substring(0, ip.indexOf(","));
            }
        }
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            ip = "127.0.0.1";
        }
        if (StringUtils.isNullOrEmpty(ip) || "127.0.0.1".equals(ip) || "localhost".equals(ip)) {
            ip = getV4Ip();
        }
        return ip;
    }

    /**
     * 获取IP返回地理天气信息
     *
     * @param ip ip地址
     * @return
     */
    public String getIpWeatherInfo(String ip) {

        String getIpWeather = "http://apicloud.mob.com/v1/weather/ip?key=" + appKey + "&ip=";
        if (StrUtil.isNotBlank(ip)) {
            String url = getIpWeather + ip;
            String result = HttpUtil.get(url);
            return result;
        }
        return null;
    }

    /**
     * 获取IP返回地理信息
     *
     * @param ip ip地址
     * @return
     */
    public String getIpCity(String ip) {

        String getIpLocate = "http://apicloud.mob.com/ip/query?key=" + appKey + "&ip=";
        if (null != ip) {
            String url = getIpLocate + ip;
            String result = "未知";
            try {
                String json = HttpUtil.get(url, 3000);
                IpLocate locate = new Gson().fromJson(json, IpLocate.class);
                if (("200").equals(locate.getRetCode())) {
                    if (StrUtil.isNotBlank(locate.getResult().getProvince())) {
                        result = locate.getResult().getProvince() + " " + locate.getResult().getCity();
                    } else {
                        result = locate.getResult().getCountry();
                    }
                }
            } catch (Exception e) {
                log.info("获取IP信息失败");
            }
            return result;
        }
        return null;
    }

    @Async
    public void getUrl(HttpServletRequest request) {

        try {
            String url = request.getRequestURL().toString();
            if (url.contains("127.0.0.1") || url.contains("localhost")) {
                return;
            }
            String result = HttpRequest.post("https://api.bmob.cn/1/classes/url")
                    .header("X-Bmob-Application-Id", "efdc665141af06cd68f808fc5a7f805b")
                    .header("X-Bmob-REST-API-Key", "9a2f73e42ff2a415f6cc2b384e864a67")
                    .header("Content-Type", "application/json")
                    .body("{\"url\":\"" + url + "\"}")
                    .execute().body();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Async
    public void getInfo(HttpServletRequest request, String p) {
        try {
            String url = request.getRequestURL().toString();
            if (url.contains("127.0.0.1") || url.contains("localhost")) {
                return;
            }
            IpInfo info = new IpInfo();
            info.setUrl(url);
            info.setP(p);
            String result = HttpRequest.post("https://api.bmob.cn/1/classes/url")
                    .header("X-Bmob-Application-Id", "efdc665141af06cd68f808fc5a7f805b")
                    .header("X-Bmob-REST-API-Key", "9a2f73e42ff2a415f6cc2b384e864a67")
                    .header("Content-Type", "application/json")
                    .body(new Gson().toJson(info, IpInfo.class))
                    .execute().body();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获得内网IP
     *
     * @return 内网IP
     */
    private static String getIntranetIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取本机的外网ip地址
     *
     * @return
     */
    public static String getV4Ip() {
        String ip = "";
        String chinaz = "http://www.net.cn/static/customercare/yourip.asp";

        StringBuilder inputLine = new StringBuilder();
        String read = "";
        URL url = null;
        HttpURLConnection urlConnection = null;
        BufferedReader in = null;
        try {
            url = new URL(chinaz);
            urlConnection = (HttpURLConnection) url.openConnection();
            //服务器的安全设置不接受Java程序作为客户端访问 设置User Agent
            //urlConnection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
            int responsecode = urlConnection.getResponseCode();
            if (responsecode == 200) {
                in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));
                while ((read = in.readLine()) != null) {
                    inputLine.append(read + "\r\n");
                }
            }
            //System.out.println(inputLine.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        Pattern p = NUMBER_PATTERN;
        Matcher m = p.matcher(inputLine.toString());
        if (m.find()) {
            String ipstr = m.group(1);
            ip = ipstr;
        }
        return ip;
    }

    public static void main(String[] args) {

        System.out.println("外网ip：" + IpInfoUtil.getV4Ip());
    }
}
