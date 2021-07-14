package de.dm.toolbox.liferay.resources.importer.internal.impl;

import com.liferay.asset.kernel.model.AssetCategory;
import com.liferay.asset.kernel.model.AssetCategoryConstants;
import com.liferay.asset.kernel.model.AssetVocabulary;
import com.liferay.asset.kernel.service.AssetCategoryLocalService;
import com.liferay.asset.kernel.service.AssetVocabularyLocalService;
import com.liferay.dynamic.data.mapping.model.DDMStructure;
import com.liferay.dynamic.data.mapping.service.DDMStructureLocalService;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.ClassName;
import com.liferay.portal.kernel.service.ClassNameLocalService;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.UnicodeProperties;
import com.liferay.portal.kernel.util.Validator;
import de.dm.toolbox.liferay.resources.importer.BaseImporter;
import de.dm.toolbox.liferay.resources.importer.Importer;
import de.dm.toolbox.liferay.resources.importer.util.ImporterUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component(
        immediate = true,
        property = {
                "importer.order=40"
        },
        service = Importer.class
)
public class VocabulariesImporter extends BaseImporter {

    private static final Log log = LogFactoryUtil.getLog(VocabulariesImporter.class);

    private static final String VOCABULARIES_JSON = "vocabularies.json";

    @Reference
    private ClassNameLocalService classNameLocalService;

    @Reference
    private DDMStructureLocalService ddmStructureLocalService;

    @Override
    protected void doRunImport(ServletContext servletContext, long companyId) throws Exception {
        addVocabularies(resourcesDir, companyId, servletContext);
    }

    private void addVocabularies(String resourcesDir, long companyId, ServletContext servletContext) throws Exception {
        JSONObject jsonObject = ImporterUtil.getJSONObject(servletContext, resourcesDir, VOCABULARIES_JSON, companyId, group.getGroupId(), userId);

        if (jsonObject == null) {
            if (log.isDebugEnabled()) {
                log.debug("No " + VOCABULARIES_JSON + " present. Skipping...");
            }

            return;
        }

        JSONArray vocabulariesJSONArray = jsonObject.getJSONArray("vocabularies");

        if (vocabulariesJSONArray != null) {
            addVocabularies(vocabulariesJSONArray, companyId);
        }

    }

    private void addVocabularies(JSONArray vocabulariesJSONArray, long companyId) throws PortalException {
        for (int i = 0; i < vocabulariesJSONArray.length(); i++) {
            JSONObject vocabularyJSONObject = vocabulariesJSONArray.getJSONObject(i);

            addVocabulary(vocabularyJSONObject, companyId);
        }
    }

    private void addVocabulary(JSONObject vocabularyJSONObject, long companyId) throws PortalException {
        String name = vocabularyJSONObject.getString("name");

        AssetVocabulary assetVocabulary = getVocabularyByName(groupId, name);

        UnicodeProperties properties = new UnicodeProperties();

        if (vocabularyJSONObject.has("multiValued")) {
            properties.put("multiValued", String.valueOf(vocabularyJSONObject.getBoolean("multiValued")));
        }
        if (vocabularyJSONObject.has("selectedClassNames")) {
            JSONArray selectedClassNames = vocabularyJSONObject.getJSONArray("selectedClassNames");

            String classNameIds = toClassNameIds(selectedClassNames);
            properties.put("selectedClassNameIds", classNameIds);
        }
        if (vocabularyJSONObject.has("requiredClassNames")) {
            JSONArray requiredClassNames = vocabularyJSONObject.getJSONArray("requiredClassNames");

            String classNameIds = toClassNameIds(requiredClassNames);
            properties.put("requiredClassNameIds", classNameIds);
        }

        if (log.isDebugEnabled()) {
            log.debug("Calculated settings for vocabulary " + name + ": " + properties.toString());
        }

        Map<Locale, String> titleMap = new HashMap<>();
        titleMap.put(LocaleUtil.getSiteDefault(), name);

        if (Validator.isNull(assetVocabulary)) {
            if (log.isInfoEnabled()) {
                log.info("Adding Vocabulary " + name);
            }
            assetVocabulary = assetVocabularyLocalService.addVocabulary(
                userId,
                groupId,
                name,
                titleMap,
                null,
                properties.toString(),
                serviceContext
            );
        } else {
            if (log.isInfoEnabled()) {
                log.info("Updating Vocabulary " + name);
            }

            assetVocabulary = assetVocabularyLocalService.updateVocabulary(
                    assetVocabulary.getVocabularyId(),
                    name,
                    titleMap,
                    null,
                    properties.toString(),
                    serviceContext
            );
        }

        JSONArray categoriesJSONArray = vocabularyJSONObject.getJSONArray("categories");

        if (categoriesJSONArray != null) {
            addCategories(assetVocabulary.getVocabularyId(), categoriesJSONArray, companyId);
        }
    }

