package org.ogema.devicefinder.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.AppID;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.units.VoltageResource;
import org.ogema.core.recordeddata.RecordedData;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.BatteryEvalBase.BatteryStatus;
import org.ogema.devicefinder.util.BatteryEvalBase.BatteryStatusPlus;
import org.ogema.devicefinder.util.BatteryEvalBase.BatteryStatusResult;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.message.MessageImpl;

import de.iwes.widgets.api.messaging.MessagePriority;
import de.iwes.widgets.api.widgets.WidgetStyle;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.label.LabelData;

public class BatteryEvalBase {
	public static final float DEFAULT_BATTERY_CHANGE_VOLTAGE = 2.7f;
	public static final float DEFAULT_BATTERY_WARN_VOLTAGE = 2.5f;
	public static final float DEFAULT_BATTERY_URGENT_VOLTAGE = 2.3f;
	public static final long TIME_TO_ASSUME_EMPTY = 1*TimeProcUtil.DAY_MILLIS;
	
	public static String getRightAlignedString(String in, int len) {
		if(in.length() >= len) return in.substring(0, len);
		return StringUtils.repeat(' ', len-in.length())+in;
	}
	public static String getLeftAlignedString(String in, int len) {
		if(in.length() >= len) return in.substring(0, len);
		return in+StringUtils.repeat(' ', len-in.length());
	}
	
	protected static void reallySendMessage(String title, String message, MessagePriority prio,
			ApplicationManagerPlus appManPlus) {
		AppID appId = appManPlus.appMan().getAppID();
		reallySendMessage(title, message, prio, appManPlus, appId);
	}
	public static void reallySendMessage(String title, String message, MessagePriority prio,
			ApplicationManagerPlus appManPlus, AppID appId) {
		appManPlus.guiService().getMessagingService().sendMessage(appId,
				new MessageImpl(title, message, prio));		
	}
	
	public static enum BatteryStatus {
		OK,
		CHANGE_RECOMMENDED,
		WARNING,
		URGENT,
		EMPTY,
		UNKNOWN,
		NO_BATTERY
	}
	public static class BatteryStatusResult {
		public InstallAppDevice iad;
		public float currentVoltage;
		public BatteryStatus status;
		public Long expectedEmptyDate;
	}
	
	public static BatteryStatus getBatteryStatus(float val, boolean changeInfoRelevant) {
		if(Float.isNaN(val) || val == 0)
			return BatteryStatus.UNKNOWN;
		if(val <= DEFAULT_BATTERY_URGENT_VOLTAGE)
			return BatteryStatus.URGENT;
		else if(val <= DEFAULT_BATTERY_WARN_VOLTAGE)
			return BatteryStatus.WARNING;
		else if(changeInfoRelevant && (val <= DEFAULT_BATTERY_CHANGE_VOLTAGE))
			return BatteryStatus.CHANGE_RECOMMENDED;
		return BatteryStatus.OK;
	}
	
	public static class BatteryStatusPlus {
		public BatteryStatus status;
		public float voltage = Float.NaN;
		public VoltageResource batRes;
	}

	
	public static WidgetStyle<Label> addBatteryStyle(Label label, float val, boolean changeInfoRelevant,
			OgemaHttpRequest req) {
		WidgetStyle<Label> result = null;
		BatteryStatus stat = getBatteryStatus(val, changeInfoRelevant);
		if(stat == BatteryStatus.URGENT || stat == BatteryStatus.EMPTY)
			result = LabelData.BOOTSTRAP_RED;
		else if(stat == BatteryStatus.WARNING)
			result = LabelData.BOOTSTRAP_ORANGE;
		else if(stat == BatteryStatus.CHANGE_RECOMMENDED)
			result = LabelData.BOOTSTRAP_BLUE;
		else if(stat == BatteryStatus.UNKNOWN || stat == BatteryStatus.NO_BATTERY)
			result = LabelData.BOOTSTRAP_GREY;
		if(result != null)
			label.addStyle(result, req);
		return result;
	}
	
	public static String getBatteryHTMLColor(BatteryStatus stat) {
		String result = null;
		if(stat == BatteryStatus.URGENT || stat == BatteryStatus.EMPTY)
			result = "red";
		else if(stat == BatteryStatus.WARNING)
			result = "orange";
		else if(stat == BatteryStatus.CHANGE_RECOMMENDED)
			result = "LightBlue";
		else if(stat == BatteryStatus.UNKNOWN || stat == BatteryStatus.NO_BATTERY)
			result = "grey";
		return result;
	}

