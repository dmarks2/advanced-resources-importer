package de.dm.toolbox.liferay.resources.importer.internal.impl;

import com.liferay.asset.kernel.service.AssetCategoryLocalService;
import com.liferay.asset.kernel.service.AssetVocabularyLocalService;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.exception.NoSuchPortletPreferencesException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.model.LayoutConstants;
import com.liferay.portal.kernel.model.LayoutPrototype;
import com.liferay.portal.kernel.model.LayoutTypePortlet;
import com.liferay.portal.kernel.model.LayoutTypePortletConstants;
import com.liferay.portal.kernel.model.Theme;
import com.liferay.portal.kernel.portlet.PortletPreferencesFactory;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.LayoutLocalService;
import com.liferay.portal.kernel.service.LayoutPrototypeLocalService;
import com.liferay.portal.kernel.service.LayoutSetLocalService;
import com.liferay.portal.kernel.service.PortletPreferencesLocalService;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.liferay.portal.kernel.service.ThemeLocalService;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.LocalizationUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.PortletKeys;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.UnicodeProperties;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.registry.collections.ServiceTrackerCollections;
import com.liferay.registry.collections.ServiceTrackerMap;
import de.dm.toolbox.liferay.resources.importer.BaseImporter;
import de.dm.toolbox.liferay.resources.importer.PortletPreferencesTranslator;
import de.dm.toolbox.liferay.resources.importer.Importer;
import de.dm.toolbox.liferay.resources.importer.internal.util.ImporterUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import javax.portlet.PortletException;
import javax.portlet.PortletPreferences;
import javax.servlet.ServletContext;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component(
        immediate = true,
        property = {
                "importer.order=80"
        },
        service = Importer.class
)
public class LayoutImporter extends BaseImporter {

    private static final Log log = LogFactoryUtil.getLog(JournalTemplateImporter.class);

    private static final String SITEMAP_JSON = "sitemap.json";

    private static final String JOURNAL_CONTENT_PORTLET_ID = "com_liferay_journal_content_web_portlet_JournalContentPortlet";

    private String defaultLayoutTemplateId;

    @Reference
    private ThemeLocalService themeLocalService;

    @Reference
    private LayoutSetLocalService layoutSetLocalService;

    @Reference
    private LayoutPrototypeLocalService layoutPrototypeLocalService;

    @Reference
    private LayoutLocalService layoutLocalService;

    @Reference
    private PortletPreferencesLocalService portletPreferencesLocalService;

    @Reference
    private PortletPreferencesFactory portletPreferencesFactory;

    @Reference(
            target = "(portlet.preferences.translator.portlet.id=default)"
    )
    private PortletPreferencesTranslator defaultPortletPreferencesTranslator;

    private ServiceTrackerMap<String, PortletPreferencesTranslator> portletPreferencesTranslators;

    @Activate
    public void activate() {
        portletPreferencesTranslators = ServiceTrackerCollections.openSingleValueMap(
                PortletPreferencesTranslator.class,
                "portlet.preferences.translator.portlet.id"
        );
    }

    @Deactivate
    public void deactivate() {
        portletPreferencesTranslators.close();
    }

    @Override
    protected void doRunImport(ServletContext servletContext, long companyId) throws Exception {
        setUpSitemap(servletContext, companyId);
    }

    private void setUpSitemap(ServletContext servletContext, long companyId) throws Exception {
        JSONObject jsonObject = ImporterUtil.getJSONObject(servletContext, resourcesDir, SITEMAP_JSON, companyId, group.getGroupId(), userId);

        if (jsonObject == null) {
            if (log.isDebugEnabled()) {
                log.debug("No " + SITEMAP_JSON + " present. Skipping...");
            }

            return;
        }

        defaultLayoutTemplateId = jsonObject.getString("layoutTemplateId", StringPool.BLANK);

        updateLayoutSetThemeId(jsonObject, companyId);

        JSONArray layoutsJSONArray = jsonObject.getJSONArray("layouts");

        if (layoutsJSONArray != null) {
            addLayouts(
                    false, LayoutConstants.DEFAULT_PARENT_LAYOUT_ID, layoutsJSONArray, companyId
            );
        } else {
            JSONArray publicPagesJSONArray = jsonObject.getJSONArray("publicPages");

            if (publicPagesJSONArray != null) {
                addLayouts(
                        false, LayoutConstants.DEFAULT_PARENT_LAYOUT_ID,  publicPagesJSONArray, companyId
                );
            }

            JSONArray privatePagesJSONArray = jsonObject.getJSONArray("privatePages");

            if (privatePagesJSONArray != null) {
                addLayouts(
                        true, LayoutConstants.DEFAULT_PARENT_LAYOUT_ID, privatePagesJSONArray, companyId
                );
            }
        }
    }

