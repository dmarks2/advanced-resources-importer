package de.dm.toolbox.liferay.resources.importer.internal.impl;

import com.liferay.asset.kernel.service.AssetCategoryLocalService;
import com.liferay.asset.kernel.service.AssetVocabularyLocalService;
import com.liferay.document.library.kernel.service.DLAppLocalService;
import com.liferay.dynamic.data.mapping.model.DDMStructure;
import com.liferay.dynamic.data.mapping.model.DDMTemplate;
import com.liferay.dynamic.data.mapping.service.DDMStructureLocalService;
import com.liferay.dynamic.data.mapping.service.DDMTemplateLocalService;
import com.liferay.journal.model.JournalArticle;
import com.liferay.journal.model.JournalArticleConstants;
import com.liferay.journal.model.JournalFolder;
import com.liferay.journal.service.JournalArticleLocalService;
import com.liferay.journal.service.JournalFolderLocalService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.LayoutLocalService;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.CharPool;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.LocalizationUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import de.dm.toolbox.liferay.resources.importer.BaseImporter;
import de.dm.toolbox.liferay.resources.importer.Importer;
import de.dm.toolbox.liferay.resources.importer.util.ImporterUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.ServletContext;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component(
        immediate = true,
        property = {
                "importer.order=90"
        },
        service = Importer.class
)
public class JournalArticleImporter extends BaseImporter {

    private static final Log log = LogFactoryUtil.getLog(JournalArticleImporter.class);

    private static final String JOURNAL_ARTICLES_DIR_NAME = "/journal/articles/";

    @Reference
    private DDMStructureLocalService ddmStructureLocalService;

    @Reference
    private DDMTemplateLocalService ddmTemplateLocalService;

    @Reference
    private JournalFolderLocalService journalFolderLocalService;

    @Reference
    private DLAppLocalService dlAppLocalService;

    @Reference
    private JournalArticleLocalService journalArticleLocalService;

    @Reference
    private LayoutLocalService layoutLocalService;

    @Override
    protected void doRunImport(ServletContext servletContext, long companyId) throws Exception {
        addJournalArticles(JOURNAL_ARTICLES_DIR_NAME, companyId, servletContext);
    }

    private void addJournalArticles(String dirName, long companyId, ServletContext servletContext) throws Exception {
        Set<String> resourcePaths = servletContext.getResourcePaths(ImporterUtil.getResourcePath(resourcesDir, dirName));

        if (resourcePaths == null) {
            if (log.isDebugEnabled()) {
                log.debug("No Journal Articles found for " + servletContext.getServletContextName() + ". Skipping...");
            }
            return;
        }

        for (String resourcePath : resourcePaths) {
            if (resourcePath.endsWith(StringPool.SLASH)) {
                String subdirName = FileUtil.getShortFileName(StringUtil.replaceLast(resourcePath, CharPool.FORWARD_SLASH, StringPool.BLANK));

                String templateKey = getKey(subdirName);

                DDMTemplate ddmTemplate = getDDMTemplate(companyId, templateKey);

                if (Validator.isNull(ddmTemplate)) {
                    if (log.isWarnEnabled()) {
                        log.warn("DDM Template " + templateKey + " not found. Unable to import Journal Articles in Folder " + resourcePath);
                    }

                    continue;
                }

                long structureId = ddmTemplate.getClassPK();
                DDMStructure ddmStructure = ddmStructureLocalService.fetchStructure(structureId);
                if (Validator.isNull(ddmStructure)) {
                    if (log.isWarnEnabled()) {
                        log.warn("DDM Structure for DDM Template " + templateKey + " not found. Unable to import Journal Articles in Folder " + resourcePath);
                    }

                    continue;
                }

                String structureKey = ddmStructure.getStructureKey();

                addJournalArticles(servletContext, structureKey, templateKey, dirName + subdirName, 0L);
            }
        }
    }

    private void addJournalArticles(ServletContext servletContext, String structureKey, String templateKey, String dirName, long folderId) throws Exception {
        Set<String> resourcePaths = servletContext.getResourcePaths(ImporterUtil.getResourcePath(resourcesDir, dirName));

        if (resourcePaths == null) {
            if (log.isDebugEnabled()) {
                log.debug("No Journal Articles found in subdirectory " + dirName + ". Skipping...");
            }

            return;
        }

        for (String resourcePath : resourcePaths) {
            if (resourcePath.endsWith(StringPool.SLASH)) {
                String folderName = FileUtil.getShortFileName(StringUtil.replaceLast(resourcePath, CharPool.FORWARD_SLASH, StringPool.BLANK));

                JournalFolder journalFolder = journalFolderLocalService.fetchFolder(groupId, folderName);

                if (journalFolder == null) {
                    if (log.isInfoEnabled()) {
                        log.info("Adding Journal Folder " + folderName);
                    }

                    journalFolder = journalFolderLocalService.addFolder(
                            userId,
                            groupId,
                            folderId,
                            folderName,
                            StringPool.BLANK,
                            serviceContext
                    );
                }

                addJournalArticles(servletContext, structureKey, templateKey, dirName + StringPool.FORWARD_SLASH + folderName, journalFolder.getFolderId());
            } else {
                String name = FileUtil.getShortFileName(resourcePath);

                URL url = servletContext.getResource(resourcePath);

                URLConnection urlConnection = url.openConnection();

                addJournalArticle(structureKey, templateKey, name, folderId, urlConnection.getInputStream());
            }
        }

    }

