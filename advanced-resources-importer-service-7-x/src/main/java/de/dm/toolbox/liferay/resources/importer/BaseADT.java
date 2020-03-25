package de.dm.toolbox.liferay.resources.importer;

import com.liferay.portal.kernel.model.ClassName;
import com.liferay.portal.kernel.service.ClassNameLocalService;
import com.liferay.portal.kernel.util.Validator;

public abstract class BaseADT implements ADT {

    private ClassNameLocalService classNameLocalService;

    public void setClassNameLocalService(ClassNameLocalService classNameLocalService) {
        this.classNameLocalService = classNameLocalService;
    }

    @Override
    public boolean isEnabled() {
        ClassName className = classNameLocalService.fetchClassName(getClassName());

        return (Validator.isNotNull(className));
    }
}