    private void addCategories(long vocabularyId, JSONArray categoriesJSONArray, long companyId) throws PortalException {
        for (int i = 0; i < categoriesJSONArray.length(); i++) {
            JSONObject categoryJSONObject = categoriesJSONArray.getJSONObject(i);

            addCategory(vocabularyId, AssetCategoryConstants.DEFAULT_PARENT_CATEGORY_ID, categoryJSONObject, companyId);
        }
    }

    private void addCategory(long vocabularyId, long parentCategoryId, JSONObject categoryJSONObject, long companyId) throws PortalException {
        String name = categoryJSONObject.getString("name");

        AssetCategory assetCategory = getCategoryByName(vocabularyId, parentCategoryId, name);

        Map<Locale, String> titleMap = getLocaleStringMap("title", categoryJSONObject);

        if (titleMap == null) {
            titleMap = new HashMap<>();
        }

        if (! titleMap.containsKey(LocaleUtil.getSiteDefault())) {
            titleMap.put(LocaleUtil.getSiteDefault(), name);
        }

        Map<Locale, String> descriptionMap = getLocaleStringMap("description", categoryJSONObject);

        String[] categoryProperties = new String[0];
        JSONArray propertiesJSONArray = categoryJSONObject.getJSONArray("properties");
        if (propertiesJSONArray != null) {
            for (int i = 0; i < propertiesJSONArray.length(); i++) {
                JSONObject propertiesJSONObject = propertiesJSONArray.getJSONObject(i);

                String key = propertiesJSONObject.names().getString(0);
                String value = propertiesJSONObject.getString(key);

                categoryProperties = ArrayUtil.append(categoryProperties, key + AssetCategoryConstants.PROPERTY_KEY_VALUE_SEPARATOR + value);
            }
        }

        if (Validator.isNull(assetCategory)) {
            if (log.isInfoEnabled()) {
                log.info("Adding category " + name);
            }

            assetCategory = assetCategoryLocalService.addCategory(
                userId,
                groupId,
                parentCategoryId,
                titleMap,
                descriptionMap,
                vocabularyId,
                categoryProperties,
                serviceContext
            );
        } else {
            if (log.isInfoEnabled()) {
                log.info("Updating category " + name);
            }

            assetCategory = assetCategoryLocalService.updateCategory(
                    userId,
                    assetCategory.getCategoryId(),
                    parentCategoryId,
                    titleMap,
                    descriptionMap,
                    vocabularyId,
                    categoryProperties,
                    serviceContext
            );
        }

        JSONArray subCategoriesJSONArray = categoryJSONObject.getJSONArray("categories");
        if (subCategoriesJSONArray != null) {
            for (int i = 0; i < subCategoriesJSONArray.length(); i++) {
                JSONObject subCategoryJSONObject = subCategoriesJSONArray.getJSONObject(i);

                addCategory(vocabularyId, assetCategory.getCategoryId(), subCategoryJSONObject, companyId);
            }
        }
    }

    private String toClassNameIds(JSONArray classNames) throws PortalException {
        List<String> result = new ArrayList<>();

        for (int i = 0; i < classNames.length(); i++) {
            String className = classNames.getString(i);

            String classType = StringPool.BLANK;

            String classTypeId = String.valueOf(AssetCategoryConstants.ALL_CLASS_TYPE_PK);

            if (className.indexOf(':') > -1) {
                classType = className.substring(className.indexOf(':') + 1);
                className = className.substring(0, className.indexOf(':'));
            }

            if (log.isDebugEnabled()) {
                log.debug("Looking up class name for " + className);
            }

            ClassName classNameObj = classNameLocalService.fetchClassName(className);

            if ( (classNameObj == null) || (classNameObj.getClassNameId() == 0) ) {
                if (log.isWarnEnabled()) {
                    log.warn("Unable to setup classname " + className + " for vocabulary. Skipping....");
                }
            } else {
                if (Validator.isNotNull(classType)) {
                    String key = getKey(classType);

                    if (log.isDebugEnabled()) {
                        log.debug("Looking up class type for " + key);
                    }

                    DDMStructure ddmStructure = ddmStructureLocalService.fetchStructure(groupId, classNameObj.getClassNameId(), key, true);

                    if (ddmStructure == null) {
                        if (log.isWarnEnabled()) {
                            log.warn("Unable to setup class type " + classType + " for vocabulary. Skipping....");
                        }
                    } else {
                        classTypeId = String.valueOf(ddmStructure.getStructureId());
                    }
                }

                result.add(String.valueOf(classNameObj.getClassNameId()) + StringPool.COLON + classTypeId);
            }
        }

        return StringUtil.merge(result);
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

    private AssetCategory getCategoryByName(long assetVocabularyId, long parentCategoryId, String name) {
        List<AssetCategory> categories = assetCategoryLocalService.getVocabularyCategories(parentCategoryId, assetVocabularyId, QueryUtil.ALL_POS, QueryUtil.ALL_POS, null);
        for (AssetCategory category : categories) {
            if (category.getName().equals(name)) {
                return category;
            }

            String title = category.getTitle(LocaleUtil.getSiteDefault(), true);
            if (Validator.isNotNull(title)) {
                if (title.equals(name)) {
                    return category;
                }
            }
        }

        return null;
    }
}
