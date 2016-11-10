/**
 * Copyright 2013 AppDynamics
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an AS IS BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.appdynamics.monitors.varnish;

import static com.appdynamics.extensions.yml.YmlReader.readFromFile;

import com.appdynamics.extensions.PathResolver;
import com.appdynamics.monitors.varnish.config.Configuration;
import com.appdynamics.monitors.varnish.config.Metric;
import com.appdynamics.monitors.varnish.config.Varnish;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class VarnishMonitor extends AManagedMonitor {

    private static final Logger logger = Logger.getLogger(VarnishMonitor.class);

    private static final String CONFIG_ARG = "config-file";

    private ExecutorService threadPool;

    /**
     * Cache to store previous metrics
     */
    private Cache<String, Long> previousMetricsMap;

    public VarnishMonitor() {
        String logMessage = String.format("Using Varnish Monitor Version [%s]",
                getImplementationVersion());
        logger.info(logMessage);
        System.out.println(logMessage);

        previousMetricsMap = CacheBuilder.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES).build();
    }

    private static String getImplementationVersion() {
        return VarnishMonitor.class.getPackage().getImplementationTitle();
    }

    private static String resolvePath(String filename) {
        if (StringUtils.isBlank(filename)) {
            return "";
        }

        //for absolute paths
        if (new File(filename).exists()) {
            return filename;
        }

        //for relative paths
        File jarPath = PathResolver.resolveDirectory(AManagedMonitor.class);
        String configFileName = String.format("%s%s%s", jarPath, File.separator, filename);
        return configFileName;
    }

    /**
     * Main execution method that uploads the metrics to the AppDynamics Controller
     *
     * @see com.singularity.ee.agent.systemagent.api.ITask#execute(java.util.Map, com.singularity.ee.agent.systemagent.api.TaskExecutionContext)
     */
    public TaskOutput execute(Map<String, String> args, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {

        logger.info(String.format("Using Varnish Monitor Version [%s]",
                getImplementationVersion()));

        try {
            String configFilename = resolvePath(args.get(CONFIG_ARG));

            Configuration config = readFromFile(configFilename, Configuration.class);

            logger.info("Executing VarnishMonitor...");

            List<Varnish> varnishList = config.getVarnish();

            threadPool = createThreadPool(config.getNumberOfVarnishThreads());

            for (Varnish varnish : varnishList) {
                Set<Metric> enabledMetrics = populateEnabledMetrics(varnish.getEnabledMetricsPath());
                VarnishWrapper task = new VarnishWrapper(this, varnish, enabledMetrics, config.getMetricPrefix());
                threadPool.submit(task);
            }

            logger.info("Executed varnish monitor successfully.");
            return new TaskOutput("Executed varnish monitor successfully.");
        } catch (Exception e) {
            logger.error("Exception: ", e);
        } finally {
            if (threadPool != null && !threadPool.isShutdown()) {
                threadPool.shutdown();
            }
        }
        return new TaskOutput("Task failed with errors");
    }

    /**
     * Reads the config file in the conf/ directory and retrieves the list of enabled metrics
     *
     * @param filePath Path to the configuration file
     */
    private Set<Metric> populateEnabledMetrics(String filePath) throws Exception {

        filePath = resolvePath(filePath);

        Set<Metric> enabledMetrics = new HashSet<Metric>();
        BufferedInputStream configFile = null;

        try {
            configFile = new BufferedInputStream(new FileInputStream(filePath));
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(configFile);

            Element enabledMetricsElement = (Element) doc.getElementsByTagName("EnabledMetrics").item(0);
            NodeList enabledMetricList = enabledMetricsElement.getElementsByTagName("Metric");

            for (int i = 0; i < enabledMetricList.getLength(); i++) {
                Node enabledMetric = enabledMetricList.item(i);
                Metric metric = new Metric();
                NamedNodeMap attributes = enabledMetric.getAttributes();

                String name = attributes.getNamedItem("name").getTextContent();
                metric.setName(name);

                Node collectDeltaNode = attributes.getNamedItem("collectDelta");
                if (collectDeltaNode != null) {
                    Boolean collectDelta = Boolean.valueOf(collectDeltaNode.getTextContent());
                    metric.setCollectDelta(collectDelta);
                }

                Node displayNameNode = attributes.getNamedItem("displayName");
                if (displayNameNode != null) {
                    metric.setDisplayName(displayNameNode.getTextContent());
                }

                enabledMetrics.add(metric);
            }
            return enabledMetrics;
        } catch (FileNotFoundException e) {
            logger.error("Config file not found");
            throw e;
        } catch (ParserConfigurationException e) {
            logger.error("Failed to instantiate new DocumentBuilder");
            throw e;
        } catch (SAXException e) {
            logger.error("Error parsing the config file");
            throw e;
        } catch (DOMException e) {
            logger.error("Could not parse metric name");
            throw e;
        } finally {
            if (configFile != null) {
                configFile.close();
            }
        }
    }

    private ExecutorService createThreadPool(int numOfThreads) {
        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("Varnish-Task-Thread-%d")
                .build();
        return Executors.newFixedThreadPool(numOfThreads,
                threadFactory);
    }

    /**
     * Returns metric value from cache
     *
     * @param metricName
     * @return
     */
    public Long getDeltaMetric(String metricName) {
        return previousMetricsMap.getIfPresent(metricName);
    }

    /**
     * Adds metric to cache
     *
     * @param name
     * @param metricValue
     */
    public void addDeltaMetric(String name, Long metricValue) {
        previousMetricsMap.put(name, metricValue);
    }

    /**
     * Removes metric from the cache
     *
     * @param metricName
     */
    public void deleteDeltaMetric(String metricName) {
        if (previousMetricsMap.size() > 0) {
            previousMetricsMap.invalidate(metricName);
        }

    }
}