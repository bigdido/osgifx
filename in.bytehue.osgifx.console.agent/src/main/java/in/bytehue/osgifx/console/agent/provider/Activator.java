package in.bytehue.osgifx.console.agent.provider;

import static org.osgi.framework.Constants.BUNDLE_ACTIVATOR;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;

import org.osgi.annotation.bundle.Header;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import aQute.lib.io.IO;
import aQute.remote.util.Link;
import in.bytehue.osgifx.console.agent.Agent;
import in.bytehue.osgifx.console.supervisor.Supervisor;

/**
 * The agent bundles uses an activator instead of DS to not constrain the target
 * environment in any way.
 */
@Header(name = BUNDLE_ACTIVATOR, value = "${@class}")
public class Activator extends Thread implements BundleActivator {
    private File                    cache;
    private ServerSocket            server;
    private BundleContext           context;
    private final List<AgentServer> agents = new CopyOnWriteArrayList<>();

    @Override
    public void start(final BundleContext context) throws Exception {
        this.context = context;

        //
        // Get the specified port in the framework properties
        //

        String port = context.getProperty(Agent.AGENT_SERVER_PORT_KEY);
        if (port == null) {
            port = Agent.DEFAULT_PORT + "";
        }

        //
        // Check if it matches the specifiction of host:port
        //

        final Matcher m = Agent.PORT_P.matcher(port);
        if (!m.matches()) {
            throw new IllegalArgumentException(
                    "Invalid port specification in property aQute.agent.server.port, expects [<host>:]<port> : " + port);
        }

        //
        // See if the host was set, otherwise use localhost
        // for security reasons
        //

        String host = m.group(1);
        if (host == null) {
            host = "localhost";
        } else {
            port = m.group(2);
        }

        System.err.println("Host " + host + " " + port);

        //
        // Get the SHA cache root file, which will be shared by all agents for
        // this process.
        //

        cache = context.getDataFile("shacache");

        final int p = Integer.parseInt(port);
        server = "*".equals(host) ? new ServerSocket(p) : new ServerSocket(p, 3, InetAddress.getByName(host));
        start();

    }

    /**
     * Main dispatcher loop
     */
    @Override
    public void run() {

        try {
            while (!isInterrupted()) {
                try {
                    final Socket socket = server.accept();

                    //
                    // Use a time out so we get interrupts
                    // and can do some checks
                    //

                    socket.setSoTimeout(1000);

                    //
                    // Create a new agent, and link it up.
                    //

                    final AgentServer sa = new AgentServer("<>", context, cache);
                    agents.add(sa);
                    final Link<Agent, Supervisor> link = new Link<Agent, Supervisor>(Supervisor.class, sa, socket) {
                        @Override
                        public void close() throws IOException {
                            agents.remove(sa);
                            super.close();
                        }
                    };
                    sa.setLink(link);
                    // initialize OSGi eventing if available
                    final boolean isEventAdminAvailable = PackageWirings.isEventAdminWired(context);
                    if (isEventAdminAvailable) {
                        final Dictionary<String, Object> properties = new Hashtable<>();
                        properties.put("event.topics", "*");
                        context.registerService("org.osgi.service.event.EventHandler", new OSGiEventHandler(link.getRemote()), properties);
                    }
                    // initialize OSGi logging if available
                    final boolean isLogAvailable = PackageWirings.isLogWired(context);
                    if (isLogAvailable) {
                        final OSGiLogListener logListener = new OSGiLogListener(link.getRemote());
                        trackLogReader(logListener);
                    }
                    link.run();
                } catch (final Exception e) {
                } catch (final Throwable t) {
                    t.printStackTrace();
                }
            }
        } catch (final Throwable t) {
            t.printStackTrace(System.err);
            throw t;
        } finally {
            IO.close(server);
        }
    }

    /**
     * Shutdown any agents & the server socket.
     */
    @Override
    public void stop(final BundleContext context) throws Exception {
        interrupt();
        IO.close(server);

        for (final AgentServer sa : agents) {
            IO.close(sa);
        }
    }

    private void trackLogReader(final OSGiLogListener logListener) {
        final ServiceTracker<Object, Object> logReaderTracker = new ServiceTracker<Object, Object>(context,
                "org.osgi.service.log.LogReaderService", null) {

            @Override
            public Object addingService(final ServiceReference<Object> reference) {
                final boolean isLogAvailable = PackageWirings.isLogWired(context);
                final Object  service        = super.addingService(reference);
                if (isLogAvailable) {
                    XLogReaderAdmin.register(service, logListener);
                }
                return service;
            }

            @Override
            public void removedService(final ServiceReference<Object> reference, final Object service) {
                final boolean isLogAvailable = PackageWirings.isLogWired(context);
                if (isLogAvailable) {
                    XLogReaderAdmin.unregister(service, logListener);
                }
            }
        };
        logReaderTracker.open();
    }

}
