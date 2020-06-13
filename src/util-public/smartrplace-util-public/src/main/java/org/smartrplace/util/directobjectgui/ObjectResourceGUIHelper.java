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

import java.awt.IllegalComponentStateException;
import java.math.BigInteger;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.array.ArrayResource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.tools.resource.util.TimeUtils;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.smartrplace.tissue.util.format.StringFormatHelperSP;
import org.smartrplace.tissue.util.resource.ValueResourceHelperSP;
import org.smartrplace.util.directresourcegui.LabelLongValue;
import org.smartrplace.util.directresourcegui.SingleValueResourceAccess;
import org.smartrplace.util.file.ApacheFileAdditions;
import org.smartrplace.util.format.ValueConverter;
import org.smartrplace.util.format.ValueFormat;

import de.iwes.util.format.StringFormatHelper;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.TemplateInitSingleEmpty;
import de.iwes.widgets.html.form.button.TemplateRedirectButton;
import de.iwes.widgets.html.form.dropdown.DropdownData;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.textfield.TextField;
import de.iwes.widgets.resource.widget.calendar.DatepickerTimeResource;
import de.iwes.widgets.resource.widget.dropdown.ResourceDropdown;
import de.iwes.widgets.resource.widget.dropdown.ValueResourceDropdown;
import de.iwes.widgets.resource.widget.init.ResourceRedirectButton;
import de.iwes.widgets.resource.widget.textfield.BooleanResourceCheckbox;

/** Provides support for efficient generation of widgets to display, edit and further use resources in tables and
 * item edit/display pages. In this generic version the template item type T may be arbitrary, but a resource
 * type has to be specified that can be derived from the template type via the method {@link #getResource(OgemaHttpRequest)}.
 */
public abstract class ObjectResourceGUIHelper<T, R extends Resource> extends ObjectGUIHelperBase<T> {
	protected abstract R getResource(T object, OgemaHttpRequest req);
	protected boolean doRegisterDependentWidgets = false;
	
	//one of init or fixedGatewayInfo them must be null
	protected final TemplateInitSingleEmpty<T> initObject;

	protected final ApplicationManager appMan;

	/** Constructor used by detail pages showing content of a single resource selected by InitWidget*/
	public ObjectResourceGUIHelper(WidgetPage<?> page, TemplateInitSingleEmpty<T> init,
			ApplicationManager appMan, boolean acceptMissingResources) {
		super(page, acceptMissingResources);
		this.initObject = init;
		this.appMan = appMan;
	}
	/** Used for initialization by ObjectGUITablePage */
	public ObjectResourceGUIHelper(WidgetPage<?> page, T fixedGatewayInfo,
			ApplicationManager appMan, boolean acceptMissingResources) {
		super(page, fixedGatewayInfo, acceptMissingResources);
		this.initObject = null;
		this.appMan = appMan;
	}
	/** Used to initialize rows, DetailPopupButton*/
	public ObjectResourceGUIHelper(OgemaWidget parent, OgemaHttpRequest req, T fixedGatewayInfo,
			ApplicationManager appMan, boolean acceptMissingResources) {
		super(parent, req, fixedGatewayInfo, acceptMissingResources);
		this.initObject = null;
		this.appMan = appMan;
	}

	public OgemaWidget valueEdit(String widgetId, String lineId, final SingleValueResource source, Row row,
			 Alert alert) {
		if(source instanceof StringResource)
			return stringEdit(widgetId, lineId, (StringResource) source, row, alert);
		if(source instanceof FloatResource)
			return floatEdit(widgetId, lineId, (FloatResource) source, row, alert,
					-Float.MAX_VALUE, Float.MAX_VALUE, null);
		if(source instanceof BooleanResource)
			return booleanEdit(widgetId, lineId, (BooleanResource) source, row);
		if(source instanceof IntegerResource)
			return integerEdit(widgetId, lineId, (IntegerResource) source, row, alert,
					Integer.MIN_VALUE, Integer.MAX_VALUE, null);
		throw new IllegalArgumentException("SingleValueResource type "+source.getResourceType()+ " not supported.");
	}
	
	public Label stringLabel(String widgetId, String lineId, final StringResource source, Row row) {
		if(checkLineId(widgetId)) return null;
		widgetId = ResourceUtils.getValidResourceName(widgetId);
		Label result = stringLabel(widgetId + lineId, source, null, null);
		finishRowSnippet(row, widgetId, result);	
		return result;
	}
	public Label stringLabel(String widgetId, String lineId, final String text, Row row) {
		if(checkLineId(widgetId)) return null;
		widgetId = ResourceUtils.getValidResourceName(widgetId);
		Label result = stringLabel(widgetId + lineId, null, null, text);
		finishRowSnippet(row, widgetId, result);	
		return result;
	}
	public Label stringLabel(final StringResource source) {
		counter++;
		return stringLabel("stringLabel"+counter, source, null, null);
	}
	public Label stringLabel(final String subResourceName) {
		counter++;
		return stringLabel("stringLabel"+counter, (StringResource)null, subResourceName, null);
	}
	private Label stringLabel(String widgetId, final StringResource optSource, String altId, final String text) {
		final SingleValueResourceAccess<StringResource> sva;
		if(text != null) {
			sva = null;
		} else {
			sva = new SingleValueResourceAccess<StringResource>(optSource, altId);
			
		}
		LabelFlex result = new LabelFlex(widgetId, this) {
			public void onGET(OgemaHttpRequest req) {
				if(text != null) {
					myLabel.setText(text, req);
				} else {
					StringResource source = getResource(sva, req, null);
					if ((source == null)||(!source.isActive())) {
						myLabel.setText("n.a.", req);
						return;
					}
					myLabel.setText(source.getValue(), req);
				}
			};
		};
		return result.myLabel;
	}

