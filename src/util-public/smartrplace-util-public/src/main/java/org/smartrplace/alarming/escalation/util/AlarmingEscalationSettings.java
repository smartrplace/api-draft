package org.smartrplace.alarming.escalation.util;

import org.ogema.core.model.ResourceList;
import org.ogema.model.prototypes.Data;

public interface AlarmingEscalationSettings extends Data {
	ResourceList<AlarmingEscalationLevel> levelData();
}
