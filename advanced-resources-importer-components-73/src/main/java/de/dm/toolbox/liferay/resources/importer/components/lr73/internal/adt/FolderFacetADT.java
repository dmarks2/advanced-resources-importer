package de.dm.toolbox.liferay.resources.importer.components.lr73.internal.adt;

import com.liferay.portal.kernel.service.ClassNameLocalService;
import de.dm.toolbox.liferay.resources.importer.ADT;
import de.dm.toolbox.liferay.resources.importer.BaseADT;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
        immediate = true,
        property = {
                "key=folder_facet"
        },
        service = ADT.class
)
public class FolderFacetADT extends BaseADT {

    @Override
    public String getClassName() {
        return "com.liferay.portal.search.web.internal.facet.display.context.FolderSearchFacetTermDisplayContext";
    }

    @Reference
    @Override
    public void setClassNameLocalService(ClassNameLocalService classNameLocalService) {
        super.setClassNameLocalService(classNameLocalService);
    }
}
