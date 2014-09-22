package org.itechkenya.mirth;

import org.apache.log4j.Logger;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author gitahi
 */
public class RouterTest {

    private Logger logger;

    @Before
    public void setUp() {
        logger = Logger.getLogger("RouterTest");
    }

    /**
     * Test of dispatchTo method, of class Router.
     */
    @org.junit.Test
    public void testDispatchTo() {
        System.out.println("dispatchTo");
        {
            Router router = new Router("ke.go.health.county1.facility1", logger);
            String channel = router.dispatchTo("ke.go.health.county1.facility1.emr");
            assertEquals(channel, "ke_go_health_county1_facility1_emr");
        }
        {
            Router router = new Router("ke.go.health.county1.facility1", logger);
            String channel = router.dispatchTo("ke.go.health.county1.repository");
            assertEquals(channel, "ke_go_health_county1");
        }
        {
            Router router = new Router("ke.go.health.county1", logger);
            String channel = router.dispatchTo("ke.go.health.county1.repository");
            assertEquals(channel, "ke_go_health_county1_repository");
        }
        {
            Router router = new Router("ke.go.health.county1", logger);
            String channel = router.dispatchTo("ke.go.health.county1.facility2.emr");
            assertEquals(channel, "ke_go_health_county1_facility2");
        }
        {
            Router router = new Router("ke.go.health.county1", logger);
            String channel = router.dispatchTo("ke.go.health.repository");
            assertEquals(channel, "ke_go_health");
        }
        {
            Router router = new Router("ke.go.health.county1", logger);
            String channel = router.dispatchTo("ke.go.health.county2.facility4.pis");
            assertEquals(channel, "ke_go_health");
        }
        {
            Router router = new Router("ke.go.health", logger);
            String channel = router.dispatchTo("ke.go.health.repository");
            assertEquals(channel, "ke_go_health_repository");
        }
        {
            Router router = new Router("ke.go.health", logger);
            String channel = router.dispatchTo("ke.go.health.county2.facility4.pis");
            assertEquals(channel, "ke_go_health_county2");
        }
    }

    /**
     * Test of getChannelName method, of class Router.
     */
    @org.junit.Test
    public void testGetChannelName() {
        System.out.println("getChannelName");
        {
            Router router = new Router("ke.go.health.county1.facility1", logger);
            String channel = router.getChannelName("ke.go.health.county1");
            assertEquals(channel, "ke_go_health_county1");
        }
        {
            Router router = new Router("ke.go.health.county1.facility1", logger);
            String channel = router.getChannelName("ke.go.health.cou_nty1");
            assertEquals(channel, "ke_go_health_cou_nty1");
        }
    }
}
