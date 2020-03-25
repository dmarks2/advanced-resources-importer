package de.dm.toolbox.liferay.resources.importer.internal.impl;

import com.liferay.asset.kernel.service.AssetCategoryLocalService;
import com.liferay.asset.kernel.service.AssetVocabularyLocalService;
import com.liferay.document.library.kernel.exception.DuplicateFileEntryException;
import com.liferay.document.library.kernel.model.DLFileEntryType;
import com.liferay.document.library.kernel.model.DLFolder;
import com.liferay.document.library.kernel.model.DLFolderConstants;
import com.liferay.document.library.kernel.service.DLAppLocalService;
import com.liferay.document.library.kernel.service.DLFileEntryLocalService;
import com.liferay.document.library.kernel.service.DLFileEntryTypeLocalService;
import com.liferay.document.library.kernel.service.DLFolderLocalService;
import com.liferay.dynamic.data.mapping.kernel.DDMStructure;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.MimeTypes;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import de.dm.toolbox.liferay.resources.importer.BaseImporter;
import de.dm.toolbox.liferay.resources.importer.Importer;
import de.dm.toolbox.liferay.resources.importer.components.DLAppHelper;
import de.dm.toolbox.liferay.resources.importer.internal.util.ImporterUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.ServletContext;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component(
        immediate = true,
        property = {
                "importer.order=50"
        },
        service = Importer.class
)
public class DLFileEntryImporter extends BaseImporter {

    private static final Log log = LogFactoryUtil.getLog(DLFileEntryImporter.class);

    private static final String DL_DOCUMENTS_DIR_NAME = "/document_library/documents/";

    private static final String FIELDS_DISPLAY_NAME = "_fieldsDisplay";

    private final Map<String, Long> folderIds = new HashMap<>();

    @Reference
    private DLFolderLocalService dlFolderLocalService;

    @Reference
    private DLAppLocalService dlAppLocalService;

    @Reference
    private DLFileEntryLocalService dlFileEntryLocalService;

    @Reference
    private MimeTypes mimeTypes;

    @Reference
    private DLFileEntryTypeLocalService dlFileEntryTypeLocalService;

    @Reference
    private DLAppHelper dlAppHelper;

    @Override
    protected void doRunImport(ServletContext servletContext, long companyId) throws Exception {
        addDLFileEntries(resourcesDir, DL_DOCUMENTS_DIR_NAME, servletContext);
    }

    private void addDLFileEntries(String resourcesDir, String dirName, ServletContext servletContext) throws Exception {
        Set<String> resourcePaths = servletContext.getResourcePaths(ImporterUtil.getResourcePath(resourcesDir, dirName));

        if (resourcePaths == null) {
            if (log.isDebugEnabled()) {
                log.debug("No DL FileEntry Directory found for " + servletContext.getServletContextName() + ". Skipping...");
            }
            return;
        }

        for (String resourcePath : resourcePaths) {
            if (resourcePath.endsWith(StringPool.SLASH)) {
                addDLFolder(DLFolderConstants.DEFAULT_PARENT_FOLDER_ID, resourcePath, servletContext);
            } else {
                addDLFileEntry(resourcePath, servletContext);
            }
        }
    }

    private long addDLFolder(long parentFolderId, String resourcePath, ServletContext servletContext) throws Exception {
        String folderPath = FileUtil.getPath(resourcePath);
        String folderName = FileUtil.getShortFileName(folderPath);

        DLFolder dlFolder = dlFolderLocalService.fetchFolder(
                groupId,
                parentFolderId,
                folderName
        );

        if (Validator.isNull(dlFolder)) {
            dlFolder = dlFolderLocalService.addFolder(
                    userId,
                    groupId,
                    groupId,
                    false,
                    parentFolderId,
                    folderName,
                    null,
                    false,
                    serviceContext
            );

            if (log.isInfoEnabled()) {
                log.info("Adding folder " + dlFolder.getPath());
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Folder " + dlFolder.getPath() + " already exists. Skipping...");
            }
        }

        long folderId = dlFolder.getFolderId();
        folderIds.put(resourcePath, folderId);

        Set<String> resourcePaths = servletContext.getResourcePaths(resourcePath);

        if (resourcePaths != null) {
            for (String curResourcePath : resourcePaths) {
                if (curResourcePath.endsWith(StringPool.SLASH)) {
                    addDLFolder(folderId, curResourcePath, servletContext);
                } else {
                    addDLFileEntry(curResourcePath, servletContext);
                }
            }
        }

        return folderId;
    }

    private void addDLFileEntry(String resourcePath, ServletContext servletContext) throws Exception {
        String filePath = FileUtil.getPath(resourcePath);
        String shortFileName = FileUtil.getShortFileName(resourcePath);

        Long parentFolderId = folderIds.get(filePath + StringPool.SLASH);

        if (parentFolderId == null) {
            parentFolderId = 0L;
        }

        URL url = servletContext.getResource(resourcePath);

        URLConnection urlConnection = url.openConnection();

        addDLFileEntry(parentFolderId, shortFileName, urlConnection.getInputStream(), urlConnection.getContentLength());
    }

