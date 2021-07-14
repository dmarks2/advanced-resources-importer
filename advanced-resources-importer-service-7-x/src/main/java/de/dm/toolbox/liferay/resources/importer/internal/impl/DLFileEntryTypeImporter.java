package de.dm.toolbox.liferay.resources.importer.internal.impl;

import com.liferay.asset.kernel.service.AssetCategoryLocalService;
import com.liferay.asset.kernel.service.AssetVocabularyLocalService;
import com.liferay.document.library.kernel.model.DLFileEntryType;
import com.liferay.document.library.kernel.service.DLFileEntryTypeLocalService;
import com.liferay.dynamic.data.mapping.model.DDMForm;
import com.liferay.dynamic.data.mapping.util.DDM;
import com.liferay.dynamic.data.mapping.util.DDMBeanTranslator;
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
import de.dm.toolbox.liferay.resources.importer.util.ImporterUtil;
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
                "importer.order=30"
        },
        service = Importer.class
)
public class DLFileEntryTypeImporter extends BaseImporter {

    private static final Log log = LogFactoryUtil.getLog(DLFileEntryTypeImporter.class);

    private static final String DL_DOCUMENT_TYPES_DIR_NAME = "/document_library/document_types/";

    @Reference
    private DLFileEntryTypeLocalService dlFileEntryTypeLocalService;

    @Reference
    private DDM ddm;

    @Reference
    private DDMBeanTranslator ddmBeanTranslator;

    @Override
    protected void doRunImport(ServletContext servletContext, long companyId) throws Exception {
        addDLFileEntryTypes(resourcesDir, DL_DOCUMENT_TYPES_DIR_NAME, servletContext);
    }

    private void addDLFileEntryTypes(String resourcesDir, String dirName, ServletContext servletContext) throws Exception {
        Set<String> resourcePaths = servletContext.getResourcePaths(ImporterUtil.getResourcePath(resourcesDir, dirName));

        if (resourcePaths == null) {
            if (log.isDebugEnabled()) {
                log.debug("No DL FileEntry Types Directory found for " + servletContext.getServletContextName() + ". Skipping...");
            }
            return;
        }

        for (String resourcePath : resourcePaths) {
            if (! resourcePath.endsWith(StringPool.SLASH)) {
                addDLFileEntryType(resourcePath, servletContext);
            }
        }

    }

    private void addDLFileEntryType(String resourcePath, ServletContext servletContext) throws Exception {
        String name = FileUtil.getShortFileName(resourcePath);

        URL url = servletContext.getResource(resourcePath);

        URLConnection urlConnection = url.openConnection();

        addDLFileEntryType(name, urlConnection.getInputStream());

    }

    private void addDLFileEntryType(String fileName, InputStream inputStream) throws Exception {
        setServiceContext(fileName);

        String name = FileUtil.stripExtension(fileName);

        String key = getKey(name);

        String content = StringUtil.read(inputStream);

        if (Validator.isNull(content)) {
            if (log.isDebugEnabled()) {
                log.debug("DL FileEntry found with key " + key + ", but file is empty. Skipping...");
            }

            return;
        }

        DLFileEntryType dlFileEntryType = dlFileEntryTypeLocalService.fetchFileEntryType(groupId, key);

        DDMForm ddmFormModel = ddm.getDDMForm(content);

        com.liferay.dynamic.data.mapping.kernel.DDMForm ddmForm = ddmBeanTranslator.translate(ddmFormModel);

        serviceContext.setAttribute("ddmForm", ddmForm);

        if (Validator.isNull(dlFileEntryType)) {
            if (log.isInfoEnabled()) {
                log.info("Adding DLFileEntryType " + key);
            }

            Map<Locale, String> titleMap = getLocalizedMapFromAssetJSONObjectMap(fileName, "title", getMap(name));
            Map<Locale, String> descriptionMap = getLocalizedMapFromAssetJSONObjectMap(fileName, "description", null);

            dlFileEntryTypeLocalService.addFileEntryType(
                    userId,
                    groupId,
                    key,
                    titleMap,
                    descriptionMap,
                    new long[0],
                    serviceContext
            );

        } else {
            if (log.isInfoEnabled()) {
                log.info("Updating DLFileEntryType " + key);
            }

            Map<Locale, String> titleMap = getLocalizedMapFromAssetJSONObjectMap(fileName, "title", dlFileEntryType.getNameMap());
            Map<Locale, String> descriptionMap = getLocalizedMapFromAssetJSONObjectMap(fileName, "description", dlFileEntryType.getDescriptionMap());


            dlFileEntryTypeLocalService.updateFileEntryType(
                    userId,
                    dlFileEntryType.getFileEntryTypeId(),
                    titleMap,
                    descriptionMap,
                    new long[0],
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