	public Label floatLabel(String widgetId, String lineId, final FloatResource source, Row row,
			 String format) {
		if(checkLineId(widgetId)) return null;
		widgetId = ResourceUtils.getValidResourceName(widgetId);
		Label result = floatLabel(widgetId + lineId, source, null, format);
		finishRowSnippet(row, widgetId, result);	
		return result;
	}
	public Label floatLabel(final FloatResource source, String format) {
		counter++;
		return floatLabel("floatLabel"+counter, source, null, format);
	}
	public Label floatLabel(final String subResourceName, String format) {
		counter++;
		return floatLabel("floatLabel"+counter, null, subResourceName, format);
	}
	/**
	 * 
	 * @param page
	 * @param id
	 * @param source
	 * @param row
	 * @param gateway
	 * @param fomat: if null "%.1f" is used
	 * @return
	 */
	private Label floatLabel(String widgetId, final FloatResource optSource, String altId, final String formatIn) {
		final SingleValueResourceAccess<FloatResource> sva = new SingleValueResourceAccess<FloatResource>(optSource, altId);
		Float minValue;
		String format;
		if(formatIn.contains("#min:")) {
			minValue = Float.parseFloat(formatIn.substring(formatIn.indexOf("#min:")+"#min:".length()));
			format = formatIn.substring(0, formatIn.indexOf("#min:"));
		} else {
			minValue = null;
			format = formatIn;
		}
		
		LabelFlex result = new LabelFlex(widgetId, this) {
			public void onGET(OgemaHttpRequest req) {
				FloatResource source = getResource(sva, req, null);
				if ((source == null)||(!source.isActive())) {
					myLabel.setText("n.a.", req);
					return;
				}
				
				float val;
				if(source instanceof TemperatureResource) {
					val = ((TemperatureResource) source).getCelsius();
				}
				else {
					val = source.getValue();
				}

				String valStr;
				if(minValue != null && val < minValue)
					valStr = "n/a";
				else if(format != null) {
					valStr = String.format(format, val);
				} else {
					valStr = String.format("%.1f", val);
				}
				myLabel.setText(valStr, req);
			}
		};
		return result.myLabel;
	}
	
	public Label intLabel(String widgetId, String lineId, final IntegerResource source, Row row,
			final int mode) {
		if(checkLineId(widgetId)) return null;
		widgetId = ResourceUtils.getValidResourceName(widgetId);
		Label result = intLabel(widgetId + lineId, source, null, mode);
		finishRowSnippet(row, widgetId, result);	
		return result;
	}
	public Label intLabel(final IntegerResource source, final int mode) {
		counter++;
		return intLabel("intLabel"+counter, source, null, mode);
	}
	public Label intLabel(final String subResourceName, int mode) {
		counter++;
		return intLabel("intLabel"+counter, null, subResourceName, mode);
	}
	/**
	 * 
	 * @param page
	 * @param id
	 * @param source
	 * @param row
	 * @param gateway
	 * @param mode 0: value unmodified
	 * @return
	 */
	private Label intLabel(String widgetId, final IntegerResource optSource, String altId, final int mode) {
		final SingleValueResourceAccess<IntegerResource> sva = new SingleValueResourceAccess<IntegerResource>(optSource, altId);
		LabelFlex result = new LabelFlex(widgetId, this) {
			public void onGET(OgemaHttpRequest req) {
				IntegerResource source = getResource(sva, req, null);
				if ((source == null)||(!source.isActive())) {
					myLabel.setText("n.a.", req);
					return;
				}
				String val;
				switch(mode) {
				default:
					val = ""+source.getValue();
				}
				myLabel.setText(val, req);
			};
		};
		return result.myLabel;
	}
	
	public Label booleanLabel(String widgetId, String lineId, final BooleanResource source, Row row,
			final int mode) {
		if(checkLineId(widgetId)) return null;
		widgetId = ResourceUtils.getValidResourceName(widgetId);
		Label result = booleanLabel(widgetId + lineId, source, null, mode);
		finishRowSnippet(row, widgetId, result);	
		return result;
	}
	public Label booleanLabel(final BooleanResource source, final int mode) {
		counter++;
		return booleanLabel("intLabel"+counter, source, null, mode);
	}
	public Label booleantLabel(final String subResourceName, int mode) {
		counter++;
		return intLabel("intLabel"+counter, null, subResourceName, mode);
	}
	/**
	 * 
	 * @param page
	 * @param id
	 * @param source
	 * @param row
	 * @param gateway
	 * @param mode 0: value unmodified
	 * @return
	 */
	private Label booleanLabel(String widgetId, final BooleanResource optSource, String altId, final int mode) {
		final SingleValueResourceAccess<BooleanResource> sva = new SingleValueResourceAccess<BooleanResource>(optSource, altId);
		LabelFlex result = new LabelFlex(widgetId, this) {
			public void onGET(OgemaHttpRequest req) {
				BooleanResource source = getResource(sva, req, null);
				if ((source == null)||(!source.isActive())) {
					myLabel.setText("n.a.", req);
					return;
				}
				String val;
				switch(mode) {
				default:
					val = ""+source.getValue();
				}
				myLabel.setText(val, req);
			};
		};
		return result.myLabel;
	}
	
	public interface LongProvider {
		LabelLongValue getValue(OgemaHttpRequest req);
	}
	

