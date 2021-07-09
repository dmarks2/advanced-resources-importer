package de.dm.toolbox.liferay.resources.importer.components.lr74.internal;

import com.liferay.dynamic.data.mapping.model.DDMForm;
import com.liferay.dynamic.data.mapping.model.DDMFormLayout;
import com.liferay.dynamic.data.mapping.util.DDM;
import com.liferay.dynamic.data.mapping.util.DDMDataDefinitionConverter;
import de.dm.toolbox.liferay.resources.importer.components.DDMFormLayoutUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
        immediate = true,
        service = DDMFormLayoutUtil.class
)
public class DDMFormLayoutUtil74 implements DDMFormLayoutUtil {

    @Reference
    private DDM ddm;

    @Reference
    private DDMDataDefinitionConverter ddmDataDefinitionConverter;

    @Override
    public DDMFormLayout getDefaultDDMFormLayout(DDMForm ddmForm) {
        DDMFormLayout ddmFormLayout = ddm.getDefaultDDMFormLayout(ddmForm);

        ddmFormLayout = ddmDataDefinitionConverter.convertDDMFormLayoutDataDefinition(ddmForm, ddmFormLayout);

        return ddmFormLayout;
    }
}
