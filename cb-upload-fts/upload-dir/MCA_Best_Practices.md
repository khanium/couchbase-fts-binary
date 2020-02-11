# Multi-Cluster Awareness - Guide/Best Practices

## Overview
The Multi-Cluster Awareness SDK is designed to enable client applications to detect certain conditions in order to make a determination that a Couchbase cluster is unstable and to automatically switch to interacting with another cluster. This is a non-trivial problem, and should be approached with care and planning. 

This version of the Couchbase SDK replaces the initial bootstrapping objects with a new set of objects that are configured with two or more clusters, along with a set of conditions that will be used to determine when the application should switch from one cluster to the next. Determining the set of conditions to be used for switching clusters is the area of using the MCA SDK where the most time and effort will likely be spent, and is a combination that is likely to be refined over time, as the team responsible develops a better understanding of what combination of events should be used as a trigger a switch in their particular use case.

This guide is intended as an extension of the MCA SDK documentation. It is not a replacement. As such, it will focus on how to approach the problem, and how to resolve particular issues that may arise. This guide will not provide an exhaustive guide to the SDK, method parameters or configuration options. For those, along with instructions for downloading and integrating the MCA SDK into your project, please refer to the SDK documentation.

- Java SDK Documentation [https://subscription.couchbase.com/multi-cluster-java/](https://subscription.couchbase.com/multi-cluster-java/)
- .NET SDK Documentation [https://subscription.couchbase.com/multi-cluster-dotnet/](https://subscription.couchbase.com/multi-cluster-dotnet/)

{% hint style="info" %}
**Note:** The MCA SDK documentation is not currently publicly available. You'll need to use the set of credentials issued to your organization by Couchbase technical staff to access either of the above sets of documentation. If your organization has not been issued a set of credentials, contact your account manager to arrange it.
{% endhint %}

## Planning Considerations

Couchbase clusters automatically handle many failure scenarios without interruption to any applications connected to them. So when planning an MCA implementation, you'll need to differentiate between failure scenarios that Couchbase can handle without need of switching to another cluster, and those that do. For instance, a single node failing can easily be handled by the Couchbase cluster, with a minimal impact on any client applications. The failure of networking equipment that would render the Couchbase cluster unreachable would more likely fall into the category of requiring a switch to another cluster.

If the applications are hosted in the same facility as the primary Couchbase cluster, another factor is introduced that must be included in the planning considerations. Would the failure of networking equipment that made the Couchbase cluster unreachable from the application server also make the entire data center unstable? If so, then that failure scenario might be more easily solved by redirecting all user traffic to another application server in a second data center.

Another factor to consider is any additional latency that your applications may incur by switching to different Couchbase cluster? If the application is switching from a local cluster to a remote cluster, what effect does the additional network latency on your application SLAs?

Assuming that you've come up with scenarios where the Couchbase cluster is either down, unstable, or unreachable, yet your application servers are still up, running, and reachable by users, now the question becomes what combination of events does your application need to encounter to determine it is time to switch to the next cluster? Is it a specific exception that might be detected by the Couchbase SDK, or any exception? Do you trigger on a single exception, or a certain number of exceptions within a time window?

Once you've come up with the failure scenarios that would cause your application to switch to the next cluster in the list, what should happen if the application gets to the last cluster in the list and the combination of events are triggered by that cluster? What should your application do? Stay with that cluster? Go back to the top of the list of clusters? Reverse course and traverse the list in reverse?

### Cluster Requirements

Because use of the MCA SDK implies that you have multiple Couchbase clusters up and running with the same data, you should have some form of cross-data-center replication (XDCR) running to keep the clusters in sync. Assuming that data updates are being originated in both clusters, you need to configure the buckets with Timestamp-based conflict resolution and have all nodes configured to have their clocks in sync using Network Time Protocol (NTP). 

Configuring NTP - [https://docs.couchbase.com/server/current/install/synchronize-clocks-using-ntp.html](https://docs.couchbase.com/server/current/install/synchronize-clocks-using-ntp.html)

Conflict Resolution - [https://docs.couchbase.com/server/current/learn/clusters-and-availability/xdcr-conflict-resolution.html](https://docs.couchbase.com/server/current/learn/clusters-and-availability/xdcr-conflict-resolution.html)

## Classes/Objects

The MCA SDK consists of only four categories of classes, most of which are configured and instantiated during the application Couchbase bootstrap process. Once the initial connection is made, the application will interact with the MultiClusterBucket object, which provides most of the same interface as the Bucket object in the standard Couchbase SDK, and needs to be handled in the same manner. The categories of objects are:

- Coordinators - determines which cluster should be active, and when to switch to the next cluster.
- Failure Detectors - watches for failure conditions, and signals to the coordinator when the configured combination of errors has been detected.
- Multi-Cluster Client - replaces the cluster and bucket objects from the standard SDK.
- Topology Administration - provides the ability to specify which cluster should be the currently active cluster.

### Coordinators

The coordinators are the objects that are responsible for maintaining the topology of clusters that the application is connecting to. In that regard, it is using the priorities and failure conditions of each cluster to determine which cluster should be active. The coordinator takes the initial cluster specs provided, and uses the provided priorities to determine the order of the clusters in the list, making the cluster with the highest priority the active cluster. It then waits until it receives a signal from the failure detector that the active cluster has met the failure conditions, and switches the active cluster to the next highest priority one in the list.

#### Isolated Coordinator

The IsolatedCoordinator is named such because it operates in isolation from other instances of the MCA SDK in use in your facility. What this means is that if you have a cluster of application servers all running the same application, connected to the same Couchbase cluster, each will make an independent determination when to switch to the next cluster in the list.

#### Coordinator Interface

If your situation requires coordinator functionality that can't be found in the provided coordinator, you have the option of building your own. The Coordinator Interface is provided for this possibility.

{% hint style="info" %}
**Warning:** The option of building your own coordinator using the interface falls in the category of "just because you can, doesn't mean you should." Building your own coordinator is something that should be approached carefully, and only after you have determined that the Couchbase-provided coordinator does not meet the needs of your situation. We recommend that you have conversations with Couchbase Engineers prior to taking on the task of building your own. If you have come to the conclusion that you need to build your own, please contact Couchbase Professional Services and we'll be happy to coordinate a call with the appropriate Couchbase Engineers to make sure you have all the information and support you need to be successful in your approach.
{% endhint %}

### Failure Detectors

The Failure Detectors monitor the interaction between the SDK and the Couchbase cluster, watching for error conditions that indicate there might be trouble with the cluster. In essence, these are acting as pass-through filters for the network connections between the SDK and the Couchbase cluster, watching all the network traffic that passes through. When it sees an error that it's interested in, it makes a note of that error, and starts the clock while watching for more. If the timer expires before the requisite number of errors are encountered, the timer is reset, and the error count is zeroed-out. But if more errors are encountered, but not enough to trigger the switch, the timer start time is reset to the next error encountered, such that it's a moving window until either the specified number of errors occur within the timer window, or the time expires with no more errors having been observed.

#### Traffic Monitoring Detector

The TrafficMonitoringFailureDetector watches the network traffic for exceptions such as timeouts, unreachable nodes, or request cancellations. These could all be symptoms of a cluster that has become unreliable or unreachable. You have control over which Couchbase services it is monitoring, along with how many exceptions within what period of time to allow before signaling to the coordinator it's time to switch clusters.

#### Node Health Failure Detector (Java Only)

The NodeHealthFailureDetector is only available in the Java MCA SDK. This is due to differences between the standard Java and .NET Couchbase SDKs. When using this failure detector, be aware that it may not detect a node failure if that node is running a single service, and the application has not had any interaction with the node. For instance, if you have a couple of nodes running just the FTS service, and one of those nodes fails, this detector may not detect the failure if the application hasn't performed any FTS searches. In general, if you are needing to monitor nodes that are running services other than the K/V Data service, you are probably better off using the TrafficMonitoringFailureDetector.

{% hint style="info" %}
**Note:** When using the NodeHealthFailureDetector, it is important to make sure the settings for the check interval and rebalance out grace period are adjusted to accommodate the characteristics of your cluster to prevent false-positive failure signals due to a rebalance being performed.
{% endhint %}

#### Composing Failure Detectors

There are two composing failure detectors, a ConjunctionFailureDetector and a DisjunctionFailureDetector. These operate as logical AND and OR operators for combining other failure detectors. The ConjunctionFailureDetector works like a logical AND operator, only signaling when all configured detectors are signaling that a failure has occurred. The DisjunctionFailureDetector works like a logical OR operator, allowing you to configure multiple failure detectors and to trigger a switch when any one of them is signaling a failure.

#### Detector Interface

If your situation requires detector functionality that can't be found in the provided coordinators, you have the option of building your own. The Detector Interface is provided for this possibility.

{% hint style="info" %}
**Warning:** The option of building your own detector using the interface falls in the category of "just because you can, doesn't mean you should." Building your own detector is something that should be approached carefully, and only after you have determined that the Couchbase-provided detectors do not meet the needs of your situation. We recommend that you have conversations with Couchbase Engineers prior to taking on the task of building your own. If you have come to the conclusion that you need to build your own, please contact Couchbase Professional Services and we'll be happy to coordinate a call with the appropriate Couchbase Engineers to make sure you have all the information and support you need to be successful in your approach.
{% endhint %}

### Multi-Cluster Client

The Multi-Cluster Client is the entry point for the standard Couchbase SDK. Once the coordinator and failure detectors have all been initialized and configured, the MCA client is instantiated, taking both the coordinator and detector as initialization parameters. Once the MCA client has been instantiated, it can be used to instantiate a MCA Bucket object, which is then used in the same fashion as the Bucket object in the regular Couchbase SDK.

#### MCA Client

The MultiClusterClient object is the object that plugs everything together and sets up the cluster connections. Once you've instantiated it, you can set the user credentials to be used when connecting to the cluster, and then you can open a connection to a specific bucket on the cluster by using the openBucket method, which returns a MultiClusterBucket object.

#### MCA Bucket

The MultiClusterBucket object is basically the same as the Bucket object in the regular Couchbase SDK. As such, it needs to be treated the same way, by using it in a Singleton pattern, with only one instance that is used from your application bootstrap to shutdown.

The MultiClusterBucket object provides mostly the same interface as the standard Bucket object with a couple of differences. The primary difference is that the methods no longer have a default timeout, each call needs to provide a timeout period.

The .NET version of the MultiClusterBucket object only implements asynchronous methods, while the Java version implements both the synchronous and asynchronous methods.

### Topology Administration

The Topology Administration objects are available for administrative access to the cluster topology, and to be able to make a controlled switch from one cluster to another.

#### Simple Topology Administrator

The SimpleTopologyAdmin object is available for building functionality into your application to specify the currently active cluster. Once initialized with the coordinator, it has methods to activate, deactivate, or fail clusters in the topology. This enables you to build functionality into your application to switch back to the original cluster once the failure conditions have been resolved.

#### Administrative Access

If you need to have administrative control over which cluster your application is connected to, and don't want to build it into your application, you can take advantage of the administrative access objects available in the two MCA SDKs. 

In the Java SDK, the JmxTopologyAdmin object provides administrative access via the Java Management Extensions, accessible via any JMX client/console. 

In the .NET SDK, the WebApiTopologyAdmin object provides a REST API via Microsoft's WebAPI functionality through IIS.

## Basic Configuration

The following is a sample bootstrap configuration for use with the Java MCA SDK. In it, there are two cluster specs created, each pointing to a different cluster. These are used in the creation of the coordinator object, which determines which cluster to be interacting with. A failure dectector is created, passing in the options to be used when detecting problems with the cluster. Finally, the coordinator and the detector are used to create a multi-cluster client, from which a multi-cluster bucket object can be instantiated.

```java
import com.couchbase.client.mc.ClusterSpec;
import com.couchbase.client.mc.MultiClusterBucket;
import com.couchbase.client.mc.MultiClusterClient;
import com.couchbase.client.mc.coordination.Coordinator;
import com.couchbase.client.mc.coordination.Coordinators;
import com.couchbase.client.mc.coordination.IsolatedCoordinator;
import com.couchbase.client.mc.detection.FailureDetectorFactory;
import com.couchbase.client.mc.detection.FailureDetectors;
import com.couchbase.client.mc.detection.NodeHealthFailureDetector;
 
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
 
public class NodeHealthExample {
 
    public static void main(String... args) throws Exception {
        Set<String> c1h = new HashSet<String>();
        c1h.add("node1");
        Set<String> c2h = new HashSet<String>();
        c2h.add("node2");
 
        ClusterSpec cluster1 = ClusterSpec.create(c1h, "c1", 2);
        ClusterSpec cluster2 = ClusterSpec.create(c2h, "c2", 1);
 
        Coordinator coordinator = Coordinators.isolated(new IsolatedCoordinator.Options()
            .clusterSpecs(Arrays.asList(cluster1, cluster2))
            .failoverNumNodes(2)
            .gracePeriod(1000L)
        );
        FailureDetectorFactory detector = FailureDetectors
	    .nodeHealth(coordinator, NodeHealthFailureDetector.options());
 
 
        MultiClusterClient client = new MultiClusterClient(coordinator, detector);
        client.authenticate("myUser", "myPassword");
        MultiClusterBucket bucket = client.openBucket("myBucket");
    }
}

```

{% hint style="info" %}
**Note:** The above example is using the NodeHealthFailureDecector, which is monitoring the health of nodes running the K/V data service. This detector is only available in the Java MCA SDK. It is not available in the .NET MCA SDK.
{% endhint %}

{% hint style="info" %}
**Note:** The above example each cluster spec is provided one node to try and bootstrap the cluster connection. In a production situation, you would normally provide multiple nodes to each cluster spec for bootstrapping the connection.
{% endhint %}

## Detecting Node and Cluster Failures

It is the failure detectors that detect and signal failure conditions in the current cluster. The primary failure detector is the TrafficMonitoringFailureDetector, which is looking for networking exceptions between the application and the cluster. The primary configuration options are how many exceptions and in what time period. You also have the ability to specify which Couchbase services to monitor.

{% hint style="info" %}
**Note:** The MCA SDK can only monitor those nodes and services with which it has direct interaction. These will be the K/V (BINARY), N1QL, FTS, and Analytics services. It will not detect any problems with the Index or Eventing services, because the client SDK never directly interacts with these services.
{% endhint %}

As you do your planning for what combination of events should trigger a cluster switch, understand that you can combine two or more failure detectors using the conjunction and disjunction detectors as logical AND and ORs. This means that if you have two different trigger conditions, each of which requires a different traffic monitoring configuration, and you want to switch if either of them occur, you can combine them using the DisjunctionFailureDetector. If you need both to occur to trigger the switch, then you combine them using the ConjunctionFailureDetector.

An example of this would be if you needed to trigger a cluster switch when either you received a set of network timeouts when trying to run N1QL queries, or at least two nodes running the data service failed. In this instance, you would configure a TrafficMonitoringFailureDetector to monitor calls to the query service, and a NodeHealthFailureDetector monitoring the k/v service, and then use a DisjunctionFailureDetector to combine them.

{% hint style="info" %}
**Note:** It is very easy to build an overly complex set of failure detectors that is difficult to debug when it isn't triggering the cluster switch at the proper times. In general, you'll be better off keeping your failure detector configurations simple. If you can accomplish your functionality/SLA needs by using a single failure detector, then don't complicate your configuration unnecessarily. If you need to build a more complex set of failure detection logic, start with a simple configuration and build it bit-by-bit, testing along the way. By generating the set of conditions to trigger the switch for each set of changes made, you can make sure that each change to the configuration works as you expect it to. If you get to the point where the switch is not being triggered correctly, you can look at the configuration, and what the last change made, and determine if there is a problem with your failure detector configuration.
{% endhint %}

## Determining if the Cluster Switch Happened

So, you've setup your MCA SDK and are connecting to the first cluster in your list when you encounter the conditions that should cause your application to switch to the next cluster. How do you know if the switch was made? 

### Examining the log

One way to detect that the coordinator has switched clusters is by checking the SDK log. If you have logging set to INFO or DEBUG level, you should be able to grep the log, looking for entries from the IsolatedCoordinator. Among these log entries should be one indicating the cluster switch. You can also grep for the failure detector if you want to see what signals are being fed to the coordinator. This should provide a means of determining if and when a cluster switch was made.

```txt
2019-04-10 10:24:43.409  INFO 6636 --- [...] c.c.c.m.c.IsolatedCoordinator            : Starting grace period of 1000 MILLISECONDS...
...
2019-04-10 10:24:44.411  INFO 6636 --- [...] c.c.c.m.c.IsolatedCoordinator            : Grace period over, moving topology to DefaultTopology{...}
2019-04-10 10:24:44.412 DEBUG 6636 --- [...] c.c.c.m.d.NodeHealthFailureDetector      : TopologyChangedEvent received, dispatching reset to green.
2019-04-10 10:24:44.412 DEBUG 6636 --- [...] c.c.c.m.d.NodeHealthFailureDetector      : Resetting Failure Detector back to GREEN.
```

{% hint style="info" %}
**Note:** The above log entries are taken from the Java SDK log. The entries in the .NET log may differ, while still providing the same information.
{% endhint %}

<!--
Question: Could the switch cluster switch also be determined by retrieving the topology from the coordinator or simple admin objects?
-->
### Examining the Current Topology

An alternative method for determining if the switch took place would be to retrieve the current topology from either the coordinator, or the simple topology admin object. Either of these methods will return a Topology object that can be examined to determine the currently active cluster.

## Specifying the Active Cluster

Once the conditions that triggered a cluster switch have been resolved, and the application should revert to interacting with the original cluster, how do you get it to switch back? 

No, you don't have to stop and restart your application to reload the original configuration. There are two options for controlling the cluster the application is connected to. One is via code and the SimpleTopologyAdmin object. The other is via JMX extensions for the Java SDK, or via REST API for the .NET SDK.

If you want to build into your application the ability to perform a cluster switch via some mechanism you've built in, all you need to do is instantiate the SimpleTopologyAdmin object, passing it the current coordinator, and then deactivate the current cluster and activate the original cluster topology entry.

```java
// Setup
List<ClusterSpec> clusterSpecs = createSpecs();
Coordinator coordinator = Coordinators.isolated(new IsolatedCoordinator.Options()
    .clusterSpecs(clusterSpecs)
);

...

SimpleTopologyAdmin admin = SimpleTopologyAdmin.create(coordinator);

// Disable the active one
admin.deactivate("cluster-2");

// Enable the inactive one
admin.activate("cluster-1");
```

If you using the Java SDK and prefer to go through the JMX management extensions, you can instantiate the JmxTopologyAdmin object, then use a JMX client/console to access and modify the current topology to switch clusters.

If you are using the .NET SDK, and want to use a REST API to make the switch, you'll need to instantiate the WebApiTopologyAdmin object to get and modify the current topology to activate the appropriate cluster.

## Test Planning

When planning how to configure the failure detectors for your application, you also need to be thinking about how to test each configuration change to make sure it's detecting the failure events you intend for it to detect. This should include both failure scenarios that should not trigger a cluster switch as well as ones that do.

For instance, if you want your application to switch clusters when two or more nodes running the data service fail within a small window of time, you need to test the scenario where only one node fails. You also need to test the scenario where two or more nodes are in a failure condition when the application starts up, to make sure that it detects the condition and switches clusters shortly after startup. So be sure to test more than just the scenario where two nodes fail while the application is running.

Each of these test scenarios need to include criteria for success as well as failure (of the test, not the cluster), and how the cluster failure conditions will be engineered (e.g. stopping the Couchbase service on one or more nodes, disconnecting network cables, etc.).