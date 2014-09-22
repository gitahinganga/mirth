package org.itechkenya.mirth;

import org.apache.log4j.Logger;

/**
 *
 * @author gitahi
 */
public class Router {

    /**
     * The address of the highest-level router. Addresses for lower-level
     * routers have the corresponding number of dot-separated tokens appended
     * accordingly. A typical root address might look like [ke.go.health]. A
     * router 3 levels lower might have the address such as
     * [ke.go.health.county1.facility1].
     * <p/>
     * Unless otherwise set, the address root address defaults to
     * [ke.go.health].
     */
    private String addressRoot = "ke.go.health";

    /**
     * The application address of this router.
     */
    private final String routerAddress;

    /**
     * A {@link Logger} object passed from Mirth to enable this Router to log
     * messages via Mirth.
     */
    private final Logger logger;

    /**
     * Checks if a given application address is valid and throws a
     * {@link RuntimeException} if the address is invalid. A valid application
     * address is one that is:
     *
     * 1. Not shorter than the {@link Router#addressRoot} 2. Prefixed with the
     * {@link Router#addressRoot} 3. Contains only dot-separated alphanumeric
     * strings with permissible underscores.
     */
    private void validateAddress(String applicationAddress) {
        int m = addressRoot.length();
        int n = applicationAddress.length();
        boolean invalid = (m > n);
        if (!invalid) {
            invalid = !applicationAddress.startsWith(addressRoot);
            if (!invalid) {
                String[] tokens = applicationAddress.split("\\.");
                for (String token : tokens) {
                    if ("".equals(token) || !token.matches("^[a-zA-Z0-9_]*$")) {
                        invalid = true;
                        break;
                    }
                }
            }
        }
        if (invalid) {
            throw new RuntimeException("Invalid application address ["
                    + applicationAddress + "]. A valid address must begin with "
                    + "the root token: [" + addressRoot + "]");
        }
    }

    /**
     * Checks if a given destination application address is valid and throws a
     * {@link RuntimeException} if the address is invalid. This method first
     * calls {@link Router#validateAddress(java.lang.String) }. It then further
     * requires that the destination address cannot be the same as the
     * {@link Router#routerAddress}.
     */
    private void validateDestinationAddress(String destinationAddress) {
        if (routerAddress.equals(destinationAddress)) {
            throw new RuntimeException("Invalid destination application address ["
                    + destinationAddress + "]. This address points to the ["
                    + routerAddress + "] router address.");
        }
    }

    /**
     * Initializes a new Router with the given address and logger.
     *
     * @param routerAddress the address of this Router.
     * @param logger the {@link Logger} for this router.
     */
    public Router(String routerAddress, Logger logger) {
        if (routerAddress == null || routerAddress.isEmpty()) {
            throw new RuntimeException("Router cannot be initialized with a null "
                    + "or empty router address.");
        }
        this.routerAddress = routerAddress;
        this.logger = logger;
        validateAddress(routerAddress);
        logger.info("---------------- Initialized new Java Router with adress "
                + routerAddress + " ----------------");
    }

    /**
     * @return the address root.
     */
    public String getAddressRoot() {
        return addressRoot;
    }

    /**
     * @param addressRoot the address root to set.
     */
    public void setAddressRoot(String addressRoot) {
        this.addressRoot = addressRoot;
    }

    /**
     * @return the router address.
     */
    public String getRouterAddress() {
        return routerAddress;
    }

    /**
     * Given the destination address of a message, this method determines the
     * channel via which to dispatch the message in its immediate next hop. This
     * mechanism successfully ensures a message successively gets closer to its
     * final destination until it is ultimately delivered.
     *
     * @param destinationAddress the destination address of the message.
     *
     * @return the name of the channel via which to dispatch the message whose
     * destination address is passed here.
     */
    public String dispatchTo(String destinationAddress) {
        String channelName;
        validateDestinationAddress(destinationAddress);
        logger.info("Dispatching to destination address: " + destinationAddress);
        boolean sendToGateway = (destinationAddress.length()
                < routerAddress.length());
        if (sendToGateway == false) {
            logger.info("Destination address longer than router address. "
                    + "Extracting router token.");
            String routerToken = getRouterToken(destinationAddress);
            logger.info("Router token is: " + routerToken);
            sendToGateway = (!routerAddress.equals(routerToken));
        } else {
            logger.info("Destination address shorter than router address. "
                    + "Routing up immediately.");
        }

        if (sendToGateway) {
            channelName = getChannelName(getGatewayAddress());
            logger.info("Routing up to: " + channelName);
        } else {
            String nearbyToken = getNearbyToken(destinationAddress);
            logger.info("Nearby token is: " + nearbyToken);
            String nearbyAddress = getNearbyAddress(nearbyToken);
            logger.info("Nearby address is: " + nearbyAddress);
            channelName = getChannelName(nearbyAddress);
            logger.info("Routing down to: " + channelName);
        }
        return channelName;
    }

    /*
     * Extracts the part of the destination address that corresponds to the
     * {@link Router#routerAddress}. For example, if the application address is
     * [ke.go.health.county1.facility1.emr] and the router address is
     * [ke.go.health.county2], then the router token would be
     * [ke.go.health.county1].
     */
    private String getRouterToken(String destinationAddress) {
        int n = routerAddress.length();
        return destinationAddress.substring(0, n);
    }

    /*
     * Extracts the "non-router" part of the destination address. See 
     * {@link Router#getRouterToken(java.lang.String) }. For example, if the
     * application address is [ke.go.health.county1.facility1.emr] and the
     * router address is [ke.go.health.county2], then the nearby token would be
     * [facility1.emr].
     */
    private String getNearbyToken(String destinationAddress) {
        int m = routerAddress.length();
        int n = destinationAddress.length();
        return destinationAddress.substring(m + 1, n);
    }

    /**
     * 
     */
    private String getGatewayAddress() {
        String gatewayAddress = "";
        if (addressRoot.equals(routerAddress)) {
            throw new RuntimeException("Impossible operation. Attempting to "
                    + "obtain a gateway address for the top-level router ["
                    + addressRoot + "]. Check destination address.");
        }
        String[] tokens = routerAddress.split("\\.");
        int n = tokens.length;
        int m = n - 1;
        for (int i = 0; i < m; i++) {
            gatewayAddress += tokens[i];
            if (i != (m - 1)) {
                gatewayAddress += ".";
            }
        }
        return gatewayAddress;
    }

    /**
     *
     */
    private String getNearbyAddress(String nearbyToken) {
        String[] nearby = nearbyToken.split("\\.");
        return routerAddress + "." + nearby[0];
    }

    /**
     *
     */
    private String getChannelName(String applicationAddress) {
        return applicationAddress.replace(".", "_");
    }
}
