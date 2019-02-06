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
package org.smartrplace.tissue.util.resource;

import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.core.recordeddata.RecordedData;

import de.iwes.util.resource.ValueResourceHelper;

/** Intended to move into {@link ValueResourceHelper} in the future.
 * 
 */
public class ValueResourceHelperSP {
	/** Get Recorded data from SingleValueResource
	 * 
	 * @param valueResource
	 * @return null if type of resource does not support getHistoricalData
	 */
	public static RecordedData getRecordedData(SingleValueResource valueResource) {
		if(valueResource instanceof FloatResource)
			return ((FloatResource)valueResource).getHistoricalData();
		if(valueResource instanceof IntegerResource)
			return ((IntegerResource)valueResource).getHistoricalData();
		if(valueResource instanceof TimeResource)
			return ((TimeResource)valueResource).getHistoricalData();
		if(valueResource instanceof BooleanResource)
			return ((BooleanResource)valueResource).getHistoricalData();
		return null;
	}
}
