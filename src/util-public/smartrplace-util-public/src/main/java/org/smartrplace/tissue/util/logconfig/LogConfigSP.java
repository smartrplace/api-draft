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
package org.smartrplace.tissue.util.logconfig;

import java.util.List;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.recordeddata.RecordedDataConfiguration;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.recordeddata.DataRecorder;
import org.ogema.recordeddata.DataRecorderException;
import org.ogema.recordeddata.RecordedDataStorage;
import org.slf4j.Logger;

import de.iwes.util.resource.ResourceHelper;

/** Intended to move to {@link LogConfig} in the future
 *
 */
public class LogConfigSP {
	/** Get resource of device to be used as primary device for the input resource
	 * for user interaction etc.
	 * @param subResource resource for which the primary device resource shall be returned
	 * @param locationRelevant if true only device resources will be returned with an active subresource
	 * 		location e.g. indicating the room where the device is placed. It is not considered if the
	 * 		subResource has an element of org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance in
	 * 		its parent path. Default is true.
	 * @param useHighest if true the highest PhysicalElement above the input subResource is returned, otherwise
	 * 		the lowest fitting. Default is true.
	 * @return device resource or null if no suitable resource was found
	 */
	public static PhysicalElement getDeviceResource(Resource subResource, boolean locationRelevant) {
		return getDeviceResource(subResource, locationRelevant, true);
	}
	public static PhysicalElement getDeviceResource(Resource subResource, boolean locationRelevant, boolean
			useHighest) {
		Resource hmCheck = ResourceHelper.getFirstParentOfType(subResource, "org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance");
		if(hmCheck != null) {
			Resource parent = hmCheck.getParent();
			if(parent == null) return null;
			List<PhysicalElement> devices = parent.getSubResources(PhysicalElement.class, false);
			if(devices.isEmpty()) return null;
			if(devices.size() > 1) return null; //throw new IllegalStateException("HmDevice should have maximum 1 PhysicalElement as child, "+parent.getLocation()+" has "+devices.size());
			return devices.get(0);
		}
		PhysicalElement device = ResourceHelper.getFirstParentOfType(subResource, PhysicalElement.class);
		if(device == null) return null;
		PhysicalElement highestWithLocation = ((!locationRelevant) || device.location().isActive())?device:null;
		Resource parent = device.getParent();
		while(true) {
			if((!useHighest) && (highestWithLocation != null)) return highestWithLocation;
			if(parent != null && parent instanceof PhysicalElement) {
				device = (PhysicalElement) parent;
				if((!locationRelevant) || device.location().isActive()) highestWithLocation = device;
				parent = device.getParent();
			} else {
				if(parent != null) {
					parent = ResourceHelper.getFirstParentOfType(parent, PhysicalElement.class);
					if(parent == null) return highestWithLocation;
					else continue;
				}
				if(highestWithLocation != null) return highestWithLocation;
				return device;
			}
		}
	}

	/** Copied from EnergyServerImport class*/
    public static RecordedDataStorage getRecordedData(FloatResource res,
    		DataRecorder dataRecorder, Logger logger) {
        String id = res.getLocation();
        RecordedDataStorage rds = dataRecorder.getRecordedDataStorage(id);
        if (rds == null) {
            RecordedDataConfiguration configuration = new RecordedDataConfiguration();
            /*setting ON_VALUE_CHANGED/ON_VALUE_UPDATE will cause a write immediately when storage is created.
             * 
             * 
             * CHANGED IMPLEMENTATION, SO THE FOLLOWING DOES NOT APPLY ANYMORE
             * But setting to FIXED_INTERVAL means that it will try to read only rounded timestamps, which does
             * not work for interval MAX_VALUE. So you have to make sure that resource contains a reasonable value
             * on every startup that will be added on startup.<br>
             * TODO: The implementation needs to be adapted regarding this.
             */
            configuration.setStorageType(RecordedDataConfiguration.StorageType.ON_VALUE_UPDATE);
            //configuration.setStorageType(RecordedDataConfiguration.StorageType.FIXED_INTERVAL);
            //configuration.setFixedInterval(Long.MAX_VALUE);
            try {
				rds = dataRecorder.createRecordedDataStorage(id, configuration);
			} catch (DataRecorderException e) {
				throw new IllegalStateException(e);
			}
            if(logger != null)
            	logger.debug("created new recorded data for {}", id);
        } else {
            if(logger != null)
            	logger.debug("got data series for {}: {}", id, rds.getConfiguration());
            if (rds.getConfiguration() == null || 
                    rds.getConfiguration().getStorageType() != RecordedDataConfiguration.StorageType.ON_VALUE_UPDATE
                    //rds.getConfiguration().getStorageType() != RecordedDataConfiguration.StorageType.FIXED_INTERVAL
                    //|| rds.getConfiguration().getFixedInterval() != Long.MAX_VALUE
                    ) {
                if(logger != null)
                	logger.debug("setting storage type to ON_VALUE_UPDATE for {}", id);
                RecordedDataConfiguration configuration = new RecordedDataConfiguration();
                configuration.setStorageType(RecordedDataConfiguration.StorageType.ON_VALUE_UPDATE);
                //configuration.setStorageType(RecordedDataConfiguration.StorageType.FIXED_INTERVAL);
                //configuration.setFixedInterval(Long.MAX_VALUE);
                rds.setConfiguration(configuration);
            }
            if(logger != null)
            	logger.debug("returning recorded data for {}", id);
        }
        return rds;
    }

    public static void storeData(List<SampledValue> toInsert,  RecordedDataStorage rds) {
        //logger.debug("inserting {} values into recorded data for {}", toInsert.size(), res.getLocation());
        
    	RecordedDataConfiguration cfg_ovc = new RecordedDataConfiguration();
        cfg_ovc.setStorageType(RecordedDataConfiguration.StorageType.ON_VALUE_CHANGED);
        rds.setConfiguration(cfg_ovc);

        if (!toInsert.isEmpty()) {
            try {
				rds.insertValues(toInsert);
			} catch (DataRecorderException e) {
				e.printStackTrace();
			} finally {
	            RecordedDataConfiguration cfg_fixed = new RecordedDataConfiguration();
	            cfg_fixed.setStorageType(RecordedDataConfiguration.StorageType.FIXED_INTERVAL);
	            cfg_fixed.setFixedInterval(Long.MAX_VALUE);
	            rds.setConfiguration(cfg_fixed);
			}
        }
    }

}
