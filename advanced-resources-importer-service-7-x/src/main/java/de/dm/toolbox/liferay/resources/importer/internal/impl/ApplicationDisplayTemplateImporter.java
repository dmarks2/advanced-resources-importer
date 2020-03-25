package de.dm.toolbox.liferay.resources.importer.internal.impl;

import com.liferay.asset.kernel.service.AssetCategoryLocalService;
import com.liferay.asset.kernel.service.AssetVocabularyLocalService;
import com.liferay.dynamic.data.mapping.model.DDMTemplate;
import com.liferay.dynamic.data.mapping.model.DDMTemplateConstants;
import com.liferay.dynamic.data.mapping.service.DDMTemplateLocalService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.ClassName;
import com.liferay.portal.kernel.service.ClassNameLocalService;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portlet.display.template.PortletDisplayTemplate;
import com.liferay.registry.collections.ServiceTrackerCollections;
import com.liferay.registry.collections.ServiceTrackerMap;
import de.dm.toolbox.liferay.resources.importer.BaseImporter;
import de.dm.toolbox.liferay.resources.importer.ADT;
import de.dm.toolbox.liferay.resources.importer.Importer;
import de.dm.toolbox.liferay.resources.importer.internal.util.ImporterUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component(
        immediate = true,
        property = {
                "importer.order=70"
        },
        service = Importer.class
)
public class ApplicationDisplayTemplateImporter extends BaseImporter {

    private static final Log log = LogFactoryUtil.getLog(ApplicationDisplayTemplateImporter.class);

    private static final String APPLICATION_DISPLAY_TEMPLATE_DIR_NAME = "/templates/application_display";

    private ServiceTrackerMap<String, ADT> adts;

    @Reference
    private ClassNameLocalService classNameLocalService;

    @Reference
    private DDMTemplateLocalService ddmTemplateLocalService;

    @Activate
    public void activate() {
        adts = ServiceTrackerCollections.openSingleValueMap(
                ADT.class,
                "key"
        );
    }

    @Deactivate
    public void deactivate() {
        adts.close();
    }

    @Override
    protected void doRunImport(ServletContext servletContext, long companyId) throws Exception {
        addApplicationDisplayTemplates(resourcesDir, APPLICATION_DISPLAY_TEMPLATE_DIR_NAME, servletContext);
    }

    private void addApplicationDisplayTemplates(String resourcesDir, String dirName, ServletContext servletContext) throws IOException, PortalException {
        Set<String> keys = adts.keySet();

        String adtPath = ImporterUtil.getResourcePath(resourcesDir, dirName);

        for (String key : keys) {
            if (log.isDebugEnabled()) {
                log.debug("Running Application Display Templates Importer for type " + key);
            }
            Set<String> resoucePaths = servletContext.getResourcePaths(adtPath + StringPool.SLASH + key);

            if (resoucePaths == null) {
                if (log.isDebugEnabled()) {
                    log.debug("No Application Display Templates found for type " + key + ". Skipping...");
                }

                continue;
            }

            for (String resourcePath : resoucePaths) {
                URL url = servletContext.getResource(resourcePath);

                URLConnection urlConnection = url.openConnection();

                String script = StringUtil.read(urlConnection.getInputStream());

                if (Validator.isNull(script)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Application Display Templates found for type " + key + ", but file is empty. Skipping...");
                    }

                    continue;
                }

                File file = new File(resourcePath);

                ADT adt = adts.getService(key);

                if (adt.isEnabled()) {

                    String className = adt.getClassName();

                    ClassName classNameValue = classNameLocalService.fetchClassName(className);

                    if (classNameValue == null) {
                        if (log.isWarnEnabled()) {
                            log.warn("Unable to import Application Display Templates for class " + className + " because it is unknown by Liferay.");
                        }

                        continue;
                    }

                    addApplicationDisplayTemplate(script, file, classNameValue.getClassNameId());
                }
            }
        }
    }

    private void addApplicationDisplayTemplate(String script, File file, long classNameId) throws PortalException {
        String language = getDDMTemplateLanguage(file.getName());

        String fileName = FileUtil.stripExtension(file.getName());

        String name = fileName;

        String key = getKey(fileName);

        DDMTemplate ddmTemplate = ddmTemplateLocalService.fetchTemplate(groupId, classNameId, key);

        long portletDisplayTemplateClassNameId = portal.getClassNameId(PortletDisplayTemplate.class);

        if (Validator.isNull(ddmTemplate)) {
            if (log.isInfoEnabled()) {
                log.info("Adding Application Display Template " + fileName);
            }

            Map<Locale, String> titleMap = getMap(name);

            //set cacheable to false by default
            ddmTemplateLocalService.addTemplate(
                    userId,
                    groupId,
                    classNameId,
                    0,
                    portletDisplayTemplateClassNameId,
                    key,
                    titleMap,
                    null,
                    DDMTemplateConstants.TEMPLATE_TYPE_DISPLAY,
                    StringPool.BLANK,
                    language,
                    script,
                    false,
                    false,
                    StringPool.BLANK,
                    null,
                    serviceContext
            );

        } else {
            if (log.isInfoEnabled()) {
                log.info("Updating Application Display Template " + fileName);
            }

            ddmTemplateLocalService.updateTemplate(
                    userId,
                    ddmTemplate.getTemplateId(),
                    ddmTemplate.getClassPK(),
                    ddmTemplate.getNameMap(),
                    null,
                    DDMTemplateConstants.TEMPLATE_TYPE_DISPLAY,
                    StringPool.BLANK,
                    language,
                    script,
                    ddmTemplate.getCacheable(),
                    serviceContext
            );
        }
    }

    @Override
    @Reference
    protected void setUserLocalService(UserLocalService userLocalService) {
        super.setUserLocalService(userLocalService);
    }

    @Override
    @Reference
    protected void setGroupLocalService(GroupLocalService groupLocalService) {
        super.setGroupLocalService(groupLocalService);
    }

    @Override
    @Reference
    protected void setAssetVocabularyLocalService(AssetVocabularyLocalService assetVocabularyLocalService) {
        super.setAssetVocabularyLocalService(assetVocabularyLocalService);
    }

    @Override
    @Reference
    protected void setAssetCategoryLocalService(AssetCategoryLocalService assetCategoryLocalService) {
        super.setAssetCategoryLocalService(assetCategoryLocalService);
    }

    @Override
    @Reference
    protected void setPortal(Portal portal) {
        super.setPortal(portal);
    }

}

