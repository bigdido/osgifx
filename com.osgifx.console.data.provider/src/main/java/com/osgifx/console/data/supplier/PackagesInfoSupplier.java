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
package com.osgifx.console.data.supplier;

import static com.osgifx.console.data.supplier.PackagesInfoSupplier.PACKAGES_ID;
import static com.osgifx.console.supervisor.Supervisor.AGENT_DISCONNECTED_EVENT_TOPIC;
import static javafx.collections.FXCollections.observableArrayList;
import static javafx.collections.FXCollections.synchronizedObservableList;

import java.util.List;
import java.util.Map;

import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.LoggerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.osgifx.console.agent.dto.XBundleDTO;
import com.osgifx.console.agent.dto.XPackageDTO;
import com.osgifx.console.data.provider.PackageDTO;
import com.osgifx.console.supervisor.Supervisor;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

@Component
@SupplierID(PACKAGES_ID)
@EventTopics(AGENT_DISCONNECTED_EVENT_TOPIC)
public final class PackagesInfoSupplier implements RuntimeInfoSupplier, EventHandler {

	public static final String PACKAGES_ID = "packages";

	@Reference
	private LoggerFactory     factory;
	@Reference
	private Supervisor        supervisor;
	@Reference
	private ThreadSynchronize threadSync;
	private FluentLogger      logger;

	private final ObservableList<PackageDTO> packages = synchronizedObservableList(observableArrayList());

	@Activate
	void activate() {
		logger = FluentLogger.of(factory.createLogger(getClass().getName()));
	}

	@Override
	public synchronized void retrieve() {
		logger.atInfo().log("Retrieving packages info from remote runtime");
		final var agent = supervisor.getAgent();
		if (agent == null) {
			logger.atWarning().log("Agent is not connected");
			return;
		}
		packages.setAll(preparePackages(agent.getAllBundles()));
		logger.atInfo().log("Packages info retrieved successfully");
	}

	@Override
	public synchronized ObservableList<?> supply() {
		return packages;
	}

	@Override
	public void handleEvent(final Event event) {
		threadSync.asyncExec(packages::clear);
	}

	private synchronized ObservableList<PackageDTO> preparePackages(final List<XBundleDTO> bundles) {
		final List<PackageDTO>        packages      = Lists.newArrayList();
		final Map<String, PackageDTO> finalPackages = Maps.newHashMap();   // key: package name, value: PackageDTO

		for (final XBundleDTO bundle : bundles) {
			final var exportedPackages = toPackageDTOs(bundle.exportedPackages);
			final var importedPackages = toPackageDTOs(bundle.importedPackages);

			exportedPackages.forEach(p -> p.exporters.add(bundle));
			importedPackages.forEach(p -> p.importers.add(bundle));

			packages.addAll(exportedPackages);
			packages.addAll(importedPackages);
		}
		for (final PackageDTO pkg : packages) {
			final var key = pkg.name + ":" + pkg.version;
			if (!finalPackages.containsKey(key)) {
				finalPackages.put(key, pkg);
			} else {
				final var packageDTO = finalPackages.get(key);

				packageDTO.exporters.addAll(pkg.exporters);
				packageDTO.importers.addAll(pkg.importers);
			}
		}
		for (final PackageDTO pkg : finalPackages.values()) {
			if (pkg.exporters.size() > 1) {
				pkg.isDuplicateExport = true;
			}
		}
		return FXCollections.observableArrayList(finalPackages.values());
	}

	private List<PackageDTO> toPackageDTOs(final List<XPackageDTO> exportedPackages) {
		return exportedPackages.stream().map(this::toPackageDTO).toList();
	}

	private PackageDTO toPackageDTO(final XPackageDTO xpkg) {
		final var pkg = new PackageDTO();

		pkg.name    = xpkg.name;
		pkg.version = xpkg.version;

		return pkg;
	}

}
