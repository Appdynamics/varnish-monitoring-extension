Varnish Monitoring Extension
============================

This extension works only with the standalone machine agent.

## Use Case

Varnish Cache is a web application accelerator also known as a caching HTTP reverse proxy. You install it in front of any server that speaks HTTP and configure it to cache the contents. The varnish-monitoring-extension gathers cache metrics exposed through Varnish's REST API and sends them to the AppDynamics Metric Browser.

## Prerequisites

This extension requires vagent2 to be installed. Please follow the steps in  https://github.com/varnish /vagent2/blob/master/INSTALL.rst to install vagent2. You can also install vagent2 from the attached binary file.


## Installation
<ol>
	<li>Type 'mvn clean install' in the command line from the varnish-monitoring-extension directory.
	</li>
	<li>Deploy the file VarnishMonitor.zip found in the 'dist' directory into the &lt;machineagent install dir&gt;/monitors/ directory.
	</li>
	<li>Unzip the deployed file.
	</li>
	<li>Configure details in &lt;machineagent install dir&gt;/monitors/VarnishMonitor/config.yml </li>
	<li>Configure metrics to collect in &lt;machineagent install dir&gt;/monitors/VarnishMonitor/EnabledMetrics.xml </li>
	<li> Restart the machine agent.
	</li>
	<li>In the AppDynamics Metric Browser, look for: Application Infrastructure Performance | &lt;Tier&gt; | Custom Metrics | Varnish
	</li>
</ol>

## Configuration

