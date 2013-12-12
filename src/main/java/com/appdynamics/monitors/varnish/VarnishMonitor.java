package com.appdynamics.monitors.varnish;

import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;

public class VarnishMonitor extends AManagedMonitor {

    private static final String METRIC_PREFIX = "Custom Metrics|Varnish|";
    private static final Logger logger = Logger.getLogger(VarnishMonitor.class.getSimpleName());
    private HashSet<String> enabledMetrics = new HashSet<String>();
    private boolean isInitialized = false;

    public static void main(String[] args) throws Exception{
		Map<String, String> taskArguments = new HashMap<String, String>();
        taskArguments.put("host", "");
        taskArguments.put("port", "6085");
        taskArguments.put("username", "");
        taskArguments.put("password", "");
        taskArguments.put("enabled-metrics-path", "conf/EnabledMetrics.xml");

        VarnishMonitor varnishMonitor = new VarnishMonitor();
        varnishMonitor.execute(taskArguments, null);
	}
    public VarnishMonitor() {
        logger.setLevel(Level.INFO);
    }

    /**
     * Initializes the list of enabled metrics by reading the configuration file specified in monitor.xml
     * @param taskArguments
     * @throws Exception
     */
    private void initialize(Map<String, String> taskArguments) throws Exception{
        if (!isInitialized) {
            populateEnabledMetrics(taskArguments.get("enabled-metrics-path"));
            isInitialized = true;
            logger.info("Got list of enabled metrics from config file: " + taskArguments.get("enabled-metrics-path"));
        }
    }

    /**
     * Main execution method that uploads the metrics to the AppDynamics Controller
     * @see com.singularity.ee.agent.systemagent.api.ITask#execute(java.util.Map, com.singularity.ee.agent.systemagent.api.TaskExecutionContext)
     */
    public TaskOutput execute(Map<String, String> taskArguments, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {
        try {
            initialize(taskArguments);
            logger.info("Exceuting VarnishMonitor...");
            VarnishWrapper varnishWrapper = new VarnishWrapper(taskArguments);
            Map metrics = varnishWrapper.gatherMetrics();
            logger.info("Gathered metrics successfully. Size of metrics: " + metrics.size());
            printMetrics(metrics);
            logger.info("Printed metrics successfully");
            return new TaskOutput("Task successful...");
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception: ", e);
        }
        return new TaskOutput("Task failed with errors");
    }

    /**
     * Writes the Varnish metrics to the controller
     * @param 	metricsMap		HashMap containing all metrics for Varnish
     */
    private void printMetrics(Map metricsMap) throws Exception{
        Iterator metricIterator = metricsMap.keySet().iterator();
        while (metricIterator.hasNext()) {
            String metricName = metricIterator.next().toString();
            Long metricValue = (Long)metricsMap.get(metricName);
            if (enabledMetrics.contains(metricName)) {
                printMetric(METRIC_PREFIX + metricName, metricValue,
                        MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE,
                        MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
            }
        }
    }

    /**
     * Returns the metric to the AppDynamics Controller.
     * @param 	metricName		Name of the Metric
     * @param 	metricValue		Value of the Metric
     * @param 	aggregation		Average OR Observation OR Sum
     * @param 	timeRollup		Average OR Current OR Sum
     * @param 	cluster			Collective OR Individual
     */
    private void printMetric(String metricName, Long metricValue, String aggregation, String timeRollup, String cluster) throws Exception
    {
        MetricWriter metricWriter = super.getMetricWriter(metricName,
                aggregation,
                timeRollup,
                cluster
        );
        metricWriter.printMetric(String.valueOf(metricValue));
    }

    /**
     * Reads the config file in the conf/ directory and retrieves the list of enabled metrics
     * @param filePath          Path to the configuration file
     */
    private void populateEnabledMetrics(String filePath) throws Exception{
        BufferedInputStream configFile = null;

        try {
            configFile = new BufferedInputStream(new FileInputStream(filePath));
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(configFile);

            Element disabledMetricsElement = (Element)doc.getElementsByTagName("EnabledMetrics").item(0);
            NodeList disabledMetricList = disabledMetricsElement.getElementsByTagName("Metric");

            for (int i=0; i < disabledMetricList.getLength(); i++) {
                Node disabledMetric = disabledMetricList.item(i);
                enabledMetrics.add(disabledMetric.getAttributes().getNamedItem("name").getTextContent());
            }
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
            configFile.close();
        }
    }
}