    private void addLayouts(boolean privateLayout, long parentLayoutId, JSONArray layoutsJSONArray, long companyId) throws PortalException, PortletException, IOException {
        if (layoutsJSONArray == null) {
            return;
        }

        for (int i = 0; i < layoutsJSONArray.length(); i++) {
            JSONObject layoutJSONObject = layoutsJSONArray.getJSONObject(i);

            addLayout(privateLayout, parentLayoutId, layoutJSONObject, companyId);
        }
    }

    private void addLayout(boolean privateLayout, long parentLayoutId, JSONObject layoutJSONObject, long companyId) throws PortalException, PortletException, IOException {
        Map<Locale, String> nameMap = getMap(layoutJSONObject, "name");
        Map<Locale, String> titleMap = getMap(layoutJSONObject, "title");

        String type = layoutJSONObject.getString("type");

        if (Validator.isNull(type)) {
            type = LayoutConstants.TYPE_PORTLET;
        }

        String typeSettings = layoutJSONObject.getString("typeSettings");

        boolean hidden = layoutJSONObject.getBoolean("hidden");

        String themeId = layoutJSONObject.getString("themeId");

        String layoutCss = layoutJSONObject.getString("layoutCss");

        String colorSchemeId = layoutJSONObject.getString("colorSchemeId");

        Map<Locale, String> friendlyURLMap = new HashMap<>();

        String friendlyURL = layoutJSONObject.getString("friendlyURL");

        if (Validator.isNotNull(friendlyURL) &&
                !friendlyURL.startsWith(StringPool.SLASH)) {

            friendlyURL = StringPool.SLASH + friendlyURL;
        }

        friendlyURLMap.put(LocaleUtil.getDefault(), friendlyURL);

        ServiceContextThreadLocal.pushServiceContext(serviceContext);

        try {
            String layoutPrototypeName = layoutJSONObject.getString("layoutPrototypeName");

            String layoutPrototypeUuid = null;

            if (Validator.isNotNull(layoutPrototypeName)) {
                LayoutPrototype layoutPrototype = getLayoutPrototype(companyId, layoutPrototypeName);

                layoutPrototypeUuid = layoutPrototype.getUuid();
            } else {
                layoutPrototypeUuid = layoutJSONObject.getString("layoutPrototypeUuid");
            }

            if (Validator.isNotNull(layoutPrototypeUuid)) {
                boolean layoutPrototypeLinkEnabled = GetterUtil.getBoolean(layoutJSONObject.getString("layoutPrototypeLinkEnabled"));

                serviceContext.setAttribute("layoutPrototypeLinkEnabled", layoutPrototypeLinkEnabled);

                serviceContext.setAttribute("layoutPrototypeUuid", layoutPrototypeUuid);
            }

            Layout layout = layoutLocalService.fetchLayoutByFriendlyURL(groupId, privateLayout, friendlyURL);

            if (layout == null) {
                if (log.isInfoEnabled()) {
                    log.info("Creating layout " + friendlyURL);
                }

                layout = layoutLocalService.addLayout(
                        userId,
                        groupId,
                        privateLayout,
                        parentLayoutId,
                        nameMap,
                        titleMap,
                        null,
                        null, null,
                        type,
                        typeSettings,
                        hidden,
                        friendlyURLMap,
                        serviceContext
                );
            } else {
                if (log.isInfoEnabled()) {
                    log.info("Updating layout " + friendlyURL);
                }

                resetLayoutColumns(layout);

                layout = layoutLocalService.updateLayout(
                        groupId,
                        privateLayout,
                        layout.getLayoutId(),
                        parentLayoutId,
                        nameMap,
                        titleMap,
                        layout.getDescriptionMap(),
                        layout.getKeywordsMap(),
                        layout.getRobotsMap(),
                        type,
                        hidden,
                        friendlyURLMap,
                        layout.getIconImage(),
                        null,
                        serviceContext
                );
            }

            if (Validator.isNotNull(themeId) || Validator.isNotNull(colorSchemeId)) {
                layoutLocalService.updateLookAndFeel(
                        groupId,
                        privateLayout,
                        layout.getLayoutId(),
                        themeId,
                        colorSchemeId,
                        layoutCss
                );
            }

            LayoutTypePortlet layoutTypePortlet = (LayoutTypePortlet)layout.getLayoutType();

            String layoutTemplateId = layoutJSONObject.getString("layoutTemplateId", defaultLayoutTemplateId);

            if (Validator.isNotNull(layoutTemplateId)) {
                layoutTypePortlet.setLayoutTemplateId(
                        userId, layoutTemplateId, false
                );
            }

            JSONArray columnsJSONArray = layoutJSONObject.getJSONArray("columns");

            addLayoutColumns(
                    layout, LayoutTypePortletConstants.COLUMN_PREFIX, columnsJSONArray
            );

            layoutLocalService.updateLayout(
                    groupId,
                    layout.isPrivateLayout(),
                    layout.getLayoutId(),
                    layout.getTypeSettings()
            );

            JSONArray layoutsJSONArray = layoutJSONObject.getJSONArray("layouts");

            addLayouts(privateLayout, layout.getLayoutId(), layoutsJSONArray, companyId);
        } finally {
            ServiceContextThreadLocal.popServiceContext();
        }

    }

