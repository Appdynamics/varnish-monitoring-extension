/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.monitors.varnish.config;

import java.util.List;

/**
 * @author Satish Muddam
 */
public class Configuration {

    private List<Varnish> varnish;
    private int numberOfVarnishThreads;
    private String metricPrefix;

    public List<Varnish> getVarnish() {
        return varnish;
    }

    public void setVarnish(List<Varnish> varnish) {
        this.varnish = varnish;
    }

    public int getNumberOfVarnishThreads() {
        return numberOfVarnishThreads;
    }

    public void setNumberOfVarnishThreads(int numberOfVarnishThreads) {
        this.numberOfVarnishThreads = numberOfVarnishThreads;
    }

    public String getMetricPrefix() {
        return metricPrefix;
    }

    public void setMetricPrefix(String metricPrefix) {
        this.metricPrefix = metricPrefix;
    }
}
