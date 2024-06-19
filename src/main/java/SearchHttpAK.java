/**
 * 默认：
 */

import com.alibaba.fastjson.JSONException;
import org.json.JSONObject;
import org.springframework.web.util.UriUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class SearchHttpAK {

    public static String URL = "https://api.map.baidu.com/geocoding/v3?";

    public static String AK = "G5V5quMKVXrZqcB6dhIDfSCnuQkTlSuJ";

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please enter the file path of \"D:RE.xlsx\" or type 'exit' to quit");
        String filePath = scanner.nextLine();

        if(filePath.equalsIgnoreCase("exit")) {
            System.out.println("Exiting the program...");
            System.exit(0);
        }

        File f1 = new File(filePath);
        List<String> cur = new ArrayList<>();
        try {
            ExcelResolveUtils utils = new ExcelResolveUtils();
            cur = utils.readExcel(f1);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        SearchHttpAK snCal = new SearchHttpAK();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("output", "json");
        params.put("ak", AK);
        params.put("callback", "showLocation");


        final int MAX_REQUESTS_PER_SECOND = 30;
        long lastRequestTime = System.currentTimeMillis();
        int requestsThisSecond = 0;
        int count = 0;

        for (int i = 0; i < cur.size(); i++) {
            try {
                long currentTime = System.currentTimeMillis();

                if (currentTime - lastRequestTime < 1000) {
                    requestsThisSecond++;
                    if (requestsThisSecond > MAX_REQUESTS_PER_SECOND) {
                        TimeUnit.MILLISECONDS.sleep(1000 - (currentTime - lastRequestTime));
                        lastRequestTime = System.currentTimeMillis();
                        requestsThisSecond = 0;
                    }
                } else {
                    lastRequestTime = currentTime;
                    requestsThisSecond = 0;
                }

                params.put("address",cur.get(i));
                String getAK = snCal.requestGetAK(URL, params);
                int startIndex = getAK.indexOf("{");
                int endIndex = getAK.lastIndexOf("}");
                String jsonStr = getAK.substring(startIndex, endIndex+1);

                JSONObject jsonObject = new JSONObject(jsonStr);
                int status = jsonObject.getInt("status");
                if (status == 0) {
                    String location = jsonObject.getJSONObject("result").getJSONObject("location").toString();
                    System.out.println(location);
                    // System.out.println(count++);
                }else if(status == 401){
                    cur.add(cur.get(i));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        String exitCommand = scanner.nextLine();
        if (exitCommand.equalsIgnoreCase("exit")) {
            System.out.println("Exiting the program...");
            System.exit(0);
        }
    }

    /**
     * 默认ak
     * 选择了ak，使用IP白名单校验：
     * 根据您选择的AK已为您生成调用代码
     * 检测到您当前的ak设置了IP白名单校验
     * 您的IP白名单中的IP非公网IP，请设置为公网IP，否则将请求失败
     * 请在IP地址为xxxxxxx的计算发起请求，否则将请求失败
     */
    public String requestGetAK(String strUrl, Map<String, String> param) throws Exception {
        if (strUrl == null || strUrl.length() <= 0 || param == null || param.size() <= 0) {
            return "";
        }

        StringBuffer queryString = new StringBuffer();
        queryString.append(strUrl);
        for (Map.Entry<?, ?> pair : param.entrySet()) {
            queryString.append(pair.getKey() + "=");
            //    第一种方式使用的 jdk 自带的转码方式  第二种方式使用的 spring 的转码方法 两种均可
            //    queryString.append(URLEncoder.encode((String) pair.getValue(), "UTF-8").replace("+", "%20") + "&");
            queryString.append(UriUtils.encode((String) pair.getValue(), "UTF-8") + "&");
        }

        if (queryString.length() > 0) {
            queryString.deleteCharAt(queryString.length() - 1);
        }

        java.net.URL url = new URL(queryString.toString());
        // System.out.println(queryString.toString());
        URLConnection httpConnection = (HttpURLConnection) url.openConnection();
        httpConnection.connect();

        InputStreamReader isr = new InputStreamReader(httpConnection.getInputStream());
        BufferedReader reader = new BufferedReader(isr);
        StringBuffer buffer = new StringBuffer();
        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }
        reader.close();
        isr.close();

        // System.out.println("AK: " + buffer.toString());
        return buffer.toString();
    }

}