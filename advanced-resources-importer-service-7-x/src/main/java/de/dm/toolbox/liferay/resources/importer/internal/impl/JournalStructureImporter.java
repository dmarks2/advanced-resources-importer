package de.dm.toolbox.liferay.resources.importer.internal.impl;

import com.liferay.asset.kernel.service.AssetCategoryLocalService;
import com.liferay.asset.kernel.service.AssetVocabularyLocalService;
import com.liferay.dynamic.data.mapping.model.DDMForm;
import com.liferay.dynamic.data.mapping.model.DDMFormLayout;
import com.liferay.dynamic.data.mapping.model.DDMStructure;
import com.liferay.dynamic.data.mapping.model.DDMStructureConstants;
import com.liferay.dynamic.data.mapping.service.DDMStructureLocalService;
import com.liferay.dynamic.data.mapping.util.DDM;
import com.liferay.dynamic.data.mapping.util.DDMXML;
import com.liferay.journal.configuration.JournalServiceConfigurationValues;
import com.liferay.journal.model.JournalArticle;
import com.liferay.journal.util.JournalConverter;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.template.TemplateConstants;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Attribute;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.SAXReader;
import de.dm.toolbox.liferay.resources.importer.BaseImporter;
import de.dm.toolbox.liferay.resources.importer.Importer;
import de.dm.toolbox.liferay.resources.importer.components.DDMFormDeserializer;
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
                "importer.order=20"
        },
        service = Importer.class
)
public class JournalStructureImporter extends BaseImporter {

    private static final Log log = LogFactoryUtil.getLog(JournalStructureImporter.class);

    private static final String JOURNAL_DDM_STRUCTURES_DIR_NAME = "/journal/structures/";

    @Reference
    private DDMStructureLocalService ddmStructureLocalService;

    @Reference
    private JournalConverter journalConverter;

    @Reference
    private SAXReader saxReader;

    @Reference
    private DDMXML ddmxml;

    @Reference
    private DDMFormDeserializer ddmFormDeserializer;

    @Reference
    private DDM ddm;

    @Override
    protected void doRunImport(ServletContext servletContext, long companyId) throws Exception {
        addDDMStructures(StringPool.BLANK, JOURNAL_DDM_STRUCTURES_DIR_NAME, servletContext);
    }

    protected void addDDMStructures(String parentStructureKey, String dirName, ServletContext servletContext) throws Exception {
        Set<String> resourcePaths = servletContext.getResourcePaths(ImporterUtil.getResourcePath(resourcesDir, dirName));

        if (resourcePaths == null) {
            if (log.isDebugEnabled()) {
                log.debug("No DDM Structures Directory found for " + servletContext.getServletContextName() + ". Skipping...");
            }
            return;
        }

        for (String resourcePath : resourcePaths) {
            if (resourcePath.endsWith(StringPool.SLASH)) {
                continue;
            }

            String name = FileUtil.getShortFileName(resourcePath);

            URL url = servletContext.getResource(resourcePath);

            URLConnection urlConnection = url.openConnection();

            addDDMStructures(parentStructureKey, name, urlConnection.getInputStream());
        }
    }

    protected void addDDMStructures(String parentDDMStructureKey, String fileName, InputStream inputStream) throws Exception {
        setServiceContext(fileName);

        String language = getDDMStructureLanguage(fileName);

        fileName = FileUtil.stripExtension(fileName);

        String name = fileName;

        String key = getKey(fileName);

        long classNameId = portal.getClassNameId(JournalArticle.class);

        DDMStructure ddmStructure = ddmStructureLocalService.fetchStructure(
                groupId,
                classNameId,
                key
        );

        String content = StringUtil.read(inputStream);

        if (Validator.isNull(content)) {
            if (log.isDebugEnabled()) {
                log.debug("Journal Structure found with key " + key + ", but file is empty. Skipping...");
            }

            return;
        }

        DDMForm ddmForm;

        if (TemplateConstants.LANG_TYPE_XML.equals(language)) {
            if (isJournalStructureXSD(content)) {
                content = journalConverter.getDDMXSD(content);
            }

            ddmxml.validateXML(content);

            ddmForm = ddmFormDeserializer.deserializeXSD(content);
        } else {
            ddmForm = ddmFormDeserializer.deserializeJSONDDMForm(content);
        }

        DDMFormLayout ddmFormLayout = ddm.getDefaultDDMFormLayout(ddmForm);

        if (Validator.isNull(ddmStructure)) {
            Map<Locale, String> titleMap = getMap(name);

            if (log.isInfoEnabled()) {
                log.info("Adding DDM Structure " + name);
            }

            ddmStructureLocalService.addStructure(
                    userId,
                    groupId,
                    parentDDMStructureKey,
                    classNameId,
                    key,
                    titleMap,
                    null,
                    ddmForm,
                    ddmFormLayout,
                    JournalServiceConfigurationValues.JOURNAL_ARTICLE_STORAGE_TYPE,
                    DDMStructureConstants.TYPE_DEFAULT,
                    serviceContext
            );
        } else {
            long parentDDMStructureId = DDMStructureConstants.DEFAULT_PARENT_STRUCTURE_ID;

            if (Validator.isNotNull(parentDDMStructureKey)) {
                DDMStructure parentDDMStructure = ddmStructureLocalService.fetchStructure(
                        groupId,
                        classNameId,
                        parentDDMStructureKey
                );

                if (Validator.isNotNull(parentDDMStructure)) {
                    parentDDMStructureId = parentDDMStructure.getParentStructureId();
                }
            }

            if (log.isInfoEnabled()) {
                log.info("Updating DDM Structure " + name);
            }

            //do not update name!
            ddmStructureLocalService.updateStructure(
                    userId,
                    ddmStructure.getStructureId(),
                    parentDDMStructureId,
                    ddmStructure.getNameMap(),
                    ddmStructure.getDescriptionMap(),
                    ddmForm,
                    ddmFormLayout,
                    serviceContext
            );
        }
    }


    protected boolean isJournalStructureXSD(String xsd) throws Exception {
        Document document = saxReader.read(xsd);

        Element rootElement = document.getRootElement();

        Attribute availableLocalesAttribute = rootElement.attribute("available-locales");

        if (availableLocalesAttribute == null) {
            return true;
        }

        return false;
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
