package in.bytehue.osgifx.console.agent.provider;

import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.framework.Constants.SERVICE_ID;
import static org.osgi.framework.namespace.HostNamespace.HOST_NAMESPACE;
import static org.osgi.framework.wiring.BundleRevision.PACKAGE_NAMESPACE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

import in.bytehue.osgifx.console.agent.dto.XBundleDTO;
import in.bytehue.osgifx.console.agent.dto.XBundleInfoDTO;
import in.bytehue.osgifx.console.agent.dto.XPackageDTO;
import in.bytehue.osgifx.console.agent.dto.XPackageDTO.XpackageType;
import in.bytehue.osgifx.console.agent.dto.XServiceInfoDTO;

public class XBundleAdmin {

    private XBundleAdmin() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static List<XBundleDTO> get(final BundleContext context) {
        requireNonNull(context);
        return Stream.of(context.getBundles()).map(XBundleAdmin::toDTO).collect(toList());
    }

    private static XBundleDTO toDTO(final Bundle bundle) {
        final XBundleDTO dto = new XBundleDTO();

        dto.id                     = bundle.getBundleId();
        dto.state                  = findState(bundle.getState());
        dto.symbolicName           = bundle.getSymbolicName();
        dto.version                = bundle.getVersion().toString();
        dto.location               = bundle.getLocation();
        dto.category               = getHeader(bundle, Constants.BUNDLE_CATEGORY);
        dto.isFragment             = getHeader(bundle, Constants.FRAGMENT_HOST) != null;
        dto.lastModified           = bundle.getLastModified();
        dto.documentation          = getHeader(bundle, Constants.BUNDLE_DOCURL);
        dto.vendor                 = getHeader(bundle, Constants.BUNDLE_VENDOR);
        dto.description            = getHeader(bundle, Constants.BUNDLE_DESCRIPTION);
        dto.startLevel             = bundle.adapt(BundleStartLevel.class).getStartLevel();
        dto.exportedPackages       = getExportedPackages(bundle);
        dto.importedPackages       = getImportedPackages(bundle);
        dto.wiredBundles           = getWiredBundles(bundle);
        dto.registeredServices     = getRegisteredServices(bundle);
        dto.manifestHeaders        = toMap(bundle.getHeaders());
        dto.usedServices           = getUsedServices(bundle);
        dto.hostBundles            = getHostBundles(bundle);
        dto.fragmentsAttached      = getAttachedFragements(bundle);
        dto.revisions              = getBundleRevisions(bundle);
        dto.isPersistentlyStarted  = getPeristentlyStarted(bundle);
        dto.isActivationPolicyUsed = getActivationPolicyUsed(bundle);

        return dto;
    }

    private static boolean getActivationPolicyUsed(final Bundle bundle) {
        final BundleStartLevel startLevel = bundle.adapt(BundleStartLevel.class);
        if (startLevel != null) {
            return startLevel.isActivationPolicyUsed();
        }
        return false;
    }

    private static boolean getPeristentlyStarted(final Bundle bundle) {
        final BundleStartLevel startLevel = bundle.adapt(BundleStartLevel.class);
        if (startLevel != null) {
            return startLevel.isPersistentlyStarted();
        }
        return false;
    }

    private static int getBundleRevisions(final Bundle bundle) {
        final BundleRevisions revisions = bundle.adapt(BundleRevisions.class);
        return revisions.getRevisions().size();
    }

    private static List<XBundleInfoDTO> getHostBundles(final Bundle bundle) {
        final List<XBundleInfoDTO> attachedHosts = new ArrayList<>();
        final BundleWiring         wiring        = bundle.adapt(BundleWiring.class);

        // wiring can be null for non-started installed bundles
        if (wiring == null) {
            return Collections.emptyList();
        }
        for (final BundleWire wire : wiring.getRequiredWires(HOST_NAMESPACE)) {
            final Bundle         b   = wire.getProviderWiring().getBundle();
            final XBundleInfoDTO dto = new XBundleInfoDTO();

            dto.id           = b.getBundleId();
            dto.symbolicName = b.getSymbolicName();
            attachedHosts.add(dto);
        }
        return attachedHosts;
    }

    private static List<XBundleInfoDTO> getAttachedFragements(final Bundle bundle) {
        final List<XBundleInfoDTO> attachedFragments = new ArrayList<>();
        final BundleWiring         wiring            = bundle.adapt(BundleWiring.class);

        // wiring can be null for non-started installed bundles
        if (wiring == null) {
            return Collections.emptyList();
        }
        for (final BundleWire wire : wiring.getProvidedWires(HOST_NAMESPACE)) {
            final Bundle         b   = wire.getRequirerWiring().getBundle();
            final XBundleInfoDTO dto = new XBundleInfoDTO();

            dto.id           = b.getBundleId();
            dto.symbolicName = b.getSymbolicName();

            attachedFragments.add(dto);
        }
        return attachedFragments;
    }

    private static List<XServiceInfoDTO> getUsedServices(final Bundle bundle) {
        final List<XServiceInfoDTO> services     = new ArrayList<>();
        final ServiceReference<?>[] usedServices = bundle.getServicesInUse();

        if (usedServices == null) {
            return Collections.emptyList();
        }
        for (final ServiceReference<?> service : usedServices) {
            final String[]     objectClass = (String[]) service.getProperty(OBJECTCLASS);
            final List<String> objectClazz = Arrays.asList(objectClass);

            services.addAll(prepareServiceInfo(service, objectClazz));
        }
        return services;
    }

