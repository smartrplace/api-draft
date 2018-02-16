package org.sp.example.smartrheating;

import java.util.LinkedHashMap;
import java.util.Map;

import org.ogema.core.model.simple.IntegerResource;
import org.smartrplace.extensionservice.ApplicationManagerMinimal;
import org.smartrplace.extensionservice.ExtensionDoneListener;
import org.smartrplace.extensionservice.ExtensionResourceType;
import org.smartrplace.extensionservice.gui.DataEntryProvider;
import org.smartrplace.extensionservice.gui.ExtensionResourceEditPage;
import org.smartrplace.util.directresourcegui.ResourceGUIHelper;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.label.Label;
import extensionmodel.smarteff.api.base.SmartEffGeneralData;


/**
 * An HTML page, generated from the Java code.
 */
public class SmartrHeatingEditPage implements DataEntryProvider<SmartrHeatingData> { //extends ExtensionResourceEditPage<SmartrHeatingData>
	SmartEffGeneralData generalData = null;
	
	@Override
	public String id() {
		return SmartrHeatingEditPage.class.getSimpleName();
	}

	@Override
	public String label(OgemaLocale locale) {
		return "Edit SmartrHeating planning data";
	}

	@Override
	public String description(OgemaLocale locale) {
		return label(locale);
	}

	@Override
	public Class<SmartrHeatingData> getResourceTypeToEdit() {
		return SmartrHeatingData.class;
	}
	
	ResourceGUIHelper<SmartrHeatingData> mh;
	//final ResourceList<BuildingData> buildings;
	
	public SmartrHeatingEditPage(ApplicationManagerMinimal appManMin) {
		//we could save the info if required
	}
	
	/*@Override
	protected List<SmartrHeatingData> getOptionItems(OgemaHttpRequest req) {
		List<SmartrHeatingData> result = new ArrayList<>();
		for(BuildingData bd: buildings.getAllElements()) {
			List<SmartrHeatingData> els = bd.getSubResources(SmartrHeatingData.class, false);
			if(els.size() == 1) result.add(els.get(0));
		}
		return result;
	}*/

	//public SmartrHeatingEditPage(final WidgetPage<?> page, ResourceList<BuildingData> buildings,
	@Override
	public void initPage(final ExtensionResourceEditPage<SmartrHeatingData> page, ExtensionResourceType generalData) {
		//SmartrHeatingData data;data.typeOfThermostats()
		this.generalData = (SmartEffGeneralData) generalData;
		/*super(page, SmartrHeatingData.class, new LabelProvider<SmartrHeatingData>() {
			@Override
			public String getLabel(SmartrHeatingData item) {
				return ResourceUtils.getHumanReadableName(item.getParent());
			}
		});
		this.buildings = buildings;*/
		//TODO: allow to use ApplicationManagerMinimal
		mh = new ResourceGUIHelper<SmartrHeatingData>(page.page, page.init, null, false);
		
		/*TemplateDropdown<BuildingData> buildingDrop = new TemplateDropdown<BuildingData>(page, "buildingDrop") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				//TODO
			}
		};
		buildingDrop.setTemplate(new DefaultDisplayTemplate<>());*/
		
		StaticTable table = new StaticTable(4, 4, new int[]{1,5,5,1});
		int c = 0;
		Label itemName = new Label(page.page, "itemName") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				setText(page.init.getSelectedItem(req).getLocationResource().getName(), req);
			}
		};
		table.setContent(c, 0, "Building").setContent(c,1, page.drop);
		c++; //2
		table.setContent(c, 0, "Item name").setContent(c,1, itemName);
		c++; //2
		Map<String, String> valuesToSet = new LinkedHashMap<>();
		valuesToSet.put("1", "Standard on Radiators");
		valuesToSet.put("2", "Control knob connected via pressure cable");
		valuesToSet.put("3", "Room control device");
		valuesToSet.put("4", "Building automation system");
		table.setContent(c, 0, "Type of thermostats").setContent(c, 1, mh.dropdown("typeOfThermostats", valuesToSet , IntegerResource.class));
		c++;
		table.setContent(c, 0, "Number of rooms").setContent(c, 1, mh.integerEdit("numberOfRooms", null, 1, 999, ""));
		page.page.append(table);

		page.finalize(table);
	}

	@Override
	public boolean providesSingleEditPage() {
		return true;
	}

	@Override
	public boolean providesOverviewTable() {
		return false;
	}

	@Override
	public void setUserData(ExtensionResourceType parentOrReference,
			ExtensionDoneListener<SmartrHeatingData> listener, OgemaHttpRequest req) {
		//we get all we need via the init widget here. This would only be required when data of the user from
		//other models is required, e.g. for an auto-complete
		
	}

	@Override
	public void initResource(SmartrHeatingData resourceToUpdateInit) {
		ValueResourceHelper.setIfNew(resourceToUpdateInit.typeOfThermostats(), 1);		
		ValueResourceHelper.setIfNew(resourceToUpdateInit.numberOfRooms(), generalData.standardRoomNum().getValue());
		//TODO: add others
	}
}
