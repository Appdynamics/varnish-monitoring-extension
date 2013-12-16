package com.appdynamics.monitors.varnish;

import com.google.gson.*;
import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.*;

public class VarnishWrapper {

    private static final Logger logger = Logger.getLogger(VarnishWrapper.class.getSimpleName());
    private String host;
    private String port;
    private String username;
    private String password;

    public VarnishWrapper(Map<String, String> taskArguments) {
        this.host = taskArguments.get("host");
        this.port = taskArguments.get("port");
        this.username = taskArguments.get("username");
        this.password = taskArguments.get("password");
    }

    /**
     * Uses Varnish's REST API to retrieve JSON response containing Varnish metrics and then converts the response into a map of metrics
     * @return  Map containing metrics for Varnish
     * @throws  Exception
     */
    public Map gatherMetrics() throws Exception{
        if (host.equals("") || port.equals("") || username.equals("") || password.equals("")) {
            throw new Exception("Either host, port, username, or password is configured incorrectly");
        }
        try {
            JsonObject responseData = getResponseData();
            HashMap metrics = constructMetricsMap(responseData);
            return metrics;
        } catch(MalformedURLException e) {
            logger.error("Invalid URL used to connect to Boundary");
            throw e;
        } catch(JsonSyntaxException e) {
            logger.error("Error parsing the Json response");
            throw e;
        } catch(IOException e) {
            throw e;
        }
    }

    /**
     * Gets the JsonObject by parsing the JSON return from hitting the /stats url
     * @return  JsonObject containing the response from hitting the /stats url for Varnish
     * @throws  Exception
     */
    private JsonObject getResponseData() throws Exception{
        String metricsURL = constructVarnishStatsURL();
        HttpGet httpGet = new HttpGet(metricsURL);
        httpGet.addHeader(BasicScheme.authenticate(
                new UsernamePasswordCredentials(username, password),
                "UTF-8", false));

        HttpClient httpClient = new DefaultHttpClient();
        HttpResponse response = httpClient.execute(httpGet);
        HttpEntity entity = response.getEntity();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(entity.getContent()));
        StringBuilder responseString = new StringBuilder();
        String line = "";
        while ((line = bufferedReader.readLine()) != null) {
            responseString.append(line);
        }
        JsonObject responseData = new JsonParser().parse(responseString.toString()).getAsJsonObject();
        return responseData;
    }

    /**
     * Constructs the metrics hashmap by iterating over the JSON response retrieved from the /stats url
     * @param   responseData the JSON response retrieved from hitting the /stats url
     * @return  HashMap containing all metrics for Varnish
     * @throws  Exception
     */
    private HashMap<String, Long> constructMetricsMap(JsonObject responseData) throws Exception{
        HashMap<String, Long> metrics = new HashMap<String, Long>();
        Iterator iterator = responseData.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, JsonElement> entry = (Map.Entry<String, JsonElement>) iterator.next();
            if (!entry.getValue().isJsonPrimitive()) {
                String metricName = entry.getKey();
                JsonObject metricObject = entry.getValue().getAsJsonObject();
                Long metricValue = metricObject.get("value").getAsLong();
                metrics.put(metricName, metricValue);
            }
        }
        return metrics;
    }

    /**
     * Constructs the varnish statistics url, which returns JSON for the metrics
     * @return Varnish statistics url
     */
    private String constructVarnishStatsURL() {
        return new StringBuilder()
                .append("http://")
                .append(host)
                .append(":")
                .append(port)
                .append("/stats")
                .toString();
    }
}

