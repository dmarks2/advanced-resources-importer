package de.dm.toolbox.liferay.resources.importer.components;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.service.ServiceContext;

import java.util.Locale;
import java.util.Map;

public interface LayoutHelper {

    Layout updateLayout(long groupId, boolean privateLayout, Layout layout, long parentLayoutId, Map<Locale, String> nameMap, Map<Locale, String> titleMap,
                        String type, boolean hidden, Map<Locale, String> friendlyURLMap, ServiceContext serviceContext) throws PortalException;

}
