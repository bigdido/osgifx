/*******************************************************************************
 * Copyright 2021-2022 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.agent.provider;

import static aQute.bnd.osgi.Constants.LAUNCH_ACTIVATION_EAGER;
import static com.osgifx.console.agent.dto.XResultDTO.ERROR;
import static com.osgifx.console.agent.dto.XResultDTO.SKIPPED;
import static java.util.Objects.requireNonNull;
import static org.osgi.framework.Bundle.START_ACTIVATION_POLICY;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.util.tracker.ServiceTracker;

import com.osgifx.console.agent.Agent;
import com.osgifx.console.agent.AgentExtension;
import com.osgifx.console.agent.dto.ConfigValue;
import com.osgifx.console.agent.dto.XAttributeDefType;
import com.osgifx.console.agent.dto.XBundleDTO;
import com.osgifx.console.agent.dto.XComponentDTO;
import com.osgifx.console.agent.dto.XConfigurationDTO;
import com.osgifx.console.agent.dto.XHeapUsageDTO;
import com.osgifx.console.agent.dto.XHeapdumpDTO;
import com.osgifx.console.agent.dto.XHttpComponentDTO;
import com.osgifx.console.agent.dto.XMemoryInfoDTO;
import com.osgifx.console.agent.dto.XPropertyDTO;
import com.osgifx.console.agent.dto.XResultDTO;
import com.osgifx.console.agent.dto.XServiceDTO;
import com.osgifx.console.agent.dto.XThreadDTO;
import com.osgifx.console.supervisor.Supervisor;

import aQute.bnd.exceptions.Exceptions;
import aQute.lib.converter.Converter;
import aQute.lib.converter.TypeReference;
import aQute.lib.io.ByteBufferInputStream;
import aQute.remote.util.Link;

/**
 * Implementation of the Agent. This implementation implements the Agent
 * interfaces and communicates with a Supervisor interfaces.
 */
public final class AgentServer implements Agent, Closeable {

	private static final Pattern BSN_P = Pattern.compile("\\s*([^;\\s]+).*");

	private Supervisor                remote;
	private BundleContext             context;
	private final Map<String, String> installed  = new HashMap<>();
	volatile boolean                  quit;
	private Redirector                redirector = new NullRedirector();
	private Link<Agent, Supervisor>   link;
	private final int                 startOptions;
	private ClassloaderLeakDetector   leakDetector;

	private final ServiceTracker<Object, Object>                 scrTracker;
	private final ServiceTracker<Object, Object>                 metatypeTracker;
	private final ServiceTracker<Object, Object>                 eventAdminTracker;
	private final ServiceTracker<Object, Object>                 configAdminTracker;
	private final ServiceTracker<Object, Object>                 gogoCommandsTracker;
	private final ServiceTracker<Object, Object>                 httpServiceRuntimeTracker;
	private final ServiceTracker<AgentExtension, AgentExtension> agentExtensionTracker;

	private final Set<String>                 gogoCommands    = new CopyOnWriteArraySet<>();
	private final Map<String, AgentExtension> agentExtensions = new ConcurrentHashMap<>();