	public Label timeLabel(String widgetId, String lineId, final TimeResource source, Row row,
			final int mode) {
		if(checkLineId(widgetId)) return null;
		widgetId = ResourceUtils.getValidResourceName(widgetId);
		Label result = timeLabel(widgetId + lineId, source, null, mode);
		finishRowSnippet(row, widgetId, result);	
		return result;
	}
	public Label timeLabel(final TimeResource source, final int mode) {
		counter++;
		return timeLabel("timeLabel"+counter, source, null, mode);
	}
	public Label timeLabel(final String subResourceName, int mode) {
		counter++;
		return timeLabel("timeLabel"+counter, null, subResourceName, mode);
	}
	/**
	 * 
	 * @param page
	 * @param id
	 * @param source
	 * @param row
	 * @param gateway
	 * @param mode 0: absolute time, 1: time in day, 2: absolute time relative to now, 3: as 2 for future,
	 * 		4: date string (year to day), absolute time
	 * @return
	 */
	private Label timeLabel(String widgetId, final TimeResource optSource, String altId, final int mode) {
		final SingleValueResourceAccess<TimeResource> sva = new SingleValueResourceAccess<TimeResource>(optSource, altId);
		LabelFlex result = new LabelFlex(widgetId, this) {
			public void onGET(OgemaHttpRequest req) {
				TimeResource source = getResource(sva, req, null);
				if ((source == null)||(!source.isActive())) {
					myLabel.setText("n.a.", req);
					return;
				}
				String time;
				switch(mode) {
				case 1:
					time = StringFormatHelper.getFormattedTimeOfDay(source.getValue());
					break;
				case 2:
					if(source.getValue() <= 0) time = "not set";
					else
						time = StringFormatHelper.getFormattedAgoValue(appMan, source);
					break;
				case 3:
					if(source.getValue() <= 0) time = "not set";
					else
						time = StringFormatHelper.getFormattedFutureValue(appMan, source);
					break;
				case 4:
					if(source.getValue() <= 0) time = "not set";
					else
						time = TimeUtils.getDateString(source.getValue());
					break;
				default:
					if(source.getValue() <= 0) time = "not set";
					else
						time = TimeUtils.getDateAndTimeString(source.getValue());
				}
				myLabel.setText(time, req);
			};
		};
		return result.myLabel;
	}
	
	public Label fileSizeLabel(String widgetId, String lineId, final TimeResource source, Row row,
			final LongProvider provider) {
		if(checkLineId(widgetId)) return null;
		widgetId = ResourceUtils.getValidResourceName(widgetId);
		Label result = fileSizeLabel(widgetId + lineId, source, null, provider);
		finishRowSnippet(row, widgetId, result);	
		return result;
	}
	public Label fileSizeLabel(final TimeResource source, final LongProvider provider) {
		counter++;
		return fileSizeLabel("fileSizeLabel"+counter, source, null, provider);
	}
	public Label fileSizeLabel(final String subResourceName, final LongProvider provider) {
		counter++;
		return fileSizeLabel("fileSizeLabel"+counter, null, subResourceName, provider);
	}
	
	
	
	/**
	 * 
	 * @param page
	 * @param widgetId
	 * @param lineId
	 * @param source you can either provide a resource as source or you define a provider that is called to get
	 * 		the actual value or an alternativeText if no value can be provided. If a provider is specified the
	 * 		source resource will not be evaluated. 
	 * @param provider
	 * @param row
	 * @param gateway may be null. In this case no test will be performed if the gateway is active. This could
	 * 		be the standard case in the future.
	 * @return
	 */
	private Label fileSizeLabel(String widgetId, final TimeResource optSource, String altId,
			final LongProvider provider) {
		final SingleValueResourceAccess<TimeResource> sva = new SingleValueResourceAccess<TimeResource>(optSource, altId);
		LabelFlex result = new LabelFlex(widgetId, this) {
			public void onGET(OgemaHttpRequest req) {
				long value;
				if(provider != null) {
					LabelLongValue llv = provider.getValue(req);
					if(llv.alternativeText != null) {
						myLabel.setText(llv.alternativeText, req);
						return;						
					} else {
						value = llv.value;
					}
				} else {
					TimeResource source = getResource(sva, req, null);
					if ((source == null)||(!source.isActive())) {
						myLabel.setText("n.a.", req);
						return;
					} else {
						value = source.getValue();
					}
				}
				myLabel.setText(ApacheFileAdditions.byteCountToDisplaySize(BigInteger.valueOf(value)), req);
			};
		};
		return result.myLabel;
	}
	
