package org.ogema.tools.app.useradmin.impl;

import javax.inject.Inject;
import org.junit.Assert;
import org.junit.Test;
import org.ogema.tools.app.useradmin.api.UserDataAccess;
import org.ogema.tools.widgets.test.base.WidgetsTestBase;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

/**
 *
 * @author jlapp
 */
@ExamReactorStrategy(PerClass.class)
public class UserDataAccessImplTest extends WidgetsTestBase {

    @Inject
    UserDataAccess dataAccess;
    
    final String ADDRESS_TYPE_1 = "testType1";
    final String ADDRESS_TYPE_2 = "testType2";
    final String TESTUSER = "master";

    public UserDataAccessImplTest() {
        super(true);
    }

    @ProbeBuilder
    public TestProbeBuilder build(TestProbeBuilder builder) {
        builder.setHeader("DynamicImport-Package", "*");
        return builder;
    }

    @Override
    @Configuration
    public Option[] config() {
        return new Option[]{
            CoreOptions.systemProperty("org.ogema.security").value("off"),
            CoreOptions.composite(super.config()),
            /*
            CoreOptions.bundle("reference:file:target/classes/"),
                        CoreOptions.mavenBundle()
            .groupId("org.smartrplace.apps")
            .artifactId("smartrplace-util-proposed")
            .version("0.9.0-SNAPSHOT").start(),
*/
            CoreOptions.mavenBundle()
            .groupId("org.ogema.model")
            .artifactId("widget-models-experimental")
            .version("2.2.2-SNAPSHOT").start(),
            CoreOptions.mavenBundle()
            .groupId("org.ogema.eval")
            .artifactId("timeseries-api-extended")
            .version("2.2.2-SNAPSHOT").start(),
            CoreOptions.mavenBundle()
            .groupId("org.ogema.eval")
            .artifactId("timeseries-multieval-garo-base")
            .version("2.2.2-SNAPSHOT").start(),
            CoreOptions.mavenBundle()
            .groupId("org.ogema.eval")
            .artifactId("timeseries-multieval-base")
            .version("2.2.2-SNAPSHOT").start(),
            CoreOptions.mavenBundle()
            .groupId("org.ogema.eval")
            .artifactId("timeseries-eval-base")
            .version("2.2.2-SNAPSHOT").start(),
            CoreOptions.mavenBundle()
            .groupId("org.ogema.model")
            .artifactId("fhg-proposed")
            .version("2.2.2-SNAPSHOT").start(),};
    }

    @Test
    public void testGetAllUsers() {
        Assert.assertFalse(dataAccess.getAllUsers().isEmpty());
    }

    @Test
    public void testMessagingAddressRW() {
        Assert.assertTrue(dataAccess.getMessagingAddresses(TESTUSER, ADDRESS_TYPE_1).isEmpty());
        Assert.assertTrue(dataAccess.getMessagingAddresses(TESTUSER, ADDRESS_TYPE_2).isEmpty());
        Assert.assertTrue(dataAccess.getMessagingAddresses(TESTUSER, null).isEmpty());
        String addrT1 = "foo@bar";
        String addrT2 = "4711";
        dataAccess.addMessagingAddress(TESTUSER, ADDRESS_TYPE_1, addrT1);
        dataAccess.addMessagingAddress(TESTUSER, ADDRESS_TYPE_2, addrT2);
        System.out.println(dataAccess.getUserPropertyResource(TESTUSER));
        System.out.println(dataAccess.getUserPropertyResource(TESTUSER).getSubResources(true));
        Assert.assertEquals(1, dataAccess.getMessagingAddresses(TESTUSER, ADDRESS_TYPE_1).size());
        Assert.assertEquals(1, dataAccess.getMessagingAddresses(TESTUSER, ADDRESS_TYPE_2).size());
        Assert.assertEquals(2, dataAccess.getMessagingAddresses(TESTUSER, null).size());
        
        Assert.assertFalse(dataAccess.removeMessagingAddress(TESTUSER, ADDRESS_TYPE_1, addrT2));
        Assert.assertEquals(2, dataAccess.getMessagingAddresses(TESTUSER, null).size());
        Assert.assertTrue(dataAccess.removeMessagingAddress(TESTUSER, ADDRESS_TYPE_1, addrT1));
        Assert.assertEquals(0, dataAccess.getMessagingAddresses(TESTUSER, ADDRESS_TYPE_1).size());
        Assert.assertEquals(1, dataAccess.getMessagingAddresses(TESTUSER, ADDRESS_TYPE_2).size());
        Assert.assertEquals(1, dataAccess.getMessagingAddresses(TESTUSER, null).size());
        
        Assert.assertTrue(dataAccess.removeMessagingAddress(TESTUSER, ADDRESS_TYPE_2, addrT2));
        Assert.assertEquals(0, dataAccess.getMessagingAddresses(TESTUSER, null).size());
    }

}
