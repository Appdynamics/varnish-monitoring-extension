package com.appdynamics.monitors.varnish;

import com.google.gson.*;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
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
    private String filePath;

    public VarnishWrapper(Map<String, String> taskArguments) {
        this.host = taskArguments.get("host");
        this.port = taskArguments.get("port");
        this.username = taskArguments.get("username");
        this.password = taskArguments.get("password");
        this.filePath = taskArguments.get("filePath");
    }

    public Map gatherMetrics() throws Exception{
        try {
            JsonObject responseData = getResponseData();
            return new HashMap();
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

    private String constructVarnishStatsURL() {
        return new StringBuilder()
                .append("https://")
                .append(host)
                .append(":")
                .append(port)
                .append("/stats")
                .toString();
    }
}

