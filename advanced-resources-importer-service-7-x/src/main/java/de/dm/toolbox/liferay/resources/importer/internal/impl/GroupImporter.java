package de.dm.toolbox.liferay.resources.importer.internal.impl;

import com.liferay.asset.kernel.service.AssetCategoryLocalService;
import com.liferay.asset.kernel.service.AssetVocabularyLocalService;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.GroupConstants;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import de.dm.toolbox.liferay.resources.importer.BaseImporter;
import de.dm.toolbox.liferay.resources.importer.Importer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.ServletContext;

@Component(
        immediate = true,
        property = {
                "importer.order=10"
        },
        service = Importer.class
)
public class GroupImporter extends BaseImporter {

    private static final Log log = LogFactoryUtil.getLog(GroupImporter.class);

    @Override
    protected void doRunImport(ServletContext servletContext, long companyId) throws Exception {
        if (
                (GroupConstants.GLOBAL.equals(groupKey)) ||
                (GroupConstants.GUEST.equals(groupKey))
        ) {
            //do not create global or guest group
            return;
        }

        if (Validator.isNull(group)) {
            if (log.isInfoEnabled()) {
                log.info("Creating group " + groupKey);
            }
            User user = userLocalService.getDefaultUser(companyId);

            long userId = user.getUserId();

            groupLocalService.addGroup(
                    userId,
                    GroupConstants.DEFAULT_PARENT_GROUP_ID,
                    StringPool.BLANK,
                    GroupConstants.DEFAULT_PARENT_GROUP_ID,
                    GroupConstants.DEFAULT_LIVE_GROUP_ID,
                    getMap(groupKey),
                    null,
                    GroupConstants.TYPE_SITE_OPEN,
                    true,
                    GroupConstants.DEFAULT_MEMBERSHIP_RESTRICTION,
                    null,
                    true,
                    true,
                    new ServiceContext()
            );
        } else {
            if (log.isInfoEnabled()) {
                log.info("Group " + groupKey + " already exists, skipping...");
            }
        }
    }

    @Override
    @Reference
    protected void setUserLocalService(UserLocalService userLocalService) {
        super.setUserLocalService(userLocalService);
    }

    @Override
    @Reference
    protected void setGroupLocalService(GroupLocalService groupLocalService) {
        super.setGroupLocalService(groupLocalService);
    }

    @Override
    @Reference
    public void setAssetVocabularyLocalService(AssetVocabularyLocalService assetVocabularyLocalService) {
        super.setAssetVocabularyLocalService(assetVocabularyLocalService);
    }

    @Override
    @Reference
    public void setAssetCategoryLocalService(AssetCategoryLocalService assetCategoryLocalService) {
        super.setAssetCategoryLocalService(assetCategoryLocalService);
    }

    @Override
    @Reference
    public void setPortal(Portal portal) {
        super.setPortal(portal);
    }
}
