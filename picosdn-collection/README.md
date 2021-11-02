# PicoSDN (ProvSDN v2.0)

Control plane provenance for the ONOS SDN controller.

## Authors

* Benjamin E. Ujcich (<benjamin.ujcich@ll.mit.edu>, <ujcich2@illinois.edu>)
* Samuel Jero (<samuel.jero@ll.mit.edu>)

## Instructions 

### Build ProvSDN ProvManager

Install first time:

```
cd $ONOS_ROOT/onos-app-provsdn
mvn clean install -DskipTests
onos-app localhost install target/onos-app-provsdn-2.0.oar
```

Reinstall:

```
onos-app localhost reinstall target/onos-app-provsdn-2.0.oar
```

### Develop ONOS with ProvSDN

Note that the following instructions are meant for development of the controller's core components (e.g., managers, services, providers) so as to be completely transparent from apps that use such core components. All app activities with the control plane are captured through the event dispatchers, event listeners, and API calls, which serve collectively as the *provenance reference monitor*.

#### Add control plane state objects (`ProvEntity`)

For a control plane state object of interest, extend its interface to include `ProvEntity` (example of `Region`):

```
public interface Region extends Annotated, ProvEntity {
...
```

Find all of the implementers of that control plane state object interface to include the UUID get/set methods:

```
public final class DefaultRegion extends AbstractAnnotated implements Region {
...
private UUID entityUuid;
...
@Override
public UUID getEntityUuid() {
    return entityUuid;
}

@Override
public void setEntityUuid(UUID uuid) {
    entityUuid = uuid;
}
```

Note that mock test classes that implement interfaces will also need to be changed for ONOS to compile correctly.

#### Add control plane API calls (`recordApiCall()`)

ProvSDN's API hooks use create/read/update/delete (CRUD) operations. This allows control plane state objects to have notions of lifecycles.

A given manager, which implements its respective service and/or provider, will typically instantiate a (distributed) internal store. As a result, ProvSDN API hooks can either be placed as close to the API call as possible or as close to the internal store's methods as possible.

Advantages to placing hooks near the API call: 1) don't have to worry about the storage mechanism (e.g., single, distributed) changing, and 2) hook-related lookups to the internal store's methods aren't kept as provenance.

Place hooks after a call to the internal store if possible.

The following patterns use `RegionManager` API calls and `Region` objects as examples.

##### Create

Typical pattern:

```
return store.createRegion(regionId, name, type,
        genAnnots(regionId),
        masterNodeIds == null ? of() : masterNodeIds);
```

Replace with:

```
Region tmp = store.createRegion(regionId, name, type,
        genAnnots(regionId),
        masterNodeIds == null ? of() : masterNodeIds);
ProvHook.recordApiCall(ProvApiCallType.CREATE, tmp);
return tmp;
```

##### Read

Typical pattern:

```
return store.getRegions();
```

Replace with:

```
Set<Region> tmp = store.getRegions();
ProvHook.recordApiCall(ProvApiCallType.READ, tmp);
return tmp;
```

##### Update

Typical pattern:

```
store.addDevices(regionId, deviceIds);
```

Replace with:

```
Region tmpPrev = store.getRegion(regionId);
store.addDevices(regionId, deviceIds);
Region tmpNew = store.getRegion(regionId);
ProvHook.recordApiCall(ProvApiCallType.UPDATE, tmpPrev, tmpNew);
```

The update operation requires having a reference to the object before its update as well as after its update, which may require internal store read calls. Since ONOS typically uses immutable objects, the object references (and possibly their content) should be different at the end. It's okay if `tmpPrev` or `tmpNew` are `null`, as ProvManager will handle them appropriately.

Semantics may vary, but note that we are *updating* the `Region` object even though the call is `add...()`.

##### Delete

Typical pattern:

```
store.removeRegion(regionId);
```

Replace with:

```
Region tmp = store.getRegion(regionId);
store.removeRegion(regionId);
ProvHook.recordApiCall(ProvApiCallType.DELETE, tmp);
```