	public static Map<Integer, Long> batteryDurationsFromLast = new HashMap<>();
	static {
		//batteryDurationsFromLast.put(33, 300*TimeProcUtil.DAY_MILLIS);
		batteryDurationsFromLast.put(33, 290*TimeProcUtil.DAY_MILLIS);
		batteryDurationsFromLast.put(32, 270*TimeProcUtil.DAY_MILLIS);
		batteryDurationsFromLast.put(31, 240*TimeProcUtil.DAY_MILLIS);
		batteryDurationsFromLast.put(30, 210*TimeProcUtil.DAY_MILLIS);
		batteryDurationsFromLast.put(29, 180*TimeProcUtil.DAY_MILLIS);
		batteryDurationsFromLast.put(28, 150*TimeProcUtil.DAY_MILLIS);
		batteryDurationsFromLast.put(27, 120*TimeProcUtil.DAY_MILLIS);
		batteryDurationsFromLast.put(26, 90*TimeProcUtil.DAY_MILLIS);
		batteryDurationsFromLast.put(25, 60*TimeProcUtil.DAY_MILLIS);
		batteryDurationsFromLast.put(24, 30*TimeProcUtil.DAY_MILLIS);
		batteryDurationsFromLast.put(23, 5*TimeProcUtil.DAY_MILLIS);
		batteryDurationsFromLast.put(22, 3*TimeProcUtil.DAY_MILLIS);
		batteryDurationsFromLast.put(21, 2*TimeProcUtil.DAY_MILLIS);
		batteryDurationsFromLast.put(20, 1*TimeProcUtil.DAY_MILLIS);
	}
	
	public static long getRemainingLifeTimeEstimation(float voltageFromWhichDroppedPermanently) {
		float curVal = voltageFromWhichDroppedPermanently;
		if(Float.isNaN(curVal))
			return -1;
		if(curVal >= 3.3f)
			return batteryDurationsFromLast.get(33);
		else if(curVal <= 2.0f)
			return batteryDurationsFromLast.get(20);
		else
			return batteryDurationsFromLast.get(Math.round(curVal*10));
		
	}
	
	public static Long getExpectedEmptyDateSimple(VoltageResource batRes, long now) {
		RecordedData ts = batRes.getHistoricalData();
		if(ts == null)
			return null;
		SampledValue svFirst = ts.getNextValue(0);
		if(svFirst == null)
			return null;
		
		float curVal = ts.getPreviousValue(now).getValue().getFloatValue();
		float preVal = curVal+0.1f;
		long curTime = getRemainingLifeTimeEstimation(curVal);
		long preTime = getRemainingLifeTimeEstimation(preVal);
		return now + (curTime+preTime)/2;
	}
	
	public static BatteryStatus getBatteryStatus(VoltageResource batRes, boolean changeInfoRelevant, Long now) {
		return getBatteryStatusPlus(batRes, changeInfoRelevant, now).status;
	}
	public static BatteryStatusPlus getBatteryStatusPlus(VoltageResource batRes, boolean changeInfoRelevant, Long now) {
		BatteryStatusPlus result = new BatteryStatusPlus();
		result.batRes = batRes;
		if(batRes == null) {
			result.status = BatteryStatus.NO_BATTERY;
			return result;
		}
		result.voltage = batRes.getValue();
		Long lastTs = null;
		if(Float.isNaN(result.voltage)) {
			RecordedData ts = batRes.getHistoricalData();
			if(ts != null) {
				SampledValue sv = ts.getPreviousValue(now!=null?now:Long.MAX_VALUE);
				if(sv != null) {
					result.voltage = sv.getValue().getFloatValue();
					lastTs = sv.getTimestamp();
				}
			}
		} else
			lastTs = batRes.getLastUpdateTime();
		result.status = getBatteryStatus(result.voltage, changeInfoRelevant);
		if(result.status != BatteryStatus.URGENT)
			return result;
		if(lastTs != null && now != null && (now - lastTs) > TIME_TO_ASSUME_EMPTY)
			result.status = BatteryStatus.EMPTY;
		return result;
	}
	
	/**
	 * 
	 * @param iad
	 * @param changeInfoRelevant
	 * @param now may be null, only required to find last value before now and to detect EMPTY by duration without value
	 * @return
	 */
	public static BatteryStatus getBatteryStatus(InstallAppDevice iad, boolean changeInfoRelevant, Long now) {
		return getBatteryStatusPlus(iad, changeInfoRelevant, now).status;
	}
	public static BatteryStatusPlus getBatteryStatusPlus(InstallAppDevice iad, boolean changeInfoRelevant, Long now) {
		VoltageResource sres = DeviceHandlerBase.getBatteryVoltage(iad.device().getLocationResource());
		if(iad.knownFault().assigned().exists()) {
			int kni = iad.knownFault().assigned().getValue();
			if(kni == AlarmingConfigUtil.ASSIGNMENT_BATTERYLOW) {
				BatteryStatusPlus result = new BatteryStatusPlus();
				result.status = BatteryStatus.EMPTY;
				result.batRes = sres;
				return result;
			}
		}
		return getBatteryStatusPlus(sres, changeInfoRelevant, now);
	}	
}
