package de.dm.toolbox.liferay.resources.importer.internal.util;

import com.liferay.osgi.service.tracker.collections.list.ServiceTrackerList;
import com.liferay.osgi.service.tracker.collections.list.ServiceTrackerListFactory;
import com.liferay.osgi.service.tracker.collections.map.PropertyServiceReferenceComparator;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import de.dm.toolbox.liferay.resources.importer.Importer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

@Component(
        immediate = true,
        service = ImporterFactory.class
)
public class ImporterFactory {

    private static final Log log = LogFactoryUtil.getLog(ImporterFactory.class);

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
                        new ServiceTrackerCustomizer<Importer, Importer>() {
                            @Override
                            public Importer addingService(ServiceReference<Importer> reference) {
                                Importer importer = bundleContext.getService(reference);

                                if (log.isDebugEnabled()) {
                                    log.debug("Adding " + importer);
                                }

                                return importer;
                            }

                            @Override
                            public void modifiedService(ServiceReference<Importer> reference, Importer service) {
                            }

                            @Override
                            public void removedService(ServiceReference<Importer> reference, Importer service) {
                            }
                        },
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
