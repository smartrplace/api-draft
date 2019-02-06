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
package org.smartrplace.apps.heatcontrol.extensionapi;

public interface GUIInitDataProvider {
	/** Services providing a list of objects may provide a sample here for
	 * {@link ObjectGUITablePages}
	 * @return sample object independently from real data objects available for
	 * initialization of the table
	 */
	<T> T getInitObject(Class<T> objectClass);
}
