package de.dm.toolbox.liferay.resources.importer;

import com.liferay.portal.kernel.json.JSONObject;

import javax.servlet.ServletContext;
import java.util.Map;

public interface Importer {

    String WAR_RESOURCES_DIR = "/WEB-INF/classes/resources-importer/";
    String WAR_TEMPLATES_DIR = "/WEB-INF/classes/templates-importer/";

    String JAR_RESOURCES_DIR = "/resources-importer/";
    String JAR_TEMPLATES_DIR = "/templates-importer/";

    void runImport(ServletContext servletContext, long companyId, String groupKey, Map<String, JSONObject> assetJSONObjectMap) throws Exception;
}
