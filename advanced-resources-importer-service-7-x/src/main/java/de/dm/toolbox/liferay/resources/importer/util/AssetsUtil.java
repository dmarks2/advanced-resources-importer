package de.dm.toolbox.liferay.resources.importer.util;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.Validator;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.ServletContext;
import java.util.HashMap;
import java.util.Map;

@Component(
        immediate = true,
        service = AssetsUtil.class
)
public class AssetsUtil {

    private static String ASSETS_JSON = "assets.json";

    @Reference
    private UserLocalService userLocalService;

    @Reference
    private GroupLocalService groupLocalService;

    public Map<String, JSONObject> getAssetJSONObjectMap(ServletContext servletContext, long companyId, String groupKey) throws Exception {
        Group group = ImporterUtil.getGroup(groupLocalService, companyId, groupKey);

        Map<String, JSONObject> result = new HashMap<>();

        if (Validator.isNotNull(group)) {
            User user = userLocalService.getDefaultUser(companyId);

            long userId = user.getUserId();

            String resourcesDir = ImporterUtil.getResourcesDir(servletContext);

            if (Validator.isNotNull(resourcesDir)) {
                JSONObject jsonObject = ImporterUtil.getJSONObject(servletContext, resourcesDir, ASSETS_JSON, companyId, group.getGroupId(), userId);
                if (jsonObject != null) {
                    JSONArray assetsJSONArray = jsonObject.getJSONArray("assets");

                    if (assetsJSONArray != null) {
                        for (int i = 0; i < assetsJSONArray.length(); i++) {
                            JSONObject assetJSONObject = assetsJSONArray.getJSONObject(i);

                            String name = assetJSONObject.getString("name");

                            result.put(name, assetJSONObject);
                        }
                    }
                }
            }
        }

        return result;
    }

}
