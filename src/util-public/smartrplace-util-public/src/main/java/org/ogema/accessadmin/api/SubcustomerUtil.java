package org.ogema.accessadmin.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.ResourceList;
import org.ogema.model.locations.BuildingPropertyUnit;
import org.ogema.model.locations.Room;
import org.ogema.timeseries.eval.simple.api.KPIResourceAccess;
import org.smartrplace.external.accessadmin.config.AccessAdminConfig;
import org.smartrplace.external.accessadmin.config.AccessConfigBase;
import org.smartrplace.external.accessadmin.config.AccessConfigUser;
import org.smartrplace.external.accessadmin.config.SubCustomerData;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.resourcelist.ResourceListHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class SubcustomerUtil {
	public static final String DECORATOR_NAME = "subcustomer";
	
	/** The ids are stored in resources persistently, so on a certain system the meaning of each integer
	 * value shall not be changed, although the exact name texts can be adapted.
	 */
	public static class SubCustomerType extends NamedIntegerType {
		/**Id is written into the {@link Room#type()} field and should this be aligned with the
		 * standard room types if possible
		 */
		public final Map<Integer, NamedIntegerType> roomTypes = new LinkedHashMap<>();
		public int[] defaultWorkingDays = new int[] {1,2,3,4,5};
		public float defaultEcoTemperatureHeating = 273.15f+16f;
		public float defaultEcoTemperatureCooling = 273.15f+30f;
		
		public SubCustomerType(int id, Map<OgemaLocale, String> name) {
			super(id, name);
		}

		public SubCustomerType(int id, String englishName) {
			super(id, englishName);
		}
		
		public SubCustomerType(int id, String englishName, String germanName) {
			super(id, englishName, germanName);
		}
		
		public void addRoomType(NamedIntegerType roomType) {
			roomTypes.put(roomType.id, roomType);
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
		ValueResourceHelper.setCreate(result.defaultEcoTemperatureHeating(), typeData.defaultEcoTemperatureHeating);
		ValueResourceHelper.setCreate(result.defaultEcoTemperatureCooling(), typeData.defaultEcoTemperatureCooling);
		
		//Note that the user group is intially empty
		AccessConfigUser userAttr = ResourceListHelper.createNewNamedElement(
				accessAdminConfigRes.userPermissions(),
				"xxx", false);
		ValueResourceHelper.setCreate(userAttr.isGroup(), 1);
		userAttr.name().setAsReference(result.name());
		userAttr.activate(true);
		result.userAttribute().setAsReference(userAttr);
		
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
		return accessAdminConfigRes.subCustomers().getAllElements();
	}
	
	public static AccessConfigUser addUserToSubcustomer(String userName, SubCustomerData data,
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
		ResourceListHelper.addReferenceUnique(userEntry.superGroups(), subcustGroup);
		/*boolean alreadyIn = false;
		for(AccessConfigUser grp: userEntry.superGroups().getAllElements()) {
			if(grp.equalsLocation(subcustGroup)) {
				alreadyIn = true;
				break;
			}
		}
		if(!alreadyIn)
			userEntry.superGroups().add(subcustGroup);*/
		if(data.aggregationType().getValue() > 0)
			ValueResourceHelper.setCreate(accessAdminConfigRes.subcustomerUserMode(), 1);
		
		//Set rooms not belonging to subcustomer to denied for user
		List<Room> all = KPIResourceAccess.getRealRooms(appMan.getResourceAccess());
		BuildingPropertyUnit subcustGroupRooms = ResourceListHelper.getNamedElementFlex(data.name().getValue(),
				accessAdminConfigRes.roomGroups());
		if(subcustGroupRooms == null)
			throw new IllegalStateException("Room Group for Subcustomer "+data.getLocation() + " missing!");
		List<Room> subcustRooms = subcustGroupRooms.rooms().getAllElements();
		for(Room room: all) {
			boolean hasAccess;
			if(ResourceHelper.containsLocation(subcustRooms, room))
				hasAccess = true;
			else
				hasAccess = false;
			PermissionCellData acc = getAccessConfig(room, UserPermissionService.USER_ROOM_PERM, userName, appMan);
			acc.setOwnStatus(hasAccess?null:false);
		}
				
		return userEntry;
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
			ApplicationManager appMan) {
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
		if(preferAggregated && (agg != null))
			return agg;
		if(selected == null && (!acceptAggregated))
			return null;
		if(selected == null && agg != null)
			selected = agg;
		return selected;
	}

	/** This method always returns the sub customers configured for a user independently of
	 * {@link AccessAdminConfig#subcustomerUserMode()}
	 * @param userName
	 * @param appMan
	 * @return
	 */
	public static List<SubCustomerData> getAllDataForUser(String userName, ApplicationManager appMan) {
		List<SubCustomerData> result = new ArrayList<>();
		
		List<SubCustomerData> subcs = getSubcustomers(appMan);
		AccessAdminConfig accessAdminConfigRes = appMan.getResourceAccess().getResource("accessAdminConfig");
		
		AccessConfigUser userEntry = ResourceListHelper.getNamedElementFlex(userName,
				accessAdminConfigRes.userPermissions());
		if(userEntry == null)
			return null;
		
		for(SubCustomerData subc: subcs) {
			AccessConfigUser subcustGroup = ResourceListHelper.getNamedElementFlex(subc.name().getValue(),
					accessAdminConfigRes.userPermissions());
			if(subcustGroup == null)
				throw new IllegalStateException("User Group for Subcustomer "+subc.getLocation() + " missing!");

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
		if(subcustGroup == null)
			throw new IllegalStateException("User Group for Subcustomer "+subc.getLocation() + " missing!");

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
		public float[] workingSpecialTemperature;
		public long[] workingSpecialTime;
		public boolean isNonWorkingSpecial;
	}

	/** Evaluation data for all rooms of a subcustomer regarding heating or cooling*/
	public static class MultiRoomSettingEval {
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
	public static final int VALUE_IDX_NUM = 10;
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
		public long startTime;
		public long endTime;
		public float usageTemperature;
		public float nonUsageTemperature;
		
		public long getStartTime() {
			return startTime;
		}
		public long getEndTime() {
			return endTime;
		}
		public float getUsageTemperature() {
			return usageTemperature;
		}
		public float getNonUsageTemperature() {
			return nonUsageTemperature;
		}		
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
}
