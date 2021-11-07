package in.bytehue.osgifx.console.ui.packages;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.osgi.framework.BundleContext;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import in.bytehue.osgifx.console.agent.dto.XBundleDTO;
import in.bytehue.osgifx.console.agent.dto.XPackageDTO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public final class PackageHelper {

    private PackageHelper() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static ObservableList<PackageDTO> prepareList(final ObservableList<XBundleDTO> bundles, final BundleContext context) {
        final List<PackageDTO>        packages      = Lists.newArrayList();
        final Map<String, PackageDTO> finalPackages = Maps.newHashMap();   // key: package name, value: PackageDTO

        for (final XBundleDTO bundle : bundles) {
            final List<PackageDTO> exportedPackages = toPackageDTOs(bundle.exportedPackages);
            final List<PackageDTO> importedPackages = toPackageDTOs(bundle.importedPackages);

            exportedPackages.forEach(p -> p.exporters.add(bundle));
            importedPackages.forEach(p -> p.importers.add(bundle));

            packages.addAll(exportedPackages);
            packages.addAll(importedPackages);
        }

        for (final PackageDTO pkg : packages) {
            final String key = pkg.name + ":" + pkg.version;
            if (!finalPackages.containsKey(key)) {
                finalPackages.put(key, pkg);
            } else {
                final PackageDTO packageDTO = finalPackages.get(key);

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

    private static List<PackageDTO> toPackageDTOs(final List<XPackageDTO> exportedPackages) {
        return exportedPackages.stream().map(PackageHelper::toPackageDTO).collect(Collectors.toList());
    }

    private static PackageDTO toPackageDTO(final XPackageDTO xpkg) {
        final PackageDTO pkg = new PackageDTO();

        pkg.name    = xpkg.name;
        pkg.version = xpkg.version;

        return pkg;
    }

}
