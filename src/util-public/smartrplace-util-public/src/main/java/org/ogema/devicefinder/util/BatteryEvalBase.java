package org.ogema.devicefinder.util;

import java.time.Instant;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.AppID;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.units.VoltageResource;
import org.ogema.core.recordeddata.RecordedData;
import org.ogema.model.devices.storage.ElectricityStorage;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.eval.EvalUtilBase;
import org.smartrplace.util.message.MessageImpl;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTiming;
import de.iwes.widgets.api.messaging.MessagePriority;
import de.iwes.widgets.api.widgets.WidgetStyle;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.label.LabelData;

public class BatteryEvalBase {
	public static final float DEFAULT_BATTERY_NOTNEW_VOLTAGE = ValueResourceHelper.getFloatProperty("org.ogema.devicefinder.util.battery.notNewvolt", 2.7f);
	public static final float DEFAULT_BATTERY_CHANGE_VOLTAGE = ValueResourceHelper.getFloatProperty("org.ogema.devicefinder.util.battery.changevolt", 2.7f);
	public static final float DEFAULT_BATTERY_WARN_VOLTAGE = ValueResourceHelper.getFloatProperty("org.ogema.devicefinder.util.battery.warnvolt", 2.5f);
	public static final float DEFAULT_BATTERY_URGENT_VOLTAGE = ValueResourceHelper.getFloatProperty("org.ogema.devicefinder.util.battery.urgentvolt", 2.3f);
	public static final long TIME_TO_ASSUME_EMPTY = 1*TimeProcUtil.DAY_MILLIS;
	
	public static FloatResource batteryLifetimeExpectedYears = null;
	
	public static String getRightAlignedString(String in, int len) {
		if(in.length() >= len) return in.substring(0, len);
		return StringUtils.repeat(' ', len-in.length())+in;
	}
	public static String getLeftAlignedString(String in, int len) {
		if(in.length() >= len) return in.substring(0, len);
		return in+StringUtils.repeat(' ', len-in.length());
	}
	