	public AgentServer(final BundleContext context, final ClassloaderLeakDetector leakDetector) throws Exception {
		requireNonNull(context, "Bundle context cannot be null");
		this.context      = context;
		this.leakDetector = leakDetector;

		final boolean eager = context.getProperty(LAUNCH_ACTIVATION_EAGER) != null;
		startOptions = eager ? 0 : START_ACTIVATION_POLICY;

		final Filter gogoCommandFilter = context.createFilter("(osgi.command.scope=*)");

		metatypeTracker           = new ServiceTracker<>(context, "org.osgi.service.metatype.MetaTypeService", null);
		eventAdminTracker         = new ServiceTracker<>(context, "org.osgi.service.event.EventAdmin", null);
		configAdminTracker        = new ServiceTracker<>(context, "org.osgi.service.cm.ConfigurationAdmin", null);
		scrTracker                = new ServiceTracker<>(context, "org.osgi.service.component.runtime.ServiceComponentRuntime", null);
		httpServiceRuntimeTracker = new ServiceTracker<>(context, "org.osgi.service.http.runtime.HttpServiceRuntime", null);
		agentExtensionTracker     = new ServiceTracker<AgentExtension, AgentExtension>(context, AgentExtension.class, null) {

										@Override
										public AgentExtension addingService(final ServiceReference<AgentExtension> reference) {
											final Object name = reference.getProperty(AgentExtension.PROPERTY_KEY);
											if (name == null) {
												return null;
											}
											final AgentExtension tracked = super.addingService(reference);
											agentExtensions.put(name.toString(), tracked);
											return tracked;
										}

										@Override
										public void modifiedService(final ServiceReference<AgentExtension> reference,
										        final AgentExtension service) {
											removedService(reference, service);
											addingService(reference);
										}

										@Override
										public void removedService(final ServiceReference<AgentExtension> reference,
										        final AgentExtension service) {
											final Object name = reference.getProperty(AgentExtension.PROPERTY_KEY);
											if (name == null) {
												return;
											}
											agentExtensions.remove(name);
										}
									};
		gogoCommandsTracker       = new ServiceTracker<Object, Object>(context, gogoCommandFilter, null) {
										@Override
										public Object addingService(final ServiceReference<Object> reference) {
											final String   scope     = String.valueOf(reference.getProperty("osgi.command.scope"));
											final String[] functions = adapt(reference.getProperty("osgi.command.function"));
											addCommand(scope, functions);
											return super.addingService(reference);
										}

										@Override
										public void removedService(final ServiceReference<Object> reference, final Object service) {
											final String   scope     = String.valueOf(reference.getProperty("osgi.command.scope"));
											final String[] functions = adapt(reference.getProperty("osgi.command.function"));
											removeCommand(scope, functions);
										}

										private String[] adapt(final Object value) {
											if (value instanceof String[]) {
												return (String[]) value;
											}
											return new String[] { value.toString() };
										}

										private void addCommand(final String scope, final String... commands) {
											Stream.of(commands).forEach(cmd -> gogoCommands.add(scope + ":" + cmd));
										}

										private void removeCommand(final String scope, final String... commands) {
											Stream.of(commands).forEach(cmd -> gogoCommands.remove(scope + ":" + cmd));
										}
									};
		scrTracker.open();
		metatypeTracker.open();
		eventAdminTracker.open();
		configAdminTracker.open();
		gogoCommandsTracker.open();
		agentExtensionTracker.open();
		httpServiceRuntimeTracker.open();
	}

	@Override
	public BundleDTO installWithData(final String location, final byte[] data, final int startLevel) throws Exception {
		return installBundleWithData(location, data, startLevel, true);
	}

	@Override
	public XResultDTO installWithMultipleData(final Collection<byte[]> data, final int startLevel) {
		requireNonNull(data);
		final XResultDTO    result = new XResultDTO();
		final StringBuilder b      = new StringBuilder();
		try {
			for (final byte[] d : data) {
				try {
					installBundleWithData(null, d, startLevel, false);
				} catch (final Exception e) {
					b.append(e.getMessage()).append(System.lineSeparator());
				}
			}
		} catch (final Exception e) {
			result.result = XResultDTO.ERROR;
		} finally {
			result.response = b.toString();
			// if there are no errors at all, perform the refresh operation
			if (result.response.isEmpty()) {
				try {
					// this sometimes causes https://issues.apache.org/jira/browse/FELIX-3414
					refresh(true);
				} catch (final Exception e) {
					Thread.currentThread().interrupt();
				}
			}
		}
		result.result = XResultDTO.SUCCESS;
		return result;
	}

	@Override
	public BundleDTO installFromURL(final String location, final String url) throws Exception {
		final InputStream is = new URL(url).openStream();
		final Bundle      b  = context.installBundle(location, is);
		installed.put(b.getLocation(), url);
		return toDTO(b);
	}

