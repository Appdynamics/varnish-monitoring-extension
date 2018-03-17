/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.monitors.varnish.config;

/**
 * @author Satish Muddam
 */
public class Metric {

    private String name;
    private boolean collectDelta;
    private String displayName;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isCollectDelta() {
        return collectDelta;
    }

    public void setCollectDelta(boolean collectDelta) {
        this.collectDelta = collectDelta;
    }

    public String getDisplayName() {

        if (displayName == null) {
            return name;
        }
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
