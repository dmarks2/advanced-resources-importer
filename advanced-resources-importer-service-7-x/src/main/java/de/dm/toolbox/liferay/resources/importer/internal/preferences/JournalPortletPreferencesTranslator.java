package de.dm.toolbox.liferay.resources.importer.internal.preferences;

import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.util.CharPool;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.StringUtil;
import de.dm.toolbox.liferay.resources.importer.PortletPreferencesTranslator;
import org.osgi.service.component.annotations.Component;

import javax.portlet.PortletException;
import javax.portlet.PortletPreferences;

@Component(
        immediate = true,
        property = "portlet.preferences.translator.portlet.id=com_liferay_journal_content_web_portlet_JournalContentPortlet",
        service = PortletPreferencesTranslator.class
)
public class JournalPortletPreferencesTranslator implements PortletPreferencesTranslator {

    @Override
    public void translate(JSONObject portletPreferencesJSONObject, String key, PortletPreferences portletPreferences, long groupId) throws PortletException {
        String value = portletPreferencesJSONObject.getString(key);

        if (key.equals("articleId")) {
            String articleId = FileUtil.stripExtension(value);

            articleId = StringUtil.toUpperCase(articleId);

            value = StringUtil.replace(articleId, CharPool.SPACE, CharPool.DASH);
        }

        portletPreferences.setValue(key, value);

    }
}
