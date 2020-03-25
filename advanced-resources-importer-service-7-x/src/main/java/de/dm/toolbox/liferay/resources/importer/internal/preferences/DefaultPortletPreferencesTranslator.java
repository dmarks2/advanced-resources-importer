package de.dm.toolbox.liferay.resources.importer.internal.preferences;

import com.liferay.portal.kernel.json.JSONObject;
import de.dm.toolbox.liferay.resources.importer.PortletPreferencesTranslator;
import org.osgi.service.component.annotations.Component;

import javax.portlet.PortletException;
import javax.portlet.PortletPreferences;

@Component(
        immediate = true,
        property = "portlet.preferences.translator.portlet.id=default",
        service = PortletPreferencesTranslator.class
)
public class DefaultPortletPreferencesTranslator implements PortletPreferencesTranslator{

    @Override
    public void translate(JSONObject portletPreferencesJSONObject, String key, PortletPreferences portletPreferences, long groupId) throws PortletException {
        String value = portletPreferencesJSONObject.getString(key);

        portletPreferences.setValue(key, value);
    }
}
