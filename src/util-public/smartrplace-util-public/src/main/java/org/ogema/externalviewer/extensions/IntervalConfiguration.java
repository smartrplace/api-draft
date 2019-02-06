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
package org.ogema.externalviewer.extensions;

/** Configuration of evaluation intervals*/
public class IntervalConfiguration {
	public long start = 0;
	public long end = 0;
	/** If multiStart is not null, also multiEnd must exist and have the same array length.
	 * In this case start and end are not used.
	 */
	public long[] multiStart = null;
	public long[] multiEnd = null;
	/** These suffixes will be checked when searching for non-existing file names for
	 * result creation
	 */
	public String[] multiFileSuffix = null;
}

