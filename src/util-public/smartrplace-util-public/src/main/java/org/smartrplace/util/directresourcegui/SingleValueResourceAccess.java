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
package org.smartrplace.util.directresourcegui;

import org.ogema.core.model.Resource;

public class SingleValueResourceAccess<S extends Resource> {
	public final String altIdUsed;
	public final boolean useGatewayInfo;
	public final S optSource;
	public SingleValueResourceAccess(S optSource, String altId) {
		this.optSource = optSource;
		if(altId != null) {
			if(altId.startsWith("L:")) {
				altIdUsed = altId.substring(2);
				useGatewayInfo = true;
			} else {
				altIdUsed = altId;
				useGatewayInfo = false;
			}
		} else {
			altIdUsed = null;
			useGatewayInfo = true;
		}
	}
}
