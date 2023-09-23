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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.widgets.api.extended.html.bricks.PageSnippet;
import de.iwes.widgets.api.extended.resource.DefaultResourceTemplate;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.TemplateRedirectButton;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.textfield.TextField;
import de.iwes.widgets.object.widget.popup.WidgetEntryData;
import de.iwes.widgets.resource.widget.calendar.DatepickerTimeResource;
import de.iwes.widgets.resource.widget.dropdown.ResourceDropdown;
import de.iwes.widgets.resource.widget.dropdown.ValueResourceDropdown;
import de.iwes.widgets.resource.widget.init.ResourceRedirectButton;
import de.iwes.widgets.resource.widget.textfield.BooleanResourceCheckbox;

/** Base class for automated widget generation. This class contains Flex-widgets that can be flex-instantiated
 * linked to page or to request. This class should not be instantiated by applications directly.
*/
public class ObjectGUIHelperBase<T> {
	protected final WidgetPage<?> page;
	protected final OgemaWidget parent;
	protected final OgemaHttpRequest req;
	public PageSnippet pageSnippet = null;
	public List<WidgetEntryData> popTableData = null;
	
	protected static int counter = -1;
	protected final boolean acceptMissingResources;
	
	protected final Set<String> widgetsInOverview = new HashSet<>();
	protected boolean detailWidgetsChosenManually = false;
	protected boolean isInDetailWidgetsSection = false;
	/**If false we are evaluating for the overview table*/
	public enum WidgetsToAdd {
		OVERVIEW,
		DETAILS_ONLY,
		ALL
	}
	protected WidgetsToAdd evaluatingForDetails = WidgetsToAdd.OVERVIEW;
	
	//one of init (in inerherited class) or fixedGatewayInfo them must be null
	protected final T fixedGatewayInfo;
	
	//All header elements including detail elements that are not shown
	protected LinkedHashMap<String,Object> fullHeaderMap  = new LinkedHashMap<>();
	//header map contains only the headers shown in the linked table
	protected LinkedHashMap<String,Object> headerMap  = new LinkedHashMap<>();
	//protected String initialLineId = null;
	
	public ObjectGUIHelperBase(WidgetPage<?> page, boolean acceptMissingResources) {
		this.page = page;
		this.parent = null;
		this.req = null;
		this.fixedGatewayInfo = null;
		this.acceptMissingResources = acceptMissingResources;
	}
	public ObjectGUIHelperBase(WidgetPage<?> page, T fixedGatewayInfo, boolean acceptMissingResources) {
		this.page = page;
		this.parent = null;
		this.req = null;
		this.fixedGatewayInfo = fixedGatewayInfo;
		this.acceptMissingResources = acceptMissingResources;
	}
	public ObjectGUIHelperBase(OgemaWidget parent, OgemaHttpRequest req, T fixedGatewayInfo,
			boolean acceptMissingResources) {
		this.page = null;
		this.parent = parent;
		this.req = req;
		this.fixedGatewayInfo = fixedGatewayInfo;
		this.acceptMissingResources = acceptMissingResources;
	}

	protected void finishRowSnippet(Row row, String widgetId, OgemaWidget result) {
		finishRowSnippet(row, widgetId, result, 0);
	}
	protected void finishRowSnippet(Row row, String widgetId, OgemaWidget result, int columnSize) {
		if(row != null) if(columnSize < 1)
			row.addCell(widgetId, result);
		else
			row.addCell(widgetId, result, columnSize);
		else if(pageSnippet != null) pageSnippet.append(result, getReq());
		else popTableData.add(new WidgetEntryData(widgetId, result));
	}
	
	public static class LabelFlex {
		public Label myLabel;
		public void onGET(OgemaHttpRequest req) {}
		public LabelFlex(String widgetId, ObjectGUIHelperBase<?> vrh) {
			if(vrh.page != null) myLabel = new Label(vrh.page, widgetId) {
				private static final long serialVersionUID = 1L;

				@Override
				public void onGET(OgemaHttpRequest req) {
					LabelFlex.this.onGET(req);
				}
			};
			else myLabel = new Label(vrh.parent, widgetId, vrh.getReq()) {
				private static final long serialVersionUID = 1L;

				@Override
				public void onGET(OgemaHttpRequest req) {
					LabelFlex.this.onGET(req);
				}
			};
		}
	}
	
