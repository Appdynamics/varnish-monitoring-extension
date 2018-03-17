/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */
package com.appdynamics.monitors.varnish;

import com.appdynamics.TaskInputArgs;
import com.appdynamics.extensions.crypto.CryptoUtil;
import com.appdynamics.extensions.http.Http4ClientBuilder;
import com.appdynamics.monitors.varnish.config.Metric;
import com.appdynamics.monitors.varnish.config.ProxyConfig;
import com.appdynamics.monitors.varnish.config.Varnish;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.commons.lang.StringUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VarnishWrapper implements Runnable {

    private static final Logger logger = Logger.getLogger(VarnishWrapper.class.getSimpleName());
    private static final String METRIC_SEPARATOR = "|";

    private Varnish varnish;
    private Set<Metric> enabledMetrics;
    private VarnishMonitor monitor;
    private String metricPrefix;

    private CloseableHttpClient httpClient;
    private HttpClientContext httpContext;

    public VarnishWrapper(VarnishMonitor monitor, Varnish varnish, Set<Metric> enabledMetrics, String metricPrefix) throws TaskExecutionException {
        this.varnish = varnish;
        this.enabledMetrics = enabledMetrics;
        this.monitor = monitor;
        this.metricPrefix = metricPrefix;

        createHTTPClient();
    }

    private void createHTTPClient() throws TaskExecutionException {

        Map map = new HashMap();
        List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        map.put("servers", list);
        HashMap<String, String> server = new HashMap<String, String>();
        server.put("uri", varnish.getScheme() + "://" + varnish.getHost());
        server.put("username", varnish.getUsername());
        server.put("password", getPassword());
        list.add(server);

        ProxyConfig proxyConfig = varnish.getProxyConfig();
        if (proxyConfig != null) {
            HashMap<String, String> proxyProps = new HashMap<String, String>();
            map.put("proxy", proxyProps);
            proxyProps.put("uri", proxyConfig.getUri());
            proxyProps.put("username", proxyConfig.getUsername());
            proxyProps.put("password", proxyConfig.getPassword());
        }

        //Workaround to ignore the certificate mismatch issue.
        SSLContext sslContext = null;
        try {
            sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                    return true;
                }
            }).build();
        } catch (NoSuchAlgorithmException e) {
            logger.error("Unable to create SSL context", e);
            throw new TaskExecutionException("Unable to create SSL context", e);
        } catch (KeyManagementException e) {
            logger.error("Unable to create SSL context", e);
            throw new TaskExecutionException("Unable to create SSL context", e);
        } catch (KeyStoreException e) {
            logger.error("Unable to create SSL context", e);
            throw new TaskExecutionException("Unable to create SSL context", e);
        }
        HostnameVerifier hostnameVerifier = SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;

        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, (X509HostnameVerifier) hostnameVerifier);
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", sslSocketFactory)
                .build();

        PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager(socketFactoryRegistry);

        HttpClientBuilder builder = Http4ClientBuilder.getBuilder(map);
        builder.setConnectionManager(connMgr);

        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(varnish.getUsername(), getPassword()));

        httpClient = builder.setSSLSocketFactory(sslSocketFactory).setDefaultCredentialsProvider(credentialsProvider).build();

        httpContext = HttpClientContext.create();
        httpContext.setCredentialsProvider(credentialsProvider);

    }

    private String getPassword() {
        String password = null;

        if (StringUtils.isNotBlank(varnish.getPassword())) {
            password = varnish.getPassword();

        } else {
            try {
                Map<String, String> args = Maps.newHashMap();
                args.put(TaskInputArgs.PASSWORD_ENCRYPTED, varnish.getPasswordEncrypted());
                args.put(TaskInputArgs.ENCRYPTION_KEY, varnish.getEncryptionKey());
                password = CryptoUtil.getPassword(args);

            } catch (IllegalArgumentException e) {
                String msg = "Encryption Key not specified. Please set the value in config.yaml.";
                logger.error(msg);
                throw new IllegalArgumentException(msg);
            }
        }
        return password;
    }

    @Override
    public void run() {
        try {
            gatherAndPrintMetrics();
        } catch (TaskExecutionException e) {
            e.printStackTrace();
        }
    }

    /**
     * Uses Varnish's REST API to retrieve JSON response containing Varnish metrics and then converts the response into a map of metrics
     */
    private void gatherAndPrintMetrics() throws TaskExecutionException {
        if ("".equals(varnish.getHost()) || varnish.getPort() == null || "".equals(varnish.getUsername()) || "".equals(varnish.getPassword())) {
            throw new TaskExecutionException("Either host, port, username, and/or password are empty");
        }

        String response = getResponse();
        if (response != null) {
            JsonObject responseData = new JsonParser().parse(response.toString()).getAsJsonObject();
            Map<String, Long> metrics = constructMetricsMap(responseData);
            printMetrics(metrics);
        } else {
            logger.info("Received empty response from varnish");
        }
    }

    private String getResponse() {
        String metricsURL = constructVarnishStatsURL();
        HttpGet httpGet = new HttpGet(metricsURL);
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(httpGet, httpContext);

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                logger.debug("Successfully executed [" + httpGet.getMethod() + "] on [" + httpGet.getURI() + "]");

                return EntityUtils.toString(response.getEntity());

            }
            logger.info("Received error response [" + statusCode + "] while executing [" + httpGet.getMethod() + "] on [" + httpGet.getURI() + "]");
            if (logger.isDebugEnabled()) {
                logger.debug("Response received [ " + EntityUtils.toString(response.getEntity()) + " ]");
            }

        } catch (Exception e) {
            logger.error("Error executing [" + httpGet.getMethod() + "] on [" + httpGet.getURI() + "]", e);
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    logger.error("Error while closing the response", e);
                }
            }

            if (httpClient != null) {

                try {
                    httpClient.close();
                } catch (IOException e) {
                    logger.error("Error while closing the connection", e);
                }

            }
        }
        return null;
    }

    /**
     * Constructs the metrics hashmap by iterating over the JSON response retrieved from the /stats url
     *
     * @param responseData the JSON response retrieved from hitting the /stats url
     * @return HashMap containing all metrics for Varnish
     */
    private HashMap<String, Long> constructMetricsMap(JsonObject responseData) {
        HashMap<String, Long> metrics = new HashMap<String, Long>();
        Iterator iterator = responseData.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, JsonElement> entry = (Map.Entry<String, JsonElement>) iterator.next();
            if (!entry.getValue().isJsonPrimitive()) {
                String metricName = entry.getKey();
                JsonElement value = entry.getValue();
                if (value instanceof JsonObject) {
                    JsonObject metricObject = value.getAsJsonObject();
                    Long metricValue = metricObject.get("value").getAsLong();
                    metrics.put(metricName, metricValue);
                }
            }
        }
        return metrics;
    }

    /**
     * Writes the Varnish metrics to the controller
     *
     * @param metricsMap HashMap containing all metrics for Varnish
     */
    private void printMetrics(Map<String, Long> metricsMap) {
        Iterator metricIterator = metricsMap.keySet().iterator();
        while (metricIterator.hasNext()) {
            String metricName = metricIterator.next().toString();
            Long metricValue = metricsMap.get(metricName);
            Metric metric = getMetric(metricName);
            if (metric != null) {

                String metricDisplayName = metric.getDisplayName();

                if (metricDisplayName.contains(",")) {
                    metricDisplayName = metricDisplayName.replace(',', '_');
                }
                printMetric(metricPrefix + varnish.getName() + METRIC_SEPARATOR + metricDisplayName, metricValue,
                        MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE,
                        MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);

                if (metric.isCollectDelta()) {
                    Long prevValue = monitor.getDeltaMetric(varnish.getName() + METRIC_SEPARATOR + metric.getName());
                    if (prevValue != null) {
                        printMetric(metricPrefix + varnish.getName() + METRIC_SEPARATOR + metricDisplayName + " Delta", metricValue - prevValue,
                                MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE,
                                MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                                MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
                    }

                    monitor.addDeltaMetric(varnish.getName() + METRIC_SEPARATOR + metric.getName(), metricValue);
                } else {
                    monitor.deleteDeltaMetric(varnish.getName() + METRIC_SEPARATOR + metric.getName());
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Ignoring metric " + metricName + " as it is not configured in EnabledMetrics.xml");
                }
            }
        }
    }

    private Metric getMetric(String metricName) {

        for (Metric metric : enabledMetrics) {
            if (metric.getName().equals(metricName)) {
                return metric;
            }
        }
        return null;
    }

    /**
     * Returns the metric to the AppDynamics Controller.
     *
     * @param metricName  Name of the Metric
     * @param metricValue Value of the Metric
     * @param aggregation Average OR Observation OR Sum
     * @param timeRollup  Average OR Current OR Sum
     * @param cluster     Collective OR Individual
     */
    private void printMetric(String metricName, Long metricValue, String aggregation, String timeRollup, String cluster) {
        MetricWriter metricWriter = monitor.getMetricWriter(metricName,
                aggregation,
                timeRollup,
                cluster
        );
        metricWriter.printMetric(String.valueOf(metricValue));

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Sending metric = %s = %s", metricName, metricValue));
        }
    }

    /**
     * Constructs the varnish statistics url, which returns JSON for the metrics
     *
     * @return Varnish statistics url
     */
    private String constructVarnishStatsURL() {
        return new StringBuilder()
                .append("http://")
                .append(varnish.getHost())
                .append(":")
                .append(varnish.getPort())
                .append("/stats")
                .toString();
    }
}