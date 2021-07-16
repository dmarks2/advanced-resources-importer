package de.dm.toolbox.liferay.resources.importer.components.lr72.internal.impl;

import de.dm.toolbox.liferay.resources.importer.FragmentsImporter;
import org.osgi.service.component.annotations.Component;

@Component(
        immediate = true,
        service = FragmentsImporter.class
)
public class FragmentsImporterDummy implements FragmentsImporter {
}