    private void addDLFileEntry(Long parentFolderId, String fileName, InputStream inputStream, int contentLength) throws PortalException {
        setServiceContext(fileName);

        String contentType = mimeTypes.getContentType(fileName);

        try {
            dlAppLocalService.addFileEntry(
                    userId,
                    groupId,
                    parentFolderId,
                    fileName,
                    contentType,
                    fileName,
                    StringPool.BLANK,
                    StringPool.BLANK,
                    inputStream,
                    contentLength,
                    serviceContext
            );

            if (log.isInfoEnabled()) {
                log.info("Adding file entry " + fileName);
            }
        } catch (DuplicateFileEntryException e) {
            if (log.isInfoEnabled()) {
                log.info("Updating file entry " + fileName);
            }

            FileEntry fileEntry = dlAppLocalService.getFileEntry(groupId, parentFolderId, fileName);

            String previousVersion = fileEntry.getVersion();

            fileEntry = dlAppHelper.updateFileEntry(
                    userId,
                    fileEntry.getFileEntryId(),
                    fileName,
                    contentType,
                    fileName,
                    StringPool.BLANK,
                    StringPool.BLANK,
                    true,
                    inputStream,
                    contentLength,
                    serviceContext
            );

            dlFileEntryLocalService.deleteFileVersion(
                    fileEntry.getUserId(),
                    fileEntry.getFileEntryId(),
                    previousVersion
            );
        }
    }

    protected void setServiceContext(String name) throws PortalException {
        super.setServiceContext(name);

        JSONObject assetJSONObject = assetJSONObjectMap.get(name);

        if (assetJSONObject != null) {
            String fileEntryType = assetJSONObject.getString("fileEntryType");
            if (Validator.isNotNull(fileEntryType)) {
                String key = getKey(fileEntryType);

                DLFileEntryType dlFileEntryType = dlFileEntryTypeLocalService.fetchFileEntryType(groupId, key);

                if (Validator.isNull(dlFileEntryType)) {
                    try {
                        long[] ancestorSiteGroupIds = portal.getAncestorSiteGroupIds(groupId);

                        for (long ancestorSiteGroupId : ancestorSiteGroupIds) {
                            dlFileEntryType = dlFileEntryTypeLocalService.fetchFileEntryType(ancestorSiteGroupId, key);

                            if (Validator.isNotNull(dlFileEntryType)) {
                                break;
                            }
                        }
                    } catch (PortalException e) {
                        if (log.isWarnEnabled()) {
                            log.warn("Unable to fetch ancestor groups: " + e.getMessage(), e);
                        }
                    }
                }

                if (Validator.isNull(dlFileEntryType)) {
                    if (log.isWarnEnabled()) {
                        log.warn("Unable to import metadata for document " + name + ", file entry type " + fileEntryType + " not found!");
                    }

                    return;
                }

                if (log.isDebugEnabled()) {
                    log.debug("Found FileEntryType " + key + " for file " + name);
                }

                serviceContext.setAttribute("fileEntryTypeId", dlFileEntryType.getFileEntryTypeId());

                JSONObject fileEntryMetadata = assetJSONObject.getJSONObject("fileEntryMetadata");
                if (Validator.isNotNull(fileEntryMetadata)) {
                    List<DDMStructure> ddmStructures = dlFileEntryType.getDDMStructures();

                    outer:
                    for (Iterator<String> i = fileEntryMetadata.keys(); i.hasNext(); ) {
                        String metadataKey = i.next();

                        String metadataValue = fileEntryMetadata.getString(metadataKey);

                        for (DDMStructure ddmStructure : ddmStructures) {
                            String namespace = String.valueOf(ddmStructure.getStructureId());

                            if (ddmStructure.hasField(metadataKey)) {
                                String serviceContextKey = metadataKey + "_INSTANCE_" + StringUtil.randomId();

                                if (log.isDebugEnabled()) {
                                    log.debug("Setting service context attribute " + namespace + serviceContextKey + " to " + metadataValue);
                                }

                                serviceContext.setAttribute(namespace + serviceContextKey, metadataValue);

                                String fieldsDisplayValue = (String)serviceContext.getAttribute(namespace + FIELDS_DISPLAY_NAME);
                                if (Validator.isNull(fieldsDisplayValue)) {
                                    fieldsDisplayValue = serviceContextKey;
                                } else {
                                    fieldsDisplayValue = fieldsDisplayValue + StringPool.COMMA + serviceContextKey;
                                }

                                serviceContext.setAttribute(namespace + FIELDS_DISPLAY_NAME, fieldsDisplayValue);

                                continue outer;
                            }
                        }
                    }
                }
            }
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
