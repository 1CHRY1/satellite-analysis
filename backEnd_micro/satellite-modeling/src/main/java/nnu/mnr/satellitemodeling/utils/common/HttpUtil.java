package nnu.mnr.satellitemodeling.utils.common;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/19 16:24
 * @Description:
 */

@Slf4j
public class HttpUtil {

    public static String doGet(String url, JSONObject body) {
        try {
            StringBuilder queryStringBuilder = new StringBuilder();
            boolean isFirst = true;
            for (String key : body.keySet()) {
                if (!isFirst) {
                    queryStringBuilder.append("&");
                } else {
                    isFirst = false;
                }
                queryStringBuilder.append(key)
                        .append("=")
                        .append(body.getString(key));
            }
            String query = queryStringBuilder.toString();
            URL obj = new URL(url + (query.isEmpty() ? "" : "?") + query);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            connection.setRequestMethod("GET");

            // 设置超时时间为10s
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            int responseCode = connection.getResponseCode();
            StringBuilder response = new StringBuilder();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
            } else {
                log.info("Get request failed with response code " + responseCode);
            }
            connection.disconnect();
            return response.toString();

        } catch (Exception e) {
            if (e instanceof java.net.SocketTimeoutException) {
                return "Connection timed out: " + e.getMessage();
            } else {
                return "Error during GET request: " + e.getMessage();
            }
        }
    }

    public static String doPost(String url, JSONObject body) {
        try {
            URL obj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            // 设置超时时间为5s
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);

            // Send post request
            try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
                wr.write(body.toString().getBytes(StandardCharsets.UTF_8));
                wr.flush();
            }

            int responseCode = connection.getResponseCode();
            StringBuilder response = new StringBuilder();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                }
            } else {
                log.error("POST request failed with response code " + responseCode);
            }

            connection.disconnect();
            return response.toString();

        } catch (Exception e) {
            if (e instanceof java.net.SocketTimeoutException) {
                return "Connection timed out: " + e.getMessage();
            } else {
                return "Error during POST request: " + e.getMessage();
            }
        }
    }

}