	public static void reallySendMessage(String title, String message, MessagePriority prio,
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
		NEW,
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
	
	/*public static BatteryStatus getBatteryStatus(float val, boolean changeInfoRelevant) {
		return getBatteryStatus(val, changeInfoRelevant, 2);
	}*/

	public static String getGermanStatus(BatteryStatus status) {
		switch(status) {
		case NEW:
		case OK:
			return "OK";
		case CHANGE_RECOMMENDED:
			return "Wechsel empfohlen";
		case WARNING:
			return "Warnung";
		case URGENT:
			return "Dringend";
		case EMPTY:
			return "Keine Versorgung mehr";
		case UNKNOWN:
			return "Unbekannt";
		case NO_BATTERY:
			return "keine Batterie";
		default:
			return status.toString();
		}
	}

	public static String getGermanLiftetimeEstimation(BatteryStatus status) {
		switch(status) {
		case NEW:
		case OK:
			return "OK";
		case CHANGE_RECOMMENDED:
			return "Wenige Monate";
		case WARNING:
			return "Einige Wochen";
		case URGENT:
			return "Wenige Tage/Wochen";
		case EMPTY:
			return "--";
		case UNKNOWN:
			return "Unbekannt";
		case NO_BATTERY:
			return "keine Batterie";
		default:
			return status.toString();
		}
	}

	/**
	 * 
	 * @param val
	 * @param changeInfoRelevant
	 * @param batteryNum if(<=0 then a chargeSensor value 0.0...1.0 is expected
	 * @return
	 */
	public static BatteryStatus getBatteryStatus(float val, boolean changeInfoRelevant, int batteryNum) {
		if(Float.isNaN(val) || val == 0)
			return BatteryStatus.UNKNOWN;
		if(batteryNum <= 0) {
			if(val <= 0.15f)
				return BatteryStatus.URGENT;
			else if(val <= 0.25f)
				return BatteryStatus.WARNING;
			else if(changeInfoRelevant && (val <= 0.4f))
				return BatteryStatus.CHANGE_RECOMMENDED;
			else
				return BatteryStatus.OK;
		} else if(batteryNum == 2) {
			if(val <= DEFAULT_BATTERY_URGENT_VOLTAGE)
				return BatteryStatus.URGENT;
			else if(val <= DEFAULT_BATTERY_WARN_VOLTAGE)
				return BatteryStatus.WARNING;
			else if(changeInfoRelevant && (val <= DEFAULT_BATTERY_CHANGE_VOLTAGE))
				return BatteryStatus.CHANGE_RECOMMENDED;
			else if(changeInfoRelevant && (val <= DEFAULT_BATTERY_NOTNEW_VOLTAGE))
				return BatteryStatus.OK;
		} else {
			if(val <= batteryNum*(DEFAULT_BATTERY_URGENT_VOLTAGE/2))
				return BatteryStatus.URGENT;
			else if(val <= batteryNum*(DEFAULT_BATTERY_WARN_VOLTAGE/2))
				return BatteryStatus.WARNING;
			else if(changeInfoRelevant && (val <= batteryNum*(DEFAULT_BATTERY_CHANGE_VOLTAGE/2)))
				return BatteryStatus.CHANGE_RECOMMENDED;
			else if(changeInfoRelevant && (val <= batteryNum*(DEFAULT_BATTERY_NOTNEW_VOLTAGE/2)))
				return BatteryStatus.OK;
		} 
		return BatteryStatus.NEW;
	}
	
	public static class BatteryStatusPlus {
		public BatteryStatus status;
		
		/** This value may also contain a SOC value if batSOC is evaluated*/
		public float voltage = Float.NaN;
		public VoltageResource batRes;
		
		/** Only available if batRes not found and batSOC is found*/
		public FloatResource batSOC;
	}

	
	public static WidgetStyle<Label> addBatteryStyle(Label label, float val, boolean changeInfoRelevant,
			String deviceLocation, OgemaHttpRequest req,
			boolean isSOCValue, boolean styleWasSet) {
		WidgetStyle<Label> result = null;
		final BatteryStatus stat;
		if(isSOCValue) {
			stat = getBatteryStatus(val, changeInfoRelevant, 0);			
		} else {
			boolean singleBattery = isSingleBattery(deviceLocation);
			stat = getBatteryStatus(val, changeInfoRelevant, singleBattery?1:2);
		}
		if(stat == BatteryStatus.URGENT || stat == BatteryStatus.EMPTY)
			result = LabelData.BOOTSTRAP_RED;
		else if(stat == BatteryStatus.WARNING)
			result = LabelData.BOOTSTRAP_ORANGE;
		else if(stat == BatteryStatus.CHANGE_RECOMMENDED)
			result = LabelData.BOOTSTRAP_BLUE;
		else if(stat == BatteryStatus.UNKNOWN || stat == BatteryStatus.NO_BATTERY)
			result = LabelData.BOOTSTRAP_GREY;
		if(result != null)
			label.setStyle(result, req);
		else if(styleWasSet)
			label.setStyle(LabelData.BOOTSTRAP_GREEN, req);
		return result;
	}
	
	public static boolean isSingleBattery(String deviceLocation) {
		if(deviceLocation == null)
			return true;
		if(!(deviceLocation.toUpperCase().contains("HMIP_SWDO")
				|| deviceLocation.toUpperCase().contains("HMIP_SRH")))
			return false;
		return !(deviceLocation.contains("SWDO_PL_2_") || deviceLocation.contains("SWDO_I_"));
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

	
	public static float expected270days = 1.33f*ValueResourceHelper.getFloatProperty("org.ogema.devicefinder.util.battery.expectedYears", 1.0f);
	public static Map<Integer, Long> batteryDurationsFromLast = new HashMap<>();
	static {
		//batteryDurationsFromLast.put(33, 300*TimeProcUtil.DAY_MILLIS);
		batteryDurationsFromLast.put(33, ((long) (expected270days*290))*TimeProcUtil.DAY_MILLIS);
		batteryDurationsFromLast.put(32, ((long) (expected270days*270))*TimeProcUtil.DAY_MILLIS);
		batteryDurationsFromLast.put(31, ((long) (expected270days*240))*TimeProcUtil.DAY_MILLIS);
		batteryDurationsFromLast.put(30, ((long) (expected270days*210))*TimeProcUtil.DAY_MILLIS);
		batteryDurationsFromLast.put(29, ((long) (expected270days*180))*TimeProcUtil.DAY_MILLIS);
		batteryDurationsFromLast.put(28, ((long) (expected270days*150))*TimeProcUtil.DAY_MILLIS);
		batteryDurationsFromLast.put(27, ((long) (expected270days*120))*TimeProcUtil.DAY_MILLIS);
		batteryDurationsFromLast.put(26, ((long) (expected270days*85))*TimeProcUtil.DAY_MILLIS);
		batteryDurationsFromLast.put(25, ((long) (expected270days*50))*TimeProcUtil.DAY_MILLIS);
		batteryDurationsFromLast.put(24, ((long) (expected270days*20))*TimeProcUtil.DAY_MILLIS);
		batteryDurationsFromLast.put(23, ((long) (expected270days*5))*TimeProcUtil.DAY_MILLIS);
		batteryDurationsFromLast.put(22, ((long) (expected270days*3))*TimeProcUtil.DAY_MILLIS);
		batteryDurationsFromLast.put(21, ((long) (expected270days*2))*TimeProcUtil.DAY_MILLIS);
		batteryDurationsFromLast.put(20, ((long) (expected270days*1))*TimeProcUtil.DAY_MILLIS);
	}
	
	public static long getRemainingLifeTimeEstimation(float voltageFromWhichDroppedPermanently) {
		if(batteryLifetimeExpectedYears != null && batteryLifetimeExpectedYears.isActive())
			return getRemainingLifeTimeEstimation(voltageFromWhichDroppedPermanently, batteryLifetimeExpectedYears.getValue());
		return getRemainingLifeTimeEstimation(voltageFromWhichDroppedPermanently, null);
	}
	public static long getRemainingLifeTimeEstimation(
			VoltageResource batRes, ApplicationManager appMan) {
		if(batteryLifetimeExpectedYears != null && batteryLifetimeExpectedYears.isActive())
			return getRemainingLifeTimeEstimation(batteryLifetimeExpectedYears.getValue(), batRes, appMan);
		return getRemainingLifeTimeEstimation(null, batRes, appMan);
	}
	public static long getRemainingLifeTimeEstimation(
			VoltageResource batRes, long now) {
		if(batteryLifetimeExpectedYears != null && batteryLifetimeExpectedYears.isActive())
			return getRemainingLifeTimeEstimation(batteryLifetimeExpectedYears.getValue(), batRes, now);
		return getRemainingLifeTimeEstimation(null, batRes, now);
	}
	/** This version requires the estimated battery lifetime to be provided by the resource batteryLifetimeExpectedYears.
	 * If the battery lifetime shall be estimated by providing the time since the last battery change, use {@link #getRemainingLifeTimeEstimation(float, long)}
	 * TODO: Take into account time since last drop of voltage*/
	private static Map<String, Long> lastDropTimeMap = new HashMap<>();
	private static Map<String, Float> lastDropVoltageMap = new HashMap<>();
	public static long getRemainingLifeTimeEstimation(float voltageFromWhichDroppedPermanently,
			Float batteryLifetimeExpectedYears) {
		float curVal = voltageFromWhichDroppedPermanently;
		
		double factor;
		if(batteryLifetimeExpectedYears != null) {
			float factorByProperty = ValueResourceHelper.getFloatProperty("org.ogema.devicefinder.util.battery.expectedYears", 1.0f);
			factor = batteryLifetimeExpectedYears / factorByProperty;
		} else
			factor = 1.0f;
		
		if(Float.isNaN(curVal))
			return -1;
		if(curVal >= 3.3f)
			return (long) (factor*batteryDurationsFromLast.get(33));
		else if(curVal <= 2.0f)
			return (long) (factor*batteryDurationsFromLast.get(20));
		else
			return (long) (factor*batteryDurationsFromLast.get(Math.round(curVal*10)));
		
	}
	public static long getRemainingLifeTimeEstimation(
			Float batteryLifetimeExpectedYears, VoltageResource batRes, ApplicationManager appMan) {
		long now = appMan.getFrameworkTime();
		return getRemainingLifeTimeEstimation(batteryLifetimeExpectedYears, batRes, now);
	}
	public static long getRemainingLifeTimeEstimation(Float batteryLifetimeExpectedYears, VoltageResource batRes, long now) {
		Long lastDropTime;
		float curVal = batRes.getValue();
		boolean singleBattery = isSingleBattery(batRes.getLocation());
		if(singleBattery)
			curVal = 2*curVal;
		float voltageFromWhichDroppedPermanently = curVal + 0.1f;

		
		Float lastDropVoltage = lastDropVoltageMap.get(batRes.getLocation());
		if(lastDropVoltage == null || lastDropVoltage != voltageFromWhichDroppedPermanently) {
			lastDropTime = EvalUtilBase.firstLargerTimeBackwardsSafe(batRes.getHistoricalData(), batRes.getValue(),
					180*1440, now);				
			if(lastDropTime < 0)
				lastDropTime = now - 180*TimeProcUtil.DAY_MILLIS;
			lastDropTimeMap.put(batRes.getLocation(), lastDropTime);
			lastDropVoltageMap.put(batRes.getLocation(), voltageFromWhichDroppedPermanently);
		} else
			lastDropTime = lastDropTimeMap.get(batRes.getLocation());
		
		long dropPreviousRemain = getRemainingLifeTimeEstimation(voltageFromWhichDroppedPermanently+0.1f, batteryLifetimeExpectedYears);
		long dropThisRemain = getRemainingLifeTimeEstimation(voltageFromWhichDroppedPermanently, batteryLifetimeExpectedYears);
		//We expect that estimated duration is at approx 20% of liftime, so we add 20% expired here. We start with 20% expired then.
		double expiredSinceLastDropFactor = ((double)(now - lastDropTime))/(dropPreviousRemain - dropThisRemain) + 0.2;
		if(expiredSinceLastDropFactor > 1.2)
			expiredSinceLastDropFactor = 1.2;
		return (long) (dropThisRemain + (1-expiredSinceLastDropFactor)*(dropPreviousRemain - dropThisRemain));
	}
	
	//TODO: This requires caching
	/*public static long getRemainingLifeTimeEstimation(InstallAppDevice iad, long now) {
		BatteryStatusPlus stat = getBatteryStatusPlus(iad, true, now);
		if(stat.batRes == null) {
			if(stat.batSOC != null) {
				long lastChange = 
			}
		}
	}*/
	
	/**
	 * 
	 * @param voltageFromWhichDroppedPermanently
	 * @param durationSinceLastChange time since last battery change
	 * @return
	 */
	public static long getRemainingLifeTimeEstimation(float voltageFromWhichDroppedPermanently,
			long durationSinceLastChange) {
		long basedOn365days = getRemainingLifeTimeEstimation(voltageFromWhichDroppedPermanently, 1.0f);
		long durationSinceLast365 = TimeProcUtil.YEAR_MILLIS - basedOn365days;
		double factor = ((double)durationSinceLastChange) / durationSinceLast365;
		long result = (long) (factor * basedOn365days);
		return result;
	}
	
	public static long getFullLifeTimeEstimation(float voltageFromWhichDroppedPermanently,
			long durationSinceLastChange) {
		long basedOn365days = getRemainingLifeTimeEstimation(voltageFromWhichDroppedPermanently, 1.0f);
		long durationSinceLast365 = TimeProcUtil.YEAR_MILLIS - basedOn365days;
		double factor = ((double)durationSinceLastChange) / durationSinceLast365;
		long result = (long) (factor * TimeProcUtil.YEAR_MILLIS);
		return result;
	}

	public static Long getExpectedEmptyDateSimple(VoltageResource batRes, long now) {
		RecordedData ts = batRes.getHistoricalData();
		if(ts == null)
			return null;
		SampledValue svFirst = ts.getNextValue(0);
		if(svFirst == null)
			return null;
		
		float curVal = ts.getPreviousValue(now).getValue().getFloatValue();
		//float preVal = curVal+0.1f;
		long curTime = getRemainingLifeTimeEstimation(batRes, now);
		return curTime;
		//long preTime = getRemainingLifeTimeEstimation(preVal, batRes, now);
		//return now + (curTime+preTime)/2;
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
		boolean singleBattery = isSingleBattery(batRes.getLocation());
		result.status = getBatteryStatus(result.voltage, changeInfoRelevant, singleBattery?1:2);
		if(now != null) {
			result.status = getAdaptedStatus(result.status, batRes, now);
			/*long remain = BatteryEvalBase.getRemainingLifeTimeEstimation(batRes, now);
			long endOfApril = getEndOfApril(now);
			long yearStart = AbsoluteTimeHelper.getIntervalStart(now, AbsoluteTiming.YEAR);
			int dayOfYear = (int) ((now - yearStart) / TimeProcUtil.DAY_MILLIS);
			if(dayOfYear < 120 && (now + remain > endOfApril))
				result.status = BatteryStatus.OK;
			else if(dayOfYear < 170)
				result.status = BatteryStatus.OK;*/
		}
		if(result.status != BatteryStatus.URGENT)
			return result;
		if(lastTs != null && now != null && (now - lastTs) > TIME_TO_ASSUME_EMPTY)
			result.status = BatteryStatus.EMPTY;
		return result;
	}
	
	public static BatteryStatus getAdaptedStatus(BatteryStatus statusStd, VoltageResource batRes, long now) {
		if(Boolean.getBoolean("org.smartrplace.apps.hw.install.gui.alarm.batteryLow.basedOnRemainingTime")
				&& (statusStd == BatteryStatus.CHANGE_RECOMMENDED || statusStd == BatteryStatus.WARNING)) {
			long remain = BatteryEvalBase.getRemainingLifeTimeEstimation(batRes, now);
			long endOfApril = getEndOfApril(now);
			long yearStart = AbsoluteTimeHelper.getIntervalStart(now, AbsoluteTiming.YEAR);
			int dayOfYear = (int) ((now - yearStart) / TimeProcUtil.DAY_MILLIS);
			if(dayOfYear < 120 && (now + remain > endOfApril))
				return BatteryStatus.OK;
			else if(dayOfYear < 170)
				return BatteryStatus.OK;
		}		
		return statusStd;
	}
	
	public static long getEndOfApril(long now0) {
		final ZonedDateTime now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(now0), ZoneId.systemDefault());
		return getEndOfApril(now);
	}
	public static long getEndOfApril(ZonedDateTime now) {
		final Month month = now.getMonth();
		ZonedDateTime t0 = now;
		if (month.compareTo(Month.APRIL) > 0 || (month == Month.APRIL && now.getDayOfMonth() == 31))
			t0 = t0.plusYears(1);
		long timestamp = t0.with(Month.APRIL).with(TemporalAdjusters.lastDayOfMonth()).truncatedTo(ChronoUnit.DAYS).toEpochSecond()*1000;
		return timestamp;
	}

	
	public static BatteryStatusPlus getBatteryStatusSOCPlus(FloatResource batSOC, boolean changeInfoRelevant, Long now) {
		BatteryStatusPlus result = new BatteryStatusPlus();
		result.batSOC = batSOC;
		if(batSOC == null) {
			result.status = BatteryStatus.NO_BATTERY;
			return result;
		}
		result.voltage = batSOC.getValue();
		Long lastTs = null;
		if(Float.isNaN(result.voltage)) {
			RecordedData ts = batSOC.getHistoricalData();
			if(ts != null) {
				SampledValue sv = ts.getPreviousValue(now!=null?now:Long.MAX_VALUE);
				if(sv != null) {
					result.voltage = sv.getValue().getFloatValue();
					lastTs = sv.getTimestamp();
				}
			}
		} else
			lastTs = batSOC.getLastUpdateTime();
		result.status = getBatteryStatus(result.voltage, changeInfoRelevant, 0);
		if(result.status != BatteryStatus.URGENT)
			return result;
		if(lastTs != null && now != null && (now - lastTs) > TIME_TO_ASSUME_EMPTY)
			result.status = BatteryStatus.EMPTY;
		return result;
	}

	public static boolean hasBattery(PhysicalElement dev, Long now) {
		BatteryStatusPlus status = getBatteryStatus(dev, now);
		if(status == null || status.status == BatteryStatus.NO_BATTERY
				|| status.status == BatteryStatus.UNKNOWN)
			return false;
		return true;
	}
	
	public static boolean hasBatteryVoltage(PhysicalElement dev, Long now) {
		BatteryStatusPlus status = getBatteryStatus(dev, now);
		if(status == null || status.status == BatteryStatus.NO_BATTERY
				|| status.status == BatteryStatus.UNKNOWN)
			return false;
		if(status.batRes == null)
			return false;
		return true;
	}

	public static BatteryStatusPlus getBatteryStatus(PhysicalElement dev, Long now) {
		return getBatteryStatus(dev, false, now);
	}
	public static BatteryStatusPlus getBatteryStatus(PhysicalElement dev, boolean forceSOCTest,
			Long now) {
		VoltageResource bat = DeviceHandlerBase.getBatteryVoltage(dev);
		if(bat != null && bat.isActive() && (!forceSOCTest)) {
			return BatteryEvalBase.getBatteryStatusPlus(bat, true, now);
		} 
		FloatResource batSOC = dev.getSubResource("battery", ElectricityStorage.class).chargeSensor().reading();
		if(batSOC != null && batSOC.isActive()) {
			return BatteryEvalBase.getBatteryStatusSOCPlus(batSOC, true, now);
		}
		BatteryStatusPlus result = new BatteryStatusPlus();
		if(now != null) {
			BooleanResource batteryStatus = DeviceHandlerBase.getSubResourceOfSibblingOrDirectChildMaintenance(dev.getLocationResource(),
					"batteryLow", BooleanResource.class);
			if(batteryStatus != null) {
				//TODO: Take into account main sensor value to check if device 
				// is online => if last status value is far in the past => OK again, then batteryStatus needs to be set back to false
				// if recent, then URGENT, if last value is false and far, then OK
				long lastStatusAgo = now - batteryStatus.getLastUpdateTime(); 
				if(lastStatusAgo > TimeProcUtil.DAY_MILLIS) {
					if(batteryStatus.getValue()) {
						//TODO
						result.status = BatteryStatus.EMPTY;
					} else
						result.status = BatteryStatus.OK;
				} else if(batteryStatus.getValue()) {
					result.status = BatteryStatus.URGENT;
				}
				return result;
			}
		}
		result.status = BatteryStatus.NO_BATTERY;
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
		if(sres == null || (!sres.isActive())) {
			Resource device2 = iad.device().getLocationResource();
			if(!device2.getLocation().contains("_cc")) {
				FloatResource batSOC = device2.getSubResource("battery", ElectricityStorage.class).chargeSensor().reading();
				if(batSOC != null && batSOC.isActive()) {
					return getBatteryStatusSOCPlus(batSOC, changeInfoRelevant, now);							
				}
			}
		}
		return getBatteryStatusPlus(sres, changeInfoRelevant, now);
	}	
}
