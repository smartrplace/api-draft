package org.smartrplace.alarming.escalation.util;

public interface EscalationManagerI {
	/** Register escalation provider. This will also register the TimedJob for the provider.*/
	void registerEscalationProvider(EscalationProvider prov);
	
	/** Unregister escalation provider if registered. Note that this function may not be implemented.
	 * 
	 * @param prov
	 * @return escalation provider unregistered if it was unregistered
	 */
	EscalationProvider unregisterEscalationProvider(EscalationProvider prov);
}