    private static List<XServiceInfoDTO> getRegisteredServices(final Bundle bundle) {
        final List<XServiceInfoDTO> services           = new ArrayList<>();
        final ServiceReference<?>[] registeredServices = bundle.getRegisteredServices();

        if (registeredServices == null) {
            return Collections.emptyList();
        }
        for (final ServiceReference<?> service : registeredServices) {
            final String[]     objectClasses = (String[]) service.getProperty(OBJECTCLASS);
            final List<String> objectClazz   = Arrays.asList(objectClasses);

            services.addAll(prepareServiceInfo(service, objectClazz));
        }
        return services;
    }

    private static List<XServiceInfoDTO> prepareServiceInfo(final ServiceReference<?> service, final List<String> objectClazz) {
        final List<XServiceInfoDTO> serviceInfos = new ArrayList<>();
        for (final String clz : objectClazz) {
            final XServiceInfoDTO dto = new XServiceInfoDTO();

            dto.id          = Long.parseLong(service.getProperty(SERVICE_ID).toString());
            dto.objectClass = clz;

            serviceInfos.add(dto);
        }
        return serviceInfos;
    }

    private static List<XBundleInfoDTO> getWiredBundles(final Bundle bundle) {
        final BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
        if (bundleWiring == null) {
            return Collections.emptyList();
        }
        final List<XBundleInfoDTO> bundles       = new ArrayList<>();
        final List<BundleWire>     providedWires = bundleWiring.getProvidedWires(null);
        final List<BundleWire>     requierdWires = bundleWiring.getRequiredWires(null);

        for (final BundleWire wire : providedWires) {
            final BundleRevision requirer = wire.getRequirer();
            final XBundleInfoDTO dto      = new XBundleInfoDTO();

            dto.id           = requirer.getBundle().getBundleId();
            dto.symbolicName = requirer.getSymbolicName();

            if (!containsWire(bundles, dto.symbolicName, dto.id)) {
                bundles.add(dto);
            }
        }
        for (final BundleWire wire : requierdWires) {
            final BundleRevision provider = wire.getProvider();
            final XBundleInfoDTO dto      = new XBundleInfoDTO();

            dto.id           = provider.getBundle().getBundleId();
            dto.symbolicName = provider.getSymbolicName();

            if (!containsWire(bundles, dto.symbolicName, dto.id)) {
                bundles.add(dto);
            }
        }
        return bundles;
    }

    private static boolean containsWire(final List<XBundleInfoDTO> bundles, final String bsn, final long id) {
        return bundles.stream().anyMatch(b -> b.symbolicName.equals(bsn) && b.id == id);
    }

    private static List<XPackageDTO> getImportedPackages(final Bundle bundle) {
        final BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
        if (bundleWiring == null) {
            return Collections.emptyList();
        }
        final List<BundleWire>  bundleWires      = bundleWiring.getRequiredWires(PACKAGE_NAMESPACE);
        final List<XPackageDTO> importedPackages = new ArrayList<>();

        for (final BundleWire bundleWire : bundleWires) {
            final String pkg     = (String) bundleWire.getCapability().getAttributes().get(PACKAGE_NAMESPACE);
            final String version = bundleWire.getCapability().getRevision().getVersion().toString();

            if (!hasPackage(importedPackages, pkg, version)) {
                final XPackageDTO dto = new XPackageDTO();

                dto.name    = pkg;
                dto.version = version;
                dto.type    = XpackageType.IMPORT;

                importedPackages.add(dto);
            }
        }
        return importedPackages;
    }

    private static List<XPackageDTO> getExportedPackages(final Bundle bundle) {
        final BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
        if (bundleWiring == null) {
            return Collections.emptyList();
        }
        final List<BundleWire>  bundleWires      = bundleWiring.getProvidedWires(PACKAGE_NAMESPACE);
        final List<XPackageDTO> exportedPackages = new ArrayList<>();

        for (final BundleWire bundleWire : bundleWires) {
            final String pkg     = (String) bundleWire.getCapability().getAttributes().get(PACKAGE_NAMESPACE);
            final String version = bundleWire.getCapability().getRevision().getVersion().toString();

            if (!hasPackage(exportedPackages, pkg, version)) {
                final XPackageDTO dto = new XPackageDTO();

                dto.name    = pkg;
                dto.version = version;
                dto.type    = XpackageType.EXPORT;

                exportedPackages.add(dto);
            }
        }
        return exportedPackages;
    }

    private static boolean hasPackage(final List<XPackageDTO> packages, final String name, final String version) {
        return packages.stream().anyMatch(o -> o.name.equals(name) && o.version.equals(version));
    }

    private static String findState(final int state) {
        switch (state) {
            case Bundle.ACTIVE:
                return "ACTIVE";
            case Bundle.INSTALLED:
                return "INSTALLED";
            case Bundle.RESOLVED:
                return "RESOLVED";
            case Bundle.STARTING:
                return "STARTING";
            case Bundle.STOPPING:
                return "STOPPING";
            case Bundle.UNINSTALLED:
                return "UNINSTALLED";
            default:
                break;
        }
        return null;
    }

    private static String getHeader(final Bundle bundle, final String header) {
        final Map<String, String> headers = toMap(bundle.getHeaders());
        return headers.get(header);
    }

    private static Map<String, String> toMap(final Dictionary<String, String> dictionary) {
        final List<String> keys = Collections.list(dictionary.keys());
        return keys.stream().collect(Collectors.toMap(identity(), dictionary::get));
    }

}