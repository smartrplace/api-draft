package org.smartrplace.smarteff.util;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.model.prototypes.Data;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.ExtensionCapabilityPublicData.EntryType;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.button.TemplateInitSingleEmpty;

public abstract class EditPageBase<T extends Resource> extends NaviPageBase<T> {
	protected abstract void getEditTableLines(EditTableBuilder etb);
	public abstract boolean checkResource(T data);
	
	@Override
	protected List<EntryType> getEntryTypes() {
		return CapabilityHelper.getStandardEntryTypeList(typeClass());
	}

	protected ObjectResourceGUIHelper<T, T> mh;
	protected Alert alert;
	
	public EditPageBase() {
		super();
	}
	
	private class EditElement {
		//Exactly one of the following should be non-null
		String title;
		OgemaWidget widgetForTitle;
		
		//Exactly one of the following should be non-null
		OgemaWidget widget;
		String stringForWidget;
		RedirectButton decriptionLink = null;
		
		public EditElement(String title, OgemaWidget widget) {
			this.title = title;
			this.widget = widget;
		}
		public EditElement(String title, String stringForWidget) {
			this.title = title;
			this.stringForWidget = stringForWidget;
		}
		public EditElement(OgemaWidget ogemaWidgetForTitle, OgemaWidget widget) {
			this.widgetForTitle = ogemaWidgetForTitle;
			this.widget = widget;
		}
		public void setDescriptionUrl(RedirectButton button) {
			decriptionLink = button;
		}
	}
	public class EditTableBuilder {
		List<EditElement> editElements = new ArrayList<>();
		public void addEditLine(String title, OgemaWidget widget) {
			editElements.add(new EditElement(title, widget));
		}
		public void addEditLine(OgemaWidget widgetForTitle, OgemaWidget widget) {
			editElements.add(new EditElement(widgetForTitle, widget));
		}
		public void addEditLine(String title, String stringForWidget) {
			editElements.add(new EditElement(title, stringForWidget));
		}
		public void addEditLine(OgemaWidget widgetForTitle, OgemaWidget widget, RedirectButton descriptionLink) {
			EditPageBase<T>.EditElement el = new EditElement(widgetForTitle, widget);
			el.setDescriptionUrl(descriptionLink);
			editElements.add(el);
		}
	}
	
	@Override
	protected void addWidgets() {
		mh = new ObjectResourceGUIHelper<T, T>(page, (TemplateInitSingleEmpty<T>)null , null, false) {

			@Override
			protected T getResource(T object, OgemaHttpRequest req) {
				return object;
			}
			@Override
			protected T getGatewayInfo(OgemaHttpRequest req) {
				return getReqData(req);
			}
			
		};
		alert = new Alert(page, "alert"+pid(), "");
		page.append(alert);
		
		EditPageBase<T>.EditTableBuilder etb = new EditTableBuilder();
		getEditTableLines(etb);

		StaticTable table = new StaticTable(etb.editElements.size()+1, 4, new int[]{1,5,5,1});
		int c = 0;
		for(EditPageBase<T>.EditElement etl: etb.editElements) {
			if((etl.title != null)&&(etl.widget != null))
				table.setContent(c, 1, etl.title).setContent(c,2, etl.widget);
			else if((etl.title != null)&&(etl.stringForWidget != null))
				table.setContent(c, 1, etl.title).setContent(c,2, etl.stringForWidget);
			else if((etl.widgetForTitle != null)&&(etl.widget != null)) {
				table.setContent(c, 1, etl.widgetForTitle).setContent(c,2, etl.widget);
				if(etl.decriptionLink != null) table.setContent(c, 3, etl.decriptionLink);
			}
			else throw new IllegalStateException("Something went wrong with building the edit line "+c+" Obj:"+etl);
			c++;
		}
		Button activateButton = new Button(page, "activateButton", "activate") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				T res = getReqData(req);
				if(res.isActive()) {
					setWidgetVisibility(false, req);
				} else {
					setWidgetVisibility(true, req);
					if(checkResource(res)) enable(req);
					else disable(req);
				}
			}
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
				T res = getReqData(req);
				appData.systemAccess().activateResource(res);
			}
		};
		TableOpenButton tableButton = new TableOpenButton(page, "addEntry", pid(), "Data Explorer", typeClass(), exPage);
		table.setContent(c, 0, activateButton).setContent(c, 1, tableButton);

		page.append(table);
		exPage.registerAppTableWidgetsDependentOnInit(table);
	}
	
	@Override
	protected PageType getPageType() {
		return PageType.EDIT_PAGE;
	}
	
	protected <R extends Resource> boolean checkResourceBase(R resource, boolean nameRelevant) {
		Data data;
		String name = null;
		if(nameRelevant) {
			data = (Data)resource;
			name = data.name().getValue();
			if(name.isEmpty()) return false;
		}
		@SuppressWarnings("unchecked")
		Class<R> type = (Class<R>) resource.getResourceType();
		List<R> otherOfType = resource.getParent().getSubResources(type, false);
		for(R ot: otherOfType) {
			if(ot.equalsLocation(resource)) continue;
			if(nameRelevant &&(((Data)ot).name().getValue().equals(name))) return false;
		}
		return true;
	}

}
