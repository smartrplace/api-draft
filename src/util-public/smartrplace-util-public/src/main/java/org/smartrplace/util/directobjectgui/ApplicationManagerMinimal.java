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
package org.smartrplace.util.directobjectgui;

/**This is a minimal implementation of the ApplicationManager that normal OGEMA applications receive.
 * Extension modules that shall not get access to ResourceManagement / ResourceAccess can get this
 * minimal framework access.
 * 
 */
public interface ApplicationManagerMinimal {
	long getFrameworkTime();
}
