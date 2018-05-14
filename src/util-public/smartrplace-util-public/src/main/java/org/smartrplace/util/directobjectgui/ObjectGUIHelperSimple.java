package org.smartrplace.util.directobjectgui;

import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.tools.resource.util.TimeUtils;
import org.smartrplace.util.directresourcegui.LabelLongValue;

import de.iwes.util.format.StringFormatHelper;
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

/** Class offering support for widget generation without taking into account OGEMA resources. If you want to use
 * any widgets linked to OGEMA resources it is recommended to use {@link ObjectGUIHelper}. This class may not be
 * well maintained.
  * @deprecated This is not used by {@link ObjectGUITablePage} and {@link ObjectGUIEditPage}, so the
 * helper will most likely be used only very rarely. For this reason it will not be really tested and
 * maintained
 */
@Deprecated
public class ObjectGUIHelperSimple<T> extends ObjectGUIHelperBase<T> {
	protected final TemplateInitSingleEmpty<T> init;
	protected final ApplicationManager appMan;
	
	public ObjectGUIHelperSimple(WidgetPage<?> page, TemplateInitSingleEmpty<T> init,
			ApplicationManager appMan, boolean acceptMissingResources) {
		super(page, acceptMissingResources);
		this.init = init;
		this.appMan = appMan;
	}
	public ObjectGUIHelperSimple(WidgetPage<?> page, T fixedGatewayInfo,
			ApplicationManager appMan, boolean acceptMissingResources) {
		super(page, fixedGatewayInfo, acceptMissingResources);
		this.init = null;
		this.appMan = appMan;
	}
	public ObjectGUIHelperSimple(OgemaWidget parent, OgemaHttpRequest req, T fixedGatewayInfo,
			ApplicationManager appMan, boolean acceptMissingResources) {
		super(parent, req, fixedGatewayInfo, acceptMissingResources);
		this.init = null;
		this.appMan = appMan;
	}

	public Label stringLabel(String widgetId, String lineId, final String text, Row row) {
		if(checkLineId(widgetId)) return null;
		widgetId = ResourceUtils.getValidResourceName(widgetId);
		Label result = stringLabel(widgetId + lineId, text);
		finishRowSnippet(row, widgetId, result);	
		return result;
	}
	public Label stringLabel(final String text) {
		counter++;
		return stringLabel("stringLabel"+counter, text);
	}
	private Label stringLabel(String widgetId, final String text) {
		LabelFlex result = new LabelFlex(widgetId, this) {
			public void onGET(OgemaHttpRequest req) {
				myLabel.setText(text, req);					
			};
		};
		return result.myLabel;
	}

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
				if (Float.isNaN(value)) {
					myLabel.setText("n.a.", req);
					return;
				}
				String val;
				if(format != null) {
					val = String.format(format, value);
				} else {
					val = String.format("%.1f", value);
				}
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
	
	public interface LongProvider {
		LabelLongValue getValue(OgemaHttpRequest req);
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
	
	public Label resourceLabel(String widgetId, String lineId, final Resource source, Row row, int mode) {
		if(checkLineId(widgetId)) return null;
		widgetId = ResourceUtils.getValidResourceName(widgetId);
		Label result = resourceLabel(widgetId + lineId, source, mode);
		finishRowSnippet(row, widgetId, result);	
		return result;
	}
	public Label resourceLabel(final Resource source, int mode) {
		counter++;
		return resourceLabel("resourceLabel"+counter, source, mode);
	}

	private Label resourceLabel(String widgetId, final Resource optSource, final int mode) {
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
				myButton.selectItem(optSource, req);
			}
		};
		return button.myButton;
	}

	public <S extends Resource> ResourceDropdown<S> referenceDropdownFixedChoice(String widgetId, String lineId, final S source, Row row,
			Map<S, String> valuesToSet) {
		if(checkLineId(widgetId)) return null;
		widgetId = ResourceUtils.getValidResourceName(widgetId);
		ResourceDropdown<S> result = referenceDropdownFixedChoice(widgetId + lineId, source, valuesToSet, null);
		finishRowSnippet(row, widgetId, result);	
		return result;
	}
	public <S extends Resource> ResourceDropdown<S> referenceDropdownFixedChoice(final S source,
			Map<S, String> valuesToSet) {
		counter++;
		return referenceDropdownFixedChoice("dropdown"+counter, source, valuesToSet, null);
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
	private <S extends Resource> ResourceDropdown<S> referenceDropdownFixedChoice(String widgetId, final S optSource,
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
	
	protected T getGatewayInfo(OgemaHttpRequest req) {
		if(fixedGatewayInfo != null) return fixedGatewayInfo;
		return init.getSelectedItem(req);
	}
}
