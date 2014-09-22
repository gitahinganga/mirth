package org.itechkenya.mirth;

import org.apache.log4j.Logger;

/**
 *
 * @author gitahi
 */
public class Main {
   
    public static void main(String[] args) {
        Router router = new Router("ke.go.health", Logger.getLogger("test logger"));
        String channelName = router.dispatchTo("ke.go.health.ouch.nyef");
        System.out.println(channelName);
    }
}
