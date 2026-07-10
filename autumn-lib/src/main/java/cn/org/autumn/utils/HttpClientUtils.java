package cn.org.autumn.utils;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

@Slf4j
public class HttpClientUtils {

    public static class HttpPostResult {
        private int statusCode;
        private String body;
        private String error;

        public int getStatusCode() {
            return statusCode;
        }

        public String getBody() {
            return body;
        }

        public String getError() {
            return error;
        }

        public boolean isSuccess() {
            return error == null && statusCode >= 200 && statusCode < 300;
        }
    }
    public  static String doGet(String url, Map<String, String> param, Map<String, String> header) {
        return doGet(url, param, header, 5000);
    }

    public  static String doGet(String url, Map<String, String> param, Map<String, String> header, int timeout) {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout)
                .setConnectionRequestTimeout(1000)
                .setSocketTimeout(timeout).build();
        return doGet(url, param, header, config);
    }

    public static String doGet(String url, Map<String, String> param, Map<String, String> header, RequestConfig config) {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        String resultString = "";
        CloseableHttpResponse response = null;
        try {
            URIBuilder builder = new URIBuilder(url);
            if (param != null) {
                for (String key : param.keySet()) {
                    builder.addParameter(key, param.get(key));
                }
            }
            URI uri = builder.build();
            HttpGet httpGet = new HttpGet(uri);
            if (null != header && header.size() > 0) {
                for (Map.Entry<String, String> kv : header.entrySet()) {
                    httpGet.setHeader(kv.getKey(), kv.getValue());
                }
            }
            if (null != config)
                httpGet.setConfig(config);
            response = httpclient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == 200) {
                resultString = EntityUtils.toString(response.getEntity(), "UTF-8");
            }
        } catch (Exception e) {
            log.debug(e.getMessage());
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
                httpclient.close();
            } catch (IOException e) {
                log.debug(e.getMessage());
            }
        }
        return resultString;
    }

    public static String doGet(String url, Map<String, String> param) {
        return doGet(url, param, null);
    }

    public static String doGet(String url, int timeout) {
        return doGet(url, null, null, timeout);
    }

    public static String doGet(String url) {
        return doGet(url, null);
    }

    public static String doPost(String url, Map<String, String> param, Map<String, String> header) {
        return doPost(url, param, header, 5000);
    }

    public static String doPost(String url, Map<String, String> param, Map<String, String> header, int timeout) {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout)
                .setConnectionRequestTimeout(1000)
                .setSocketTimeout(timeout).build();
        return doPost(url, param, header, config);
    }

    public static String doPost(String url, Map<String, String> param, Map<String, String> header, RequestConfig config) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        String resultString = "";
        try {
            HttpPost httpPost = new HttpPost(url);
            if (param != null) {
                List<NameValuePair> paramList = new ArrayList<>();
                for (String key : param.keySet()) {
                    paramList.add(new BasicNameValuePair(key, param.get(key)));
                }
                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(paramList);
                httpPost.setEntity(entity);
            }
            if (null != header && header.size() > 0) {
                for (Map.Entry<String, String> kv : header.entrySet()) {
                    httpPost.setHeader(kv.getKey(), kv.getValue());
                }
            }
            if (null != config)
                httpPost.setConfig(config);
            response = httpClient.execute(httpPost);
            resultString = EntityUtils.toString(response.getEntity(), "utf-8");
        } catch (Exception e) {
            log.debug(e.getMessage());
        } finally {
            try {
                if (null != response)
                    response.close();
            } catch (IOException e) {
                log.debug(e.getMessage());
            }
        }
        return resultString;
    }

    public static String doPost(String url, Map<String, String> param) {
        return doPost(url, param, null);
    }

    public static String doPost(String url) {
        return doPost(url, null);
    }

    public static String doPostJson(String url, String json) {
        return doPostJson(url, json, 5000);
    }

    public static String doPostJson(String url, String json, int timeout) {
        return doPostJson(url, json, null, timeout);
    }

    public static String doPostJson(String url, String json, Map<String, String> header, int timeout) {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout)
                .setConnectionRequestTimeout(1000)
                .setSocketTimeout(timeout).build();
        return doPostJson(url, json, header, config);
    }

    public static String doPostJson(String url, String json, RequestConfig config) {
        return doPostJson(url, json, null, config);
    }

    public static String doPostJson(String url, String json, Map<String, String> header, RequestConfig config) {
        return doPostJsonDetailed(url, json, header, config).getBody();
    }

    public static HttpPostResult doPostJsonDetailed(String url, String json, Map<String, String> header, int timeout) {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout)
                .setConnectionRequestTimeout(1000)
                .setSocketTimeout(timeout).build();
        return doPostJsonDetailed(url, json, header, config);
    }

    public static HttpPostResult doPostJsonDetailed(String url, String json, Map<String, String> header, RequestConfig config) {
        HttpPostResult result = new HttpPostResult();
        CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        try {
            HttpPost httpPost = new HttpPost(url);
            if (null != config) {
                httpPost.setConfig(config);
            }
            if (null != header && header.size() > 0) {
                for (Map.Entry<String, String> kv : header.entrySet()) {
                    httpPost.setHeader(kv.getKey(), kv.getValue());
                }
            }
            StringEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON);
            httpPost.setEntity(entity);
            response = httpClient.execute(httpPost);
            result.statusCode = response.getStatusLine().getStatusCode();
            if (response.getEntity() != null) {
                result.body = EntityUtils.toString(response.getEntity(), "utf-8");
            }
        } catch (Exception e) {
            result.error = e.getMessage();
            log.warn("HTTP POST JSON failed url={}: {}", url, e.getMessage());
        } finally {
            try {
                if (null != response) {
                    response.close();
                }
            } catch (IOException e) {
                log.debug(e.getMessage());
            }
        }
        return result;
    }
}
