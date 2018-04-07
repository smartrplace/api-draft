package org.smartrplace.smarteff.defaultservice;

import java.util.List;

import org.smartrplace.extensionservice.ExtensionCapabilityPublicData.EntryType;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.EditPageBase;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.api.base.BuildingData;

public class BuildingEditPage extends EditPageBase<BuildingData> {
	@Override
	public String label(OgemaLocale locale) {
		return "Standard Building Edit Page";
	}
	
	public boolean checkResource(BuildingData data) {
		String name = data.name().getValue();
		if(name.isEmpty()) return false;
		List<BuildingData> otherOfType = data.getParent().getSubResources(BuildingData.class, false);
		for(BuildingData ot: otherOfType) {
			if(ot.equalsLocation(data)) continue;
			if(ot.name().getValue().equals(name)) return false;
		}
		if(data.heatedLivingSpace().getValue() <= 0) return false;
		return true;
	}

	@Override
	protected Class<BuildingData> typeClass() {
		return BuildingData.class;
	}

	@Override
	protected void getEditTableLines(EditPageBase<BuildingData>.EditTableBuilder etb) {
		etb.addEditLine("Name", mh.stringEdit("name", alert));
		etb.addEditLine("Beheizte Fläche", mh.floatEdit("heatedLivingSpace", alert, 1, 999999, "Heated Living Space value outside range!"));
	}
	
	@Override
	protected List<EntryType> getEntryTypes() {
		return CapabilityHelper.getStandardEntryTypeList(typeClass());
	}
}
