package de.dm.toolbox.liferay.resources.importer.components.lr74.internal.impl;

import com.liferay.asset.kernel.service.AssetCategoryLocalService;
import com.liferay.asset.kernel.service.AssetVocabularyLocalService;
import com.liferay.exportimport.kernel.lar.ExportImportThreadLocal;
import com.liferay.fragment.contributor.FragmentCollectionContributorTracker;
import com.liferay.fragment.model.FragmentEntry;
import com.liferay.fragment.model.FragmentEntryLink;
import com.liferay.fragment.processor.FragmentEntryProcessorRegistry;
import com.liferay.fragment.service.FragmentEntryLinkLocalService;
import com.liferay.fragment.service.FragmentEntryLocalService;
import com.liferay.layout.page.template.model.LayoutPageTemplateStructure;
import com.liferay.layout.page.template.service.LayoutPageTemplateStructureLocalService;
import com.liferay.layout.util.LayoutCopyHelper;
import com.liferay.layout.util.structure.LayoutStructure;
import com.liferay.layout.util.structure.LayoutStructureItem;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.model.LayoutConstants;
import com.liferay.portal.kernel.model.LayoutTypePortlet;
import com.liferay.portal.kernel.model.LayoutTypePortletConstants;
import com.liferay.portal.kernel.portlet.PortletIdCodec;
import com.liferay.portal.kernel.portlet.PortletPreferencesFactory;
import com.liferay.portal.kernel.security.auth.PrincipalThreadLocal;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.LayoutLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.PortletKeys;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.UnicodeProperties;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.registry.collections.ServiceTrackerCollections;
import com.liferay.registry.collections.ServiceTrackerMap;
import de.dm.toolbox.liferay.resources.importer.BaseImporter;
import de.dm.toolbox.liferay.resources.importer.Importer;
import de.dm.toolbox.liferay.resources.importer.PortletPreferencesTranslator;
import de.dm.toolbox.liferay.resources.importer.util.ImporterUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import javax.portlet.PortletException;
import javax.portlet.PortletPreferences;
import javax.servlet.ServletContext;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

@Component(
	immediate = true, property = {"importer.order=90"}, service = Importer.class
)
public class FragmentsImporter extends BaseImporter {

	@Override
	protected void doRunImport(ServletContext servletContext, long companyId)
		throws Exception {

		setUpFragments(servletContext, companyId);
	}

	protected void resetFragments(Layout layout) throws PortalException {
		Layout draftLayout = layout;

		if (layout.fetchDraftLayout() != null) {
			draftLayout = layout.fetchDraftLayout();
		}

		LayoutPageTemplateStructure layoutPageTemplateStructure = layoutPageTemplateStructureLocalService.fetchLayoutPageTemplateStructure(draftLayout.getGroupId(), draftLayout.getPlid());

		if (layoutPageTemplateStructure != null) {
			layoutPageTemplateStructureLocalService.deleteLayoutPageTemplateStructure(draftLayout.getGroupId(), draftLayout.getPlid());
		}

		fragmentEntryLinkLocalService.deleteLayoutPageTemplateEntryFragmentEntryLinks(draftLayout.getGroupId(), draftLayout.getPlid());
	}

	@Override
	@Reference
	protected void setAssetCategoryLocalService(
		AssetCategoryLocalService assetCategoryLocalService) {

		super.setAssetCategoryLocalService(assetCategoryLocalService);
	}

	@Override
	@Reference
	protected void setAssetVocabularyLocalService(
		AssetVocabularyLocalService assetVocabularyLocalService) {

		super.setAssetVocabularyLocalService(assetVocabularyLocalService);
	}

	@Override
	@Reference
	protected void setGroupLocalService(GroupLocalService groupLocalService) {
		super.setGroupLocalService(groupLocalService);
	}

	@Override
	@Reference
	protected void setPortal(Portal portal) {
		super.setPortal(portal);
	}

	@Override
	@Reference
	protected void setUserLocalService(UserLocalService userLocalService) {
		super.setUserLocalService(userLocalService);
	}

