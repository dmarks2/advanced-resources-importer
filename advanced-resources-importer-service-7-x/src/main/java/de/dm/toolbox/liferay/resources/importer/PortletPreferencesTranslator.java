package de.dm.toolbox.liferay.resources.importer;

import com.liferay.portal.kernel.json.JSONObject;

import javax.portlet.PortletException;
import javax.portlet.PortletPreferences;

public interface PortletPreferencesTranslator {

    void translate(JSONObject portletPreferencesJSONObject, String key, PortletPreferences portletPreferences, long groupId) throws PortletException;

}
