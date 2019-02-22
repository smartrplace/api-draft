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
package org.smartrplace.smarteff.access.api;

import java.util.List;

import org.ogema.core.model.Resource;

public class ConfigInfoExt {
	public ConfigInfoExt(int entryIdx, List<Resource> entryResources) {
		this.entryIdx = entryIdx;
		this.entryResources = entryResources;
	}
	public int entryIdx;
	public List<Resource> entryResources;
	
	public Resource lastPrimaryResource;
	public Object lastContext;
	public String lastConfigId;
	
	public Object context;
	//public String configId;
}
