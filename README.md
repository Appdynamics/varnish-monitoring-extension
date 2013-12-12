Varnish Monitoring Extension
============================

This extension works only with the standalone machine agent.

## Use Case

Varnish Cache is a web application accelerator also known as a caching HTTP reverse proxy. You install it in front of any server that speaks HTTP and configure it to cache the contents. The varnish-monitoring-extension gathers cache metrics exposed through Varnish's REST API and sends them to the AppDynamics Metric Browser.

## Installation
<ol>
	<li>Type 'ant package' in the command line from the varnish-monitoring-extension directory.
	</li>
	<li>Deploy the file VarnishMonitor.zip found in the 'dist' directory into the &lt;machineagent install dir&gt;/monitors/ directory.
	</li>
	<li>Unzip the deployed file.
	</li>
	<li>Open &lt;machineagent install dir&gt;/monitors/VarnishMonitor/monitor.xml and configure the Varnish parameters.
<p></p>
<pre>
	 &lt;argument name="host" is-required="false" default-value="localhost"/&gt;
     &lt;argument name="port" is-required="false" default-value="6085"/&gt;
     &lt;argument name="username" is-required="false" default-value="username"/&gt;
     &lt;argument name="password" is-required="false" default-value="password"/&gt;
     &lt;argument name="disabled-metrics-path" is-required="false" default-value="monitors/VarnishMonitor/conf/EnabledMetrics.xml"/&gt;
</pre>
	</li>
	<li>Open &lt;machineagent install dir&gt;/monitors/VarnishMonitor/conf/EnabledMetrics.xml and configure the list of enabled metrics. Here is a sample configuration of the enabled metrics:
<p></p>
<pre>
	 &lt;Metric name="mem_free"/&gt;
	 &lt;Metric name="mem_total"/&gt;
</pre>
	</li>	
	<li> Restart the machine agent.
	</li>
	<li>In the AppDynamics Metric Browser, look for: Application Infrastructure Performance | &lt;Tier&gt; | Custom Metrics | Varnish
	</li>
</ol>

## Directory Structure

| Directory/File | Description |
|----------------|-------------|
|conf            | Contains the monitor.xml, EnabledMetrics.xml |
|lib             | Contains third-party project references |
|src             | Contains source code of the Varnish monitoring extension |
|dist            | Only obtained when using ant. Run 'ant build' to get binaries. Run 'ant package' to get the distributable .zip file |
|build.xml       | Ant build script to package the project (required only if changing Java code) |

## Metrics

Refer to the metric table in this wiki page: https://singularity.jira.com/wiki/display/ACE/Varnish+Monitoring+Extension+Design+Specs

## Custom Dashboard

![](https://raw.github.com/Appdynamics/varnish-monitoring-extension/master/Varnish%20Dashboard.png?token=2880440__eyJzY29wZSI6IlJhd0Jsb2I6QXBwZHluYW1pY3MvYm91bmRhcnktbW9uaXRvcmluZy1leHRlbnNpb24vbWFzdGVyL0JvdW5kYXJ5IERhc2hib2FyZC5wbmciLCJleHBpcmVzIjoxMzg2ODAyNTAyfQ%3D%3D--dfa89e2e3461bbd8ef09d5db317bb02c430da0b9)

##Contributing

Always feel free to fork and contribute any changes directly via [GitHub](https://github.com/Appdynamics/varnish-monitoring-extension).

##Community

Find out more in the [AppSphere](http://appsphere.appdynamics.com/t5/eXchange/Varnish-Monitoring-Extension/idi-p/4851) community.

##Support

For any questions or feature request, please contact [AppDynamics Center of Excellence](mailto:ace-request@appdynamics.com).
