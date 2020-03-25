package de.dm.toolbox.liferay.resources.importer.internal.util;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.GroupConstants;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import de.dm.toolbox.liferay.resources.importer.Importer;

import javax.servlet.ServletContext;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Set;

public class ImporterUtil {

    public static String getResourcePath(String resourcesDir, String dirName) {
        if (resourcesDir.endsWith(StringPool.SLASH) &&dirName.startsWith(StringPool.SLASH)) {
            return resourcesDir.concat(dirName.substring(1));
        }

        return resourcesDir.concat(dirName);
    }

    public static InputStream getInputStream(ServletContext servletContext, String resourcesDir, String fileName) throws Exception {
        String resourcePath = getResourcePath(resourcesDir, fileName);

        URL url = servletContext.getResource(resourcePath);

        if (url == null) {
            return null;
        }

        URLConnection urlConnection = url.openConnection();

        return urlConnection.getInputStream();
    }

    public static JSONObject getJSONObject(ServletContext servletContext, String resourcesDir, String fileName, long companyId, long groupId, long userId) throws Exception {
        String json = null;

        InputStream inputStream = getInputStream(servletContext, resourcesDir, fileName);

        if (inputStream == null) {
            return null;
        }

        try {
            json = StringUtil.read(inputStream);
        } finally {
            inputStream.close();
        }

        json = StringUtil.replace(
                json,
                new String[] {"${companyId}", "${groupId}", "${userId}"},
                new String[] {
                        String.valueOf(companyId),
                        String.valueOf(groupId),
                        String.valueOf(userId)
                });

        return JSONFactoryUtil.createJSONObject(json);
    }

    public static Group getGroup(GroupLocalService groupLocalService, long companyId, String groupKey) throws PortalException {
        if (GroupConstants.GLOBAL.equals(groupKey)) {
            return groupLocalService.getCompanyGroup(companyId);
        }

        if (GroupConstants.GUEST.equals(groupKey)) {
            return groupLocalService.getGroup(companyId, GroupConstants.GUEST);
        }

        return groupLocalService.fetchGroup(companyId, groupKey);
    }

    public static String getResourcesDir(ServletContext servletContext) {
        Set<String> warResourcesPath = servletContext.getResourcePaths(Importer.WAR_RESOURCES_DIR);
        Set<String> warTemplatesPath = servletContext.getResourcePaths(Importer.WAR_TEMPLATES_DIR);
        Set<String> jarResourcesPath = servletContext.getResourcePaths(Importer.JAR_RESOURCES_DIR);
        Set<String> jarTemplatesPath = servletContext.getResourcePaths(Importer.JAR_TEMPLATES_DIR);

        if ( (warResourcesPath != null) && (!warResourcesPath.isEmpty()) ) {
            return Importer.WAR_RESOURCES_DIR;
        }

        if ( (warTemplatesPath != null) && (!warTemplatesPath.isEmpty()) ) {
            return Importer.WAR_TEMPLATES_DIR;
        }

        if ( (jarResourcesPath != null) && (!jarResourcesPath.isEmpty()) ) {
            return Importer.JAR_RESOURCES_DIR;
        }

        if ( (jarTemplatesPath != null) && (!jarTemplatesPath.isEmpty()) ) {
            return Importer.JAR_TEMPLATES_DIR;
        }

        return null;
    }
}
