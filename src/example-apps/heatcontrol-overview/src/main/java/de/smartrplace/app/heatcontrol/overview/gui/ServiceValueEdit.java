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
package de.smartrplace.app.heatcontrol.overview.gui;

import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.textfield.TextField;

public abstract class ServiceValueEdit {
	protected abstract String getValue(OgemaHttpRequest req);
	protected abstract void setValue(float value, OgemaHttpRequest req);
	private TextField textFieldWidget;	
	/** TODO: Move logic into common method with ObjectResourceGUIHelper.floatEdit
	 * 
	 * @param widgetId
	 * @param parent
	 * @param row
	 * @param req
	 * @param alert
	 * @param minimumAllowed
	 * @param maximumAllowed
	 * @param notAllowedMessage
	 */
	public ServiceValueEdit(String columnName, String lineId, Row row, final Alert alert,
			final float minimumAllowed, final float maximumAllowed, String notAllowedMessage,
			ObjectResourceGUIHelper<?, ?> vh) {
		if(vh.checkLineId(columnName)) return;
		final String notAllowedMessageUsed;
		if(notAllowedMessage == null) {
			notAllowedMessageUsed = "Value not Allowed!";
		} else
			notAllowedMessageUsed = notAllowedMessage;
		OgemaHttpRequest req = vh.getReq();
		OgemaWidget parent = vh.getParent();
		
		String columnId = ResourceUtils.getValidResourceName(columnName);
		String widgetId = columnId + lineId;
		textFieldWidget = new TextField(parent, widgetId, req) {
			private static final long serialVersionUID = 1L;
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				String value = ServiceValueEdit.this.getValue(req);
				setValue(value,req);
			}
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				String val =getValue(req);
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
				ServiceValueEdit.this.setValue(value, req);
				if(alert != null) alert.showAlert("New value: " + value, true, req);
			}
			
		};
		row.addCell(columnId, textFieldWidget);
	}
	
	public TextField getWidget() {
		return textFieldWidget;
	}
}
