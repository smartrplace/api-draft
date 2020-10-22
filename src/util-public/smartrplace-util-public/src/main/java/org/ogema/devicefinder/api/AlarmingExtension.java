package org.ogema.devicefinder.api;

import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.model.extended.alarming.AlarmConfiguration;

import de.iwes.widgets.template.LabelledItem;

/** An alarming extension defines an additional alarming evaluation that can be applied
 * to any {@link SingleValueResource}. For implementation information see {@link AlarmingExtensionListener}.
 *
 */
public interface AlarmingExtension extends LabelledItem {
	/** The method shall be able to handle a null argument asking whether the extension
	 * shall be offered at all on the general page
	 * @param res
	 * @return
	 */
	boolean offerInGeneralAlarmingConfiguration(AlarmConfiguration ac);
	
	AlarmingExtensionListener getListener(SingleValueResource res, AlarmConfiguration ac);
}
