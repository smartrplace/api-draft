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

import org.apache.commons.lang3.ArrayUtils;
import org.ogema.core.model.array.ArrayResource;
import org.ogema.core.model.array.BooleanArrayResource;
import org.ogema.core.model.array.ByteArrayResource;
import org.ogema.core.model.array.FloatArrayResource;
import org.ogema.core.model.array.IntegerArrayResource;
import org.ogema.core.model.array.StringArrayResource;
import org.ogema.core.model.array.TimeArrayResource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.core.recordeddata.RecordedData;
import org.ogema.core.resourcemanager.ResourceAccess;
import org.ogema.core.resourcemanager.ResourceManagement;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.smartrplace.autoconfig.api.OneTimeConfigStep;

import de.iwes.util.resource.ResourceHelper;
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
	
	/** To be moved to {@link ValueResourceUtils}*/
	public static String getValue(ArrayResource res) {
		Object[] array;
		if(res instanceof StringArrayResource)
			array = ((StringArrayResource)res).getValues();
		else if(res instanceof FloatArrayResource)
			array = ArrayUtils.toObject(((FloatArrayResource)res).getValues());
		else if(res instanceof IntegerArrayResource)
			array = ArrayUtils.toObject(((IntegerArrayResource)res).getValues());
		else if(res instanceof BooleanArrayResource)
			array = ArrayUtils.toObject(((BooleanArrayResource)res).getValues());
		else if(res instanceof TimeArrayResource)
			array = ArrayUtils.toObject(((TimeArrayResource)res).getValues());
		else if(res instanceof ByteArrayResource)
			array = ArrayUtils.toObject(((ByteArrayResource)res).getValues());
		else throw new UnsupportedOperationException("ArrayResource of type "+res.getResourceType()+" not supported!");
		return getAsString(array);
	}
	
	public static String getAsString(Object[] array) {
		return getAsString(array, false);
	}
	public static String getAsString(Object[] array, boolean addIndices) {
		String result = null;
		int idx = 0;
		for(Object obj: array) {
			String strEl;
			if(addIndices) strEl = "["+idx+"]:"+obj.toString();
			else strEl = obj.toString();
			if(result == null) result = strEl;
			else result += ", "+strEl;
			idx++;
		}
		return result;		
	}
	
	/** create resource if it does not yet exist. If the resource is newly created write
	 * value into it, otherwise do nothing. Note that the activation status is not changed,
	 * so the resource usually has to be activated later on if it was created.
	 * @return true if resource was created and value was written
	 */
	public static boolean setIfNew(String resLocation, String value, OneTimeConfigStep otc,
			ResourceAccess resAcc) {
		if(!otc.performConfig(resLocation))
			return false;
		StringResource fres = ResourceHelperSP.getSubResource(null, resLocation, StringResource.class, resAcc);
		if(!fres.exists()) {
			fres.create();
			fres.setValue(value);
			return true;
		}
		return false;
	}

}