	public Label resourceLabel(String widgetId, String lineId, final Resource source, Row row, int mode) {
		if(checkLineId(widgetId)) return null;
		widgetId = ResourceUtils.getValidResourceName(widgetId);
		Label result = resourceLabel(widgetId + lineId, source, null, mode);
		finishRowSnippet(row, widgetId, result);	
		return result;
	}
	public Label resourceLabel(final Resource source, int mode) {
		counter++;
		return resourceLabel("resourceLabel"+counter, source, null, mode);
	}
	public Label resourceLabel(final String subResourceName, int mode) {
		counter++;
		return resourceLabel("resourceLabel"+counter, null, subResourceName, mode);
	}
	private Label resourceLabel(String widgetId, final Resource optSource, String altId, final int mode) {
		final SingleValueResourceAccess<Resource> sva = new SingleValueResourceAccess<Resource>(optSource, altId);
		LabelFlex result = new LabelFlex(widgetId, this) {
			public void onGET(OgemaHttpRequest req) {
				Resource source = getResource(sva, req, null);
				if ((source == null)||(!source.isActive())) {
					myLabel.setText("n.a.", req);
					return;
				}
				switch(mode) {
				case 1:
					myLabel.setText(ResourceUtils.getHumanReadableShortName(source), req);
					break;
				case 2:
					myLabel.setText(source.getLocation(), req);
					break;
				case 3:
					myLabel.setText(source.getPath(), req);
					break;
				case 4:
					myLabel.setText(source.getName(), req);
					break;
				case 10:
					myLabel.setText(source.getResourceType().getName(), req);
					break;					
				case 11:
					myLabel.setText(source.getResourceType().getSimpleName(), req);
					break;					
				case 20:
					if(source instanceof SingleValueResource)
						myLabel.setText(ValueResourceUtils.getValue((SingleValueResource)source), req);
					else if(source instanceof ArrayResource)
						myLabel.setText(ValueResourceHelperSP.getValue((ArrayResource)source), req);
					else
						myLabel.setText("--", req);
					break;					
				default:
					myLabel.setText(ResourceUtils.getHumanReadableName(source), req);
				}
			};
		};
		return result.myLabel;
	}

	
	public TextField integerEdit(String widgetId, String lineId, final IntegerResource source, Row row,
			final Alert alert,final int minimumAllowed, final int maximumAllowed, String notAllowedMessage) {
		if(checkLineId(widgetId)) return null;
		widgetId = ResourceUtils.getValidResourceName(widgetId);
		TextField result = integerEdit(widgetId + lineId, source, null,
				alert, minimumAllowed, maximumAllowed, notAllowedMessage);
		finishRowSnippet(row, widgetId, result);	
		return result;
	}
	public TextField integerEdit(final IntegerResource source, final Alert alert,
			final int minimumAllowed, final int maximumAllowed, String notAllowedMessage) {
		counter++;
		return integerEdit("integerEdit"+counter, source, null,
				alert, minimumAllowed, maximumAllowed, notAllowedMessage);
	}
	public TextField integerEdit(final String subResourceName, final Alert alert,
			final int minimumAllowed, final int maximumAllowed, String notAllowedMessage) {
		counter++;
		return integerEdit("integerEdit"+counter, null, subResourceName, alert, minimumAllowed, maximumAllowed, notAllowedMessage);
	}
	private TextField integerEdit(String widgetId, final IntegerResource optSource, String altId, final Alert alert,
			final int minimumAllowed, final int maximumAllowed, String notAllowedMessage) {
		final SingleValueResourceAccess<IntegerResource> sva = new SingleValueResourceAccess<IntegerResource>(optSource, altId);
		final String notAllowedMessageUsed;
		if(notAllowedMessage == null) {
			notAllowedMessageUsed = "Value not Allowed!";
		} else
			notAllowedMessageUsed = notAllowedMessage;
		TextFieldFlex updateInterval = new TextFieldFlex(widgetId, this) {
			@Override
			public void onGET(OgemaHttpRequest req) {
				IntegerResource source = getResource(sva, req, IntegerResource.class);
				myField.setValue(source.getValue()+"",req);
			}
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				IntegerResource source = getResource(sva, req, IntegerResource.class);
				String val = myField.getValue(req);
				int value;
				try {
					value  = Integer.parseInt(val);
				} catch (NumberFormatException | NullPointerException e) {
					if(alert != null) alert.showAlert(notAllowedMessageUsed, false, req);
					return;
				}
				if (value < minimumAllowed) {
					if(alert != null) alert.showAlert(notAllowedMessageUsed, false, req);
					return;
				}
				if (value > maximumAllowed) {
					if(alert != null) alert.showAlert(notAllowedMessageUsed, false, req);
					return;
				}
				if(!source.exists()) {
					source.create();
					source.setValue(value);
					source.activate(true);
				} else {
					source.setValue(value);
				}
				if(alert != null) alert.showAlert("New value: " + value, true, req);
			}
			
		};
		if(alert != null) triggerOnPost(updateInterval.myField, alert); //updateInterval.myField.triggerAction(alert, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		updateInterval.myField.setDefaultInputmode("numeric");
		return updateInterval.myField;
	}
	public TextField integerEditExt(String widgetId, String lineId, final IntegerResource source, Row row,
			ValueConverter checker) {
		if(checkLineId(widgetId)) return null;
		widgetId = ResourceUtils.getValidResourceName(widgetId);
		TextField result = integerEditExt(widgetId + lineId, source, null,
				checker);
		finishRowSnippet(row, widgetId, result);	
		return result;
	}
	public TextField integerEditExt(final String subResourceName, ValueConverter checker) {
		counter++;
		return integerEditExt("integerEdit"+counter, null, subResourceName, checker);
	}
	public TextField integerEditExt(String widgetId, final IntegerResource optSource, String altId,
			ValueConverter checker) {
		final SingleValueResourceAccess<IntegerResource> sva = new SingleValueResourceAccess<IntegerResource>(optSource, altId);
		TextFieldFlex updateInterval = new TextFieldFlex(widgetId, this) {
			@Override
			public void onGET(OgemaHttpRequest req) {
				IntegerResource source = getResource(sva, req, IntegerResource.class);
				myField.setValue(source.getValue()+"",req);
			}
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				IntegerResource source = getResource(sva, req, IntegerResource.class);
				String val = myField.getValue(req);
				Integer value = checker.checkNewValueInt(val, req);
				if(value == null) return;
				if(!source.exists()) {
					source.create();
					source.setValue(value);
					source.activate(true);
				} else {
					source.setValue(value);
				}
			}
			
		};
		if(checker.getAlert() != null) triggerOnPost(updateInterval.myField, checker.getAlert());
		updateInterval.myField.setDefaultInputmode("numeric");
		return updateInterval.myField;
	}

	public TextField timeEdit(String widgetId, String lineId, final TimeResource source, Row row,
			final Alert alert, final long minimumAllowed, final long maximumAllowed, String notAllowedMessage,  final int mode) {
		if(checkLineId(widgetId)) return null;
		widgetId = ResourceUtils.getValidResourceName(widgetId);
		TextField result = timeEdit(widgetId + lineId, source, null,
				alert, minimumAllowed, maximumAllowed, notAllowedMessage, mode);
		finishRowSnippet(row, widgetId, result);	
		return result;
	}
	public TextField timeEdit(final TimeResource source, final Alert alert,
			final long minimumAllowed, final long maximumAllowed, String notAllowedMessage,  final int mode) {
		counter++;
		return timeEdit("timeEdit"+counter, source, null,
				alert, minimumAllowed, maximumAllowed, notAllowedMessage, mode);
	}
	public TextField timeEdit(final String subResourceName, final Alert alert,
			final long minimumAllowed, final long maximumAllowed, String notAllowedMessage,  final int mode) {
		counter++;
		return timeEdit("timeEdit"+counter, null, subResourceName, alert, minimumAllowed, maximumAllowed, notAllowedMessage, mode);
	}
	/**
	 * 
	 * @param widgetId
	 * @param optSource
	 * @param altId
	 * @param alert
	 * @param minimumAllowed
	 * @param maximumAllowed
	 * @param notAllowedMessage
	 * @param mode 0:milliseconds; 1:seconds; 2:minutes; 3:hours; 4:days; 5:months; -1: auto
	 * @return
	 */
	private TextField timeEdit(String widgetId, final TimeResource optSource, String altId, final Alert alert,
			final long minimumAllowed, final long maximumAllowed, String notAllowedMessage, final int mode) {
		final SingleValueResourceAccess<TimeResource> sva = new SingleValueResourceAccess<TimeResource>(optSource, altId);
		final String notAllowedMessageUsed;
		class LastMode {
			public int lastMode;
		}
		final LastMode lastMode = new LastMode();
		if(notAllowedMessage == null) {
			notAllowedMessageUsed = "Value not Allowed!";
		} else
			notAllowedMessageUsed = notAllowedMessage;
		TextFieldFlex updateInterval = new TextFieldFlex(widgetId, this) {
			@Override
			public void onGET(OgemaHttpRequest req) {
				TimeResource source = getResource(sva, req, TimeResource.class);
				switch(mode) {
				case 1:
					myField.setValue(source.getValue()/1000+"",req);
					break;
				case 2:
					myField.setValue(source.getValue()/60000+"",req);
					break;
				case 3:
					myField.setValue(source.getValue()/(60*60000)+"",req);
					break;
				case 4:
					myField.setValue(source.getValue()/(24*60*60000)+"",req);
					break;
				case 5:
					myField.setValue(source.getValue()/(30*24*60*60000)+"",req);
					break;
				case -1:
					String s = StringFormatHelperSP.getFormattedValue(source.getValue(), 360);
					lastMode.lastMode = getLastMode(s);
					myField.setValue(s, req);
					break;
				default:
					myField.setValue(source.getValue()+"",req);
				}
			}
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				TimeResource source = getResource(sva, req, TimeResource.class);
				String val = myField.getValue(req);
				int value;
				int flexMode = -99; //init here should never be relevant
				if(mode == -1) {
					flexMode = getLastMode(val);
					if(flexMode <= 0) flexMode = lastMode.lastMode;
					val = val.replaceAll("[^\\d.]", "");
				}
				try {
					value  = Integer.parseInt(val);
				} catch (NumberFormatException | NullPointerException e) {
					if(alert != null) alert.showAlert(notAllowedMessageUsed, false, req);
					return;
				}
				if (value < minimumAllowed) {
					if(alert != null) alert.showAlert(notAllowedMessageUsed, false, req);
					return;
				}
				if (value > maximumAllowed) {
					alert.showAlert(notAllowedMessageUsed, false, req);
					return;
				}
				if(mode == -1) {
					value = getCorerrectedValue(value, flexMode);
				} else {
					value = getCorerrectedValue(value, mode);
				}
				if(!source.exists()) {
					source.create();
					source.setValue(value);
					source.activate(true);
				} else {
					source.setValue(value);
				}
				if(alert != null) alert.showAlert("New interval " + value + " ms" , true, req);
			}
			private int getLastMode(String s) {
				if(s.endsWith("sec")) return 1;
				if(s.endsWith("min")) return 2;
				if(s.endsWith("h")) return 3;
				if(s.endsWith("d")) return 4;
				if(s.endsWith("month")) return 5;
				return 0;
			}
			private int getCorerrectedValue(int value, int mode) {
				switch(mode) {
				case 1:
					return value * 1000;
				case 2:
					return value * 60000;
				case 3:
					return value * (60*60000);
				case 4:
					return value * (24*60*60000);
				case 5:
					return value * (30*24*60*60000);
				default:
					return value;
				}
			}
		};
		if(alert != null) triggerOnPost(updateInterval.myField, alert); //updateInterval.myField.triggerAction(alert, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		return updateInterval.myField;
	}
	
	public TextField floatEdit(String widgetId, String lineId, final FloatResource source, Row row,
			final Alert alert,final float minimumAllowed, final float maximumAllowed, String notAllowedMessage) {
		if(checkLineId(widgetId)) return null;
		widgetId = ResourceUtils.getValidResourceName(widgetId);
		TextField result = floatEdit(widgetId + lineId, source, null,
				alert, minimumAllowed, maximumAllowed, notAllowedMessage, 0);
		finishRowSnippet(row, widgetId, result);	
		return result;
	}
	public TextField floatEdit(final FloatResource source, final Alert alert,
			final float minimumAllowed, final float maximumAllowed, String notAllowedMessage) {
		counter++;
		return floatEdit("floatEdit"+counter, source, null,
				alert, minimumAllowed, maximumAllowed, notAllowedMessage, 0);
	}
	public TextField floatEdit(final String subResourceName, final Alert alert,
			final float minimumAllowed, final float maximumAllowed, String notAllowedMessage) {
		counter++;
		return floatEdit("floatEdit"+counter, null, subResourceName, alert, minimumAllowed, maximumAllowed, notAllowedMessage, 0);
	}
	/**
	 * 
	 * @param widgetId
	 * @param optSource
	 * @param altId
	 * @param alert
	 * @param minimumAllowed
	 * @param maximumAllowed
	 * @param notAllowedMessage
	 * @param mode 0: default, 1:no value transformation
	 * @return
	 */
	private TextField floatEdit(String widgetId, final FloatResource optSource, String altId, final Alert alert,
			final float minimumAllowed, final float maximumAllowed, String notAllowedMessage, int mode) {
		final SingleValueResourceAccess<FloatResource> sva = new SingleValueResourceAccess<FloatResource>(optSource, altId);
		final String notAllowedMessageUsed;
		if(notAllowedMessage == null) {
			notAllowedMessageUsed = "Value not Allowed!";
		} else
			notAllowedMessageUsed = notAllowedMessage;
		TextFieldFlex updateInterval = new TextFieldFlex(widgetId, this) {
			@Override
			public void onGET(OgemaHttpRequest req) {
				FloatResource source = getResource(sva, req, FloatResource.class);
				if((source instanceof TemperatureResource)&&(mode == 0))
					myField.setValue(((TemperatureResource)source).getCelsius()+"",req);
				else {
					myField.setValue(source.getValue()+"",req);
				}
			}
			
			private void setValue(FloatResource source, float value) {
				if((source instanceof TemperatureResource)&&(mode == 0))
					((TemperatureResource)source).setCelsius(value);
				else
					source.setValue(value);
				
			}
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				FloatResource source = getResource(sva, req, FloatResource.class);
				String val = myField.getValue(req);
				float value;
				try {
					value  = Float.parseFloat(val);
				} catch (NumberFormatException | NullPointerException e) {
					if(alert != null) alert.showAlert(notAllowedMessageUsed, false, req);
					return;
				}
				if (value < minimumAllowed) {
					if(alert != null) alert.showAlert(notAllowedMessageUsed, false, req);
					return;
				}
				if (value > maximumAllowed) {
					if(alert != null) alert.showAlert(notAllowedMessageUsed, false, req);
					return;
				}
				if(!source.exists()) {
					source.create();
					setValue(source, value);
					source.activate(true);
				} else {
					setValue(source, value);
				}
				if(alert != null) alert.showAlert("New value: " + value, true, req);
			}
			
		};
		if(alert != null) triggerOnPost(updateInterval.myField, alert); //updateInterval.myField.triggerAction(alert, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		updateInterval.myField.setDefaultInputmode("decimal");
		return updateInterval.myField;
	}
	
	public TextField stringEdit(String widgetId, String lineId, final StringResource source, Row row,
			final Alert alert) {
		if(checkLineId(widgetId)) return null;
		widgetId = ResourceUtils.getValidResourceName(widgetId);
		TextField result = stringEdit(widgetId + lineId, source, null, alert);
		finishRowSnippet(row, widgetId, result);	
		return result;
	}
	public TextField stringEdit(final StringResource source, final Alert alert) {
		counter++;
		return stringEdit("stringEdit"+counter, source, null, alert);
	}
	public TextField stringEdit(final String subResourceName, final Alert alert) {
		counter++;
		return stringEdit("stringEdit"+counter, null, subResourceName, alert);
	}
	private TextField stringEdit(String widgetId, final StringResource optSource, String altId, final Alert alert) {
		final SingleValueResourceAccess<StringResource> sva = new SingleValueResourceAccess<StringResource>(optSource, altId);
		TextFieldFlex updateInterval = new TextFieldFlex(widgetId, this) {
			@Override
			public void onGET(OgemaHttpRequest req) {
				StringResource source = getResource(sva, req, StringResource.class);
				myField.setValue(source.getValue()+"",req);
			}
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				StringResource source = getResource(sva, req, StringResource.class);
				String val = myField.getValue(req);
				/*String value;
				try {
					value  = Float.parseFloat(val);
				} catch (NumberFormatException | NullPointerException e) {
					if(alert != null) alert.showAlert(notAllowedMessageUsed, false, req);
					return;
				}
				if (value < minimumAllowed) {
					if(alert != null) alert.showAlert(notAllowedMessageUsed, false, req);
					return;
				}
				if (value > maximumAllowed) {
					if(alert != null) alert.showAlert(notAllowedMessageUsed, false, req);
					return;
				}*/
				if(!source.exists()) {
					source.create();
					source.setValue(val);
					source.activate(true);
				} else {
					source.setValue(val);
				}
				if(alert != null) alert.showAlert("New value: " + val, true, req);
			}
			
		};
		if(alert != null) triggerOnPost(updateInterval.myField, alert); //updateInterval.myField.triggerAction(alert, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		return updateInterval.myField;
	}

	
	public DatepickerTimeResource datepicker(String widgetId, String lineId, final TimeResource source, Row row) {
		if(checkLineId(widgetId)) return null;
		widgetId = ResourceUtils.getValidResourceName(widgetId);
		DatepickerTimeResource result = datepicker(widgetId + lineId, source, null, null, null, null);
		finishRowSnippet(row, widgetId, result);	
		return result;
	}
	public DatepickerTimeResource datepicker(final TimeResource source) {
		counter++;
		return datepicker("timeEdit"+counter, source, null, null, null, null);
	}
	public DatepickerTimeResource datepicker(final String subResourceName) {
		counter++;
		return datepicker("timeEdit"+counter, null, subResourceName, null, null, null);
	}
	public DatepickerTimeResource datepicker(final String subResourceName, String format, String defaultDate, String viewMode) {
		counter++;
		return datepicker("timeEdit"+counter, null, subResourceName, format, defaultDate, viewMode);
	}
	private DatepickerTimeResource datepicker(String widgetId, final TimeResource optSource, String altId,
			String format, String defaultDate, String viewMode) {
		final SingleValueResourceAccess<TimeResource> sva = new SingleValueResourceAccess<TimeResource>(optSource, altId);
		DatepickerFlex updateInterval = new DatepickerFlex(widgetId, this, format, defaultDate, viewMode) {
			@Override
			public void onGET(OgemaHttpRequest req) {
				TimeResource source = getResource(sva, req, TimeResource.class);
				myDrop.selectItem(source, req);
			}
		};
		return updateInterval.myDrop;
	}
	

	public TemplateRedirectButton<T> linkingButton(String widgetId, String lineId, final T source, Row row,
			String buttonText, String url) {
		if(checkLineId(widgetId)) return null;
		widgetId = ResourceUtils.getValidResourceName(widgetId);
		TemplateRedirectButton<T> result = linkingButton(widgetId + lineId, source, 
				buttonText, url);
		finishRowSnippet(row, widgetId, result);	
		return result;
	}
	public TemplateRedirectButton<T> linkingButton(final T source, String buttonText, String url) {
		counter++;
		return linkingButton("linkingButton"+counter, source, buttonText, url);
	}
	private TemplateRedirectButton<T> linkingButton(String widgetId, final T optSource,
			String buttonText, String url) {
			TemplateRedirectButtonFlex<T> button = new TemplateRedirectButtonFlex<T>(widgetId, this,
				buttonText, url) {
			public void onPrePOST(String s, OgemaHttpRequest req) {
				if(optSource != null)
					myButton.selectItem(optSource, req);
			}
		};
		return button.myButton;
	}
	
	/** The following two methods are copied from ResourceGUIHelperBak and adapted*/
	public ResourceRedirectButton<R> linkingButton(final String subResourceName,
			String buttonText, String url) {
		counter++;
		return linkingButton("linkingButton"+counter, null, subResourceName, buttonText, url);
	}
	private ResourceRedirectButton<R> linkingButton(String widgetId, final R optSource, String altId,
			String buttonText, String url) {
		final SingleValueResourceAccess<R> sva = new SingleValueResourceAccess<R>(optSource, altId);
		ResourceRedirectButtonFlex<R> button = new ResourceRedirectButtonFlex<R>(widgetId, this,
				buttonText, url) {
			public void onPrePOST(String s, OgemaHttpRequest req) {
				R source = getResource(sva, req, null);
				myButton.selectItem(source, req);
			}
		};
		return button.myButton;
	}

	
	public BooleanResourceCheckbox booleanEdit(String widgetId, String lineId, final BooleanResource source, Row row) {
		if(checkLineId(widgetId)) return null;
		widgetId = ResourceUtils.getValidResourceName(widgetId);
		BooleanResourceCheckbox result = booleanEdit(widgetId + lineId, source, null);
		finishRowSnippet(row, widgetId, result);	
		return result;
	}
	public BooleanResourceCheckbox booleanEdit(final BooleanResource source) {
		counter++;
		return booleanEdit("booleanEdit"+counter, source, null);
	}
	public BooleanResourceCheckbox booleanEdit(final String subResourceName) {
		counter++;
		return booleanEdit("booleanEdit"+counter, null, subResourceName);
	}
	private BooleanResourceCheckbox booleanEdit(String widgetId, final BooleanResource optSource, String altId) {
		final SingleValueResourceAccess<BooleanResource> sva = new SingleValueResourceAccess<BooleanResource>(optSource, altId);
		BooleanResourceCheckboxFlex boolWidget = new BooleanResourceCheckboxFlex(widgetId, this) {
			public void onGET(OgemaHttpRequest req) {
				BooleanResource source = getResource(sva, req, BooleanResource.class);
				myCheckbox.selectItem(source, req);
			}
			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				BooleanResource source = getResource(sva, req, BooleanResource.class);
				if(!source.exists()) {
					source.create();
					source.activate(true);
				}
			}
		};
		return boolWidget.myCheckbox;
	}
	
	public <S extends SingleValueResource> ValueResourceDropdown<S> dropdown(String widgetId, String lineId, final S source, Row row,
			Map<String, String> valuesToSet) {
		if(checkLineId(widgetId)) return null;
		widgetId = ResourceUtils.getValidResourceName(widgetId);
		ValueResourceDropdown<S> result = dropdown(widgetId + lineId, source, null, valuesToSet, null);
		finishRowSnippet(row, widgetId, result);	
		return result;
	}
	public <S extends SingleValueResource> ValueResourceDropdown<S> dropdown(final S source,
			Map<String, String> valuesToSet) {
		counter++;
		return dropdown("dropdown"+counter, source, null, valuesToSet, null);
	}
	public <S extends SingleValueResource> ValueResourceDropdown<S> dropdown(final String subResourceName,
			Map<String, String> valuesToSet, final Class<S> resourceType) {
		counter++;
		return dropdown("dropdown"+counter, null, subResourceName, valuesToSet, resourceType);
	}
	private <S extends SingleValueResource> ValueResourceDropdown<S> dropdown(String widgetId, final S optSource, String altId,
			Map<String, String> valuesToSet, final Class<S> resourceType) {
		final SingleValueResourceAccess<S> sva = new SingleValueResourceAccess<S>(optSource, altId);
		ValueResourceDropdownFlex<S> widget = new ValueResourceDropdownFlex<S>(widgetId, this, valuesToSet) {
			public void onGET(OgemaHttpRequest req) {
				S source = getResource(sva, req, resourceType);
				myDrop.selectItem(source, req);
			}
			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				S source = getResource(sva, req, resourceType);
				if(!source.exists()) {
					source.create();
					source.activate(true);
				}
			}
		};
		return widget.myDrop;
	}

	public <S extends Resource> ResourceDropdown<S> referenceDropdownFixedChoice(String widgetId, String lineId, final S source, Row row,
			Map<S, String> valuesToSet) {
		return referenceDropdownFixedChoice(widgetId, lineId, source, row, valuesToSet, 0);
	}
	public <S extends Resource> ResourceDropdown<S> referenceDropdownFixedChoice(String widgetId, String lineId, final S source, Row row,
			Map<S, String> valuesToSet, int columnSize) {
		if(checkLineId(widgetId)) return null;
		widgetId = ResourceUtils.getValidResourceName(widgetId);
		ResourceDropdown<S> result = referenceDropdownFixedChoice(widgetId + lineId, source, null, valuesToSet, null);
		finishRowSnippet(row, widgetId, result, columnSize);	
		return result;
	}
	public <S extends Resource> ResourceDropdown<S> referenceDropdownFixedChoice(final S source,
			Map<S, String> valuesToSet) {
		counter++;
		return referenceDropdownFixedChoice("dropdown"+counter, source, null, valuesToSet, null);
	}
	public <S extends Resource> ResourceDropdown<S> referenceDropdownFixedChoice(final String subResourceName,
			Map<S, String> valuesToSet, final Class<S> resourceType) {
		counter++;
		return referenceDropdownFixedChoice("dropdown"+counter, null, subResourceName, valuesToSet, resourceType);
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
	private <S extends Resource> ResourceDropdown<S> referenceDropdownFixedChoice(String widgetId, final S optSource, String altId,
			final Map<S, String> valuesToSet, final Class<S> resourceType) {
		final SingleValueResourceAccess<S> sva = new SingleValueResourceAccess<S>(optSource, altId);
		ResourceDropdownFlex<S> widget = new ResourceDropdownFlex<S>(widgetId, this) {
			@Override
			public String getLabel(S object, OgemaLocale locale) {
				String result = valuesToSet.get(object);
				if(result != null) return result;
				return super.getLabel(object, locale);
			}
			
			@SuppressWarnings("unchecked")
			public void onGET(OgemaHttpRequest req) {
				S source = getResource(sva, req, resourceType);
				if(source.exists())
					myDrop.selectItem((S) source.getLocationResource(), req);
				else
					myDrop.selectSingleOption(DropdownData.EMPTY_OPT_ID, req);
			}
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				S source = getResource(sva, req, resourceType);
				if(!source.exists()) {
					source.create();
					source.activate(true);
				}
				S selection = myDrop.getSelectedItem(req);
				if(selection == null) source.delete();
				else source.setAsReference(selection);
			}
		};
		widget.myDrop.setDefaultItems(valuesToSet.keySet());
		widget.myDrop.setDefaultAddEmptyOption(true, "(not set)");
		return widget.myDrop;
	}
	
	/*protected R getGatewayInfo(T object, OgemaHttpRequest req) {
		return getResource(object, req);
	}*/
	protected T getGatewayInfo(OgemaHttpRequest req) {
		if(fixedGatewayInfo != null) return fixedGatewayInfo;
		return initObject.getSelectedItem(req);
	}
	
	/** Method to provide a resource based on access information
	 * 
	 * @param sva information on base resource and bath or direct resource reference
	 * @param req
	 * @param typeToCreate
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected <S extends Resource> S getResource(SingleValueResourceAccess<S> sva,
			OgemaHttpRequest req, Class<? extends S> typeToCreate) {
		R gw = getResource(getGatewayInfo(req), req);
		if(sva.optSource == null) {
			if(gw == null) return null;
			if(sva.altIdUsed == null) return null;
			S result;
			if(sva.altIdUsed.equals("")) {
				result = (S) gw;
			}
			else if(typeToCreate == null) {
				result = ResourceHelper.getSubResource(gw, sva.altIdUsed);
			} else {
				result = ResourceHelper.getSubResource(gw, sva.altIdUsed, typeToCreate);				
			}
			if((!acceptMissingResources)&&(result == null)) {
				throw new IllegalStateException("Subresource "+sva.altIdUsed+" not found!");
			}
			if(result.getLocationResource() == null) {
				throw new IllegalComponentStateException("Invalid location should throw framework exception:"+result.getLocation());
			}
			return result;
		} else {
			return sva.optSource;
		}
	}

	/*********************************** 
	 * Label widgets for various values
	 * TODO: Provide conversion to String in a separate class and avoid doubling the code between
	 * Resource-based label widgets and the resource-less widgets below 
	 * *********************************/

	public Label floatLabel(String widgetId, String lineId, final float value, Row row,
			 String format) {
		if(checkLineId(widgetId)) return null;
		widgetId = ResourceUtils.getValidResourceName(widgetId);
		Label result = floatLabel(widgetId + lineId, value, format);
		finishRowSnippet(row, widgetId, result);	
		return result;
	}
	public Label floatLabel(final float value, String format) {
		counter++;
		return floatLabel("floatLabel"+counter, value, format);
	}
	/**
	 * 
	 * @param page
	 * @param id
	 * @param source
	 * @param row
	 * @param gateway
	 * @param fomat: if null "%.1f" is used
	 * @return
	 */
	private Label floatLabel(String widgetId, final float value, final String format) {
		LabelFlex result = new LabelFlex(widgetId, this) {
			public void onGET(OgemaHttpRequest req) {
				String val = ValueFormat.floatVal(value, format);
				myLabel.setText(val, req);
			}
		};
		return result.myLabel;
	}
	
	public Label intLabel(String widgetId, String lineId, final Integer value, Row row,
			final int mode) {
		if(checkLineId(widgetId)) return null;
		widgetId = ResourceUtils.getValidResourceName(widgetId);
		Label result = intLabel(widgetId + lineId, value, mode);
		finishRowSnippet(row, widgetId, result);	
		return result;
	}
	public Label intLabel(final Integer value, final int mode) {
		counter++;
		return intLabel("intLabel"+counter, value, mode);
	}
	/**
	 * 
	 * @param page
	 * @param id
	 * @param source
	 * @param row
	 * @param gateway
	 * @param mode 0: value unmodified
	 * @return
	 */
	private Label intLabel(String widgetId, final Integer value, final int mode) {
		LabelFlex result = new LabelFlex(widgetId, this) {
			public void onGET(OgemaHttpRequest req) {
				if ((value == null)) {
					myLabel.setText("n.a.", req);
					return;
				}
				String val;
				switch(mode) {
				default:
					val = ""+value;
				}
				myLabel.setText(val, req);
			};
		};
		return result.myLabel;
	}
	
	public Label timeLabel(String widgetId, String lineId, final Long value, Row row,
			final int mode) {
		if(checkLineId(widgetId)) return null;
		widgetId = ResourceUtils.getValidResourceName(widgetId);
		Label result = timeLabel(widgetId + lineId, value, mode);
		finishRowSnippet(row, widgetId, result);	
		return result;
	}
	public Label timeLabel(final Long value, final int mode) {
		counter++;
		return timeLabel("timeLabel"+counter, value, mode);
	}
	/**
	 * 
	 * @param page
	 * @param id
	 * @param source
	 * @param row
	 * @param gateway
	 * @param mode 0: absolute time, 1: time in day, 2: absolute time relative to now
	 * @return
	 */
	private Label timeLabel(String widgetId, final Long value, final int mode) {
		LabelFlex result = new LabelFlex(widgetId, this) {
			public void onGET(OgemaHttpRequest req) {
				if ((value == null)) {
					myLabel.setText("n.a.", req);
					return;
				}
				String time;
				switch(mode) {
				case 1:
					time = StringFormatHelper.getFormattedTimeOfDay(value);
					break;
				case 2:
					if(value <= 0) time = "not set";
					else
						time = StringFormatHelper.getFormattedAgoValue(appMan, value);
					break;
				case 3:
					if(value <= 0) time = "not set";
					else
						time = StringFormatHelper.getFormattedFutureValue(appMan, value);
					break;
				default:
					if(value <= 0) time = "not set";
					else
						time = TimeUtils.getDateAndTimeString(value);
				}
				myLabel.setText(time, req);
			};
		};
		return result.myLabel;
	}
	
	public void triggerOnPost(OgemaWidget governor, OgemaWidget target) {
		if(doRegisterDependentWidgets) governor.registerDependentWidget(target);
		else governor.triggerAction(target, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
	}
	public void triggerOnPostForRequest(OgemaWidget governor, OgemaWidget target) {
		if(doRegisterDependentWidgets) governor.registerDependentWidget(target, req);
		else governor.triggerAction(target, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, req);
	}
	public void setDoRegisterDependentWidgets(boolean doRegisterDependentWidgets) {
		this.doRegisterDependentWidgets = doRegisterDependentWidgets;
	}
}