	private void addFragmentContent(
			Layout layout, int position, JSONObject contentJSONObject)
			throws PortalException, PortletException, IOException {

		Layout draftLayout = layout;

		if (layout.fetchDraftLayout() != null) {
			draftLayout = layout.fetchDraftLayout();
		}

		String fragmentKey = contentJSONObject.getString("fragmentKey");
		String portletId = contentJSONObject.getString("portletId");
		String instanceId = contentJSONObject.getString("instanceId");
		String configuration = contentJSONObject.getString("configuration");
		String css = contentJSONObject.getString("css");
		String html = contentJSONObject.getString("html");
		String js = contentJSONObject.getString("js");

		LayoutPageTemplateStructure layoutPageTemplateStructure = layoutPageTemplateStructureLocalService.fetchLayoutPageTemplateStructure(groupId, draftLayout.getPlid(), true);

		long fragmentEntryId = 0L;

		String editableValues = null;

		if (Validator.isNotNull(fragmentKey)) {
			FragmentEntry fragmentEntry = getFragmentEntry(groupId, fragmentKey, PortalUtil.getSiteDefaultLocale(groupId));

			if (Validator.isNull(fragmentEntry)) {
				log.warn("Fragment " + fragmentKey + " not found!");

				return;
			}

			if (Validator.isNull(css)) {
				css = fragmentEntry.getCss();
			}
			if (Validator.isNull(html)) {
				html = fragmentEntry.getHtml();
			}
			if (Validator.isNull(js)) {
				js = fragmentEntry.getJs();
			}
			if (Validator.isNull(configuration)) {
				configuration = fragmentEntry.getConfiguration();
			}
		} else if (Validator.isNotNull(portletId)) {
			JSONObject editableValueJSONObject = fragmentEntryProcessorRegistry.getDefaultEditableValuesJSONObject(StringPool.BLANK, StringPool.BLANK);

			editableValueJSONObject.put(
					"instanceId", instanceId
			).put(
					"portletId", portletId
			);

			css = StringPool.BLANK;
			html = StringPool.BLANK;
			js = StringPool.BLANK;
			configuration = StringPool.BLANK;
			editableValues = editableValueJSONObject.toString();
		} else {
			log.warn("No Fragment or Portlet ID given. Skipping entry...");

			return;
		}

		FragmentEntryLink fragmentEntryLink = fragmentEntryLinkLocalService.addFragmentEntryLink(
				userId,
				groupId,
				0L,
				fragmentEntryId,
				0L,
				draftLayout.getPlid(),
				css,
				html,
				js,
				configuration,
				editableValues,
				StringPool.BLANK,
				position,
				fragmentKey,
				serviceContext
		);

		String templateStructureData = layoutPageTemplateStructure.getData(0L);

		LayoutStructure layoutStructure = LayoutStructure.of(templateStructureData);

		LayoutStructureItem layoutStructureItem = layoutStructure.addFragmentStyledLayoutStructureItem(
				fragmentEntryLink.getFragmentEntryLinkId(), layoutStructure.getMainItemId(), position
		);

		layoutPageTemplateStructureLocalService.updateLayoutPageTemplateStructureData(
				groupId,
				draftLayout.getPlid(),
				0L,
				layoutStructure.toString()
		);

		if (Validator.isNotNull(portletId)) {
			configurePortlet(draftLayout, contentJSONObject);
		}
	}

	private void configurePortlet(Layout layout, JSONObject contentJSONObject) throws PortletException, IOException {
		String rootPortletId = contentJSONObject.getString("portletId");
		String instanceId = contentJSONObject.getString("instanceId");

		PortletPreferencesTranslator portletPreferencesTranslator = portletPreferencesTranslators.getService(rootPortletId);

		if (Validator.isNull(portletPreferencesTranslator)) {
			portletPreferencesTranslator = defaultPortletPreferencesTranslator;
		}

		JSONObject portletPreferencesJSONObject = contentJSONObject.getJSONObject("portletPreferences");

		if (
				(portletPreferencesJSONObject == null) ||
				(portletPreferencesJSONObject.length() == 0)
		) {
			return;
		}

		String portletInstanceId = PortletIdCodec.encode(rootPortletId, instanceId);

		if (portletPreferencesTranslator != null) {
			PortletPreferences portletSetup = portletPreferencesFactory.getLayoutPortletSetup(layout, portletInstanceId);

			Iterator<String> iterator = portletPreferencesJSONObject.keys();

			while (iterator.hasNext()) {
				String key = iterator.next();

				portletPreferencesTranslator.translate(portletPreferencesJSONObject, key, portletSetup, groupId);
			}

			portletSetup.store();
		}

		//TODO Nested Portlet?

	}

	private void addFragmentContents(Layout layout, JSONArray contentsJSONArray)
			throws PortalException, PortletException, IOException {

		if (contentsJSONArray == null) {
			return;
		}

		for (int i = 0; i < contentsJSONArray.length(); i++) {
			JSONObject contentJSONObject = contentsJSONArray.getJSONObject(i);

			addFragmentContent(layout, i, contentJSONObject);
		}
	}

	private void setUpFragments(ServletContext servletContext, long companyId)
		throws Exception {

		long oldUserId = PrincipalThreadLocal.getUserId();

		PrincipalThreadLocal.setName(user.getUserId());

		ServiceContext serviceContext = new ServiceContext();

		ServiceContextThreadLocal.pushServiceContext(serviceContext);

		boolean layoutImportInProcess = ExportImportThreadLocal.isLayoutImportInProcess();
		boolean portletImportInProcess = ExportImportThreadLocal.isPortletImportInProcess();

		ExportImportThreadLocal.setLayoutImportInProcess(false);
		ExportImportThreadLocal.setPortletImportInProcess(false);

		JSONObject jsonObject = ImporterUtil.getJSONObject(
			servletContext, resourcesDir, SITEMAP_JSON, companyId,
			group.getGroupId(), userId);

		if (jsonObject == null) {
			if (log.isDebugEnabled()) {
				log.debug("No " + SITEMAP_JSON + " present. Skipping...");
			}

			return;
		}

		JSONArray publicPagesJSONArray = jsonObject.getJSONArray("publicPages");

		if (publicPagesJSONArray != null) {
			updateFragments(
				false, LayoutConstants.DEFAULT_PARENT_LAYOUT_ID,
				publicPagesJSONArray, companyId);
		}

		PrincipalThreadLocal.setName(oldUserId);

		ServiceContextThreadLocal.popServiceContext();

		ExportImportThreadLocal.setLayoutImportInProcess(layoutImportInProcess);
		ExportImportThreadLocal.setPortletImportInProcess(portletImportInProcess);
	}

