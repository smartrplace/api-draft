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
package de.smartrplace.app.heatcontrol.common.util;

import java.util.List;

import org.ogema.core.model.units.TemperatureResource;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.sensors.DoorWindowSensor;
import org.ogema.model.sensors.HumiditySensor;
import org.ogema.model.sensors.TemperatureSensor;

public class RoomDataUtil {
	public static float getRoomTemperatureMeasurement(List<TemperatureSensor> sensorList, List<Thermostat> tlist) {
		float val = getAverageRoomSensorMeasurement(sensorList);
		if(val >= 0) {
			return val;
		} else {
			if((tlist == null) || tlist.isEmpty()) return -1;
			TemperatureResource tempSens = tlist.get(0).temperatureSensor().reading();
			if(tempSens.isActive()) return tempSens.getValue();
		}
		return -1;
	}
	
	public static float getAverageRoomSensorMeasurement(List<TemperatureSensor> sensorList) {
		if((sensorList == null) || sensorList.isEmpty()) return -1;
		if(sensorList.size() == 1) return sensorList.get(0).reading().getValue();
		int count = 0;
		float sum = 0;
		for( TemperatureSensor sens: sensorList) {
			sum += sens.reading().getValue();
			count++;
		}
		return (sum / count);
	}

	public static float getAverageRoomHumditiyMeasurement(List<HumiditySensor> sensorList) {
		if((sensorList == null) || sensorList.isEmpty()) return -1;
		if(sensorList.size() == 1) return sensorList.get(0).reading().getValue();
		int count = 0;
		float sum = 0;
		for( HumiditySensor sens: sensorList) {
			if(Float.isNaN(sens.reading().getValue())) continue;
			sum += sens.reading().getValue();
			count++;
		}
		if(count == 0) return -1;
		return (sum / count);
	}

	public static float getTotalValveOpening(List<Thermostat> tlist) {
		float sum = 0;
		for( Thermostat th: tlist) {
			sum += th.valve().setting().stateFeedback().getValue();
		}
		return sum;		
	}
	
	public static int getNumberOpenWindows(List<DoorWindowSensor> winList) {
		int sum = 0;
		for( DoorWindowSensor win: winList) {
			if(win.reading().getValue() ) sum ++;
		}
		return sum;				
	}
}
