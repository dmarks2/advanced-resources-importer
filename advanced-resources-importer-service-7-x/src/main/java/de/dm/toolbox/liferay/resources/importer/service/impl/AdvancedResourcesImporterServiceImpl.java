package de.dm.toolbox.liferay.resources.importer.service.impl;

import com.liferay.osgi.service.tracker.collections.list.ServiceTrackerList;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.service.CompanyLocalService;
import de.dm.toolbox.liferay.resources.importer.Importer;
import de.dm.toolbox.liferay.resources.importer.internal.messaging.AdvancedResourcesImporterHotDeployMessageListener;
import de.dm.toolbox.liferay.resources.importer.internal.util.AssetsUtil;
import de.dm.toolbox.liferay.resources.importer.internal.util.ImporterFactory;
import de.dm.toolbox.liferay.resources.importer.service.AdvancedResourcesImporterService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.ServletContext;
import java.util.Map;

@Component(
        immediate = true,
        service = AdvancedResourcesImporterService.class
)
public class AdvancedResourcesImporterServiceImpl implements AdvancedResourcesImporterService {

    private static final Log log = LogFactoryUtil.getLog(AdvancedResourcesImporterServiceImpl.class);

    @Reference
    private ImporterFactory importerFactory;

    @Reference
    private CompanyLocalService companyLocalService;

    @Reference
    private AssetsUtil assetsUtil;

    public void importResources(Company company, ServletContext servletContext, String groupKey) {
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


}
