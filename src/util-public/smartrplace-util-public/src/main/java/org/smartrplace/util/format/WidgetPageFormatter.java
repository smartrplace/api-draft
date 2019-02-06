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
package org.smartrplace.util.format;

import de.iwes.widgets.api.extended.WidgetData;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.WidgetStyle;
import de.iwes.widgets.api.widgets.html.HtmlItem;
import de.iwes.widgets.api.widgets.html.HtmlStyle;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.ButtonData;
import de.iwes.widgets.html.form.label.Header;

/** Format all widgets of a page according to a predefined style<br>
 *  Note that some formats have to be applied individually:
 *  - setMaxWidth(NN%): E.g. on dropdowns if they shall not fill entire page/column width
  */
public class WidgetPageFormatter {
	private WidgetStyle<?> headerAlignment = WidgetData.TEXT_ALIGNMENT_LEFT;
	private boolean tableStriped = true;
	private HtmlStyle coloredRowStyle = new HtmlStyle("class", "success");
	private HtmlStyle headerRowStyle = new HtmlStyle("style", "background-color: #77933C");
    private WidgetStyle<Button> buttonStyle = ButtonData.BOOTSTRAP_GREEN;
	private String backgroundImg = null;
	
	/** We do not get StaticTables here*/
	public void formatPage(WidgetPage<?> page) {
		for(OgemaWidget widget: page.getAllWidgets().getWidgets()) {
			if(widget instanceof Header) {
				((Header)widget).addDefaultStyle(headerAlignment);
			}
			if(widget instanceof Button) {
				((Button)widget).addDefaultStyle(buttonStyle);
			}
		}
		if(backgroundImg != null) page.setBackgroundImg(backgroundImg);
	}
	
	/** TODO: We cannot set the first line as table header here like in the LoadMonitoring App.
	 * This requires an extension of StaticTable
	 */
	public void formatStaticTable(StaticTable table) {
		if(tableStriped) table.addStyle(StaticTable.TABLE_STRIPED);
		setRowStyle(table, 0, headerRowStyle);
		for(int i=2;;i+=2) {
			HtmlItem item = (HtmlItem) table.getSubItem(i);
			if(item == null) break;
			item.addStyle(coloredRowStyle);
		}
	}
	
	public void setHeaderAlignment(WidgetStyle<?> headerAlignment) {
		this.headerAlignment = headerAlignment;
	}
	
	public void areTablesStriped(boolean isStriped) {
		tableStriped = isStriped;
	}

	private void setRowStyle(StaticTable table, int row, HtmlStyle style) {
		HtmlItem rw = (HtmlItem) table.getSubItem(row);
		rw.addStyle(style);
	}

	public void setButtonStype(WidgetStyle<Button> style) {
		this.buttonStyle = style;
	}
	
	public void setBackgroundImage(String image) {
		this.backgroundImg = image;
	}
}

