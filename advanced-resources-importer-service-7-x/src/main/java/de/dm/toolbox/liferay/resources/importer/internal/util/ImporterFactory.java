package de.dm.toolbox.liferay.resources.importer.internal.util;

import com.liferay.osgi.service.tracker.collections.list.ServiceTrackerList;
import com.liferay.osgi.service.tracker.collections.list.ServiceTrackerListFactory;
import com.liferay.osgi.service.tracker.collections.map.PropertyServiceReferenceComparator;
import de.dm.toolbox.liferay.resources.importer.Importer;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component(
        immediate = true,
        service = ImporterFactory.class
)
public class ImporterFactory {

    private ServiceTrackerList<Importer, Importer> importers;

    public ServiceTrackerList<Importer, Importer> getImporters() {
        return importers;
    }

    @Activate
    protected void activate(final BundleContext bundleContext) {
        importers =
                ServiceTrackerListFactory.open(
                        bundleContext,
                        Importer.class,
                        null,
                        new PropertyServiceReferenceComparator<Importer>(
                                "importer.order"
                        ).reversed()
                );
    }

    @Deactivate
    protected void deactivate() {
        importers.close();
    }

}
