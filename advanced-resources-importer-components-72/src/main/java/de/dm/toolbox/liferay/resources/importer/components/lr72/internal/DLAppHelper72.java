package de.dm.toolbox.liferay.resources.importer.components.lr72.internal;

import com.liferay.document.library.kernel.service.DLAppLocalService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.service.ServiceContext;
import de.dm.toolbox.liferay.resources.importer.components.DLAppHelper;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.InputStream;

@Component(
        immediate = true,
        service = DLAppHelper.class
)
public class DLAppHelper72 implements DLAppHelper {

    @Reference
    private DLAppLocalService dlAppLocalService;

    @Override
    public FileEntry updateFileEntry(long userId, long fileEntryId, String sourceFileName, String mimeType, String title, String description, String changeLog, boolean majorVersion, InputStream is, long size, ServiceContext serviceContext) throws PortalException {
        return dlAppLocalService.updateFileEntry(
                userId,
                fileEntryId,
                sourceFileName,
                mimeType,
                title,
                description,
                changeLog,
                majorVersion,
                is,
                size,
                serviceContext
        );
    }
}
