package org.smartrplace.smarteff.util;

import java.util.Arrays;
import java.util.List;

import org.smartrplace.extenservice.resourcecreate.ExtensionPageSystemAccessForCreate;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.ExtensionCapabilityPublicData.EntryType;
import org.smartrplace.extensionservice.ExtensionResourceType;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.directresourcegui.ResourceGUIHelper;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.html.complextable.RowTemplate.Row;

public class SPPageUtil {
	public static OgemaWidget addOpenButtonStartPage(String columnName,
			Class<? extends ExtensionResourceType> type, ObjectResourceGUIHelper<?, ?> vh, String id, Row row,
			NavigationPublicPageData pageData,
			ExtensionPageSystemAccessForCreate systemAccess,
			String text, String alternativeText) {
		if(pageData != null) {
			String configId = systemAccess.accessPage(pageData, getEntryIdx(pageData, type),
					null);
			return vh.linkingButton(columnName, id, null, row, "Edit", pageData.getUrl()+"?configId="+configId);
		} else {
			return vh.stringLabel(columnName, id, alternativeText, row);
		}
	}
	public static OgemaWidget addOpenButton(String columnName, ExtensionResourceType object,
			Class<? extends ExtensionResourceType> type, ResourceGUIHelper<?> vh, String id, Row row,
			NavigationPublicPageData pageData,
			ExtensionPageSystemAccessForCreate systemAccess,
			String text, String alternativeText) {
		if(pageData != null) {
			if(!systemAccess.isLocked(object)) {
				String configId = systemAccess.accessPage(pageData, getEntryIdx(pageData, type),
						Arrays.asList(new ExtensionResourceType[]{object}));
				return vh.linkingButton(columnName, id, null, row, "Edit", pageData.getUrl()+"?configId="+configId);
			} else {
				return vh.stringLabel(columnName, id, text, row);						
			}
		} else {
			return vh.stringLabel(columnName, id, alternativeText, row);
		}
	}
	public static OgemaWidget addResOpenButton(String columnName, ExtensionResourceType object,
			Class<? extends ExtensionResourceType> type,
			ResourceGUIHelper<?> vh, String id, Row row,
			ExtensionResourceAccessInitData appData) {
		if(appData != null) {
			NavigationPublicPageData pageData = getPageData(appData, type);
			return addOpenButton(columnName, object, type, vh, id, row, pageData, appData.systemAccess(), "Edit", "Locked");
			/*if(pageData != null) {
				if(!appData.systemAccess().isLocked(object)) {
					String configId = appData.systemAccess().accessPage(pageData, getEntryIdx(pageData, type),
							Arrays.asList(new ExtensionResourceType[]{object}));
					return vh.linkingButton(name, id, null, row, "Edit", pageData.getUrl()+"?configId="+configId);
				} else {
					return vh.stringLabel(name, id, "Locked", row);						
				}
			} else {
				return vh.stringLabel(name, id, "No Editor", row);
			}*/
			
		} else {
			vh.registerHeaderEntry(columnName);
			return null;
		}
	}
	
	static NavigationPublicPageData getPageData(ExtensionResourceAccessInitData appData,
			Class<? extends ExtensionResourceType> type) {
		List<NavigationPublicPageData> pages = appData.systemAccessForPageOpening().getPages(type);
		if(pages.isEmpty()) return null;
		else return pages.get(0);
	}
	static int getEntryIdx(NavigationPublicPageData navi, Class<? extends ExtensionResourceType> type) {
		int idx = 0;
		for(EntryType et: navi.getEntryTypes()) {
			if(type.isAssignableFrom(et.getType())) {
				return idx;
			}
			idx++;
		}
		throw new IllegalStateException("BuildinData entry type not found in Building Edit Page!");
	}

}
