package de.dm.toolbox.liferay.resources.importer.components.lr72.internal;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.service.LayoutLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import de.dm.toolbox.liferay.resources.importer.components.LayoutHelper;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.Locale;
import java.util.Map;

@Component(
        immediate = true,
        service = LayoutHelper.class
)
public class LayoutHelper72 implements LayoutHelper {

    @Reference
    private LayoutLocalService layoutLocalService;

    public Layout updateLayout(long groupId, boolean privateLayout, Layout layout, long parentLayoutId, Map<Locale, String> nameMap,  Map<Locale, String> titleMap,
                               String type, boolean hidden, Map<Locale, String> friendlyURLMap, ServiceContext serviceContext) throws PortalException {
        return layoutLocalService.updateLayout(
                groupId,
                privateLayout,
                layout.getLayoutId(),
                parentLayoutId,
                nameMap,
                titleMap,
                layout.getDescriptionMap(),
                layout.getKeywordsMap(),
                layout.getRobotsMap(),
                type,
                hidden,
                friendlyURLMap,
                layout.getIconImage(),
                null,
                serviceContext
        );
    }

}
