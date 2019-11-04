# emoncms-connector

Copies data obtained from the EmonCMS REST API into OGEMA Resources.

Tested with EmonCMS Version 9.9.6.

Prerequisites:
 * EmonCMS set up and running

## Configuration

### Obtaining an API Key

After logging into the web interface of EmonCMS, click _Setup_ in the
top right (wrench icon), then click _Feeds_.  In the feed overview, take
note of the feed ids you wish to mirror into OGEMA.  Click _Feed API
Help_ in the top right corner.  The _Read Only_ API key is stored as
_apiKeyRead_, the _Read & Write_ key as _apiKeyWrite_.

You can also verify the API base _url_ here.  If the JSON API URLs shown
are like `http://rexometer.example.com/feed/timevalue.json?id=1`, your
base URL is `http://rexometer.example.com/feed/`.

### OGEMA Configuration

Configuration of the connector is done through configuration resources
of type `EmonCMSConnection`.  An `EmonCMSConnection` holds the **API
Base URL**, **API Keys** as well as **read <!-- and write -->
configurations**:

#### Example Configuration Resource

You can adapt the following example configuration:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<og:resource xmlns:og="http://www.ogema-source.net/REST" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <name>EmonCMSConnection</name>
    <type>org.smartrplace.rexometer.driver.emoncms.model.EmonCMSConnection</type>
    <path>EmonCMSConnection</path>
    <decorating>false</decorating>
    <active>true</active>

    <!-- Base URL, usually ends in `/feed` -->
    <resource xsi:type="og:StringResource">
        <name>url</name>
        <type>org.ogema.core.model.simple.StringResource</type>
        <path>EmonCMSConnection/url</path>
        <decorating>false</decorating>
        <active>true</active>
        <value>http://rexometer.example.com/feed</value>
    </resource>

    <!-- API Key for read access -->
    <resource xsi:type="og:StringResource">
        <name>apiKeyRead</name>
        <type>org.ogema.core.model.simple.StringResource</type>
        <path>EmonCMSConnection/apiKeyRead</path>
        <decorating>false</decorating>
        <active>true</active>
        <value>********************************</value>
    </resource>

    <!-- API Key for write access (currently unused) -->
    <resource xsi:type="og:StringResource">
        <name>apiKeyWrite</name>
        <type>org.ogema.core.model.simple.StringResource</type>
        <path>EmonCMSConnection/apiKeyWrite</path>
        <decorating>false</decorating>
        <active>false</active>
        <value>********************************</value>
    </resource>

    <!-- Read Configurations (one per feed) -->
    <resource xsi:type="og:ResourceList">
        <name>readConfigurations</name>
        <type>org.ogema.core.model.ResourceList</type>
        <path>EmonCMSConnection/readConfigurations</path>
        <decorating>false</decorating>
        <active>true</active>
        <resource>
            <name>Power</name>
            <type>org.smartrplace.rexometer.driver.emoncms.model.EmonCMSReadConfiguration</type>
            <path>EmonCMSConnection/readConfigurations/Power</path>
            <decorating>true</decorating>
            <active>true</active>

            <!-- Destination to which the value will be written -->
            <resourcelink>
                <link>building/electricityConnection/totalPower</link>
                <type>org.ogema.core.model.units.PowerResource</type>
                <name>destination</name>
            </resourcelink>

            <!-- Update rate in ms -->
            <resource xsi:type="og:TimeResource">
                <name>pollRate</name>
                <type>org.ogema.core.model.simple.TimeResource</type>
                <path>EmonCMSConnection/readConfigurations/Power/pollRate</path>
                <decorating>false</decorating>
                <active>true</active>
                <value>20000</value>
            </resource>

            <!-- Feed ID -->
            <resource xsi:type="og:IntegerResource">
                <name>fieldId</name>
                <type>org.ogema.core.model.simple.IntegerResource</type>
                <path>EmonCMSConnection/readConfigurations/Power/fieldId</path>
                <decorating>false</decorating>
                <active>true</active>
                <value>43</value>
            </resource>
        </resource>
        <elementType>org.smartrplace.rexometer.driver.emoncms.model.EmonCMSReadConfiguration</elementType>
    </resource>
</og:resource>
```
