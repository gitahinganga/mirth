package org.itechkenya.mirth;

import org.apache.log4j.Logger;

/**
 * Provides a mechanism for routing messages in Mirth Connect based on
 * application addresses. An application address, as opposed to a network or
 * physical address is a unique static reference to a software system. It is
 * static because it does not change even if the system it refers to is migrated
 * to a different network or physical location.
 * <p/>
 * Software systems that exchange data need only know the application addresses
 * of each other. It is the job of Mirth Connect, supported by this class, to
 * interpret the application address and route the message to the correct
 * network address.
 * <p/>
 * This class supports the reverse-internet-domain-name addressing scheme,
 * similar to the one used for Java packages. Thus the root address should be
 * the reverse internet domain name of the organization deploying this
 * technology. The goal is to reduce chances of address collisions. Further
 * dot-separated alphanumeric tokens may be appended as necessary. Underscores
 * are also permissible.
 *
 * @author gitahi
 */
public class Router {

    /**
     * The address of the highest-level router. Addresses for lower-level
     * routers have the corresponding number of dot-separated tokens appended
     * accordingly. A typical root address might look like [ke.go.health]. A
     * router one level lower might have the address [ke.go.health.county1] and
     * the one below that might have the address
     * [ke.go.health.county1.facility1] and so on.
     * <p/>
     * By default, the root address is [ke.go.health].
     */
    private String rootAddress = "ke.go.health";

    /**
     * The application address of this router. Router addresses should be mapped
     * to well-known, fairly static network locations since other participating
     * software systems need to know the network location of their closest
     * routing channel.
     */
    private final String routerAddress;

    /**
     * A {@link Logger} object passed from Mirth Connect to enable this Router
     * to log messages via Mirth Connect.
     */
    private final Logger logger;

    /**
     * Initializes a new Router with the given address and logger. The router
     * address passed is validated here and if found to be invalid a
     * {@link RuntimeException} is thrown.
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
     * Same as {@link Router#Router(java.lang.String, org.apache.log4j.Logger) }
     * but also sets {@link Router#rootAddress}.
     *
     * @param rootAddress the root address to set.
     * @param routerAddress the address of this Router.
     * @param logger the {@link Logger} for this router.
     */
    public Router(String rootAddress, String routerAddress, Logger logger) {
        this(routerAddress, logger);
        this.rootAddress = rootAddress;
    }

    /**
     * @return the root address.
     */
    public String getRootAddress() {
        return rootAddress;
    }

    /**
     * Sets the root address.
     *
     * @param rootAddress the root address to set.
     */
    public void setRootAddress(String rootAddress) {
        this.rootAddress = rootAddress;
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
     * mechanism ensures a message successively gets closer to its final
     * destination until it is ultimately delivered to an endpoint.
     *
     * @param destinationAddress the destination address of the message.
     *
     * @return the name of the channel via which to dispatch the message.
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

    /**
     * Creates a channel name from an application address by some convention.
     * The convention followed in the default implementation is to replace all
     * dots in the application address with underscore characters. You may
     * override this method to support a different convention.
     *
     * @param applicationAddress the application address.
     *
     * @return the channel name.
     */
    protected String getChannelName(String applicationAddress) {
        return applicationAddress.replace(".", "_");
    }

    /*
     * Extracts the gateway address for this router. The gateway address is the
     * address of another router, typically on a different network. Messages are
     * routed to gateways when they cannot be delivered to any endpoint in the
     * router's proximity. If this is a top level router, this method throws a
     * {@link RuntimeException}.
     */
    private String getGatewayAddress() {
        String gatewayAddress = "";
        if (rootAddress.equals(routerAddress)) {
            throw new RuntimeException("Impossible operation. Attempting to "
                    + "obtain a gateway address for the top-level router ["
                    + rootAddress + "]. Check destination address.");
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

    /*
     * Extracts the application address of the immediate next node given the
     * nearby token from a destination address. See 
     * {@link Router#getNearbyToken(java.lang.String) }. For example, if the
     * nearby address is [county1.facility1.emr], the nearby address is
     * [ke.go.health.county1].
     */
    private String getNearbyAddress(String nearbyToken) {
        String[] nearby = nearbyToken.split("\\.");
        return routerAddress + "." + nearby[0];
    }

    /*
     * Validates a given application address. If the address is invalid, a
     * {@link RuntimeException} is thrown.
     *
     * @param applicationAddress the application address to validate.
     */
    private void validateAddress(String applicationAddress) {
        int m = rootAddress.length();
        int n = applicationAddress.length();
        boolean invalid = (m > n);
        if (!invalid) {
            invalid = !applicationAddress.startsWith(rootAddress);
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
                    + "the root token: [" + rootAddress + "]");
        }
    }

    /*
     * Validates a given destination application address. If the address is
     * invalid, a {@link RuntimeException} is thrown.
     *
     * @param destinationAddress the application address to validate.
     */
    private void validateDestinationAddress(String destinationAddress) {
        if (routerAddress.equals(destinationAddress)) {
            throw new RuntimeException("Invalid destination application address ["
                    + destinationAddress + "]. This address points to the ["
                    + routerAddress + "] router address.");
        }
    }
}