    private void addLayoutColumns(Layout layout, String columnPrefix, JSONArray columnsJSONArray) throws PortletException, IOException {
        if (columnsJSONArray == null) {
            return;
        }


        for (int i = 0; i < columnsJSONArray.length(); i++) {
            JSONArray columnJSONArray = columnsJSONArray.getJSONArray(i);

            addLayoutColumn(layout, columnPrefix + (i + 1), columnJSONArray);
        }
    }

    private void addLayoutColumn(Layout layout, String columnId, JSONArray columnJSONArray) throws PortletException, IOException {
        if (columnJSONArray == null) {
            return;
        }

        for (int i = 0; i < columnJSONArray.length(); i++) {
            JSONObject portletJSONObject = columnJSONArray.getJSONObject(i);

            if (portletJSONObject == null) {
                String journalArticleId = getJournalId(columnJSONArray.getString(i));

                portletJSONObject = getDefaultPortletJSONObject(journalArticleId);
            }

            addLayoutColumnPortlet(layout, columnId, portletJSONObject);
        }

    }

    private void addLayoutColumnPortlet(Layout layout, String columnId, JSONObject portletJSONObject) throws PortletException, IOException {
        LayoutTypePortlet layoutTypePortlet = (LayoutTypePortlet)layout.getLayoutType();

        String rootPortletId = portletJSONObject.getString("portletId");

        if (Validator.isNull(rootPortletId)) {
            if (log.isWarnEnabled()) {
                log.warn("PortletID is not specified, unable to import");
            }

            return;
        }

        PortletPreferencesTranslator portletPreferencesTranslator = portletPreferencesTranslators.getService(rootPortletId);

        if (Validator.isNull(portletPreferencesTranslator)) {
            portletPreferencesTranslator = defaultPortletPreferencesTranslator;
        }

        String portletId = layoutTypePortlet.addPortletId(
                userId, rootPortletId, columnId, -1, false
        );

        if (portletId == null) {
            return;
        }

        JSONObject portletPreferencesJSONObject = portletJSONObject.getJSONObject("portletPreferences");

        if (
                (portletPreferencesJSONObject == null) ||
                (portletPreferencesJSONObject.length() == 0)
        ) {
            return;
        }

        if (portletPreferencesTranslator != null) {
            PortletPreferences portletSetup = portletPreferencesFactory.getLayoutPortletSetup(layout, portletId);

            Iterator<String> iterator = portletPreferencesJSONObject.keys();

            while (iterator.hasNext()) {
                String key = iterator.next();

                portletPreferencesTranslator.translate(portletPreferencesJSONObject, key, portletSetup, groupId);
            }

            portletSetup.store();
        }

        if (rootPortletId.equals(PortletKeys.NESTED_PORTLETS)) {
            JSONArray columnsJSONArray = portletPreferencesJSONObject.getJSONArray("columns");

            StringBundler sb = new StringBundler(4);

            sb.append(StringPool.UNDERLINE);
            sb.append(portletId);
            sb.append(StringPool.DOUBLE_UNDERLINE);
            sb.append(LayoutTypePortletConstants.COLUMN_PREFIX);

            addLayoutColumns(layout, sb.toString(), columnsJSONArray);
        }
    }

