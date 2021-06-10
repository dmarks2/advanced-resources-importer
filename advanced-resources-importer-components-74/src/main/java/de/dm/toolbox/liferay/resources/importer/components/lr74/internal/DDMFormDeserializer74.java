package de.dm.toolbox.liferay.resources.importer.components.lr74.internal;

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
public class DDMFormDeserializer74 implements DDMFormDeserializer {

    @Reference
    private com.liferay.dynamic.data.mapping.io.DDMFormDeserializer ddmFormDeserializer;

    @Override
    public DDMForm deserializeJSONDDMForm(String content) throws PortalException {
        DDMFormDeserializerDeserializeRequest.Builder builder =
                DDMFormDeserializerDeserializeRequest.Builder.newBuilder(content);

        DDMFormDeserializerDeserializeResponse
                ddmFormDeserializerDeserializeResponse =
                ddmFormDeserializer.deserialize(builder.build());

        return ddmFormDeserializerDeserializeResponse.getDDMForm();
    }

    @Override
    public DDMForm deserializeXSD(String content) throws PortalException {
        DDMFormDeserializerDeserializeRequest.Builder builder =
                DDMFormDeserializerDeserializeRequest.Builder.newBuilder(content);

        DDMFormDeserializerDeserializeResponse
                ddmFormDeserializerDeserializeResponse =
                ddmFormDeserializer.deserialize(builder.build());

        return ddmFormDeserializerDeserializeResponse.getDDMForm();
    }
}
