package de.dm.toolbox.liferay.resources.importer.components;

import com.liferay.dynamic.data.mapping.model.DDMForm;
import com.liferay.portal.kernel.exception.PortalException;

public interface DDMFormDeserializer {

    DDMForm deserializeJSONDDMForm(String content, long groupId, String parentDDMStructureKey) throws PortalException;

    DDMForm deserializeXSD(String content, long groupId, String parentDDMStructureKey) throws PortalException;

}