	@Override
	public String start(final long... ids) {
		final StringBuilder sb = new StringBuilder();

		for (final long id : ids) {
			final Bundle bundle = context.getBundle(id);
			try {
				bundle.start(startOptions);
			} catch (final BundleException e) {
				sb.append(e.getMessage()).append("\n");
			}
		}
		return sb.length() == 0 ? null : sb.toString();
	}

	@Override
	public String stop(final long... ids) {
		final StringBuilder sb = new StringBuilder();

		for (final long id : ids) {
			final Bundle bundle = context.getBundle(id);
			try {
				bundle.stop();
			} catch (final BundleException e) {
				sb.append(e.getMessage()).append("\n");
			}
		}
		return sb.length() == 0 ? null : sb.toString();
	}

	@Override
	public String uninstall(final long... ids) {
		final StringBuilder sb = new StringBuilder();

		for (final long id : ids) {
			final Bundle bundle = context.getBundle(id);
			try {
				bundle.uninstall();
				installed.remove(bundle.getLocation());
			} catch (final BundleException e) {
				sb.append(e.getMessage()).append("\n");
			}
		}
		return sb.length() == 0 ? null : sb.toString();
	}

	@Override
	public String updateFromURL(final long id, final String url) throws Exception {
		final StringBuilder sb = new StringBuilder();
		try (final InputStream is = new URL(url).openStream()) {
			final Bundle bundle = context.getBundle(id);
			bundle.update(is);
			refresh(true);
		} catch (final Exception e) {
			sb.append(e.getMessage()).append("\n");
		}

		return sb.length() == 0 ? null : sb.toString();
	}

	@Override
	public boolean redirect(final int port) throws Exception {
		if (redirector != null) {
			if (redirector.getPort() == port) {
				return false;
			}
			redirector.close();
			redirector = new NullRedirector();
		}

		if (port == Agent.NONE) {
			return true;
		}

		if (port <= Agent.COMMAND_SESSION) {
			try {
				redirector = new GogoRedirector(this, context);
			} catch (final Exception e) {
				throw new IllegalStateException("Gogo is not present in this framework", e);
			}
			return true;
		}

		if (port == Agent.CONSOLE) {
			redirector = new ConsoleRedirector(this);
			return true;
		}
		redirector = new SocketRedirector(this, port);
		return true;
	}

	@Override
	public boolean stdin(final String s) throws Exception {
		if (redirector != null) {
			redirector.stdin(s);
			return true;
		}
		return false;
	}

	@Override
	public String shell(final String cmd) throws Exception {
		redirect(Agent.COMMAND_SESSION);
		stdin(cmd);
		final PrintStream ps = redirector.getOut();
		if (ps instanceof RedirectOutput) {
			final RedirectOutput rout = (RedirectOutput) ps;
			return rout.getLastOutput();
		}
		return null;
	}

	public void setSupervisor(final Supervisor remote) {
		setRemote(remote);
	}

	private BundleDTO toDTO(final Bundle b) {
		final BundleDTO bd = new BundleDTO();
		bd.id           = b.getBundleId();
		bd.lastModified = b.getLastModified();
		bd.state        = b.getState();
		bd.symbolicName = b.getSymbolicName();
		bd.version      = b.getVersion() == null ? "0" : b.getVersion().toString();
		return bd;
	}

	void cleanup(final int event) throws Exception {
		if (quit) {
			return;
		}

		quit = true;
		redirect(0);
		link.close();
	}

	@Override
	public void close() throws IOException {
		try {
			cleanup(-2);

			scrTracker.close();
			metatypeTracker.close();
			configAdminTracker.close();
			gogoCommandsTracker.close();
			agentExtensionTracker.close();
			httpServiceRuntimeTracker.close();
		} catch (final Exception e) {
			throw new IOException(e);
		}
	}

	@Override
	public void abort() throws Exception {
		cleanup(-3);
	}

	public void setRemote(final Supervisor supervisor) {
		remote = supervisor;
	}

	public Supervisor getSupervisor() {
		return remote;
	}

	public void setLink(final Link<Agent, Supervisor> link) {
		setRemote(link.getRemote());
		this.link = link;
	}

