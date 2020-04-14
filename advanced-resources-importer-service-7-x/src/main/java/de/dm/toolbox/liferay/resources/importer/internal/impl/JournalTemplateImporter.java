package de.dm.toolbox.liferay.resources.importer.internal.impl;

import com.liferay.asset.kernel.service.AssetCategoryLocalService;
import com.liferay.asset.kernel.service.AssetVocabularyLocalService;
import com.liferay.document.library.kernel.service.DLAppLocalService;
import com.liferay.dynamic.data.mapping.model.DDMStructure;
import com.liferay.dynamic.data.mapping.model.DDMTemplate;
import com.liferay.dynamic.data.mapping.model.DDMTemplateConstants;
import com.liferay.dynamic.data.mapping.service.DDMStructureLocalService;
import com.liferay.dynamic.data.mapping.service.DDMTemplateLocalService;
import com.liferay.journal.model.JournalArticle;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import de.dm.toolbox.liferay.resources.importer.BaseImporter;
import de.dm.toolbox.liferay.resources.importer.Importer;
import de.dm.toolbox.liferay.resources.importer.internal.util.ImporterUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.ServletContext;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component(
        immediate = true,
        property = {
                "importer.order=60"
        },
        service = Importer.class
)
public class JournalTemplateImporter extends BaseImporter {

    private static final Log log = LogFactoryUtil.getLog(JournalTemplateImporter.class);

    private static final String JOURNAL_DDM_TEMPLATES_DIR_NAME = "/journal/templates/";

    @Reference
    private DDMStructureLocalService ddmStructureLocalService;

    @Reference
    private DDMTemplateLocalService ddmTemplateLocalService;

    @Reference
    private DLAppLocalService dlAppLocalService;

    @Override
    protected void doRunImport(ServletContext servletContext, long companyId) throws Exception {
        addDDMTemplates(resourcesDir, JOURNAL_DDM_TEMPLATES_DIR_NAME, servletContext);
    }

    protected void addDDMTemplates(String resourcesDir, String dirName, ServletContext servletContext) throws Exception {
        Set<String> resourcePaths = servletContext.getResourcePaths(ImporterUtil.getResourcePath(resourcesDir, dirName));

        if (resourcePaths == null) {
            if (log.isDebugEnabled()) {
                log.debug("No DDM Templates Directory found for " + servletContext.getServletContextName() + ". Skipping...");
            }
            return;
        }

        for (String resourcePath : resourcePaths) {
            if (log.isDebugEnabled()) {
                log.debug("Processing " + resourcePath);
            }
            if (resourcePath.endsWith(StringPool.SLASH)) {
                String ddmStructureKey = StringUtil.replaceLast(resourcePath, StringPool.SLASH, StringPool.BLANK);

                ddmStructureKey = FileUtil.getShortFileName(ddmStructureKey);

                ddmStructureKey = getKey(ddmStructureKey);

                if (log.isDebugEnabled()) {
                    log.debug("Calculated structure key is " + ddmStructureKey);
                }

                Set<String> templatePaths = servletContext.getResourcePaths(resourcePath);

                for (String templatePath : templatePaths) {
                    if (log.isDebugEnabled()) {
                        log.debug("Processing " + templatePath);
                    }
                    if (templatePath.endsWith(StringPool.SLASH)) {
                        continue;
                    }

                    String name = FileUtil.getShortFileName(templatePath);

                    URL url = servletContext.getResource(templatePath);

                    URLConnection urlConnection = url.openConnection();

                    addDDMTemplate(ddmStructureKey, name, urlConnection.getInputStream());
                }
            }
        }
    }

    private void addDDMTemplate(String ddmStructureKey, String fileName, InputStream inputStream) throws Exception {
        setServiceContext(fileName);

        String language = getDDMTemplateLanguage(fileName);

        fileName = FileUtil.stripExtension(fileName);

        String name = fileName;

        String key = getKey(fileName);

        String script = StringUtil.read(inputStream);

        if (Validator.isNull(script)) {
            if (log.isDebugEnabled()) {
                log.debug("Journal Template found with key " + key + ", but file is empty. Skipping...");
            }

            return;
        }

        long journalArticleClassNameId = portal.getClassNameId(JournalArticle.class);

        long ddmStructureClassNameId = portal.getClassNameId(DDMStructure.class);

        DDMStructure ddmStructure = ddmStructureLocalService.fetchStructure(
                groupId,
                journalArticleClassNameId,
                ddmStructureKey
        );

        if (Validator.isNull(ddmStructure)) {
            if (log.isWarnEnabled()) {
                log.warn("Structure " + ddmStructureKey + " not found. Skipping template " + fileName);
            }

            return;
        }

        DDMTemplate ddmTemplate = ddmTemplateLocalService.fetchTemplate(
                groupId,
                ddmStructureClassNameId,
                key
        );

        if (Validator.isNull(ddmTemplate)) {
            if (log.isInfoEnabled()) {
                log.info("Adding template " + fileName + " for structure " + ddmStructureKey);
            }

            Map<Locale, String> titleMap = getMap(name);

            //default cacheable to false
            ddmTemplateLocalService.addTemplate(
                    userId,
                    groupId,
                    ddmStructureClassNameId,
                    ddmStructure.getStructureId(),
                    journalArticleClassNameId,
                    key,
                    titleMap,
                    null,
                    DDMTemplateConstants.TEMPLATE_TYPE_DISPLAY,
                    null,
                    language,
                    replaceFileEntryURL(script, groupId, dlAppLocalService),
                    false,
                    false,
                    null,
                    null,
                    serviceContext);
        } else {
            if (log.isInfoEnabled()) {
                log.info("Updating template " + fileName + " for structure " + ddmStructureKey);
            }

            ddmTemplateLocalService.updateTemplate(
                    userId,
                    ddmTemplate.getTemplateId(),
                    ddmStructureClassNameId,
                    ddmTemplate.getNameMap(),
                    null,
                    DDMTemplateConstants.TEMPLATE_TYPE_DISPLAY,
                    null,
                    language,
                    replaceFileEntryURL(script, groupId, dlAppLocalService),
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
