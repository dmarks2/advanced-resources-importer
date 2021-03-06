package de.dm.toolbox.liferay.resources.importer.components.lr72.internal;

import com.liferay.dynamic.data.mapping.model.DDMForm;
import com.liferay.dynamic.data.mapping.model.DDMFormLayout;
import com.liferay.dynamic.data.mapping.util.DDM;
import de.dm.toolbox.liferay.resources.importer.components.DDMFormLayoutUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
        immediate = true,
        service = DDMFormLayoutUtil.class
)
public class DDMFormLayoutUtil72 implements DDMFormLayoutUtil {

    @Reference
    private DDM ddm;

    @Override
    public DDMFormLayout getDefaultDDMFormLayout(DDMForm ddmForm) {
        return ddm.getDefaultDDMFormLayout(ddmForm);
    }
}