	@Override
	public boolean ping() {
		return true;
	}

	public BundleContext getContext() {
		return context;
	}

	public void refresh(final boolean async) throws InterruptedException {
		final FrameworkWiring f = context.getBundle(0).adapt(FrameworkWiring.class);
		if (f != null) {
			final CountDownLatch refresh = new CountDownLatch(1);
			f.refreshBundles(null, event -> refresh.countDown());
			if (async) {
				return;
			}
			refresh.await();
		}
	}

	private Entry<String, Version> getIdentity(final byte[] data) throws IOException {
		try (JarInputStream jin = new JarInputStream(new ByteArrayInputStream(data))) {
			final Manifest manifest = jin.getManifest();
			if (manifest == null) {
				throw new IllegalArgumentException("No manifest in bundle");
			}
			final Attributes mainAttributes = manifest.getMainAttributes();
			final String     value          = mainAttributes.getValue(Constants.BUNDLE_SYMBOLICNAME);
			final Matcher    matcher        = BSN_P.matcher(value);

			if (!matcher.matches()) {
				throw new IllegalArgumentException("No proper Bundle-SymbolicName in bundle: " + value);
			}

			final String bsn = matcher.group(1);

			String versionString = mainAttributes.getValue(Constants.BUNDLE_VERSION);
			if (versionString == null) {
				versionString = "0";
			}

			final Version version = Version.parseVersion(versionString);

			return new AbstractMap.SimpleEntry<>(bsn, version);
		}
	}

	private Set<Bundle> findBundles(final String bsn, final Version version) {

		return Stream.of(context.getBundles()).filter(b -> bsn.equals(b.getSymbolicName()))
		        .filter(b -> version == null || version.equals(b.getVersion())).collect(Collectors.toSet());
	}

	private String getLocation(final byte[] data) throws IOException {
		final Map.Entry<String, Version> entry   = getIdentity(data);
		final Set<Bundle>                bundles = findBundles(entry.getKey(), null);
		switch (bundles.size()) {
		case 0:
			return "manual:" + entry.getKey();

		case 1:
			return bundles.iterator().next().getLocation();

		default:
			throw new IllegalArgumentException(
			        "No location specified but there are multiple bundles with the same bsn " + entry.getKey() + ": " + bundles);
		}
	}

	@Override
	public List<XBundleDTO> getAllBundles() {
		return XBundleAdmin.get(context);
	}

	@Override
	public List<XComponentDTO> getAllComponents() {
		final boolean isScrAvailable = PackageWirings.isScrWired(context);
		if (isScrAvailable) {
			final XComponentAdmin scrAdmin = new XComponentAdmin(scrTracker.getService());
			return scrAdmin.getComponents();
		}
		return Collections.emptyList();
	}

	@Override
	public List<XConfigurationDTO> getAllConfigurations() {
		final boolean isConfigAdminAvailable = PackageWirings.isConfigAdminWired(context);
		final boolean isMetatypeAvailable    = PackageWirings.isMetatypeWired(context);

		final List<XConfigurationDTO> configs = new ArrayList<>();
		if (isConfigAdminAvailable) {
			final XConfigurationAdmin configAdmin = new XConfigurationAdmin(context, configAdminTracker.getService(),
			        metatypeTracker.getService());
			configs.addAll(configAdmin.getConfigurations());
		}
		if (isMetatypeAvailable) {
			final XMetaTypeAdmin metatypeAdmin = new XMetaTypeAdmin(context, configAdminTracker.getService(), metatypeTracker.getService());
			configs.addAll(metatypeAdmin.getConfigurations());
		}
		return configs;
	}

	@Override
	public List<XPropertyDTO> getAllProperties() {
		return XPropertyAdmin.get(context);
	}

	@Override
	public List<XServiceDTO> getAllServices() {
		return XServiceAdmin.get(context);
	}

	@Override
	public List<XThreadDTO> getAllThreads() {
		return XThreadAdmin.get();
	}

