package de.dm.toolbox.liferay.resources.importer.internal.preferences;

import com.liferay.portal.kernel.json.JSONObject;
import de.dm.toolbox.liferay.resources.importer.PortletPreferencesTranslator;
import org.osgi.service.component.annotations.Component;

import javax.portlet.PortletException;
import javax.portlet.PortletPreferences;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Component(
        immediate = true,
        property = "portlet.preferences.translator.portlet.id=com_liferay_rss_web_portlet_RSSPortlet",
        service = PortletPreferencesTranslator.class
)
public class RSSPortletPreferencesTranslator implements PortletPreferencesTranslator {

    @Override
    public void translate(JSONObject portletPreferencesJSONObject, String key, PortletPreferences portletPreferences, long groupId) throws PortletException {
        if (!key.equals("titles") && !key.equals("urls")) {
            String value = portletPreferencesJSONObject.getString(key);

            portletPreferences.setValue(key, value);

            return;
        }

        List<String> valuesList = new ArrayList<>();

        JSONObject jsonObject = portletPreferencesJSONObject.getJSONObject(key);

        Iterator<String> iterator = jsonObject.keys();

        while (iterator.hasNext()) {
            String jsonObjectKey = iterator.next();

            valuesList.add(jsonObject.getString(jsonObjectKey));
        }

        String[] values = valuesList.toArray(new String[valuesList.size()]);

        portletPreferences.setValues(key, values);
    }
}
