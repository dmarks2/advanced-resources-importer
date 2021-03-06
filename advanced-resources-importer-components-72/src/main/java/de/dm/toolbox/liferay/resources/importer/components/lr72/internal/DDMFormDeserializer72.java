package de.dm.toolbox.liferay.resources.importer.components.lr72.internal;

import com.liferay.dynamic.data.mapping.io.DDMFormJSONDeserializer;
import com.liferay.dynamic.data.mapping.io.DDMFormXSDDeserializer;
import com.liferay.dynamic.data.mapping.model.DDMForm;
import com.liferay.portal.kernel.exception.PortalException;
import de.dm.toolbox.liferay.resources.importer.components.DDMFormDeserializer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
        immediate = true,
        service = DDMFormDeserializer.class
)
public class DDMFormDeserializer72 implements DDMFormDeserializer {

    @Reference
    private DDMFormJSONDeserializer ddmFormJSONDeserializer;

    @Reference
    private DDMFormXSDDeserializer ddmFormXSDDeserializer;

    @Override
    public DDMForm deserializeJSONDDMForm(String content, long groupId, String parentDDMStructureKey) throws PortalException {
        return ddmFormJSONDeserializer.deserialize(content);
    }

    @Override
    public DDMForm deserializeXSD(String content, long groupId, String parentDDMStructureKey) throws PortalException {
        return ddmFormXSDDeserializer.deserialize(content);
    }
}
