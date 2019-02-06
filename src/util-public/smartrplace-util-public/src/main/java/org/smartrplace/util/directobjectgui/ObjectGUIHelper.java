/**
 * ﻿Copyright 2018 Smartrplace UG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.smartrplace.util.directobjectgui;

import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.tools.resource.util.ResourceUtils;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.TemplateInitSingleEmpty;
import de.iwes.widgets.html.form.button.TemplateRedirectButton;
import de.iwes.widgets.html.form.dropdown.DropdownData;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.resource.widget.dropdown.ResourceDropdown;

/** Variant of {@link ObjectResourceGUIHelper} that contains also options for all kinds of labels and 
 * links that do not require to involve a resource. If you do not use the resource-based widgets you 
 * do not have to override {@link #getResource(OgemaHttpRequest)} here, but if you use them you
 * have to override the method or an exception will be thrown.
 * @deprecated This is not used by {@link ObjectGUITablePage} and {@link ObjectGUIEditPage}, so the
 * helper will most likely be used only very rarely. For this reason it will not be really tested and
 * maintained
 */
@Deprecated
public class ObjectGUIHelper<T, R extends Resource> extends ObjectResourceGUIHelper<T, R> {
	protected final TemplateInitSingleEmpty<T> init;
	
	/** Override this method if you want to use widgets that require resource access 
	 */
	@Override
	protected R getResource(T object, OgemaHttpRequest req) {
		throw new IllegalStateException("getResource has not been overriden and widget with resource access is used");
	}

	
	public ObjectGUIHelper(WidgetPage<?> page, TemplateInitSingleEmpty<T> init,
			ApplicationManager appMan, boolean acceptMissingResources) {
		super(page, init, appMan, acceptMissingResources);
		this.init = init;
	}
	public ObjectGUIHelper(WidgetPage<?> page, T fixedGatewayInfo,
			ApplicationManager appMan, boolean acceptMissingResources) {
		super(page, fixedGatewayInfo, appMan, acceptMissingResources);
		this.init = null;
	}
	public ObjectGUIHelper(OgemaWidget parent, OgemaHttpRequest req, T fixedGatewayInfo,
			ApplicationManager appMan, boolean acceptMissingResources) {
		super(parent, req, fixedGatewayInfo, appMan, acceptMissingResources);
		this.init = null;
	}

/*************************************
 * TODO: Unclear whether the widget options below really make sense compared to the inherited variants.
 * We leave them in for now for testing	
 *************************************/
	
	public Label resourceLabelObject(String widgetId, String lineId, final Resource source, Row row, int mode) {
		if(checkLineId(widgetId)) return null;
		widgetId = ResourceUtils.getValidResourceName(widgetId);
		Label result = resourceLabelObject(widgetId + lineId, source, mode);
		finishRowSnippet(row, widgetId, result);	
		return result;
	}
	public Label resourceLabelObject(final Resource source, int mode) {
		counter++;
		return resourceLabelObject("resourceLabel"+counter, source, mode);
	}

	private Label resourceLabelObject(String widgetId, final Resource optSource, final int mode) {
		LabelFlex result = new LabelFlex(widgetId, this) {
			public void onGET(OgemaHttpRequest req) {
				if ((optSource == null)||(!optSource.isActive())) {
					myLabel.setText("n.a.", req);
					return;
				}
				switch(mode) {
				case 1:
					myLabel.setText(ResourceUtils.getHumanReadableShortName(optSource), req);
					break;
				case 2:
					myLabel.setText(optSource.getLocation(), req);
					break;
				case 3:
					myLabel.setText(optSource.getPath(), req);
					break;
				case 4:
					myLabel.setText(optSource.getName(), req);
					break;
				case 10:
					myLabel.setText(optSource.getResourceType().getName(), req);
					break;					
				case 11:
					myLabel.setText(optSource.getResourceType().getSimpleName(), req);
					break;					
				default:
					myLabel.setText(ResourceUtils.getHumanReadableName(optSource), req);
				}
			};
		};
		return result.myLabel;
	}

	public TemplateRedirectButton<T> linkingButtonObject(String widgetId, String lineId, final T source, Row row,
			String buttonText, String url) {
		if(checkLineId(widgetId)) return null;
		widgetId = ResourceUtils.getValidResourceName(widgetId);
		TemplateRedirectButton<T> result = linkingButtonObject(widgetId + lineId, source, 
				buttonText, url);
		finishRowSnippet(row, widgetId, result);	
		return result;
	}
	public TemplateRedirectButton<T> linkingButtonObject(final T source, String buttonText, String url) {
		counter++;
		return linkingButtonObject("linkingButton"+counter, source, buttonText, url);
	}
	private TemplateRedirectButton<T> linkingButtonObject(String widgetId, final T optSource,
			String buttonText, String url) {
			TemplateRedirectButtonFlex<T> button = new TemplateRedirectButtonFlex<T>(widgetId, this,
				buttonText, url) {
			public void onPrePOST(String s, OgemaHttpRequest req) {
				myButton.selectItem(optSource, req);
			}
		};
		return button.myButton;
	}

	public <S extends Resource> ResourceDropdown<S> referenceDropdownFixedChoiceObject(String widgetId, String lineId, final S source, Row row,
			Map<S, String> valuesToSet) {
		if(checkLineId(widgetId)) return null;
		widgetId = ResourceUtils.getValidResourceName(widgetId);
		ResourceDropdown<S> result = referenceDropdownFixedChoiceObject(widgetId + lineId, source, valuesToSet, null);
		finishRowSnippet(row, widgetId, result);	
		return result;
	}
	public <S extends Resource> ResourceDropdown<S> referenceDropdownFixedChoiceObject(final S source,
			Map<S, String> valuesToSet) {
		counter++;
		return referenceDropdownFixedChoiceObject("dropdown"+counter, source, valuesToSet, null);
	}
	/** Dropdown to set resource referenced on a certain path. The choices are fixed here, so this is usually only
	 * suitable for dynamic widgets inside a table.
	 * TODO: Provide also more flexible versions that provide all elements from a ResourceList or all resources
	 * of a certain type on the system as options
	 * 
	 * @param widgetId
	 * @param optSource
	 * @param altId
	 * @param valuesToSet map containing resources offered and the labels to be displayed
	 * @param resourceType
	 * @return
	 */
	private <S extends Resource> ResourceDropdown<S> referenceDropdownFixedChoiceObject(String widgetId, final S optSource,
			final Map<S, String> valuesToSet, final Class<S> resourceType) {
		ResourceDropdownFlex<S> widget = new ResourceDropdownFlex<S>(widgetId, this) {
			@Override
			public String getLabel(S object, OgemaLocale locale) {
				String result = valuesToSet.get(object);
				if(result != null) return result;
				return super.getLabel(object, locale);
			}
			
			@SuppressWarnings("unchecked")
			public void onGET(OgemaHttpRequest req) {
				if((optSource != null) && optSource.exists())
					myDrop.selectItem((S) optSource.getLocationResource(), req);
				else
					myDrop.selectSingleOption(DropdownData.EMPTY_OPT_ID, req);
			}
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				if(!optSource.exists()) {
					optSource.create();
					optSource.activate(true);
				}
				S selection = myDrop.getSelectedItem(req);
				if(selection == null) optSource.delete();
				else optSource.setAsReference(selection);
			}
		};
		widget.myDrop.setDefaultItems(valuesToSet.keySet());
		widget.myDrop.setDefaultAddEmptyOption(true, "(not set)");
		return widget.myDrop;
	}
}