Note : Please make sure to not use tab (\t) while editing yaml files. You may want to validate the yaml file using a [yaml validator](http://yamllint.com/)

1. Configure the Varnish Vagent2 details by editing the config.yml file in `<MACHINE_AGENT_HOME>/monitors/VarnishMonitor/`.
2. Below is the default config.yml
   
   ```
varnish:
  - name: "LocalVarnish"
    host: "localhost"
    port: 6085
    scheme: "http"
    username: "admin"
    # Provide either password or passwordEncrypted and encryptionKey
    password: "admin"
    passwordEncrypted: ""
    encryptionKey: ""
    enabledMetricsPath: "monitors/VarnishMonitor/EnabledMetrics.xml"
    proxyConfig:
      uri:
      username:
      password:
  - name: "RemoteVarnish"
    host: "localhost"
    port: 6085
    scheme: "http"
    username: "admin"
    password: "admin"
    passwordEncrypted: ""
    encryptionKey: ""
    enabledMetricsPath: "monitors/VarnishMonitor/EnabledMetrics.xml"
    proxyConfig:
      uri:
      username:
      password:

# Number of concurrent threads for
# dealing with multiple Varnish instances
# Note: You don't necessarily have to match the no of threads
# to the no of Varnish instances configured above
# unless you have a lot of CPUs in your machine
numberOfVarnishThreads: 2


#This will create this metric in all the tiers, under this path
metricPrefix: "Custom Metrics|Varnish|"

#This will create it in specific Tier/Component. Make sure to replace <COMPONENT_ID> with the appropriate one from your environment.
#To find the <COMPONENT_ID> in your environment, please follow the screenshot https://docs.appdynamics.com/display/PRO42/Build+a+Monitoring+Extension+Using+Java
#metricPrefix: Server|Component:<COMPONENT_ID>|Custom Metrics|Varnish 
```

3. Default EnabledMetrics.xml

```
<!-- Only metrics present here will be shown in the Controller. Remaining metrics will be ignored -->
<EnabledMetrics>
    <!-- <Metric name="timestamp"/> -->
    <Metric name="MAIN.uptime" displayName="Uptime"/>
    <Metric name="MAIN.pools" collectDelta="true" displayName="No Of Pools"/>
    <Metric name="MAIN.threads" collectDelta="true"/>
    <Metric name="MAIN.threads_created" collectDelta="true"/>
    <Metric name="MAIN.n_backend"/>
    <Metric name="MAIN.shm_records"/>
    <Metric name="MAIN.shm_writes"/>
    <Metric name="MAIN.n_vcl"/>
    <Metric name="MAIN.n_vcl_avail"/>
    <Metric name="MAIN.bans"/>
    <Metric name="MAIN.bans_completed"/>
    <Metric name="MAIN.bans_added"/>
    <Metric name="MAIN.bans_persisted_bytes"/>
    <Metric name="MAIN.vsm_free"/>
    <Metric name="MAIN.vsm_used"/>
    <Metric name="MGT.uptime"/>
    <Metric name="MGT.child_start"/>
    <Metric name="MEMPOOL.busyobj.pool"/>
    <Metric name="MEMPOOL.busyobj.sz_wanted"/>
    <Metric name="MEMPOOL.busyobj.sz_actual"/>
    <Metric name="MEMPOOL.req0.pool"/>
    <Metric name="MEMPOOL.req0.sz_wanted"/>
    <Metric name="MEMPOOL.req0.sz_actual"/>

</EnabledMetrics>

```


## Password Encryption Support

To avoid setting the clear text password in the config.yaml, please follow the process below to encrypt the password

1. Download the util jar to encrypt the password from [https://github.com/Appdynamics/maven-repo/blob/master/releases/com/appdynamics/appd-exts-commons/1.1.2/appd-exts-commons-1.1.2.jar](https://github.com/Appdynamics/maven-repo/blob/master/releases/com/appdynamics/appd-exts-commons/1.1.2/appd-exts-commons-1.1.2.jar) and navigate to the downloaded directory
2. Encrypt password from the commandline
`java -cp appd-exts-commons-1.1.2.jar com.appdynamics.extensions.crypto.Encryptor encryptionKey myPassword`
3. Specify the passwordEncrypted and encryptionKey in config.yaml

## Metrics

NOTE: Only metrics configured in the EnabledMetrics.xml file will be collected. Please add the metrics you want to collect to this file.

Here are some of the varnish metrics available through vagent2.

| Metric Name | Description |
|----------------|-------------|
|client_conn				| Client connections accepted|
|client_drop				| Connection dropped, no sess/wrk|
|client_req				| Client requests received|
|cache_hit				|Cache hits|
|cache_hitpass				|Cache hits for pass|
|cache_miss				|Cache misses|
|backend_conn				|Backend conn. success|
|backend_unhealthy				|Backend conn. not attempted|
|backend_busy				|Backend conn. too many|
|backend_fail				|Backend conn. failures|
|backend_reuse				|Backend conn. reuses|
|backend_toolate				|Backend conn. was closed|
|backend_recycle				|Backend conn. recycles|
|backend_retry				|Backend conn. retry|
|n_wrk				|N worker threads|
|n_wrk_create				|N worker threads created|
|n_wrk_failed				|N worker threads not created|
|n_wrk_max				|N worker threads limited|
|n_wrk_lqueue				|work request queue length|
|n_wrk_queued				|N queued work requests|
|n_wrk_drop				|N dropped work requests|
|n_backend				|N backends|
|n_expired				|N expired objects|
|s_sess				|Total Sessions|
|s_req				|Total Requests|
|uptime				|Client uptime|
|dir_dns_lookups				|DNS director lookups|
|dir_dns_failed				|DNS director failed lookups|
|dir_dns_hit				|DNS director cached lookups hit|
|dir_dns_cache_full				|DNS director full dnscache|
|timestamp				|Timestamp in milliseconds|
|fetch_head				|Fetch head|
|fetch_length				|Fetch with Length|
|fetch_chunked				|Fetch chunked|
|fetch_eof				|Fetch EOF|
|fetch_bad				|Fetch had bad headers|
|fetch_close				|Fetch wanted close|
|fetch_oldhttp				|Fetch pre HTTP/1.1 closed|
|fetch_zero				|Fetch zero len|
|fetch_failed				|Fetch failed|
|fetch_1xx				|Fetch no body (1xx)|
|fetch_204				|Fetch no body (204)|
|fetch_304				|Fetch no body (304)|
|n_sess_mem				|N struct sess_mem|
|n_sess				|N struct sess|
|n_object				|N struct object|
|n_vampireobject				|N unresurrected objects|
|n_objectcore				|N struct objectcore|
|n_objecthead				|N struct objecthead|
|n_waitinglist				|N struct waitinglist|
|n_vbc				|N struct vbc|
|n_lru_nuked				|N LRU nuked objects|
|n_lru_moved				|N LRU moved objects|
|losthdr				|HTTP header overflows|
|n_objsendfile				|Objects sent with sendfile|
|n_objwrite				|Objects sent with write|
|n_objoverflow				|Objects overflowing workspace|
|s_pipe				|Total pipe|
|s_pass				|Total pass|
|s_fetch				|Total fetch|
|s_hdrbytes				|Total header bytes|
|s_bodybytes				|Total body bytes|
|sess_closed				|Session Closed|
|sess_pipeline				|Session Pipeline|
|sess_readahead				|Session Read Ahead|
|sess_linger				|Session Linger|
|sess_herd				|Session herd|
|shm_records				|SHM records|
|shm_writes				|SHM writes|
|shm_flushes				|SHM flushes due to overflow|
|shm_cont				|SHM MTX contention|
|shm_cycles				|SHM cycles through buffer|
|sms_nreq				|SMS allocator requests|
|sms_nobj				|SMS outstanding allocations|
|sms_nbytes				|SMS outstanding bytes|
|sms_balloc				|SMS bytes allocated|
|sms_bfree				|SMS bytes freed|
|backend_req				|Backend requests made|
|n_vcl				|N vcl total|
|n_vcl_avail				|N vcl available|
|n_vcl_discard				|N vcl discarded|
|n_ban				|N total active bans|
|n_ban_gone				|N total gone bans|
|n_ban_add				|N new bans added|
|n_ban_retire				|N old bans deleted|
|n_ban_obj_test				|N objects tested|
|n_ban_re_test				|N regexps tested against|
|n_ban_dups				|N duplicate bans removed|
|hcb_nolock				|HCB Lookups without lock|
|hcb_lock				|HCB Lookups with lock|
|hcb_insert				|HCB Inserts|
|esi_errors				|ESI parse errors (unlock)|
|esi_warnings				|ESI parse warnings (unlock)|
|accept_fail				|Accept failures|
|client_drop_late				|Connection dropped late|
|vmods				|Loaded VMODs|
|n_gzip				|Gzip operations|
|n_gunzip				|Gunzip operations|
|sess_pipe_overflow				|Dropped sessions due to session pipe overflow|
|LCK.sms.creat				|Created locks|
|LCK.sms.destroy				|Destroyed locks|
|LCK.sms.locks				|Lock Operations|
|LCK.sms.colls				|Collisions|
|LCK.smp.creat				|Created locks|
|LCK.smp.destroy				|Destroyed locks|
|LCK.smp.locks				|Lock Operations|
|LCK.smp.colls				|Collisions|
|LCK.sma.creat				|Created locks|
|LCK.sma.destroy				|Destroyed locks|
|LCK.sma.locks				|Lock Operations|
|LCK.sma.colls				|Collisions|
|LCK.smf.creat				|Created locks|
|LCK.smf.destroy				|Destroyed locks|
|LCK.smf.locks				|Lock Operations|
|LCK.smf.colls				|Collisions|
|LCK.hsl.creat				|Created locks|
|LCK.hsl.destroy				|Destroyed locks|
|LCK.hsl.locks				|Lock Operations|
|LCK.hsl.colls				|Collisions|
|LCK.hcb.creat				|Created locks|
|LCK.hcb.destroy				|Destroyed locks|
|LCK.hcb.locks				|Lock Operations|
|LCK.hcb.colls				|Collisions|
|LCK.hcl.creat				|Created locks|
|LCK.hcl.destroy				|Destroyed locks|
|LCK.hcl.locks				|Lock Operations|
|LCK.hcl.colls				|Collisions|
|LCK.vcl.creat				|Created locks|
|LCK.vcl.destroy				|Destroyed locks|
|LCK.vcl.locks				|Lock Operations|
|LCK.vcl.colls				|Collisions|
|LCK.stat.creat				|Created locks|
|LCK.stat.destroy				|Destroyed locks|
|LCK.stat.locks				|Lock Operations|
|LCK.stat.colls				|Collisions|
|LCK.sessmem.creat				|Created locks|
|LCK.sessmem.destroy				|Destroyed locks|
|LCK.sessmem.locks				|Lock Operations|
|LCK.sessmem.colls				|Collisions|
|LCK.wstat.creat				|Created locks|
|LCK.wstat.destroy				|Destroyed locks|
|LCK.wstat.locks				|Lock Operations|
|LCK.wstat.colls				|Collisions|
|LCK.herder.creat			|Created locks|
|LCK.herder.destroy				|Destroyed locks|
|LCK.herder.locks				|Lock Operations|
|LCK.herder.colls				|Collisions|
|LCK.wq.creat				|Created locks|
|LCK.wq.destroy				|Destroyed locks|
|LCK.wq.locks				|Lock Operations|
|LCK.wq.colls				|Collisions|
|LCK.objhdr.creat				|Created locks|
|LCK.objhdr.destroy				|Destroyed locks|
|LCK.objhdr.locks				|Lock Operations|
|LCK.objhdr.colls				|Collisions|
|LCK.exp.creat				|Created locks|
|LCK.exp.destroy				|Destroyed locks|
|LCK.exp.locks				|Lock Operations|
|LCK.exp.colls				|Collisions|
|LCK.lru.creat				|Created locks|
|LCK.lru.destroy				|Destroyed locks|
|LCK.lru.locks				|Lock Operations|
|LCK.lru.colls				|Collisions|
|LCK.cli.creat				|Created locks|
|LCK.cli.destroy				|Destroyed locks|
|LCK.cli.locks				|Lock Operations|
|LCK.cli.colls				|Collisions|
|LCK.ban.creat				|Created locks|
|LCK.ban.destroy				|Destroyed locks|
|LCK.ban.locks				|Lock Operations|
|LCK.ban.colls				|Collisions|
|LCK.vbp.creat				|Created locks|
|LCK.vbp.destroy				|Destroyed locks|
|LCK.vbp.locks				|Lock Operations|
|LCK.vbp.colls				|Collisions|
|LCK.vbe.creat				|Created locks|
|LCK.vbe.destroy				|Destroyed locks|
|LCK.vbe.locks				|Lock Operations|
|LCK.vbe.colls				|Collisions|
|LCK.backend.creat				|Created locks|
|LCK.backend.destroy				|Destroyed locks|
|LCK.backend.locks				|Lock Operations|
|LCK.backend.colls				|Collisions|
|SMA.s0.c_req				|Allocator requests|
|SMA.s0.c_fail				|Allocator failures|
|SMA.s0.c_bytes				|Bytes allocated|
|SMA.s0.c_freed				|Bytes freed|
|SMA.s0.g_alloc				|Allocations outstanding|
|SMA.s0.g_bytes				|Bytes outstanding|
|SMA.s0.g_space				|Bytes available|
|SMA.Transient.c_req				|Allocator requests|
|SMA.Transient.c_fail				|Allocator failures|
|SMA.Transient.c_bytes				|Bytes allocated|
|SMA.Transient.c_freed				|Bytes freed|
|SMA.Transient.g_alloc				|Allocations outstanding|
|SMA.Transient.g_bytes				|Bytes outstanding|
|SMA.Transient.g_space				|Bytes available|
|VBE.default.vcls				|VCL references|
|VBE.default.happy				|Happy health probes|



## Custom Dashboard

![](https://raw.github.com/Appdynamics/varnish-monitoring-extension/master/VarnishDashboard.png)

##Contributing

Always feel free to fork and contribute any changes directly here on GitHub.

##Community

Find out more in the [AppSphere](http://appsphere.appdynamics.com/t5/eXchange/Varnish-Monitoring-Extension/idi-p/5617) community.

##Support

For any questions or feature request, please contact [AppDynamics Center of Excellence](mailto:help@appdynamics.com).