    private void updateLayoutSetThemeId(JSONObject sitemapJSONObject, long companyId) throws PortalException {
        String themeId = sitemapJSONObject.getString("themeId");

        if (Validator.isNotNull(themeId)) {
            Theme theme = themeLocalService.fetchTheme(companyId, themeId);

            if (theme == null) {
                if (log.isWarnEnabled()) {
                    log.warn("Theme ID " + themeId + " was given but no theme found with this ID. Skipping...");
                }

                themeId = null;
            }
        }

        if (Validator.isNotNull(themeId)) {
            if (log.isDebugEnabled()) {
                log.debug("Updating Layout Set theme to " + themeId);
            }
            layoutSetLocalService.updateLookAndFeel(
                    groupId, themeId, null, null
            );
        }
    }

    protected Map<Locale, String> getMap(JSONObject layoutJSONObject, String name) {
        Map<Locale, String> map = new HashMap<>();

        JSONObject jsonObject = layoutJSONObject.getJSONObject(name.concat("Map"));

        if (jsonObject != null) {
            map = (Map<Locale, String>) LocalizationUtil.deserialize(jsonObject);

            if (!map.containsKey(LocaleUtil.getDefault())) {
                Iterator<String> keys = jsonObject.keys();
                if (keys.hasNext()) {
                    String key = keys.next();

                    String value = jsonObject.getString(key);

                    map.put(LocaleUtil.getDefault(), value);
                }
            }
        }
        else {
            String value = layoutJSONObject.getString(name);

            map.put(LocaleUtil.getDefault(), value);
        }

        return map;
    }

    protected LayoutPrototype getLayoutPrototype(long companyId, String name) {
        Locale locale = LocaleUtil.getDefault();

        List<LayoutPrototype> layoutPrototypes =
                layoutPrototypeLocalService.search(
                        companyId, null, QueryUtil.ALL_POS, QueryUtil.ALL_POS, null);

        for (LayoutPrototype layoutPrototype : layoutPrototypes) {
            if (name.equals(layoutPrototype.getName(locale))) {
                return layoutPrototype;
            }
        }

        return null;
    }

    protected void resetLayoutColumns(Layout layout) {
        UnicodeProperties typeSettings = layout.getTypeSettingsProperties();

        Set<Map.Entry<String, String>> set = typeSettings.entrySet();

        Iterator<Map.Entry<String, String>> iterator = set.iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();

            String key = entry.getKey();

            if (!key.startsWith("column-")) {
                continue;
            }

            String[] portletIds = StringUtil.split(entry.getValue());

            for (String portletId : portletIds) {
                try {
                    portletPreferencesLocalService.deletePortletPreferences(
                            PortletKeys.PREFS_OWNER_ID_DEFAULT,
                            PortletKeys.PREFS_OWNER_TYPE_LAYOUT, layout.getPlid(),
                            portletId);
                } catch (NoSuchPortletPreferencesException pe) {
                    //no preferences were present
                } catch (PortalException e) {
                    if (log.isWarnEnabled()) {
                        log.warn(
                                "Unable to delete portlet preferences for " +
                                        "portlet " + portletId,
                                e);
                    }
                }
            }

            iterator.remove();
        }

        layout.setTypeSettingsProperties(typeSettings);

        layoutLocalService.updateLayout(layout);
    }

    protected JSONObject getDefaultPortletJSONObject(String journalArticleId) {
        JSONObject portletJSONObject = JSONFactoryUtil.createJSONObject();

        portletJSONObject.put("portletId", JOURNAL_CONTENT_PORTLET_ID);

        JSONObject portletPreferencesJSONObject = JSONFactoryUtil.createJSONObject();

        portletPreferencesJSONObject.put("articleId", journalArticleId);
        portletPreferencesJSONObject.put("groupId", groupId);
        portletPreferencesJSONObject.put("portletSetupPortletDecoratorId", "borderless");

        portletJSONObject.put("portletPreferences", portletPreferencesJSONObject);

        return portletJSONObject;
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