    private void addJournalArticle(String structureKey, String templateKey, String fileName, long folderId, InputStream inputStream) throws Exception {
        String title = FileUtil.stripExtension(fileName);

        JSONObject assetJSONObject = assetJSONObjectMap.get(fileName);

        Map<Locale, String> descriptionMap = null;

        boolean indexable = true;

        if (assetJSONObject != null) {
            String abstractSummary = assetJSONObject.getString("abstractSummary");

            descriptionMap = getMap(abstractSummary);

            indexable = GetterUtil.getBoolean(assetJSONObject.getString("indexable"), true);
        }

        String content = StringUtil.read(inputStream);

        if (Validator.isNull(content)) {
            if (log.isDebugEnabled()) {
                log.debug("Journal Article found with title " + title + ", but file is empty. Skipping...");
            }

            return;
        }

        content = replaceFileEntryURL(content, groupId, dlAppLocalService);

        content = replaceLayoutURL(content, groupId, layoutLocalService);

        if (log.isDebugEnabled()) {
            log.debug("Modified XML content for Journal Article " + fileName + " = " + content);
        }

        Locale articleDefaultLocale = LocaleUtil.fromLanguageId(LocalizationUtil.getDefaultLanguageId(content));

        boolean smallImage = false;
        String smallImageURL = StringPool.BLANK;

        if (assetJSONObject != null) {
            String smallImageFileName = assetJSONObject.getString("smallImage");

            if (Validator.isNotNull(smallImageFileName)) {
                smallImage = true;

                smallImageURL = getFileEntryURL(smallImageFileName, dlAppLocalService);
            }
        }

        setServiceContext(fileName);

        String journalArticleId = getJournalId(fileName);

        JournalArticle journalArticle = journalArticleLocalService.fetchLatestArticle(groupId, journalArticleId, WorkflowConstants.STATUS_ANY);

        Map<Locale, String> titleMap = getMap(articleDefaultLocale, title);

        if (Validator.isNull(journalArticle)) {
            if (log.isInfoEnabled()) {
                log.info("Adding Journal Article " + journalArticleId);
            }

            journalArticle = journalArticleLocalService.addArticle(
                    userId,
                    groupId,
                    folderId,
                    0,
                    0,
                    journalArticleId,
                    false,
                    JournalArticleConstants.VERSION_DEFAULT,
                    titleMap,
                    descriptionMap,
                    content,
                    structureKey,
                    templateKey,
                    StringPool.BLANK,
                    1, 1, 2010, 0, 0,
                    0, 0, 0, 0, 0, true,
                    0, 0, 0, 0, 0, true,
                    indexable,
                    smallImage,
                    smallImageURL,
                    null,
                    new HashMap<String, byte[]>(),
                    StringPool.BLANK,
                    serviceContext
            );

        } else {
            if (log.isInfoEnabled()) {
                log.info("Updating Journal Article " + journalArticleId);
            }

            journalArticle = journalArticleLocalService.updateArticle(
                    userId,
                    groupId,
                    folderId,
                    journalArticleId,
                    journalArticle.getVersion(),
                    titleMap,
                    descriptionMap,
                    content,
                    structureKey,
                    templateKey,
                    StringPool.BLANK,
                    1, 1, 2010, 0, 0,
                    0, 0, 0, 0, 0, true,
                    0, 0, 0, 0, 0, true,
                    indexable,
                    smallImage,
                    smallImageURL,
                    null,
                    new HashMap<String, byte[]>(),
                    StringPool.BLANK,
                    serviceContext
            );
        }

        journalArticleLocalService.updateStatus(
                userId,
                groupId,
                journalArticle.getArticleId(),
                journalArticle.getVersion(),
                WorkflowConstants.STATUS_APPROVED,
                StringPool.BLANK,
                new HashMap<String, Serializable>(),
                serviceContext
        );
    }

    private DDMTemplate getDDMTemplate(long companyId, String templateKey) throws PortalException {
        long ddmStructureClassNameId = portal.getClassNameId(DDMStructure.class);

        return ddmTemplateLocalService.fetchTemplate(groupId, ddmStructureClassNameId, templateKey, true);
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
