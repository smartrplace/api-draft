package org.ogema.accessadmin.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.model.gateway.LocalGatewayInformation;
import org.ogema.model.locations.BuildingPropertyUnit;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.Configuration;
import org.ogema.timeseries.eval.simple.api.KPIResourceAccess;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.external.accessadmin.config.AccessAdminConfig;
import org.smartrplace.external.accessadmin.config.AccessConfigBase;
import org.smartrplace.external.accessadmin.config.AccessConfigUser;
import org.smartrplace.external.accessadmin.config.SubCustomerData;
import org.smartrplace.external.accessadmin.config.SubCustomerSuperiorData;
import org.smartrplace.gateway.device.GatewaySuperiorData;
import org.smartrplace.util.virtualdevice.ChartsUtil;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.resourcelist.ResourceListHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class SubcustomerUtil {
	public static final String DECORATOR_NAME = "subcustomer";
	public static final String ALL_ROOMS_GROUP_NAME = "All Rooms";
	private static final long MAX_UPDATE_INTERVAL = 10*TimeProcUtil.MINUTE_MILLIS;
	
	public static boolean isSuperior() {
		return Boolean.getBoolean("org.smartrplace.app.srcmon.server.issuperior")
				|| Boolean.getBoolean("org.smartplace.app.srcmon.server.issuperior");
	}

	/** The ids are stored in resources persistently, so on a certain system the meaning of each integer
	 * value shall not be changed, although the exact name texts can be adapted.
	 */
	public static class SubCustomerType extends NamedIntegerType {
		/**Id is written into the {@link Room#type()} field and should this be aligned with the
		 * standard room types if possible
		 */
		public final Map<Integer, NamedIntegerType> roomTypes = new LinkedHashMap<>();
		public NamedIntegerType defaultRoomType;
		public int[] defaultWorkingDays = new int[] {1,2,3,4,5};
		//public float defaultEcoTemperatureHeating = 273.15f+16f;
		//public float defaultEcoTemperatureCooling = 273.15f+30f;
		
		public SubCustomerType(int id, Map<OgemaLocale, String> name) {
			super(id, false, name);
		}

		public SubCustomerType(int id, String englishName) {
			super(id, false, englishName);
		}
		
		public SubCustomerType(int id, String englishName, String germanName) {
			super(id, false, englishName, germanName);
		}
		
		public void addRoomType(NamedIntegerType roomType) {
			addRoomType(Arrays.asList(new NamedIntegerType[] {roomType,
					new NamedIntegerType(roomType.getType(), true, roomType.getName(), roomType.idPrefix)}));
		}
		public void addRoomType(List<NamedIntegerType> roomTypeList) {
			if(roomTypes.isEmpty())
				defaultRoomType = roomTypeList.get(0);
			for(NamedIntegerType roomType: roomTypeList)
				roomTypes.put(roomType.getTypeUnique(), roomType);
		}
	}
	public static Map<Integer, SubCustomerType> subCustomerTypes = new HashMap<>();
	
	public static class SubCustomer {
		public SubCustomerType type;
		public SubCustomerData res;
	}
	public static SubCustomer getFullObject(SubCustomerData res) {
		SubCustomer result = new SubCustomer();
		result.res = res;
		result.type = subCustomerTypes.get(res.subCustomerType().getValue());
		if(result.type == null)
			result.type = subCustomerTypes.get(10);
		if(result.type == null)
			return null;
		return result;
	}
	
	/**
	 * 
	 * @param type
	 * @return true if an existing entry with a different name was overwritten
	 */
	public static boolean addCustomerType(SubCustomerType type) {
		SubCustomerType prev = subCustomerTypes.put(type.id, type);
		return (prev != null)&&(!prev.label(null).equals(type.label(null)));
	}

	private static AccessConfigUser initUserGroup(SubCustomerData subc, AccessAdminConfig accessAdminConfigRes,
			boolean linkToRoomGroup) {
		//Note that the user group is intially empty
		AccessConfigUser userAttr = ResourceListHelper.createNewNamedElement(
				accessAdminConfigRes.userPermissions(),
				"xxx", false);
		ValueResourceHelper.setCreate(userAttr.isGroup(), 1);
		userAttr.name().setAsReference(subc.name());
		userAttr.activate(true);
		subc.userAttribute().setAsReference(userAttr);
		
		if(linkToRoomGroup) {
			BuildingPropertyUnit subcustGroup = ResourceListHelper.getNamedElementFlex(subc.name().getValue(),
					accessAdminConfigRes.roomGroups());
			if(subcustGroup == null) {
				//Init also room group
				BuildingPropertyUnit roomGroup = ResourceListHelper.createNewNamedElement(
						accessAdminConfigRes.roomGroups(),
						"xxy", false);
				roomGroup.activate(true);
				roomGroup.name().setAsReference(subc.name());
				roomGroup.getSubResource(DECORATOR_NAME, SubCustomerData.class).setAsReference(subc);
				subc.roomGroup().setAsReference(roomGroup);				
			}
			AccessConfigBase configRes = userAttr.roompermissionData();
			UserPermissionUtil.addPermission(subcustGroup.getLocation(), UserPermissionService.USER_ROOM_PERM, configRes);			
		}
		return userAttr;
	}
	
	public static SubCustomerData addSubcustomer(int type, String name, List<Room> rooms,
			ApplicationManager appMan) {
		SubCustomerType typeData = subCustomerTypes.get(type);
		if(typeData == null)
			throw new IllegalStateException("Type data for "+type+" not registered before trying to use it!");
		
		AccessAdminConfig accessAdminConfigRes = appMan.getResourceAccess().getResource("accessAdminConfig");
		SubCustomerData result = ResourceListHelper.createNewNamedElement(
				accessAdminConfigRes.subCustomers(), name, false);
		ValueResourceHelper.setCreate(result.subCustomerType(), type);
		ValueResourceHelper.setCreate(result.ecoModeActive(), false);
		ValueResourceHelper.setCreate(result.workingDays(), typeData.defaultWorkingDays);
		//ValueResourceHelper.setCreate(result.defaultEcoTemperatureHeating(), typeData.defaultEcoTemperatureHeating);
		//ValueResourceHelper.setCreate(result.defaultEcoTemperatureCooling(), typeData.defaultEcoTemperatureCooling);
		
		//Note that the user group is intially empty
		AccessConfigUser userAttr = initUserGroup(result, accessAdminConfigRes, false);
		/*AccessConfigUser userAttr = ResourceListHelper.createNewNamedElement(
				accessAdminConfigRes.userPermissions(),
				"xxx", false);
		ValueResourceHelper.setCreate(userAttr.isGroup(), 1);
		userAttr.name().setAsReference(result.name());
		userAttr.activate(true);
		result.userAttribute().setAsReference(userAttr);*/
		
		BuildingPropertyUnit roomGroup = ResourceListHelper.createNewNamedElement(
				accessAdminConfigRes.roomGroups(),
				"xxy", false);
		roomGroup.activate(true);
		roomGroup.name().setAsReference(result.name());
		roomGroup.getSubResource(DECORATOR_NAME, SubCustomerData.class).setAsReference(result);
		result.roomGroup().setAsReference(roomGroup);
		AccessConfigBase configRes = userAttr.roompermissionData();
		UserPermissionUtil.addPermission(roomGroup.getLocation(), UserPermissionService.USER_ROOM_PERM, configRes);
		
		for(Room room: rooms) {
			if(!room.exists())
				continue;
			addRoomToGroup(room, roomGroup);
		}
		
		result.activate(true);
		return result;
	}
	public static SubCustomerData getSubcustomer(String name, ApplicationManager appMan) {
		AccessAdminConfig accessAdminConfigRes = appMan.getResourceAccess().getResource("accessAdminConfig");
		return ResourceListHelper.getNamedElementFlex(
				name, accessAdminConfigRes.subCustomers());
	}
	public static List<SubCustomerData> getSubcustomers(ApplicationManager appMan) {
		AccessAdminConfig accessAdminConfigRes = appMan.getResourceAccess().getResource("accessAdminConfig");
		return accessAdminConfigRes != null ? accessAdminConfigRes.subCustomers().getAllElements() : Collections.emptyList();
	}
	
	public static SubCustomerSuperiorData getSubcustomerDatabase(String name, ApplicationManager appMan, boolean createIfMissing) {
		GatewaySuperiorData gwSubRes = appMan.getResourceAccess().getResource("gatewaySuperiorDataRes");
		if(gwSubRes == null)
			return null;
		if(!gwSubRes.isActive()) {
			gwSubRes.activate(false);
		}
		if(createIfMissing)
			return ResourceListHelper.getOrCreateNamedElementFlex(
					name, gwSubRes.tenantData());
		return ResourceListHelper.getNamedElementFlex(
				name, gwSubRes.tenantData());
	}
	public static List<SubCustomerSuperiorData> getSubcustomersDatabase(ApplicationManager appMan) {
		GatewaySuperiorData gwSubRes = appMan.getResourceAccess().getResource("gatewaySuperiorDataRes");
		if(gwSubRes == null) {
			gwSubRes = appMan.getResourceManagement().createResource("gatewaySuperiorDataRes", GatewaySuperiorData.class);
		}
		if(!gwSubRes.isActive()) {
			gwSubRes.activate(false);
		}
		return gwSubRes.tenantData().getAllElements();
	}
	
	private static Map<String, Long> lastRoomPermissionUpdate = new HashMap<>();
	public static AccessConfigUser addUserToSubcustomer(String userName, SubCustomerData data,
			ApplicationManagerPlus appMan) {
		AccessAdminConfig accessAdminConfigRes = appMan.getResourceAccess().getResource("accessAdminConfig");
		AccessConfigUser subcustGroup = ResourceListHelper.getNamedElementFlex(data.name().getValue(),
				accessAdminConfigRes.userPermissions());
		if(subcustGroup == null) {
			throw new IllegalStateException("User Group for Subcustomer "+data.getLocation() + " missing!");
		}
		
		AccessConfigUser userEntry = ResourceListHelper.getNamedElementFlex(userName,
				accessAdminConfigRes.userPermissions());
		if(userEntry == null)
			return null;

		long now = appMan.getFrameworkTime();
		Long lastUpdate = lastRoomPermissionUpdate.get(userName);
		if(lastUpdate == null)
			lastUpdate = 0l;
		if((ResourceListHelper.addReferenceUnique(userEntry.superGroups(), subcustGroup) == null)
				&& (now - lastUpdate < MAX_UPDATE_INTERVAL))
			return userEntry; //already in subcustomer

		//if(data.aggregationType().getValue() > 0)
		ValueResourceHelper.setCreate(accessAdminConfigRes.subcustomerUserMode(), 1);

		lastRoomPermissionUpdate.put(userName, now);
		/*boolean alreadyIn = false;
		for(AccessConfigUser grp: userEntry.superGroups().getAllElements()) {
			if(grp.equalsLocation(subcustGroup)) {
				alreadyIn = true;
				break;
			}
		}
		if(!alreadyIn)
			userEntry.superGroups().add(subcustGroup);*/
		setUserRoomPermissions(userName, userEntry, appMan);
		return userEntry;
	}
	
	public static void setUserRoomPermissions(String userName, AccessConfigUser userEntry,
			ApplicationManagerPlus appMan) {
		//Set rooms not belonging to subcustomer to denied for user
		List<Room> all = KPIResourceAccess.getRealRooms(appMan.getResourceAccess());
		/*BuildingPropertyUnit subcustGroupRooms = ResourceListHelper.getNamedElementFlex(data.name().getValue(),
				accessAdminConfigRes.roomGroups());
		if(subcustGroupRooms == null)
			throw new IllegalStateException("Room Group for Subcustomer "+data.getLocation() + " missing!");
		//List<Room> subcustRooms = subcustGroupRooms.rooms().getAllElements();*/
		Collection<Room> subcustRooms = getUserRoomsBySubcustomers(userName, appMan.appMan());
		for(Room room: all) {
			boolean hasAccess;
			if(ResourceHelper.containsLocation(subcustRooms, room))
				hasAccess = true;
			else
				hasAccess = false;
			PermissionCellData acc = getAccessConfig(room, UserPermissionService.USER_ROOM_PERM, userName, appMan);
			acc.setOwnStatus(hasAccess?null:false);
		}
	}
	
	public static ConfigurablePermission getAccessConfig(Room object, String permissionID,
			String userName, ApplicationManagerPlus appManPlus) {
		AccessAdminConfig accessAdminConfigRes = appManPlus.getResourceAccess().getResource("accessAdminConfig");
		ResourceList<AccessConfigUser> userPerms = accessAdminConfigRes.userPermissions();
		return getAccessConfig(object, permissionID, userName, appManPlus, userPerms);
	}
	public static ConfigurablePermission getAccessConfig(Room object, String permissionID,
			String userName, ApplicationManagerPlus appManPlus,
			ResourceList<AccessConfigUser> userPerms) {
		AccessConfigUser userAcc = UserPermissionUtil.getUserPermissions(
				userPerms, userName);
		ConfigurablePermission result = new ConfigurablePermission() {
			@Override
			public boolean supportsUnset() {
				return true;
			}
		};
		//We have to choose the right permission data for the page here
		if(userAcc == null)
			userAcc = UserPermissionUtil.getOrCreateUserPermissions(userPerms, userName);
		result.accessConfig = userAcc.roompermissionData();
		result.resourceId = object.getLocation();
		result.permissionId = permissionID;
		//String userName = userAcc.name().getValue();
		result.defaultStatus = appManPlus.userPermService().getUserPermissionForRoom(userName, result.resourceId,
				permissionID, true) > 0;
		return result;
	}
	
	public static AccessConfigUser removeUserFromSubcustomer(String userName, SubCustomerData data,
			ApplicationManagerPlus appMan) {
		AccessAdminConfig accessAdminConfigRes = appMan.getResourceAccess().getResource("accessAdminConfig");
		AccessConfigUser subcustGroup = ResourceListHelper.getNamedElementFlex(data.name().getValue(),
				accessAdminConfigRes.userPermissions());
		if(subcustGroup == null)
			throw new IllegalStateException("User Group for Subcustomer "+data.getLocation() + " missing!");
		
		AccessConfigUser userEntry = ResourceListHelper.getNamedElementFlex(userName,
				accessAdminConfigRes.userPermissions());
		if(userEntry == null)
			return null;
		
		ResourceListHelper.removeReferenceOrObject(userEntry.superGroups(), subcustGroup);	

		setUserRoomPermissions(userName, userEntry, appMan);

		return userEntry;
	}
	
	/** Get primary subcustomer for user*/
	public static SubCustomerData getDataForUser(String userName, ApplicationManager appMan,
			boolean acceptAggregated, boolean preferAggregated) {
		SubCustomerData selected = null;
		SubCustomerData agg = null;
		List<SubCustomerData> subcs = getSubcustomers(appMan);

		AccessAdminConfig accessAdminConfigRes = appMan.getResourceAccess().getResource("accessAdminConfig");
		int mode = accessAdminConfigRes.subcustomerUserMode().getValue();
		if(mode == 0)
			return getEntireBuildingSubcustomer(appMan);
		
		AccessConfigUser userEntry = ResourceListHelper.getNamedElementFlex(userName,
				accessAdminConfigRes.userPermissions());
		if(userEntry == null)
			return null;
		if(userEntry.superGroups().size() == 0 && acceptAggregated)
			return getEntireBuildingSubcustomer(appMan);
		
		for(SubCustomerData subc: subcs) {
			AccessConfigUser subcustGroup = ResourceListHelper.getNamedElementFlex(subc.name().getValue(),
					accessAdminConfigRes.userPermissions());
			if(subcustGroup == null)
				throw new IllegalStateException("User Group for Subcustomer "+subc.getLocation() + " missing!");

			if(ResourceHelper.containsLocation(userEntry.superGroups().getAllElements(), subcustGroup)) {
				if(subc.aggregationType().getValue() > 0) {
					agg = subc;
				} else {
					selected = subc;
					if(!preferAggregated)
						break;
				}
			}
		}
		if(preferAggregated && (agg != null)) {
			if(acceptAggregated)
				return agg;
			else
				return null;
		}
		if(selected == null && (!acceptAggregated))
			return null;
		if(selected == null && agg != null)
			selected = agg;
		return selected;
	}

	/** Get all (sub-)primary subcustomers for user*/
	public static Collection<SubCustomerData> getDataForUserMulti(String userName, ApplicationManager appMan,
			boolean acceptAggregated, boolean preferAggregated) {
		SubCustomerData selected = null;
		SubCustomerData agg = null;
		List<SubCustomerData> subcs = getSubcustomers(appMan);

		AccessAdminConfig accessAdminConfigRes = appMan.getResourceAccess().getResource("accessAdminConfig");
		int mode = accessAdminConfigRes.subcustomerUserMode().getValue();
		if(mode == 0)
			return Arrays.asList(new SubCustomerData[] {getEntireBuildingSubcustomer(appMan)});
		
		AccessConfigUser userEntry = ResourceListHelper.getNamedElementFlex(userName,
				accessAdminConfigRes.userPermissions());
		if(userEntry == null)
			return null;
		if(userEntry.superGroups().size() == 0 && acceptAggregated)
			return Arrays.asList(new SubCustomerData[] {getEntireBuildingSubcustomer(appMan)});
		
		Set<SubCustomerData> result = new HashSet<>();
		for(SubCustomerData subc: subcs) {
			AccessConfigUser subcustGroup = ResourceListHelper.getNamedElementFlex(subc.name().getValue(),
					accessAdminConfigRes.userPermissions());
			if(subcustGroup == null)
				throw new IllegalStateException("User Group for Subcustomer "+subc.getLocation() + " missing!");

			if(ResourceHelper.containsLocation(userEntry.superGroups().getAllElements(), subcustGroup)) {
				if(subc.aggregationType().getValue() > 0) {
					agg = subc;
				} else {
					result.add(subc);
					selected = subc;
					//if(!preferAggregated)
					//	break;
				}
			}
		}
		if(preferAggregated && (agg != null)) {
			if(acceptAggregated)
				result.add(agg);
			//else
			//	return null;
		}
		if(selected == null && (!acceptAggregated))
			return result;
		if(selected == null && agg != null)
			selected = agg;
		if(selected != null)
			result.add(selected);
		return result;
	}

	public static Collection<Room> getUserRoomsBySubcustomers(String userName, ApplicationManager appMan) {
		
		SubCustomerData activeSubcust = SubcustomerUtil.getDataForUser(userName, appMan, true, true);
		if((activeSubcust != null) && (activeSubcust.aggregationType().getValue() > 0)) {
			return KPIResourceAccess.getRealRooms(appMan.getResourceAccess());
		}
		List<SubCustomerData> subcdlist = getAllDataForUser(userName, appMan);
		Set<Room> result = new HashSet<>();
		for(SubCustomerData subc: subcdlist) {
			List<Room> rooms = subc.roomGroup().rooms().getAllElements();
			for(Room room: rooms) {
				result.add(room.getLocationResource());
			}
		}
		return result ;
	}
	
	public static List<SubCustomerData> getAllDataForUserSafe(String userName, ApplicationManager appMan) {
		return getAllDataForUser(userName, appMan, false);
	}
	public static List<SubCustomerData> getAllDataForUser(String userName, ApplicationManager appMan) {
		return getAllDataForUser(userName, appMan, true);
	}
	/** This method always returns the sub customers configured for a user independently of
	 * {@link AccessAdminConfig#subcustomerUserMode()}
	 * @param userName
	 * @param appMan
	 * @return null of user has no permission entry (yet)
	 */
	public static List<SubCustomerData> getAllDataForUser(String userName, ApplicationManager appMan,
			boolean returnNullIfNoUserEntry) {
		List<SubCustomerData> result = new ArrayList<>();
		
		List<SubCustomerData> subcs = getSubcustomers(appMan);
		AccessAdminConfig accessAdminConfigRes = appMan.getResourceAccess().getResource("accessAdminConfig");
		
		AccessConfigUser userEntry = ResourceListHelper.getNamedElementFlex(userName,
				accessAdminConfigRes.userPermissions());
		if(userEntry == null) {
			if(returnNullIfNoUserEntry)
				return null;
			else
				return Collections.emptyList();
		}
		
		for(SubCustomerData subc: subcs) {
			AccessConfigUser subcustGroup = ResourceListHelper.getNamedElementFlex(subc.name().getValue(),
					accessAdminConfigRes.userPermissions());
			if(subcustGroup == null) {
				appMan.getLogger().warn("User Group for Subcustomer "+subc.getLocation() + " missing!");
				subcustGroup = initUserGroup(subc, accessAdminConfigRes, true);
				//throw new IllegalStateException("User Group for Subcustomer "+subc.getLocation() + " missing!");
			}
			
			if(ResourceHelper.containsLocation(userEntry.superGroups().getAllElements(), subcustGroup)) {
				result.add(subc);
			}
		}
		return result ;
	}

	public static List<AccessConfigUser> getAllUsersForSubcustomer(SubCustomerData subc, ApplicationManager appMan) {
		AccessAdminConfig accessAdminConfigRes = appMan.getResourceAccess().getResource("accessAdminConfig");
		AccessConfigUser subcustGroup = ResourceListHelper.getNamedElementFlex(subc.name().getValue(),
				accessAdminConfigRes.userPermissions());
		if(subcustGroup == null) {
			appMan.getLogger().warn("User Group for Subcustomer "+subc.getLocation() + " missing!");
			subcustGroup = initUserGroup(subc, accessAdminConfigRes, true);
			//throw new IllegalStateException("User Group for Subcustomer "+subc.getLocation() + " missing!");
		}
		List<AccessConfigUser> result = new ArrayList<>();
		for(AccessConfigUser userEntry: accessAdminConfigRes.userPermissions().getAllElements()) {
			if(ResourceHelper.containsLocation(userEntry.superGroups().getAllElements(), subcustGroup)) {
				result.add(userEntry);
			}			
		}
		return result;
	}
	
	public static BuildingPropertyUnit addRoomToSubcustomer(Room room, SubCustomerData data,
			ApplicationManager appMan) {
		AccessAdminConfig accessAdminConfigRes = appMan.getResourceAccess().getResource("accessAdminConfig");
		BuildingPropertyUnit subcustGroup = ResourceListHelper.getNamedElementFlex(data.name().getValue(),
				accessAdminConfigRes.roomGroups());
		if(subcustGroup == null)
			throw new IllegalStateException("Room Group for Subcustomer "+data.getLocation() + " missing!");
		
		addRoomToGroup(room, subcustGroup);
		
		return subcustGroup;
	}
	
	public static BuildingPropertyUnit removeRoomFromSubcustomer(Room room, SubCustomerData data,
			ApplicationManager appMan) {
		AccessAdminConfig accessAdminConfigRes = appMan.getResourceAccess().getResource("accessAdminConfig");
		BuildingPropertyUnit subcustGroup = ResourceListHelper.getNamedElementFlex(data.name().getValue(),
				accessAdminConfigRes.roomGroups());
		if(subcustGroup == null)
			throw new IllegalStateException("Room Group for Subcustomer "+data.getLocation() + " missing!");
		
		ResourceListHelper.removeReferenceOrObject(subcustGroup.rooms(), room);
		
		return subcustGroup;
	}

	public static List<Room> getAllRoomsForSubcustomer(SubCustomerData data,
			ApplicationManager appMan) {
		AccessAdminConfig accessAdminConfigRes = appMan.getResourceAccess().getResource("accessAdminConfig");
		BuildingPropertyUnit subcustGroup = ResourceListHelper.getNamedElementFlex(data.name().getValue(),
				accessAdminConfigRes.roomGroups());
		if(subcustGroup == null)
			throw new IllegalStateException("Room Group for Subcustomer "+data.getLocation() + " missing!");
		
		return subcustGroup.rooms().getAllElements();
	}
	
	/** Data for a single room (pro forma base, real data in roomcontrol)*/
	public static class SingleRoomSettingEvalBase {
		/** Non-usage temperature, usage temperature; 
		 * Note: We do not support special times for non-working days anymore. These are always overwritten. */
		public float[] workingSpecialTemperature;
		
		/** Start time of usage and end time of usage, maybe just one element -1 if more complex <br>
		 * Note: We do not support special times for non-working days anymore. These are always overwritten. */
		public long[] workingSpecialTime;
		
		/** If this is true the nonworking setting may not be overwritten at all in the master mode*/
		public boolean isNonWorkingSpecial;
	}

	/** Evaluation data for all rooms of a subcustomer regarding heating or cooling*/
	public static class MultiRoomSettingEval {
		public final NamedIntegerType roomType;
		public final SubCustomerData subCustomerData;

		public MultiRoomSettingEval(NamedIntegerType roomType, SubCustomerData scd) {
			this.roomType = roomType;
			this.subCustomerData = scd;
		}
		/** Single room data*/
		public List<SingleRoomSettingEvalBase> roomData = new ArrayList<>();
		public boolean isHeating;
		/** Standard settings for the subcustomer*/
		public HeatCoolData standardSettings;
		
	}

	/** Master and standard setting data for a room type within a subcustomer*/
	public static class RoomTypeGroupData {
		// Master data
		public final NamedIntegerType roomType;
		//public final String id;
		//public final int type;
		//public final String name;
		public RoomTypeGroupData(NamedIntegerType roomType) {
			this.roomType = roomType;
		}
		public List<String> rooms = new ArrayList<>();
		public int roomNumHeating = 0;
		public int roomNumCooling = 0;
		
		// Standard setting data. This is also containted in MultiRoomSettingEval
		public HeatCoolData heatingData;
		public HeatCoolData coolingData;

		/** Heating evaluation including data per room */
		public MultiRoomSettingEval multiHeat;
		/** Cooling evaluation including data per room */
		public MultiRoomSettingEval multiCool;
	}
	public static final int STARTTIME_IDX = 0;
	public static final int ENDTIME_IDX = 1;
	public static final int USAGETEMP_IDX = 2;
	public static final int NON_USAGETEMP_IDX = 3;
	public static final int COMFORTTEMP_IDX = 4;
	public static final int ECOTEMP_IDX = 5;
	public static final int WINDOWTEMP_IDX = 6;
	public static final int MIN_SETPOINT_IDX = 7;
	public static final int MAX_SETPOINT_IDX = 8;
	public static final int ECO_EQUAlS_OFF_IDX = 9;
	public static final int ECOTEMP_V2_IDX = 10;
	public static final int WINDOWTEMP_V2_IDX = 11;
	public static final int COMFORT_TEMP_RELEVANT_IDX = 12;
	public static final int ECO_TEMP_RELEVANT_IDX = 13;
	
	public static final int VALUE_IDX_NUM = 14;

	public static class ValueVariance {
		public ValueVariance(int valueIdx) {
			this.valueIdx = valueIdx;
		}
		/** IDX of the choices above*/
		public int valueIdx;
		/** Values found among the input values*/
		public Map<Float, Integer> valuesFound = new HashMap<>();
	
		/** Most common value found*/
		public float standardValue;
		/** Number of occurences of special settings*/
		public int specialSettings;
	}
	public static class SettingsBaseData {
		/** get general heating start time in */
		public long startTimeFromRoomData;
		public long endTimeFromRoomData;
		
		/** Alternative evaluation of full startEndTimes of curve in
		 * if RoomcontrolSetUtil.getSettingsBase is used with determineFullStartEndTimes=true
		 */
		public List<Long> startEndTimesFromRoomData;
		
		public Float usageTemperature;
		public Float nonUsageTemperature;
		
		public boolean isSimpleCurve = true;
	}

	public static long getStartTimeStatic(Map<Integer, List<Long>> startEndTimes) {
		return getStartTimeStatic(startEndTimes, HeatCoolData.STARTEND_WORKINGDAY_IDX);
	}
	public static long getStartTimeStatic(Map<Integer, List<Long>> startEndTimes, int dayIdx) {
		List<Long> workingdayTime = startEndTimes.get(dayIdx);
		if(workingdayTime == null || workingdayTime.isEmpty())
			return 1500*TimeProcUtil.MINUTE_MILLIS;
		return workingdayTime.get(0);
	}
	public static String getStartOrEndTimeStaticUD(Map<Integer, List<Long>> startEndTimes, boolean startTime) {
		Map<Long, String> startSum = new HashMap<>();
		for(int idx=0; idx<7; idx++) {
			long startMin = startTime?getStartTimeStatic(startEndTimes, idx):getEndTimeStatic(startEndTimes, idx);
			String days = startSum.get(startMin);
			if(days == null)
				days = ""+idx;
			else
				days = days+","+idx;
			startSum.put(startMin, days);
		}
		if(startSum.isEmpty())
			return "time empty"; // should never occur
		else if(startSum.size() == 1)
			return ""+(startSum.keySet().iterator().next()/TimeProcUtil.MINUTE_MILLIS);
		String result = null;
		for(Entry<Long, String> el: startSum.entrySet()) {
			String elStr = ""+(el.getKey()/TimeProcUtil.MINUTE_MILLIS)+"("+el.getValue()+")";
			if(result == null)
				result = elStr;
			else
				result += ", "+elStr;
		}
		return result;
	}

	public static long getEndTimeStatic(Map<Integer, List<Long>> startEndTimes) {
		return getEndTimeStatic(startEndTimes, HeatCoolData.STARTEND_WORKINGDAY_IDX);
	}
	public static long getEndTimeStatic(Map<Integer, List<Long>> startEndTimes, int dayIdx) {
		List<Long> workingdayTime = startEndTimes.get(dayIdx);
		if(workingdayTime == null || (workingdayTime.size() < 2))
			return 1510*TimeProcUtil.MINUTE_MILLIS;
		return workingdayTime.get(1);
	}
	
	public static void addRoomToGroup(Room object, BuildingPropertyUnit bu) {
		ResourceListHelper.addReferenceUnique(bu.rooms(), object);
	}
	
	public static List<AccessConfigUser> getUserGroups(boolean includeNaturalUsers, boolean includetype2Groups,
			AccessAdminConfig appConfigData) {
		List<AccessConfigUser> result = new ArrayList<>();
		for(AccessConfigUser user: appConfigData.userPermissions().getAllElements()) {
			if(!includetype2Groups && (user.isGroup().getValue() == 2))
				continue;
			if(includeNaturalUsers || (user.isGroup().getValue() > 0))
				result.add(user);
		}
		return result ;
	}
	
	public static AccessConfigUser getUserGroup(String name, AccessAdminConfig appConfigData) {
		for(AccessConfigUser user: appConfigData.userPermissions().getAllElements()) {
			if((user.isGroup().getValue() > 0))
				continue;
			if(user.name().getValue().equals(name))
				return user;
		}
		return null;
	}
	
	public static boolean isSubcustomerRoomgroup(BuildingPropertyUnit bu) {
		return bu.getSubResource(DECORATOR_NAME) != null;
	}
	public static List<BuildingPropertyUnit> getSubcustomerRoomgroups(ApplicationManager appMan, boolean invert) {
		AccessAdminConfig accessAdminConfigRes = appMan.getResourceAccess().getResource("accessAdminConfig");
		List<BuildingPropertyUnit> result = new ArrayList<>();
		for(BuildingPropertyUnit bu: accessAdminConfigRes.roomGroups().getAllElements()) {
			if(invert != (isSubcustomerRoomgroup(bu)))
				result.add(bu);
		}
		return result;
	}
	
	public static SubCustomerData getDataForRoom(Room room, ApplicationManager appMan) {
		return getDataForRoom(room, appMan, true);
	}
	public static SubCustomerData getDataForRoom(Room room, ApplicationManager appMan,
			boolean acceptAggregated) {
		SubCustomerData selected = null;
		SubCustomerData agg = null;
		SubCustomerData aggGen = null;
		List<SubCustomerData> subcs = getSubcustomers(appMan);
		for(SubCustomerData subc: subcs) {
			if(ResourceHelper.containsLocation(subc.roomGroup().rooms().getAllElements(), room)) {
				if(subc.aggregationType().getValue() > 0)
					agg = subc;
				else {
					selected = subc;
					break;
				}
			} else if(subc.aggregationType().getValue() > 0)
				aggGen = subc;
		}
		if(selected == null && (!acceptAggregated))
			return null;
		if(selected == null && agg != null)
			selected = agg;
		if(selected == null && aggGen != null)
			selected = aggGen;
		return selected;
	}

	/** TODO: There may be more than one aggregation subcustomer in the future, then this method needs to be adapted*/
	public static SubCustomerData getEntireBuildingSubcustomer(ApplicationManager appMan) {
		List<SubCustomerData> all = getSubcustomers(appMan);
		for(SubCustomerData sub: all) {
			if(sub.aggregationType().getValue() > 0)
				return sub;
		}
		return null;
	}
	public static SubCustomerSuperiorData getEntireBuildingSubcustomerDatabase(GatewaySuperiorData gwData) {
		List<SubCustomerSuperiorData> all = gwData.tenantData().getAllElements();
		for(SubCustomerSuperiorData sub: all) {
			if(sub.aggregationType().getValue() > 0)
				return sub;
		}
		return null;
	}
	
	public static SubCustomerSuperiorData getEntireBuildingSubcustomerDatabase(ApplicationManager appMan) {
		if(Boolean.getBoolean("org.smartplace.app.srcmon.server.issuperior")) {
			List<SubCustomerSuperiorData> all = getSubcustomersDatabase(appMan);
			for(SubCustomerSuperiorData sub: all) {
				if(sub.aggregationType().getValue() > 0)
					return sub;
			}
			return null;
		}
		SubCustomerData subc = getEntireBuildingSubcustomer(appMan);
		return subc != null ? getDatabaseData(subc, appMan) : null;
	}
	
	/** Initialize room regarding standard room groups
	 * 
	 * @param room
	 * @param resAcc
	 */
	public static void initRoom(Room room, ApplicationManager appMan) {
		AccessAdminConfig appConfigData = appMan.getResourceAccess().getResource("accessAdminConfig");
		ResourceList<BuildingPropertyUnit> roomGroups = appConfigData.roomGroups();
		initRoom(room, roomGroups, appConfigData, appMan);
	}
	public static void initRoom(Room object, ResourceList<BuildingPropertyUnit> roomGroups, AccessAdminConfig appConfigData,
			ApplicationManager appMan) {
		BuildingPropertyUnit allRoomsGroup = null;
		for(BuildingPropertyUnit g: roomGroups.getAllElements()) {
			if(ResourceUtils.getHumanReadableShortName(g).equals(ALL_ROOMS_GROUP_NAME)) {
				allRoomsGroup = g;
				break;
			}
		}
		if(allRoomsGroup == null) {
			//create
			allRoomsGroup = ResourceListHelper.createNewNamedElement(
					roomGroups,
					ALL_ROOMS_GROUP_NAME, false);
			allRoomsGroup.activate(true);
			for(AccessConfigUser userPerm: appConfigData.userPermissions().getAllElements()) {
				if(userPerm.isGroup().getValue() != 2)
					continue;
				switch(userPerm.name().getValue()) {
				case "User Standard":
				case "Secretary":
				case "Facility Manager":
				case "Manager":
				case "Master Administrator":
				case "Organisation Administrator":
					AccessConfigBase configRes = userPerm.roompermissionData();
					UserPermissionUtil.addPermission(allRoomsGroup.getLocation(), UserPermissionService.USER_ROOM_PERM, configRes);
				}
			}
		}
		ResourceListHelper.addReferenceUnique(allRoomsGroup.rooms(), object);
		
		SubCustomerData entireBuilding = getEntireBuildingSubcustomer(appMan);
		if(entireBuilding == null) {
			entireBuilding = SubcustomerUtil.getSubcustomer("Gesamtgebäude(C)", appMan);
			if(entireBuilding != null)
				ValueResourceHelper.setCreate(entireBuilding.aggregationType(), 1);
		}
		if(entireBuilding != null) {
			SubcustomerUtil.addRoomToSubcustomer(object, entireBuilding, appMan);
		}
		
		//setGroups(object, Arrays.asList(new BuildingPropertyUnit[] {allRoomsGroup}), roomGroups.getAllElements());
	}
	
	@SuppressWarnings("deprecation")
	public static SubCustomerSuperiorData getDatabaseData(SubCustomerData subc, ApplicationManager appMan) {
		if(subc.databaseData().isActive())
			return subc.databaseData().getLocationResource();
		SubCustomerSuperiorData sdb = getSubcustomerDatabase(ResourceUtils.getHumanReadableShortName(subc), appMan, true);
		if(sdb != null) {
			ValueResourceHelper.setCreate(sdb.aggregationType(), subc.aggregationType().getValue());
			ValueResourceHelper.setCreate(sdb.additionalAdminEmailAddresses(), subc.additionalAdminEmailAddresses().getValue());
			ValueResourceHelper.setCreate(sdb.personalSalutations(), subc.personalSalutations().getValue());
			//if(!subc.useOnlyAdditionalAddresses().isActive()))
			//	ValueResourceHelper.setCreate(sdb.useOnlyAdditionalAddresses(), true);
			//else
			//	ValueResourceHelper.setCreate(sdb.useOnlyAdditionalAddresses(), subc.useOnlyAdditionalAddresses().getValue());
			ValueResourceHelper.setCreate(sdb.emailAddressesIT(), subc.emailAddressesIT().getValue());
			ValueResourceHelper.setCreate(sdb.personalSalutationsIT(), subc.personalSalutationsIT().getValue());
			subc.databaseData().setAsReference(sdb);

			LocalGatewayInformation localgw = ResourceHelper.getLocalGwInfo(appMan.getResourceAccess());
			if(localgw != null) {
				ValueResourceHelper.setCreate(sdb.gatewayBaseUrl(), localgw.gatewayBaseUrl().getValue());
				ValueResourceHelper.setCreate(sdb.gatewayOperationDatabaseUrl(), localgw.gatewayOperationDatabaseUrl().getValue());
				ValueResourceHelper.setCreate(sdb.gatewayLinkOverviewUrl(), localgw.gatewayLinkOverviewUrl().getValue());
				ValueResourceHelper.setCreate(sdb.systemLocale(), localgw.systemLocale().getValue());
			}
		}
		return sdb;
	}
	
	public static StringResource getGatewayOperationDatabaseUrl(ApplicationManager appMan) {
		final SubCustomerSuperiorData subc;
		if(!isSuperior()) {
			subc = SubcustomerUtil.getEntireBuildingSubcustomerDatabase(appMan);
		} else
			subc = null;
		if(subc != null)
			return getGatewayOperationDatabaseUrl(subc, null);
		LocalGatewayInformation gwInfo = ResourceHelper.getLocalGwInfo(appMan);
		return getGatewayOperationDatabaseUrl(null, gwInfo);
	}
	@SuppressWarnings("deprecation")
	public static StringResource getGatewayOperationDatabaseUrl(SubCustomerSuperiorData subc, LocalGatewayInformation gwInfo) {
		return subc != null? subc.gatewayOperationDatabaseUrl():gwInfo.gatewayOperationDatabaseUrl();
	}
	
	public static StringResource getGatewayLinkOverviewUrl(ApplicationManager appMan) {
		final SubCustomerSuperiorData subc;
		if(!isSuperior()) {
			subc = SubcustomerUtil.getEntireBuildingSubcustomerDatabase(appMan);
		} else
			subc = null;
		if(subc != null)
			return getGatewayLinkOverviewUrl(subc, null);
		LocalGatewayInformation gwInfo = ResourceHelper.getLocalGwInfo(appMan);
		return getGatewayLinkOverviewUrl(null, gwInfo);
	}
	@SuppressWarnings("deprecation")
	public static StringResource getGatewayLinkOverviewUrl(SubCustomerSuperiorData subc, LocalGatewayInformation gwInfo) {
		return subc != null? subc.gatewayLinkOverviewUrl():gwInfo.gatewayLinkOverviewUrl();
	}

	public static boolean showIdlemodeSwitch(SubCustomerData subc, ApplicationManager appMan) {
		if(subc == null)
			return Boolean.getBoolean("org.smartrplace.apps.heatcontrolservlet.servlet.idlemodeswitch.show");
		SubCustomerSuperiorData subcDb = SubcustomerUtil.getDatabaseData(subc, appMan);
		switch(subcDb.frontendMode().getValue()) {
			case 1:
			case 4:
				return true;
			case -1:
			case 2:
			case 3:
				return false;
		}
		
		return Boolean.getBoolean("org.smartrplace.apps.heatcontrolservlet.servlet.idlemodeswitch.show");
	}
	
	public static boolean showReducedHeatPlan(SubCustomerData subc, ApplicationManager appMan) {
		if(subc == null)
			return Boolean.getBoolean("org.smartrplace.apps.heatcontrolservlet.servlet.setUsageData");
		SubCustomerSuperiorData subcDb = SubcustomerUtil.getDatabaseData(subc, appMan);
		switch(subcDb.frontendMode().getValue()) {
			case 2:
			case 4:
				return true;
			case 3:
				return false;
		}
		
		return Boolean.getBoolean("org.smartrplace.apps.heatcontrolservlet.servlet.setUsageData");
	}

	public static boolean isAutomatedManagementActive(SubCustomerData subcustUser,
			ApplicationManager appMan) {
		boolean isActive = false;
		List<Room> rooms = getAllRoomsForSubcustomer(subcustUser, appMan);
		for(Room uroom: rooms) {
			Configuration rts = ChartsUtil.getRoomTemperatureSetting(uroom, appMan.getResourceAccess());
			if(rts != null && (rts.getSubResource("roomIdleMode", IntegerResource.class).getValue() == 0))
				isActive = true;
		}
		return isActive;
	}
}
