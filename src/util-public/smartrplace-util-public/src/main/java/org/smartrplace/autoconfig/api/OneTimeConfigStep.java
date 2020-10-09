package org.smartrplace.autoconfig.api;

/** A OneTimeConfigStep is used to perform a configuration once one a system and store the information persistently
 * that the configuration has been done
 *
 */
public interface OneTimeConfigStep {
	boolean performConfig(String variableLocation);
}