	@Override
	public XResultDTO enableComponentById(final long id) {
		final boolean isScrAvailable = PackageWirings.isScrWired(context);
		if (isScrAvailable) {
			final XComponentAdmin scrAdmin = new XComponentAdmin(scrTracker.getService());
			return scrAdmin.enableComponent(id);
		}
		return createResult(SKIPPED, "SCR bundle is not installed to process this request");
	}

	@Override
	public XResultDTO enableComponentByName(final String name) {
		requireNonNull(name, "Component name cannot be null");

		final boolean isScrAvailable = PackageWirings.isScrWired(context);
		if (isScrAvailable) {
			final XComponentAdmin scrAdmin = new XComponentAdmin(scrTracker.getService());
			return scrAdmin.enableComponent(name);
		}
		return createResult(SKIPPED, "SCR bundle is not installed to process this request");
	}

	@Override
	public XResultDTO disableComponentById(final long id) {
		final boolean isScrAvailable = PackageWirings.isScrWired(getContext());
		if (isScrAvailable) {
			final XComponentAdmin scrAdmin = new XComponentAdmin(scrTracker.getService());
			return scrAdmin.disableComponent(id);
		}
		return createResult(SKIPPED, "SCR bundle is not installed to process this request");
	}

	@Override
	public XResultDTO disableComponentByName(final String name) {
		requireNonNull(name, "Component name cannot be null");

		final boolean isScrAvailable = PackageWirings.isScrWired(getContext());
		if (isScrAvailable) {
			final XComponentAdmin scrAdmin = new XComponentAdmin(scrTracker.getService());
			return scrAdmin.disableComponent(name);
		}
		return createResult(SKIPPED, "SCR bundle is not installed to process this request");
	}

	@Override
	public XResultDTO createOrUpdateConfiguration(final String pid, final List<ConfigValue> newProperties) {
		requireNonNull(newProperties, "Configuration properties cannot be null");
		try {
			final Map<String, Object> finalProperties = parseProperties(newProperties);
			return createOrUpdateConfiguration(pid, finalProperties);
		} catch (final Exception e) {
			return createResult(ERROR, "One or more configuration properties cannot be converted to the requested type");
		}
	}

	@Override
	public XResultDTO createOrUpdateConfiguration(final String pid, final Map<String, Object> newProperties) {
		requireNonNull(pid, "Configuration PID cannot be null");
		requireNonNull(newProperties, "Configuration properties cannot be null");

		final boolean isConfigAdminAvailable = PackageWirings.isConfigAdminWired(context);
		if (isConfigAdminAvailable) {
			final XConfigurationAdmin configAdmin = new XConfigurationAdmin(context, configAdminTracker.getService(),
			        metatypeTracker.getService());
			try {
				return configAdmin.createOrUpdateConfiguration(pid, newProperties);
			} catch (final Exception e) {
				return createResult(ERROR, "One or more configuration properties cannot be converted to the requested type");
			}
		}
		return createResult(SKIPPED, "ConfigAdmin bundle is not installed to process this request");
	}

	@Override
	public Map<String, XResultDTO> createOrUpdateConfigurations(final Map<String, Map<String, Object>> configurations) {
		final Map<String, XResultDTO> results = new HashMap<>();
		configurations.forEach((k, v) -> results.put(k, createOrUpdateConfiguration(k, v)));
		return results;
	}

	@Override
	public XResultDTO deleteConfiguration(final String pid) {
		requireNonNull(pid, "Configuration PID cannot be null");

		final boolean isConfigAdminAvailable = PackageWirings.isConfigAdminWired(getContext());
		if (isConfigAdminAvailable) {
			final XConfigurationAdmin configAdmin = new XConfigurationAdmin(context, configAdminTracker.getService(),
			        metatypeTracker.getService());
			return configAdmin.deleteConfiguration(pid);
		}
		return createResult(SKIPPED, "ConfigAdmin bundle is not installed to process this request");
	}

