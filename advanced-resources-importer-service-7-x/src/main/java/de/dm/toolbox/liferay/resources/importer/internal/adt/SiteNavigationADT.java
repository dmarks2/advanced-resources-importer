package de.dm.toolbox.liferay.resources.importer.internal.adt;

import com.liferay.portal.kernel.service.ClassNameLocalService;
import de.dm.toolbox.liferay.resources.importer.BaseADT;
import de.dm.toolbox.liferay.resources.importer.ADT;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
        immediate = true,
        property = {
            "key=site_navigation"
        },
        service = ADT.class
)
public class SiteNavigationADT extends BaseADT {

    @Override
    public String getClassName() {
        return "com.liferay.portal.kernel.theme.NavItem";
    }

    @Reference
    @Override
    public void setClassNameLocalService(ClassNameLocalService classNameLocalService) {
        super.setClassNameLocalService(classNameLocalService);
    }
}
