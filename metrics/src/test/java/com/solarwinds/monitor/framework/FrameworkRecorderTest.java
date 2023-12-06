package com.solarwinds.monitor.framework;

import com.solarwinds.monitor.framework.FrameworkRecorder.FrameworkInfo;
import org.junit.jupiter.api.Test;

import java.util.jar.Attributes;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FrameworkRecorderTest {

    @Test
     public void testExtractNoFramework() {
        //manifest from ognl
        Attributes attributes = new Attributes();
        attributes.putValue("Manifest-Version", "1.0");
        attributes.putValue("Archiver-Version", "Plexus Archiver");
        attributes.putValue("Created-By", "Apache Maven");
        attributes.putValue("Built-By", "lukaszlenart");
        attributes.putValue("Build-Jdk", "1.6.0_37");

        FrameworkInfo frameworkInfo = FrameworkRecorder.extractInfoFromAttributes(attributes);
        assertEquals(null, frameworkInfo);
    }

    @Test
    public void testExtractBundleFramework() {
        //manifest from mongodb

        Attributes attributes = new Attributes();
        attributes.putValue("Manifest-Version", "1.0");
        attributes.putValue("Ant-Version", "Apache Ant 1.7.0");
        attributes.putValue("Created-By", "1.7.0-b21 (Sun Microsystems Inc.)");
        attributes.putValue("Bundle-ManifestVersion", "2");
        attributes.putValue("Bundle-Name", "MongoDB");
        attributes.putValue("Bundle-SymbolicName", "com.mongodb");
        attributes.putValue("Bundle-Version", "2.1.0");
        attributes.putValue("Export-Package", "com.mongodb, com.mongodb.io, com.mongodb.util, com.mongodb.gridfs, org.bson, org.bson.util, org.bson.types, org.bson.io");

        FrameworkInfo frameworkInfo = FrameworkRecorder.extractInfoFromAttributes(attributes);
        assertEquals("com.mongodb", frameworkInfo.getId());
        assertEquals("2.1.0", frameworkInfo.getVersion());
    }

    @Test
    public void testExtractSpecificationFramework() {
        //manifest from javassist
        Attributes attributes = new Attributes();
        attributes.putValue("Manifest-Version", "1.1");
        attributes.putValue("Ant-Version", "Apache Ant 1.6.5");
        attributes.putValue("Created-By", "Shigeru Chiba, Tokyo Institute of Technology");
        attributes.putValue("Specification-Title", "Javassist");
        attributes.putValue("Specification-Vendor", "Shigeru Chiba, Tokyo Institute of Technology");
        attributes.putValue("Specification-Version", "3.11.0.GA");
        attributes.putValue("Main-Class", "javassist.CtClass");
        attributes.putValue("Name", "javassist/");


        FrameworkInfo frameworkInfo = FrameworkRecorder.extractInfoFromAttributes(attributes);
        assertEquals("Javassist", frameworkInfo.getId());
        assertEquals("3.11.0.GA", frameworkInfo.getVersion());
    }

    @Test
    public void testExtractImplementationFramework() {
        //manifest from freemarker
        Attributes attributes = new Attributes();
        attributes.putValue("Manifest-Version", "1.0");
        attributes.putValue("Ant-Version", "Apache Ant 1.8.1");
        attributes.putValue("Created-By", "1.6.0_22-b04 (Sun Microsystems Inc.)");
        attributes.putValue("Main-Class", "freemarker.core.CommandLine");
        attributes.putValue("Extension-name", "FreeMarker");
        attributes.putValue("Specification-Title", "FreeMarker");
        attributes.putValue("Specification-Version", "2.3.19");
        attributes.putValue("Specification-Vendor", "Visigoth Software Society");
        attributes.putValue("Implementation-Title", "VSS Java FreeMarker");
        attributes.putValue("Implementation-Version", "2.3.19");
        attributes.putValue("Implementation-Vendor", "Visigoth Software Society");

        FrameworkInfo frameworkInfo = FrameworkRecorder.extractInfoFromAttributes(attributes);
        assertEquals("VSS Java FreeMarker", frameworkInfo.getId());
        assertEquals("2.3.19", frameworkInfo.getVersion());
    }
}