	@Override
	public XResultDTO createFactoryConfiguration(final String factoryPid, final List<ConfigValue> newProperties) {
		requireNonNull(factoryPid, "Configuration factory PID cannot be null");
		requireNonNull(newProperties, "Configuration properties cannot be null");

		final boolean isConfigAdminAvailable = PackageWirings.isConfigAdminWired(context);

		if (isConfigAdminAvailable) {
			final XConfigurationAdmin configAdmin = new XConfigurationAdmin(context, configAdminTracker.getService(),
			        metatypeTracker.getService());
			try {
				final Map<String, Object> finalProperties = parseProperties(newProperties);
				return configAdmin.createFactoryConfiguration(factoryPid, finalProperties);
			} catch (final Exception e) {
				return createResult(ERROR, "One or configuration properties cannot be converted to the requested type");
			}
		}
		return createResult(SKIPPED, "ConfigAdmin bundle is not installed to process this request");
	}

	@Override
	public void sendEvent(final String topic, final List<ConfigValue> properties) {
		requireNonNull(topic, "Event topic cannot be null");
		requireNonNull(properties, "Event properties cannot be null");

		final boolean isEventAdminAvailable = PackageWirings.isEventAdminWired(context);
		if (isEventAdminAvailable) {
			final XEventAdmin eventAdmin = new XEventAdmin(eventAdminTracker.getService());
			try {
				final Map<String, Object> finalProperties = parseProperties(properties);
				eventAdmin.sendEvent(topic, finalProperties);
			} catch (final Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void postEvent(final String topic, final List<ConfigValue> properties) {
		requireNonNull(topic, "Event topic cannot be null");
		requireNonNull(properties, "Event properties cannot be null");

		final boolean isEventAdminAvailable = PackageWirings.isEventAdminWired(context);
		if (isEventAdminAvailable) {
			final XEventAdmin eventAdmin = new XEventAdmin(eventAdminTracker.getService());
			try {
				final Map<String, Object> finalProperties = parseProperties(properties);
				eventAdmin.postEvent(topic, finalProperties);
			} catch (final Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public XMemoryInfoDTO getMemoryInfo() {
		final XMemoryInfoDTO dto = new XMemoryInfoDTO();

		dto.uptime      = getSystemUptime();
		dto.maxMemory   = Runtime.getRuntime().maxMemory();
		dto.freeMemory  = Runtime.getRuntime().freeMemory();
		dto.totalMemory = Runtime.getRuntime().totalMemory();

		return dto;
	}

	@Override
	public Set<String> getGogoCommands() {
		return new HashSet<>(gogoCommands);
	}

	@Override
	public Object executeExtension(final String name, final Map<String, Object> context) {
		if (!agentExtensions.containsKey(name)) {
			throw new RuntimeException("Agent extension with name '" + name + "' doesn't exist");
		}
		try {
			return agentExtensions.get(name).execute(context);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String exec(final String command) {
		try {
			final CommandLine                 cmdLine       = CommandLine.parse(command);
			final DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

			final ExecuteWatchdog watchdog = new ExecuteWatchdog(30_000L);
			final Executor        executor = new DefaultExecutor();
			executor.setExitValue(1);
			executor.setWatchdog(watchdog);
			final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

			final PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
			executor.setStreamHandler(streamHandler);
			executor.execute(cmdLine, resultHandler);

			resultHandler.waitFor();
			return outputStream.toString();
		} catch (final Exception e) {
			Thread.currentThread().interrupt();
			return Exceptions.toString(e);
		}
	}

	@Override
	public Set<XBundleDTO> getClassloaderLeaks() {
		return leakDetector.getSuspiciousBundles();
	}

	@Override
	public List<XHttpComponentDTO> getHttpComponents() {
		final boolean isHttpServiceRuntimeWired = PackageWirings.isHttpServiceRuntimeWired(context);
		if (isHttpServiceRuntimeWired) {
			final Object service = httpServiceRuntimeTracker.getService();
			if (service == null) {
				return Collections.emptyList();
			}
			final XHttpAdmin httpAdmin = new XHttpAdmin(service);
			return httpAdmin.runtime();
		}
		return Collections.emptyList();
	}

	@Override
	public XHeapUsageDTO getHeapUsage() {
		final boolean isJMXWired = PackageWirings.isJmxWired(context);
		return isJMXWired ? XHeapAdmin.init() : null;
	}

	@Override
	public void gc() {
		System.gc();
	}

	@Override
	public XHeapdumpDTO heapdump() throws Exception {
		final boolean isJMXWired = PackageWirings.isJmxWired(context);
		return isJMXWired ? XHeapAdmin.heapdump() : null;
	}

	@Override
	public boolean isConfigAdminAvailable() {
		return PackageWirings.isConfigAdminWired(context);
	}

	private long getSystemUptime() {
		final boolean isJMXWired = PackageWirings.isJmxWired(context);
		return isJMXWired ? ManagementFactory.getRuntimeMXBean().getUptime() : 0;
	}

	public static <K, V> Map<K, V> valueOf(final Dictionary<K, V> dictionary) {
		if (dictionary == null) {
			return null;
		}
		final Map<K, V>      map  = new HashMap<>(dictionary.size());
		final Enumeration<K> keys = dictionary.keys();
		while (keys.hasMoreElements()) {
			final K key = keys.nextElement();
			map.put(key, dictionary.get(key));
		}
		return map;
	}

	public static XResultDTO createResult(final int result, final String response) {
		final XResultDTO dto = new XResultDTO();

		dto.result   = result;
		dto.response = response;

		return dto;
	}

	private BundleDTO installBundleWithData(String location, final byte[] data, final int startLevel, final boolean shouldRefresh)
	        throws IOException, BundleException, InterruptedException {
		requireNonNull(data);

		Bundle installedBundle;

		if (location == null) {
			location = getLocation(data);
		}

		try (InputStream stream = new ByteBufferInputStream(data)) {
			installedBundle = context.getBundle(location);
			if (installedBundle == null) {
				installedBundle = context.installBundle(location, stream);
				installedBundle.adapt(BundleStartLevel.class).setStartLevel(startLevel);
			} else {
				installedBundle.update(stream);
				if (shouldRefresh) {
					refresh(true);
				}
			}
		}
		return toDTO(installedBundle);
	}

	private Map<String, Object> parseProperties(final List<ConfigValue> newProperties) throws Exception {
		final Map<String, Object> properties = new HashMap<>();
		for (final ConfigValue entry : newProperties) {
			final Object convertedValue = convert(entry);
			properties.put(entry.key, convertedValue);
		}
		return properties;
	}

	public static Object convert(final ConfigValue entry) throws Exception {
		final Object            source = entry.value;
		final XAttributeDefType type   = entry.type;
		switch (type) {
		case STRING_ARRAY:
			return Converter.cnv(String[].class, source);
		case STRING_LIST:
			return Converter.cnv(new TypeReference<List<String>>() {
			}, source);
		case INTEGER_ARRAY:
			return Converter.cnv(int[].class, source);
		case INTEGER_LIST:
			return Converter.cnv(new TypeReference<List<Integer>>() {
			}, source);
		case BOOLEAN_ARRAY:
			return Converter.cnv(boolean[].class, source);
		case BOOLEAN_LIST:
			return Converter.cnv(new TypeReference<List<Boolean>>() {
			}, source);
		case DOUBLE_ARRAY:
			return Converter.cnv(double[].class, source);
		case DOUBLE_LIST:
			return Converter.cnv(new TypeReference<List<Double>>() {
			}, source);
		case FLOAT_ARRAY:
			return Converter.cnv(float[].class, source);
		case FLOAT_LIST:
			return Converter.cnv(new TypeReference<List<Float>>() {
			}, source);
		case CHAR_ARRAY:
			return Converter.cnv(char[].class, source);
		case CHAR_LIST:
			return Converter.cnv(new TypeReference<List<Character>>() {
			}, source);
		case LONG_ARRAY:
			return Converter.cnv(long[].class, source);
		case LONG_LIST:
			return Converter.cnv(new TypeReference<List<Long>>() {
			}, source);
		default:
			return Converter.cnv(XAttributeDefType.clazz(type), source);
		}
	}

}