Note that we may have to do a read before the deletion to get the right object. If we hook close to the API call and not the internal store call, then we can avoid having to mark that read in the provenance (as the original deletion didn't do a read).

#### Add inter-object dependencies (`recordDerivation()`)

Rather than inferring object derivations across threads, it may be easier to record their derivations to keep track of causal links. To do this, use `recordDerivation()` to link a child (or children) `ProvEntity` with the parent `ProvEntity` that it derives from.

#### Add unit of execution objects (`ProvActivity`)

These are objects that either listen or process, are loop-based, and represent a unit of execution. Similar to `ProvEntity`, extend the interface with `ProvActivity` and implement UUID get/set methods in the implementing classes.

#### Add unit of execution dispatchers and listeners (`recordDispatch()` and `recordListen()`)

Add `recordDispatch()` within a dispatcher before dispatching any listeners.

Add `recordListen()` right before dispatching the event to a listener. This denotes the start of execution of that listener. A dispatch *must* be called before a listen, as otherwise ProvManager won't know what to do with the activity.

### Develop ProvSDN

#### PROV relation semantics

* `wasDerivedFrom`: causally related
* `wasRevisionOf`: not causally related

#### Relationship between `ProvActivity` and `W3CProvActivity`

A `ProvActivity` object represents a notion of a dispatch to all of the listeners. A `W3CProvActivity` object represents the specific listener. Thus, for *N* number of listeners, there is a 1 to *N* mapping between `ProvActivity` and `W3CProvActivity` objects.

#### Relationship between `ProvEntity` and `W3CProvEntity`

At any given time, there is a 1 to 1 mapping between `ProvEntity` and `W3CProvEntity` objects. The internal fields of the `ProvEntity` may change, which causes a new `W3CProvEntity` object to be created to represent the change.

#### Modify ProvSDN ProvHook or ProvService API

Publish the Maven artifacts (*Warning: takes a while*):

```
cd $ONOS_ROOT
onos-buck-publish-local
```

Then, build as usual.

### Monitor ProvSDN ProvManager

From ONOS client:

```
log:tail | grep ProvManager
```

# ONOS : Open Network Operating System


## What is ONOS?
ONOS is the only SDN controller platform that supports the transition from legacy “brown field” networks to SDN “green field” networks.
This enables exciting new capabilities, and disruptive deployment and operational cost points for network operators.

## Top-Level Features

* High availability through clustering and distributed state management.
* Scalability through clustering and sharding of network device control.
* Performance that is good for a first release, and which has an architecture
  that will continue to support improvements.
* Northbound abstractions for a global network view, network graph, and
  application intents.
* Pluggable southbound for support of OpenFlow and new or legacy protocols.
* Graphical user interface to view multi-layer topologies and inspect elements
  of the topology.
* REST API for access to Northbound abstractions as well as CLI commands.
* CLI for debugging.
* Support for both proactive and reactive flow setup.
* SDN-IP application to support interworking with traditional IP networks
  controlled by distributed routing protocols such as BGP.
* IP-Optical use case demonstration.


## Getting started

### Dependencies

The following packages are reuqired:

* git
* zip
* curl
* unzip
* python2.7
* Oracle JDK8

To install Oracle JDK8, use following commands (Ubuntu):
```bash
$ sudo apt-get install software-properties-common -y && \
  sudo add-apt-repository ppa:webupd8team/java -y && \
  sudo apt-get update && \
  echo "oracle-java8-installer shared/accepted-oracle-license-v1-1 select true" | sudo debconf-set-selections && \
  sudo apt-get install oracle-java8-installer oracle-java8-set-default -y
```

### Build ONOS from source

1. Clone the code from ONOS gerrit repository
```bash
$ git clone https://gerrit.onosproject.org/onos
```

2. Add ONOS developer environment to your bash profile, no need to do this step again if you had done this before
```bash
$ cd onos
$ cat << EOF >> ~/.bash_profile
export ONOS_ROOT="`pwd`"
source $ONOS_ROOT/tools/dev/bash_profile
EOF
$ . ~/.bash_profile
```

3. Build ONOS with Buck
```bash
$ cd $ONOS_ROOT
$ onos-buck build onos [--show-output]
```

ONOS currently uses a modified version of Buck (`onos-buck`), which has been packaged with ONOS. Please use this version until our changes have been upstreamed and released as part of an official Buck release. 

This will compile all source code assemble the installable onos.tar.gz, which is located in the buck-out directory. Note the --show-output option, which can be omitted, will display the path to this file.


### Start ONOS on local machine

To run ONOS locally on the development machine, simply run the following command:

```bash
$ onos-buck run onos-local [-- [clean] [debug]]
```

or simplier one:

```bash
$ ok [clean] [debug]
```

The above command will create a local installation from the onos.tar.gz file (re-building it if necessary) and will start the ONOS server in the background.
In the foreground, it will display a continuous view of the ONOS (Apache Karaf) log file.
Options following the double-dash (–) are passed through to the ONOS Apache Karaf and can be omitted.
Here, the `clean` option forces a clean installation of ONOS and the `debug` option means that the default debug port 5005 will be available for attaching a remote debugger.

### Interacting with ONOS

To access ONOS UI, use browser to open [http://localhost:8181/onos/ui](http://localhost:8181/onos/ui) or use `onos-gui localhost` command

The default username and password is **onos/rocks**

To attach to the ONOS CLI console, run:

```bash
$ onos localhost
```

### Unit Tests

To run ONOS unit tests, including code Checkstyle validation, run the following command:

```bash
$ onos-buck test
```

Or more specific tests:

```bash
$ onos-buck test [buck-test-rule]
```

## Contributing

ONOS code is hosted and maintained using [Gerrit](https://gerrit.onosproject.org/).

Code on GitHub is only a mirror. The ONOS project does **NOT** accept code through pull requests on GitHub. 

To contribute to ONOS, please refer to [Sample Gerrit Workflow](https://wiki.onosproject.org/display/ONOS/Sample+Gerrit+Workflow). It should includes most of the things you'll need to get your contribution started!


## More information

For more information, please check out our wiki page or mailing lists:

* [Wiki](https://wiki.onosproject.org/)
* [Google group](https://groups.google.com/a/onosproject.org/forum/#!forum/onos-dev)
* [Slack](https://onosproject.slack.com)

## License

ONOS (Open Network Operating System) is published under [Apache License 2.0](https://github.com/opennetworkinglab/onos/blob/master/LICENSE.txt)
