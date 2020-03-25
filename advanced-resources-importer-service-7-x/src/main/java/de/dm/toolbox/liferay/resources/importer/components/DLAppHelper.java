package de.dm.toolbox.liferay.resources.importer.components;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.service.ServiceContext;

import java.io.InputStream;

public interface DLAppHelper {

    FileEntry updateFileEntry(long userId, long fileEntryId,
                              String sourceFileName, String mimeType,
                              String title, String description,
                              String changeLog, boolean majorVersion, InputStream is,
                              long size, ServiceContext serviceContext) throws PortalException;
}
