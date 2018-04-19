package org.smartrplace.smarteff.util.editgeneric;

import org.ogema.core.model.Resource;
import org.smartrplace.smarteff.util.EditPageBase;
import org.smartrplace.smarteff.util.button.BackButton;
import org.smartrplace.smarteff.util.button.ProposalProvTableOpenButton;
import org.smartrplace.smarteff.util.button.TableOpenButton;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.RedirectButton;

public abstract class EditPageGenericParams<T extends Resource> extends EditPageGenericWithTable<T> {
	public EditPageGenericParams() {
		super(false);
	}
	public EditPageGenericParams(boolean isWithTable) {
		super(isWithTable);
	}
	
	protected class EditElementParams extends EditElement {
		public final OgemaWidget controlWidget;
		public EditElementParams(OgemaWidget ogemaWidgetForTitle, OgemaWidget valueWidget,
				OgemaWidget controlWidget) {
			super(ogemaWidgetForTitle, valueWidget);
			this.controlWidget = controlWidget;
		}
	}
	
	protected class EditTableBuilderParams extends EditTableBuilder {
		public void addEditLine(OgemaWidget widgetForTitle, OgemaWidget widget,
				OgemaWidget controlWidget, OgemaWidget descriptionLink) {
			EditElementParams el = new EditElementParams(widgetForTitle, widget, controlWidget);
			el.setDescriptionUrl(descriptionLink);
			editElements.add(el);
		}
	}
	
	@Override
	protected void buildMainTable() {
		EditTableBuilderParams etb = new EditTableBuilderParams();
		getEditTableLines(etb);

		StaticTable table = new StaticTable(etb.editElements.size()+1, 5, new int[]{1,3,4,3,1});
		int c = 0;
		for(EditElement etl: etb.editElements) {
			if((etl.title != null)&&(etl.widget != null)) {
				table.setContent(c, 1, etl.title).setContent(c,2, etl.widget);
				//etl.widget.registerDependentWidget(activateButton);
			} else if((etl.title != null)&&(etl.stringForWidget != null))
				table.setContent(c, 1, etl.title).setContent(c,2, etl.stringForWidget);
			else if((etl.widgetForTitle != null)&&(etl.widget != null)) {
				table.setContent(c, 1, etl.widgetForTitle).setContent(c,2, etl.widget);
				if(etl.decriptionLink != null) table.setContent(c, 3, etl.decriptionLink);
				//etl.widget.registerDependentWidget(activateButton);
				if(etl instanceof EditPageGenericParams.EditElementParams) {
					@SuppressWarnings("unchecked")
					EditElementParams etlp = (EditElementParams)etl;
					if(etlp.controlWidget != null) table.setContent(c, 3, etlp.controlWidget);
				}
			}
			else
				throw new IllegalStateException("Something went wrong with building the edit line "+c+" Obj:"+etl);
			c++;
		}
		RedirectButton allParamsButton = new RedirectButton(page, "allParamsButton", "My Params", "org_smartrplace_smarteff_defaultservice_TopConfigTablePage.html");
		allParamsButton.setDefaultOpenInNewTab(false);
		TableOpenButton backButton = new BackButton(page, "back", pid(), exPage, null);
		table.setContent(c, 0, "").setContent(c, 1, backButton);
		TableOpenButton proposalTableOpenButton = new ProposalProvTableOpenButton(page, "proposalTableOpenButton", pid(), exPage, null);
		table.setContent(c, 2, proposalTableOpenButton);

		page.append(table);
		exPage.registerAppTableWidgetsDependentOnInit(table);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void performAddEditLine(OgemaWidget label, OgemaWidget valueWidget, OgemaWidget linkButton, String sub,
			EditPageGeneric<T>.TypeResult type, EditPageBase<T>.EditTableBuilder etb) {
		Button control = new Button(page, "control") {
			private static final long serialVersionUID = 1L;
			
		};
		if(etb instanceof EditPageGenericParams.EditTableBuilderParams) {
			((EditTableBuilderParams)etb).addEditLine(label, valueWidget, linkButton, control);
		} else etb.addEditLine(label, valueWidget, linkButton);
	}
}
