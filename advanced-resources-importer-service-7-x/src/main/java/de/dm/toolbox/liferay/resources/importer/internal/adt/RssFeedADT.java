package de.dm.toolbox.liferay.resources.importer.internal.adt;

import com.liferay.portal.kernel.service.ClassNameLocalService;
import de.dm.toolbox.liferay.resources.importer.ADT;
import de.dm.toolbox.liferay.resources.importer.BaseADT;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
        immediate = true,
        property = {
            "key=rss_feed"
        },
        service = ADT.class
)
public class RssFeedADT extends BaseADT {

    @Override
    public String getClassName() {
        return "com.liferay.rss.web.util.RSSFeed";
    }

    @Reference
    @Override
    public void setClassNameLocalService(ClassNameLocalService classNameLocalService) {
        super.setClassNameLocalService(classNameLocalService);
    }
}
