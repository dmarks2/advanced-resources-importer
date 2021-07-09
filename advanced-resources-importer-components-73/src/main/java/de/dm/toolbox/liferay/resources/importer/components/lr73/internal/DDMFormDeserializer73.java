package de.dm.toolbox.liferay.resources.importer.components.lr73.internal;

import com.liferay.dynamic.data.mapping.io.DDMFormDeserializerDeserializeRequest;
import com.liferay.dynamic.data.mapping.io.DDMFormDeserializerDeserializeResponse;
import com.liferay.dynamic.data.mapping.model.DDMForm;
import com.liferay.portal.kernel.exception.PortalException;
import de.dm.toolbox.liferay.resources.importer.components.DDMFormDeserializer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
        immediate = true,
        service = DDMFormDeserializer.class
)
public class DDMFormDeserializer73 implements DDMFormDeserializer {

    @Reference
    private com.liferay.dynamic.data.mapping.io.DDMFormDeserializer ddmFormDeserializer;

    @Override
    public DDMForm deserializeJSONDDMForm(String content, long groupId, String parentDDMStructureKey) throws PortalException {
        DDMFormDeserializerDeserializeRequest.Builder builder =
                DDMFormDeserializerDeserializeRequest.Builder.newBuilder(content);

        DDMFormDeserializerDeserializeResponse
                ddmFormDeserializerDeserializeResponse =
                ddmFormDeserializer.deserialize(builder.build());

        return ddmFormDeserializerDeserializeResponse.getDDMForm();
    }

    @Override
    public DDMForm deserializeXSD(String content, long groupId, String parentDDMStructureKey) throws PortalException {
        DDMFormDeserializerDeserializeRequest.Builder builder =
                DDMFormDeserializerDeserializeRequest.Builder.newBuilder(content);

        DDMFormDeserializerDeserializeResponse
                ddmFormDeserializerDeserializeResponse =
                ddmFormDeserializer.deserialize(builder.build());

        return ddmFormDeserializerDeserializeResponse.getDDMForm();
    }
}