	public static class TextFieldFlex {
		public TextField myField;
		public void onGET(OgemaHttpRequest req) {}
		public void onPrePOST(String data, OgemaHttpRequest req) {}
		public void onPOSTComplete(String data, OgemaHttpRequest req) {}
		public TextFieldFlex(String widgetId, ObjectGUIHelperBase<?> vrh) {
			if(vrh.page != null) myField = new TextField(vrh.page, widgetId) {
				private static final long serialVersionUID = 1L;

				@Override
				public void onGET(OgemaHttpRequest req) {
					TextFieldFlex.this.onGET(req);
				}
				@Override
				public void onPrePOST(String data, OgemaHttpRequest req) {
					TextFieldFlex.this.onPrePOST(data, req);
				}
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					TextFieldFlex.this.onPOSTComplete(data, req);
				}
			};
			else myField = new TextField(vrh.parent, widgetId, vrh.getReq()) {
				private static final long serialVersionUID = 1L;

				@Override
				public void onGET(OgemaHttpRequest req) {
					TextFieldFlex.this.onGET(req);
				}
				@Override
				public void onPrePOST(String data, OgemaHttpRequest req) {
					TextFieldFlex.this.onPrePOST(data, req);
				}
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					TextFieldFlex.this.onPOSTComplete(data, req);
				}
			};
		}
	}
	
	public static class BooleanResourceCheckboxFlex {
		public BooleanResourceCheckbox myCheckbox;
		public void onGET(OgemaHttpRequest req) {}
		public void onPrePOST(String data, OgemaHttpRequest req) {}
		public void onPOSTComplete(String data, OgemaHttpRequest req) {}
		public BooleanResourceCheckboxFlex(String widgetId, ObjectGUIHelperBase<?> vrh) {
			if(vrh.page != null) myCheckbox = new BooleanResourceCheckbox(vrh.page, widgetId, "") {
				private static final long serialVersionUID = 1L;

				@Override
				public void onGET(OgemaHttpRequest req) {
					BooleanResourceCheckboxFlex.this.onGET(req);
				}
				@Override
				public void onPrePOST(String data, OgemaHttpRequest req) {
					BooleanResourceCheckboxFlex.this.onPrePOST(data, req);
				}
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					BooleanResourceCheckboxFlex.this.onPOSTComplete(data, req);
				}
			};
			else myCheckbox = new BooleanResourceCheckbox(vrh.parent, widgetId, "", vrh.getReq()) {
				private static final long serialVersionUID = 1L;

				@Override
				public void onGET(OgemaHttpRequest req) {
					BooleanResourceCheckboxFlex.this.onGET(req);
				}
				@Override
				public void onPrePOST(String data, OgemaHttpRequest req) {
					BooleanResourceCheckboxFlex.this.onPrePOST(data, req);
				}
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					BooleanResourceCheckboxFlex.this.onPOSTComplete(data, req);
				}
			};
		}
	}

	public static class ResourceRedirectButtonFlex<S extends Resource> {
		public ResourceRedirectButton<S> myButton;
		public void onPrePOST(String s, OgemaHttpRequest req) {}
		public ResourceRedirectButtonFlex(String widgetId, ObjectGUIHelperBase<?> vrh,
				String buttonText, String url) {
			if(vrh.page != null) myButton = new ResourceRedirectButton<S>(vrh.page, widgetId, buttonText, url) {
				private static final long serialVersionUID = 1L;

				@Override
				public void onPrePOST(String s, OgemaHttpRequest req) {
					ResourceRedirectButtonFlex.this.onPrePOST(s, req);
				}
			};
			else myButton = new ResourceRedirectButton<S>(vrh.parent, widgetId, buttonText, url, vrh.getReq()) {
				private static final long serialVersionUID = 1L;

				@Override
				public void onPrePOST(String s, OgemaHttpRequest req) {
					ResourceRedirectButtonFlex.this.onPrePOST(s, req);
				}
			};
		}
	}
	
	public static class TemplateRedirectButtonFlex<S> {
		TemplateRedirectButton<S> myButton;
		public void onPrePOST(String s, OgemaHttpRequest req) {}
		public TemplateRedirectButtonFlex(String widgetId, ObjectGUIHelperBase<?> vrh,
				String buttonText, String url) {
			if(vrh.page != null) myButton = new TemplateRedirectButton<S>(vrh.page, widgetId, buttonText, url) {
				private static final long serialVersionUID = 1L;

				@Override
				public void onPrePOST(String s, OgemaHttpRequest req) {
					TemplateRedirectButtonFlex.this.onPrePOST(s, req);
				}
			};
			else myButton = new TemplateRedirectButton<S>(vrh.parent, widgetId, buttonText, url, vrh.getReq()) {
				private static final long serialVersionUID = 1L;

				@Override
				public void onPrePOST(String s, OgemaHttpRequest req) {
					TemplateRedirectButtonFlex.this.onPrePOST(s, req);
				}
			};
		}
	}

	public static class ValueResourceDropdownFlex<S extends SingleValueResource> {
		public ValueResourceDropdown<S> myDrop;
		Map<String, String> valuesToSet;
		public void onGET(OgemaHttpRequest req) {}
		public void onPrePOST(String data, OgemaHttpRequest req) {}
		public void onPOSTComplete(String data, OgemaHttpRequest req) {}
		public ValueResourceDropdownFlex(String widgetId, ObjectGUIHelperBase<?> vrh,
				final Map<String, String> valuesToSet) {
			if(vrh.page != null) myDrop = new ValueResourceDropdown<S>(vrh.page, widgetId) {
				private static final long serialVersionUID = 1L;

				@Override
				public void onGET(OgemaHttpRequest req) {
					ValueResourceDropdownFlex.this.onGET(req);
				}
				@Override
				public void onPrePOST(String s, OgemaHttpRequest req) {
					ValueResourceDropdownFlex.this.onPrePOST(s, req);
				}
				@Override
				public void onPOSTComplete(String s, OgemaHttpRequest req) {
					ValueResourceDropdownFlex.this.onPOSTComplete(s, req);
				}
				@Override
				public String getSelection(S resource, Locale locale, List<String> displayedValues) {
					if(valuesToSet == null) return super.getSelection(resource, locale, displayedValues);
					String value;
					if(resource instanceof FloatResource)
						value = String.format("%.1f", ((FloatResource)resource).getValue());
					else
						value = ValueResourceUtils.getValue(resource);
					String display = valuesToSet.get(value);
					if(display == null) return displayedValues.get(0);
					return display;
				}
				@Override
				protected void setResourceValue(S resource, String value, List<String> displayedValues) {
					if(valuesToSet == null) super.setResourceValue(resource, value, displayedValues);
					for(Entry<String, String> e: valuesToSet.entrySet()) {
						if(e.getValue().equals(value)) {
							ValueResourceUtils.setValue(resource, e.getKey());
							return;
						}
					}
				}
			};
			else myDrop = new ValueResourceDropdown<S>(vrh.parent, widgetId, vrh.getReq()) {
				private static final long serialVersionUID = 1L;

				@Override
				public void onGET(OgemaHttpRequest req) {
					ValueResourceDropdownFlex.this.onGET(req);
				}
				@Override
				public void onPrePOST(String s, OgemaHttpRequest req) {
					ValueResourceDropdownFlex.this.onPrePOST(s, req);
				}
				@Override
				public void onPOSTComplete(String s, OgemaHttpRequest req) {
					ValueResourceDropdownFlex.this.onPOSTComplete(s, req);
				}
				@Override
				public String getSelection(S resource, Locale locale, List<String> displayedValues) {
					if(valuesToSet == null) return super.getSelection(resource, locale, displayedValues);
					String value = ValueResourceUtils.getValue(resource);
					String display = valuesToSet.get(value);
					if(display == null) return displayedValues.get(0);
					return display;
				}
				@Override
				protected void setResourceValue(S resource, String value, List<String> displayedValues) {
					if(valuesToSet == null) super.setResourceValue(resource, value, displayedValues);
					for(Entry<String, String> e: valuesToSet.entrySet()) {
						if(e.getValue().equals(value)) {
							ValueResourceUtils.setValue(resource, e.getKey());
							return;
						}
					}
				}
			};
			myDrop.setDefaultDisplayedValues(new ArrayList<>(valuesToSet.values()));
		}
	}

	public static class ResourceDropdownFlex<S extends Resource> {
		public ResourceDropdown<S> myDrop;
		public void onGET(OgemaHttpRequest req) {}
		public void onPrePOST(String data, OgemaHttpRequest req) {}
		public void onPOSTComplete(String data, OgemaHttpRequest req) {}
		public String getLabel(S object, OgemaLocale locale) {
			return ResourceUtils.getHumanReadableName(object);
		}
		public ResourceDropdownFlex(String widgetId, ObjectGUIHelperBase<?> vrh) {
			if(vrh.page != null) myDrop = new ResourceDropdown<S>(vrh.page, widgetId) {
				private static final long serialVersionUID = 1L;

				@Override
				public void onGET(OgemaHttpRequest req) {
					ResourceDropdownFlex.this.onGET(req);
				}
				@Override
				public void onPrePOST(String s, OgemaHttpRequest req) {
					ResourceDropdownFlex.this.onPrePOST(s, req);
				}
				@Override
				public void onPOSTComplete(String s, OgemaHttpRequest req) {
					ResourceDropdownFlex.this.onPOSTComplete(s, req);
				}
			};
			else myDrop = new ResourceDropdown<S>(vrh.parent, widgetId, vrh.getReq()) {
				private static final long serialVersionUID = 1L;

				@Override
				public void onGET(OgemaHttpRequest req) {
					ResourceDropdownFlex.this.onGET(req);
				}
				@Override
				public void onPrePOST(String s, OgemaHttpRequest req) {
					ResourceDropdownFlex.this.onPrePOST(s, req);
				}
				@Override
				public void onPOSTComplete(String s, OgemaHttpRequest req) {
					ResourceDropdownFlex.this.onPOSTComplete(s, req);
				}
			};
			DefaultResourceTemplate<S> displayTemplate = new DefaultResourceTemplate<S>() {
				@Override
				public String getLabel(S object, OgemaLocale locale) {
					return ResourceDropdownFlex.this.getLabel(object, locale);
				}
			};
			myDrop.setTemplate(displayTemplate);
		}
	}
	
	public static class DatepickerFlex {
		public DatepickerTimeResource myDrop;
		public void onGET(OgemaHttpRequest req) {}
		public void onPrePOST(String data, OgemaHttpRequest req) {}
		public void onPOSTComplete(String data, OgemaHttpRequest req) {}
		public DatepickerFlex(String widgetId, ObjectGUIHelperBase<?> vrh, String format, String defaultDate, String viewMode) {
			if(vrh.page != null) myDrop = new DatepickerTimeResource(vrh.page, widgetId, null, format, defaultDate, viewMode, null) {
				private static final long serialVersionUID = 1L;

				@Override
				public void onGET(OgemaHttpRequest req) {
					DatepickerFlex.this.onGET(req);
				}
				@Override
				public void onPrePOST(String s, OgemaHttpRequest req) {
					DatepickerFlex.this.onPrePOST(s, req);
				}
				@Override
				public void onPOSTComplete(String s, OgemaHttpRequest req) {
					DatepickerFlex.this.onPOSTComplete(s, req);
				}
			};
			else {
					myDrop = new DatepickerTimeResource(vrh.parent, widgetId, vrh.getReq()) {
					private static final long serialVersionUID = 1L;
	
					@Override
					public void onGET(OgemaHttpRequest req) {
						DatepickerFlex.this.onGET(req);
					}
					@Override
					public void onPrePOST(String s, OgemaHttpRequest req) {
						DatepickerFlex.this.onPrePOST(s, req);
					}
					@Override
					public void onPOSTComplete(String s, OgemaHttpRequest req) {
						DatepickerFlex.this.onPOSTComplete(s, req);
					}
				};
				if(format != null) myDrop.setFormat(format, vrh.getReq());
				if(defaultDate != null) myDrop.setDefaultDate(defaultDate);
				if(viewMode != null) myDrop.setViewMode(viewMode, vrh.getReq());
			}
		}
	}
	
	public boolean checkLineId(String widgetId) {
		String canonicalWidgetId = WidgetHelper.getValidWidgetId(widgetId);
		fullHeaderMap.put(canonicalWidgetId, widgetId);
		if(detailWidgetsChosenManually) {
			boolean val = widgetsInOverview.contains(canonicalWidgetId);
			switch(evaluatingForDetails) {
			case OVERVIEW:
				if(!val) return true;
				break;
			case DETAILS_ONLY:
				if(val) return true;
			default:
				break;
			}
		} else {
			if(isInDetailWidgetsSection) {
				widgetsInOverview.remove(canonicalWidgetId);
				if(evaluatingForDetails == WidgetsToAdd.OVERVIEW) return true;
			} else {
				widgetsInOverview.add(canonicalWidgetId);				
				if(evaluatingForDetails == WidgetsToAdd.DETAILS_ONLY) return true;
			}
		}
		headerMap.put(canonicalWidgetId, widgetId);
		if(fixedGatewayInfo == null) return true;
		return false;
	}
	
	public LinkedHashMap<String, Object>  getHeader() {
		return headerMap;
	}
	public LinkedHashMap<String, Object>  getFullHeaderList() {
		return fullHeaderMap;
	}

	public Set<String>  getOverviewColumns() {
		return widgetsInOverview;
	}
	
	public void registerHeaderEntry(String widgetId) {
		checkLineId(widgetId);
		//headerMap.put(ResourceUtils.getValidResourceName(widgetId), widgetId);
	}
	
	public void inDetailSection(boolean detailStatus) {
		isInDetailWidgetsSection = detailStatus;
	}
	/**If false we are evaluating for the overview table*/
	public void evaluteForDetailsPopup(WidgetsToAdd status) {
		evaluatingForDetails = status;
	}
	/**If true the widgets to be shown in overview table are chosen via a MultiSelect or
	 * similar
	 */
	public void widgetsChosenManually(boolean status) {
		detailWidgetsChosenManually = status;
	}
	public OgemaHttpRequest getReq() {
		return req;
	}
	public OgemaWidget getParent() {
		return parent;
	}
}
