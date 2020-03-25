package de.dm.toolbox.liferay.resources.importer.internal.adt;

import com.liferay.portal.kernel.service.ClassNameLocalService;
import de.dm.toolbox.liferay.resources.importer.BaseADT;
import de.dm.toolbox.liferay.resources.importer.ADT;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
        immediate = true,
        property = {
            "key=asset_category"
        },
        service = ADT.class
)
public class AssetCategoryADT extends BaseADT {

    @Override
    public String getClassName() {
        return "com.liferay.asset.kernel.model.AssetCategory";
    }

    @Reference
    @Override
    public void setClassNameLocalService(ClassNameLocalService classNameLocalService) {
        super.setClassNameLocalService(classNameLocalService);
    }
}
