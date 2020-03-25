package de.dm.toolbox.liferay.resources.importer.internal.preferences;

import com.liferay.dynamic.data.mapping.model.DDMStructure;
import com.liferay.dynamic.data.mapping.service.DDMStructureLocalService;
import com.liferay.journal.model.JournalArticle;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.ClassName;
import com.liferay.portal.kernel.service.ClassNameLocalService;
import com.liferay.portal.kernel.util.CharPool;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import de.dm.toolbox.liferay.resources.importer.PortletPreferencesTranslator;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.portlet.PortletException;
import javax.portlet.PortletPreferences;
import java.util.ArrayList;
import java.util.List;

@Component(
        immediate = true,
        property = "portlet.preferences.translator.portlet.id=com_liferay_asset_publisher_web_portlet_AssetPublisherPortlet",
        service = PortletPreferencesTranslator.class
)
public class AssetPublisherPortletPreferencesTranslator implements PortletPreferencesTranslator {

    private static final Log log = LogFactoryUtil.getLog(AssetPublisherPortletPreferencesTranslator.class);

    @Reference
    private Portal portal;

    @Reference
    private DDMStructureLocalService ddmStructureLocalService;

    @Reference
    private ClassNameLocalService classNameLocalService;

    @Override
    public void translate(JSONObject portletPreferencesJSONObject, String key, PortletPreferences portletPreferences, long groupId) throws PortletException {
        String value = portletPreferencesJSONObject.getString(key);

        if (key.equals("anyAssetType")) {
            if (Validator.isNotNull(value)) {
                String[] parts = StringUtil.split(value, ",");

                List<String> classNameIds = new ArrayList<>();

                for (String part : parts) {
                    ClassName className = classNameLocalService.fetchClassName(part);

                    if ((Validator.isNull(className)) || (className.getClassNameId() == 0)) {
                        if (log.isWarnEnabled()) {
                            log.warn("Unable to determine classNameId for class " + part);
                        }
                    } else {
                        classNameIds.add(String.valueOf(className.getClassNameId()));
                    }
                }

                value = ListUtil.toString(classNameIds, (String)null);
            }
        } else if (
                (key.equals("classTypeIds")) ||
                (key.equals("anyClassTypeJournalArticleAssetRendererFactory"))
        ) {
            String anyAssetType = portletPreferencesJSONObject.getString("anyAssetType");
            try {
                long anyAssetTypeClassNameId = Long.parseLong(anyAssetType);

                ClassName className = classNameLocalService.fetchByClassNameId(anyAssetTypeClassNameId);

                if ( (Validator.isNull(className)) || (className.getClassNameId() == 0)) {
                    if (log.isWarnEnabled()) {
                        log.warn("Unable to determine ClassName for classNameId " + anyAssetTypeClassNameId);
                    }
                } else {
                    anyAssetType = className.getClassName();
                }
            } catch (NumberFormatException e) {
                //was not a number
            }

            if ("com.liferay.journal.model.JournalArticle".equals(anyAssetType)) {
                if (Validator.isNotNull(value)) {
                    String[] parts = StringUtil.split(value, ",");

                    List<String> classTypeIds = new ArrayList<>();

                    long journalArticleClassNameId = portal.getClassNameId(JournalArticle.class);

                    for (String part : parts) {
                        String ddmStructureKey = getKey(part);

                        try {
                            DDMStructure ddmStructure = ddmStructureLocalService.fetchStructure(
                                    groupId,
                                    journalArticleClassNameId,
                                    ddmStructureKey,
                                    true
                            );

                            if (log.isDebugEnabled()) {
                                log.debug("Adding " + ddmStructure.getStructureId() + " as classTypeId for Structure " + part);
                            }

                            classTypeIds.add(String.valueOf(ddmStructure.getStructureId()));

                        } catch (PortalException e) {
                            if (log.isWarnEnabled()) {
                                log.warn("Unable to find classTypeId for Structure " + part);
                            }
                            throw new PortletException(e.getMessage(), e);
                        }
                    }

                    value = ListUtil.toString(classTypeIds, (String)null);
                }
            }
        }

        portletPreferences.setValue(key, value);
    }

    protected String getKey(String name) {
        name = StringUtil.replace(name, CharPool.SPACE, CharPool.DASH);

        name = StringUtil.toUpperCase(name);

        return name;
    }
}
