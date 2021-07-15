package org.ogema.tools.app.useradmin.config;

import org.ogema.core.model.simple.StringResource;
import org.ogema.model.prototypes.Data;

/**
 *
 * @author jlapp
 */
public interface MessagingAddress extends Data {
    
    StringResource address();
    
    StringResource addressType();
    
}
