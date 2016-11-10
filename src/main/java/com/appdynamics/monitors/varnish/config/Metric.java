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
