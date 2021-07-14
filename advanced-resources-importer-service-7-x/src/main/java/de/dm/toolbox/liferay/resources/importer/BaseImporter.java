package de.dm.toolbox.liferay.resources.importer;

import com.liferay.asset.kernel.model.AssetCategory;
import com.liferay.asset.kernel.model.AssetCategoryConstants;
import com.liferay.asset.kernel.model.AssetVocabulary;
import com.liferay.asset.kernel.service.AssetCategoryLocalService;
import com.liferay.asset.kernel.service.AssetVocabularyLocalService;
import com.liferay.document.library.kernel.exception.NoSuchFileEntryException;
import com.liferay.document.library.kernel.exception.NoSuchFolderException;
import com.liferay.document.library.kernel.service.DLAppLocalService;
import com.liferay.document.library.kernel.util.DLUtil;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.Folder;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.LayoutLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.template.TemplateConstants;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.CharPool;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.LocalizationUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import de.dm.toolbox.liferay.resources.importer.util.ImporterUtil;

import javax.servlet.ServletContext;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BaseImporter implements Importer {

    private static final Log log = LogFactoryUtil.getLog(BaseImporter.class);

    private final Pattern FILE_ENTRY_PATTERN = Pattern.compile("\\[\\$FILE=([^\\$]+)\\$\\]");
    private final Pattern LAYOUT_PATTERN = Pattern.compile("\\[\\$LAYOUT=([^\\$]+)\\$\\]");

    protected Map<String, JSONObject> assetJSONObjectMap;

    protected String resourcesDir;

    protected Group group;
    protected String groupKey;
    protected long groupId;

    protected User user;
    protected long userId;

    protected ServiceContext serviceContext;

    protected UserLocalService userLocalService;
    protected GroupLocalService groupLocalService;
    protected AssetVocabularyLocalService assetVocabularyLocalService;
    protected AssetCategoryLocalService assetCategoryLocalService;
    protected Portal portal;

    @Override
    public void runImport(ServletContext servletContext, long companyId, String groupKey, Map<String, JSONObject> assetJSONObjectMap) throws Exception {
        setAssetJSONObjectMap(assetJSONObjectMap);

        this.groupKey = groupKey;

        this.group = ImporterUtil.getGroup(groupLocalService, companyId, groupKey);
        if (this.group != null) {
            this.groupId = group.getGroupId();
        }

        this.resourcesDir = ImporterUtil.getResourcesDir(servletContext);

        if (Validator.isNull(resourcesDir)) {
            if (log.isDebugEnabled()) {
                log.debug("No Resources Directory found for " + servletContext.getServletContextName() + ". Skipping...");
            }
            return;
        }

        user = userLocalService.getDefaultUser(companyId);
        userId = user.getUserId();

        this.serviceContext = new ServiceContext();

        serviceContext.setAddGroupPermissions(true);
        serviceContext.setAddGuestPermissions(true);
        serviceContext.setCompanyId(companyId);
        serviceContext.setScopeGroupId(groupId);
        serviceContext.setUserId(userId);

        doRunImport(servletContext, companyId);
    }

    protected abstract void doRunImport(ServletContext servletContext, long companyId) throws Exception;

    protected void setUserLocalService(UserLocalService userLocalService) {
        this.userLocalService = userLocalService;
    }

    protected void setGroupLocalService(GroupLocalService groupLocalService) {
        this.groupLocalService = groupLocalService;
    }

    protected void setAssetVocabularyLocalService(AssetVocabularyLocalService assetVocabularyLocalService) {
        this.assetVocabularyLocalService = assetVocabularyLocalService;
    }

    protected void setAssetCategoryLocalService(AssetCategoryLocalService assetCategoryLocalService) {
        this.assetCategoryLocalService = assetCategoryLocalService;
    }

    protected void setPortal(Portal portal) {
        this.portal = portal;
    }

    protected void setAssetJSONObjectMap(Map<String, JSONObject> assetJSONObjectMap) {
        this.assetJSONObjectMap = assetJSONObjectMap;
    }

    protected Map<Locale, String> getMap(Locale locale, String value) {
        Map<Locale, String> map = new HashMap<>();

        map.put(locale, value);

        return map;
    }

    protected Map<Locale, String> getMap(String value) {
        return getMap(LocaleUtil.getDefault(), value);
    }

    protected Map<Locale, String> getLocalizedMapFromAssetJSONObjectMap(String filename, String assetJsonObjectMapKey, Map<Locale, String> defaultValue) {
        if (this.assetJSONObjectMap != null) {
            if (this.assetJSONObjectMap.containsKey(filename)) {
                JSONObject assetJSONObject = assetJSONObjectMap.get(filename);
                if (assetJSONObject.has(assetJsonObjectMapKey)) {
                    Map<Locale, String> map = getLocaleStringMap(assetJsonObjectMapKey, assetJSONObject);
                    if (map != null) return map;
                }
            }
        }

        return defaultValue;
    }

    protected Map<Locale, String> getLocaleStringMap(String jsonObjectMapKey, JSONObject jsonObject) {
        JSONObject localeMapJsonObject = jsonObject.getJSONObject(jsonObjectMapKey);

        if (localeMapJsonObject != null) {
            Map<Locale, String> map = (Map<Locale, String>) LocalizationUtil.deserialize(localeMapJsonObject);

            //no supported languages found
            if (!map.containsKey(LocaleUtil.getDefault())) {
                Iterator<String> keys = localeMapJsonObject.keys();
                if (keys.hasNext()) {
                    String key = keys.next();

                    String value = localeMapJsonObject.getString(key);

                    map.put(LocaleUtil.getDefault(), value);
                }
            }

            return map;
        }
        return null;
    }

    protected String getKey(String name) {
        name = StringUtil.replace(name, CharPool.SPACE, CharPool.DASH);

        name = StringUtil.toUpperCase(name);

        return name;
    }

    protected String replaceFileEntryURL(String content, long groupId, DLAppLocalService dlAppLocalService) throws Exception {
        Matcher matcher = FILE_ENTRY_PATTERN.matcher(content);

        while (matcher.find()) {
            String fileName = matcher.group(1);

            String fileEntryURL = getFileEntryURL(fileName, dlAppLocalService);

            content = matcher.replaceFirst(fileEntryURL);

            matcher.reset(content);
        }

        return content;
    }

    protected String replaceLayoutURL(String content, long groupId, LayoutLocalService layoutLocalService) {
        Matcher matcher = LAYOUT_PATTERN.matcher(content);

        while (matcher.find()) {
            String friendlyURL = matcher.group(1);

            String layoutIdentifier = getLayoutIdentifier(friendlyURL, layoutLocalService);

            content = matcher.replaceFirst(layoutIdentifier);

            matcher.reset(content);
        }

        return content;
    }

    protected String getFileEntryURL(String fileName, DLAppLocalService dlAppLocalService) throws PortalException {
        String[] parts = StringUtil.split(fileName, StringPool.SLASH);

        long folderId = 0L;

        FileEntry fileEntry = null;

        try {
            for (int i = 0; i < parts.length; i++) {
                if (i < (parts.length - 1)) {
                    Folder folder = dlAppLocalService.getFolder(groupId, folderId, parts[i]);

                    folderId = folder.getFolderId();
                } else {
                    fileEntry = dlAppLocalService.getFileEntry(groupId, folderId, parts[i]);
                }
            }
        } catch (NoSuchFolderException | NoSuchFileEntryException e) {
            e.printStackTrace();
        }

        String fileEntryURL = StringPool.BLANK;

        if (fileEntry != null) {
            fileEntryURL = DLUtil.getPreviewURL(
                    fileEntry,
                    fileEntry.getFileVersion(),
                    null,
                    StringPool.BLANK,
                    false,
                    true
            );
        }

        return fileEntryURL;
    }

    protected String getLayoutIdentifier(String friendlyURL, LayoutLocalService layoutLocalService) {
        Layout layout = layoutLocalService.fetchLayoutByFriendlyURL(groupId, false, friendlyURL);
        if (Validator.isNull(layout)) {
            layout = layoutLocalService.fetchLayoutByFriendlyURL(groupId, true, friendlyURL);
        }
        if (Validator.isNull(layout)) {
            if (log.isWarnEnabled()) {
                log.warn("No Layout found with friendly URL " + friendlyURL);
            }
            return StringPool.BLANK;
        }

        String identifier =
                String.valueOf(layout.getLayoutId()) +
                StringPool.AT +
                (layout.isPublicLayout() ? "public" : "private-group") +
                StringPool.AT +
                String.valueOf(layout.getGroupId());

        return identifier;
    }

    protected String getDDMTemplateLanguage(String fileName) {
        String extension = FileUtil.getExtension(fileName);

        if (extension.equals(TemplateConstants.LANG_TYPE_CSS) ||
                extension.equals(TemplateConstants.LANG_TYPE_FTL) ||
                extension.equals(TemplateConstants.LANG_TYPE_VM) ||
                extension.equals(TemplateConstants.LANG_TYPE_XSL)) {

            return extension;
        }

        return TemplateConstants.LANG_TYPE_VM;
    }

    protected String[] getJSONArrayAsStringArray(JSONObject jsonObject, String key) {

        JSONArray jsonArray = jsonObject.getJSONArray(key);

        if (jsonArray != null) {
            return ArrayUtil.toStringArray(jsonArray);
        }

        return new String[0];
    }

    protected void setServiceContext(String name) throws PortalException {
        JSONObject assetJSONObject = assetJSONObjectMap.get(name);

        String[] assetTagNames = null;

        if (assetJSONObject != null) {
            assetTagNames = getJSONArrayAsStringArray(assetJSONObject, "tags");
        }

        serviceContext.setAssetTagNames(assetTagNames);

        if (assetJSONObject != null) {

            JSONArray categoriesJSONArray = assetJSONObject.getJSONArray("categories");
            if (categoriesJSONArray != null) {
                long[] categoryIds = new long[0];

                for (int i = 0; i < categoriesJSONArray.length(); i++) {
                    JSONObject categoriesJSONObject = categoriesJSONArray.getJSONObject(i);

                    JSONArray keys = categoriesJSONObject.names();
                    for (int j = 0; j < keys.length(); j++) {
                        String key = keys.getString(i);

                        AssetVocabulary assetVocabulary = getVocabularyByName(groupId, key);
                        if (Validator.isNull(assetVocabulary)) {
                            long[] ancestorSiteGroupIds = portal.getAncestorSiteGroupIds(groupId);
                            for (long ancestorSiteGroupId : ancestorSiteGroupIds) {
                                assetVocabulary = getVocabularyByName(ancestorSiteGroupId, key);

                                if (Validator.isNotNull(assetVocabulary)) {
                                    break;
                                }
                            }
                        }

                        if (Validator.isNull(assetVocabulary)) {
                            if (log.isWarnEnabled()) {
                                log.warn("Unable to select vocabulary " + key);
                            }

                            continue;
                        }

                        String[] categories = getJSONArrayAsStringArray(categoriesJSONObject, key);

                        for (String category : categories) {
                            AssetCategory assetCategory = getCategoryByPath(assetVocabulary.getVocabularyId(), AssetCategoryConstants.DEFAULT_PARENT_CATEGORY_ID, category);

                            if (Validator.isNull(assetCategory)) {
                                if (log.isWarnEnabled()) {
                                    log.warn("Unable to select category " + category);
                                }

                                continue;
                            }

                            categoryIds = ArrayUtil.append(categoryIds, assetCategory.getCategoryId());
                        }
                    }
                }

                serviceContext.setAssetCategoryIds(categoryIds);
            }
        }
    }

    protected String getJournalId(String fileName) {
        String id = FileUtil.stripExtension(fileName);

        id = StringUtil.toUpperCase(id);

        return StringUtil.replace(id, CharPool.SPACE, CharPool.DASH);
    }

    protected String getDDMStructureLanguage(String fileName) {
        String extension = FileUtil.getExtension(fileName);

        if (extension.equals(TemplateConstants.LANG_TYPE_JSON) ||
                extension.equals(TemplateConstants.LANG_TYPE_XML)) {

            return extension;
        }

        return TemplateConstants.LANG_TYPE_XML;
    }

    private AssetVocabulary getVocabularyByName(long groupId, String name) throws PortalException {
        List<AssetVocabulary> vocabularies = assetVocabularyLocalService.getGroupVocabularies(groupId);
        for (AssetVocabulary vocabulary : vocabularies) {
            if (vocabulary.getName().equals(name)) {
                return vocabulary;
            }
            String title = vocabulary.getTitle(LocaleUtil.getSiteDefault(), true);
            if (Validator.isNotNull(title)) {
                if (title.equals(name)) {
                    return vocabulary;
                }
            }
        }
        return null;
    }

    private AssetCategory getCategoryByPath(long assetVocabularyId, long parentCategoryId, String path) {
        String name = path;

        boolean hasSubcategories = false;

        if (path.indexOf('\\') > -1) {
            name = path.substring(0, path.indexOf('\\'));
            path = path.substring(path.indexOf('\\') + 1);

            hasSubcategories = true;
        }

        List<AssetCategory> categories = assetCategoryLocalService.getVocabularyCategories(parentCategoryId, assetVocabularyId, QueryUtil.ALL_POS, QueryUtil.ALL_POS, null);
        for (AssetCategory category : categories) {
            if (category.getName().equals(name)) {
                if (hasSubcategories) {
                    return getCategoryByPath(assetVocabularyId, category.getCategoryId(), path);
                } else {
                    return category;
                }
            }

            String title = category.getTitle(LocaleUtil.getSiteDefault(), true);
            if (Validator.isNotNull(title)) {
                if (title.equals(name)) {
                    if (hasSubcategories) {
                        return getCategoryByPath(assetVocabularyId, category.getCategoryId(), path);
                    } else {
                        return category;
                    }
                }
            }
        }

        return null;
    }
}
