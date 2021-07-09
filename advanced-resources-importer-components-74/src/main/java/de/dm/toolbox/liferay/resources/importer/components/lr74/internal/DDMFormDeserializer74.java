package de.dm.toolbox.liferay.resources.importer.components.lr74.internal;

import com.liferay.dynamic.data.mapping.constants.DDMStructureConstants;
import com.liferay.dynamic.data.mapping.io.DDMFormDeserializerDeserializeRequest;
import com.liferay.dynamic.data.mapping.io.DDMFormDeserializerDeserializeResponse;
import com.liferay.dynamic.data.mapping.model.DDMForm;
import com.liferay.dynamic.data.mapping.model.DDMStructure;
import com.liferay.dynamic.data.mapping.service.DDMStructureLocalService;
import com.liferay.dynamic.data.mapping.util.DDMDataDefinitionConverter;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.service.ClassNameLocalService;
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

    @Reference
    private DDMStructureLocalService ddmStructureLocalService;

    @Reference
    private ClassNameLocalService classNameLocalService;

    @Reference
    private DDMDataDefinitionConverter ddmDataDefinitionConverter;

    @Override
    public DDMForm deserializeJSONDDMForm(String content, long groupId, String parentDDMStructureKey) throws PortalException {
        DDMFormDeserializerDeserializeRequest.Builder builder =
                DDMFormDeserializerDeserializeRequest.Builder.newBuilder(content);

        DDMFormDeserializerDeserializeResponse
                ddmFormDeserializerDeserializeResponse =
                ddmFormDeserializer.deserialize(builder.build());

        DDMForm ddmForm = ddmFormDeserializerDeserializeResponse.getDDMForm();

        return convertDDMForm(groupId, parentDDMStructureKey, ddmForm);
    }

    @Override
    public DDMForm deserializeXSD(String content, long groupId, String parentDDMStructureKey) throws PortalException {
        DDMFormDeserializerDeserializeRequest.Builder builder =
                DDMFormDeserializerDeserializeRequest.Builder.newBuilder(content);

        DDMFormDeserializerDeserializeResponse
                ddmFormDeserializerDeserializeResponse =
                ddmFormDeserializer.deserialize(builder.build());

        DDMForm ddmForm = ddmFormDeserializerDeserializeResponse.getDDMForm();

        return convertDDMForm(groupId, parentDDMStructureKey, ddmForm);
    }

    private DDMForm convertDDMForm(long groupId, String parentDDMStructureKey, DDMForm ddmForm) {
        long classNameId = classNameLocalService.getClassNameId("com.liferay.journal.model.JournalArticle");

        DDMStructure parentStructure = ddmStructureLocalService.fetchStructure(groupId, classNameId, parentDDMStructureKey);

        long parentStructureLayoutId = 0;

        long parentStructureId = DDMStructureConstants.DEFAULT_PARENT_STRUCTURE_ID;

        if (parentStructure != null) {
            parentStructureId = parentStructure.getStructureId();

            parentStructureLayoutId = parentStructure.getDefaultDDMStructureLayoutId();
        }

        return ddmDataDefinitionConverter.convertDDMFormDataDefinition(ddmForm, parentStructureId, parentStructureLayoutId);
    }
}
