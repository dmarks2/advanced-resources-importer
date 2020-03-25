package de.dm.toolbox.liferay.resources.importer.internal.messaging;

import com.liferay.exportimport.kernel.lar.ExportImportThreadLocal;
import com.liferay.osgi.service.tracker.collections.list.ServiceTrackerList;
import com.liferay.osgi.service.tracker.collections.map.ServiceTrackerMap;
import com.liferay.osgi.service.tracker.collections.map.ServiceTrackerMapFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.util.GetterUtil;
import de.dm.toolbox.liferay.resources.importer.internal.util.ImporterFactory;
import de.dm.toolbox.liferay.resources.importer.Importer;
import de.dm.toolbox.liferay.resources.importer.internal.util.AssetsUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.ServletContext;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;

@Component(
        immediate = true,
        service = AdvancedResourcesImporterHotDeployMessageListener.class
)
public class AdvancedResourcesImporterHotDeployMessageListener {

    private static final String ADVANCED_RESOURCES_IMPORTER = "Advanced-Resources-Importer";
    private static final String ADVANCED_RESOURCES_IMPORTER_GROUP = "Advanced-Resources-Importer-Group";

    private static final Log log = LogFactoryUtil.getLog(AdvancedResourcesImporterHotDeployMessageListener.class);

    private ServiceTrackerMap<String, ServletContext> servletContexts;

    @Reference
    private ImporterFactory importerFactory;

    @Reference
    private CompanyLocalService companyLocalService;

    @Reference
    private AssetsUtil assetsUtil;

    @Activate
    protected void activate(final BundleContext bundleContext) {
        servletContexts = ServiceTrackerMapFactory.openSingleValueMap(
                bundleContext,
                ServletContext.class,
                null,
                (serviceReference, emitter) -> {
                    try {
                        ServletContext servletContext = bundleContext.getService(serviceReference);

                        String servletContextName = GetterUtil.getString(servletContext.getServletContextName());

                        //use servletContextName as the key for the service tracker map
                        emitter.emit(servletContextName);

                        if (log.isDebugEnabled()) {
                            log.debug("Tracking servlet context " + servletContextName);
                        }

                        runImporter(servletContext);
                    } finally {
                        bundleContext.ungetService(serviceReference);
                    }
                }
        );
    }

    private void runImporter(ServletContext servletContext) {
        BundleContext bundleContext = (BundleContext) servletContext.getAttribute("osgi-bundlecontext");
        if (bundleContext != null) {
            if (log.isDebugEnabled()) {
                log.debug("Found bundlecontext for " + servletContext.getServletContextName());
            }
            Bundle bundle = bundleContext.getBundle();
            if (bundle != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Found bundle for " + servletContext.getServletContextName());
                }

                Dictionary<String, String> headers = bundle.getHeaders();

                if (log.isDebugEnabled()) {
                    log.debug("headers for " + servletContext.getServletContextName() + ": " + String.valueOf(headers));
                }

                String advancedResourcesImporterValue = headers.get(ADVANCED_RESOURCES_IMPORTER);

                boolean isAdvancedResourcesImporter = GetterUtil.getBoolean(advancedResourcesImporterValue, false);
                String groupKey = GetterUtil.getString(headers.get(ADVANCED_RESOURCES_IMPORTER_GROUP));

                if (isAdvancedResourcesImporter) {
                    if (log.isInfoEnabled()) {
                        log.info("Running advanced resources importer for " + servletContext.getServletContextName());
                    }

                    List<Company> companies = companyLocalService.getCompanies();

                    try {
                        ExportImportThreadLocal.setLayoutImportInProcess(true);
                        ExportImportThreadLocal.setPortletImportInProcess(true);

                        for (Company company : companies) {
                            importResources(company, servletContext, groupKey);
                        }
                    }
                    finally {
                        ExportImportThreadLocal.setLayoutImportInProcess(false);
                        ExportImportThreadLocal.setPortletImportInProcess(false);
                    }
                }
            }
        }
    }

    private void importResources(Company company, ServletContext servletContext, String groupKey) {
        long companyId = CompanyThreadLocal.getCompanyId();

        try {
            CompanyThreadLocal.setCompanyId(company.getCompanyId());

            ServiceTrackerList<Importer, Importer> importers = importerFactory.getImporters();

            Map<String, JSONObject> assetJSONObjectMap = assetsUtil.getAssetJSONObjectMap(servletContext, company.getCompanyId(), groupKey);

            for (Importer importer : importers) {
                if (log.isDebugEnabled()) {
                    log.debug("Running importer " + importer.getClass().getName());
                }
                importer.runImport(servletContext, company.getCompanyId(), groupKey, assetJSONObjectMap);
            }
        } catch (Exception e) {
            log.error("Error importing resources from " + servletContext.getServletContextName() + " into " + groupKey, e);
        } finally {
            CompanyThreadLocal.setCompanyId(companyId);
        }
    }

    @Deactivate
    protected void deactivate() {
        servletContexts.close();
    }
}