	private void updateFragment(
			boolean privateLayout, long parentLayoutId,
			JSONObject layoutJSONObject, long companyId)
			throws Exception {

		String type = layoutJSONObject.getString("type");

		if (!"content".equals(type)) {
			if (log.isDebugEnabled()) {
				log.debug("Layout is not of type \"content\". Skipping...");
			}

			return;
		}

		String friendlyURL = layoutJSONObject.getString("friendlyURL");

		Layout layout = layoutLocalService.fetchLayoutByFriendlyURL(
			groupId, privateLayout, friendlyURL);

		if (Validator.isNull(layout)) {
			log.warn("Layout " + friendlyURL + " not found!");

			return;
		}

		resetFragments(layout);

		JSONArray contentsJSONArray = layoutJSONObject.getJSONArray("content");

		addFragmentContents(layout, contentsJSONArray);

		releaseLayout(layout);

		JSONArray layoutsJSONArray = layoutJSONObject.getJSONArray("layouts");

		updateFragments(
			privateLayout, layout.getLayoutId(), layoutsJSONArray, companyId);

	}

	private void releaseLayout(Layout layout) throws Exception {
		Layout draftLayout = layout.fetchDraftLayout();

		layout = layoutCopyHelper.copyLayout(draftLayout, layout);

		layout.setType(draftLayout.getType());
		layout.setStatus(WorkflowConstants.STATUS_APPROVED);

		String layoutPrototypeUuid = layout.getLayoutPrototypeUuid();

		layout.setLayoutPrototypeUuid(null);

		layoutLocalService.updateLayout(layout);

		draftLayout = layoutLocalService.getLayout(draftLayout.getPlid());

		UnicodeProperties typeSettingsUnicodeProperties =
				draftLayout.getTypeSettingsProperties();

		if (Validator.isNotNull(layoutPrototypeUuid)) {
			typeSettingsUnicodeProperties.setProperty(
					"layoutPrototypeUuid", layoutPrototypeUuid);
		}

		draftLayout.setStatus(WorkflowConstants.STATUS_APPROVED);

		layoutLocalService.updateLayout(draftLayout);
	}

	private void updateFragments(
			boolean privateLayout, long parentLayoutId,
			JSONArray layoutsJSONArray, long companyId)
			throws Exception {

		if (layoutsJSONArray == null) {
			return;
		}

		for (int i = 0; i < layoutsJSONArray.length(); i++) {
			JSONObject layoutJSONObject = layoutsJSONArray.getJSONObject(i);

			updateFragment(
				privateLayout, parentLayoutId, layoutJSONObject, companyId);
		}
	}

	private FragmentEntry getFragmentEntry(long groupId, String fragmentEntryKey, Locale locale) {
		FragmentEntry fragmentEntry = fragmentEntryLocalService.fetchFragmentEntry(groupId, fragmentEntryKey);

		if (fragmentEntry != null) {
			return fragmentEntry;
		}

		Map<String, FragmentEntry> fragmentEntries = fragmentCollectionContributorTracker.getFragmentEntries(locale);

		return fragmentEntries.get(fragmentEntryKey);
	}

	private static final String SITEMAP_JSON = "sitemap.json";

	private static final Log log = LogFactoryUtil.getLog(
		FragmentsImporter.class);

	@Reference
	private FragmentEntryLinkLocalService fragmentEntryLinkLocalService;

	@Reference
	private LayoutLocalService layoutLocalService;

	@Reference
	private LayoutPageTemplateStructureLocalService layoutPageTemplateStructureLocalService;

	@Reference
	private FragmentCollectionContributorTracker fragmentCollectionContributorTracker;

	@Reference
	private FragmentEntryLocalService fragmentEntryLocalService;

	@Reference
	private LayoutCopyHelper layoutCopyHelper;

	@Reference
	private FragmentEntryProcessorRegistry fragmentEntryProcessorRegistry;

	@Reference
	private PortletPreferencesFactory portletPreferencesFactory;

	@Reference(
			target = "(portlet.preferences.translator.portlet.id=default)"
	)
	private PortletPreferencesTranslator defaultPortletPreferencesTranslator;

	private ServiceTrackerMap<String, PortletPreferencesTranslator> portletPreferencesTranslators;

	@Activate
	public void activate() {
		portletPreferencesTranslators = ServiceTrackerCollections.openSingleValueMap(
				PortletPreferencesTranslator.class,
				"portlet.preferences.translator.portlet.id"
		);
	}

	@Deactivate
	public void deactivate() {
		portletPreferencesTranslators.close();
	}


}