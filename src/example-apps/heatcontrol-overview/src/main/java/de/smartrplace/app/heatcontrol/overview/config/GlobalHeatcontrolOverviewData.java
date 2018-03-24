package de.smartrplace.app.heatcontrol.overview.config;

import org.ogema.core.model.simple.TimeResource;
import org.ogema.model.prototypes.Data;

public interface GlobalHeatcontrolOverviewData extends Data {
	TimeResource updateRate(); 
}
