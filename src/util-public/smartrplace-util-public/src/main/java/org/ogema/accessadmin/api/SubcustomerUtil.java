package org.ogema.accessadmin.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.model.locations.BuildingPropertyUnit;
import org.ogema.model.locations.Room;
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
		public final Map<Integer, NamedIntegerType> roomTypes = new HashMap<>();
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
	
	public static class RoomTypeGroupData {
		public String id;
		public String name;
		public List<String> rooms = new ArrayList<>();
		public int roomNumHeating = 0;
		public int roomNumCooling = 0;
		public HeatCoolData heatingData;
		public HeatCoolData coolingData;
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
	public static final int VALUE_IDX_NUM = 9;
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
	}
	/** COPIED FROM UserPermissionUtil 
	 * We have to check whether an existing value has to be overwritten or not
	 * @return true if a new entry was added*/
	/*public static boolean addPermission(String resourceId, String permissionType, AccessConfigBase configRes) {
		return addPermission(resourceId, permissionType, configRes, 1);
	}
	public static boolean addPermission(String resourceId, String permissionType, AccessConfigBase configRes, int value) {
		Integer idx = getIndexOfExisting(resourceId, permissionType, configRes);
		if(idx != null) {
			configRes.permissionValues().setElementValue(value, idx);
			return false;
		}
		try {
			Thread.sleep(0);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		boolean isNew = false;
		if(!configRes.isActive()) {
			configRes.create();
			configRes.resourceIds().create();
			configRes.permissionTypes().create();
			configRes.permissionValues().create();
			isNew = true;
		}
		ValueResourceUtils.appendValue(configRes.resourceIds(), resourceId);
		ValueResourceUtils.appendValue(configRes.permissionTypes(), permissionType);
		ValueResourceUtils.appendValue(configRes.permissionValues(), value);
		if(isNew)
			configRes.activate(true);
		return true;
	}
	public static Integer getIndexOfExisting(String resourceId, String permissionType, AccessConfigBase configRes) {
		int idx = 0;
		String[] types = configRes.permissionTypes().getValues();
		for(String id: configRes.resourceIds().getValues()) {
			if(id.contentEquals(resourceId) && types[idx].equals(permissionType)) {
				return idx;
			}
			idx++;
		}
		return null;
	}*/

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
		SubCustomerData selected = null;
		List<SubCustomerData> subcs = getSubcustomers(appMan);
		for(SubCustomerData subc: subcs) {
			if(ResourceHelper.containsLocation(subc.roomGroup().rooms().getAllElements(), room)) {
				selected = subc;
				break;
			}
		}
		if(selected == null && (!subcs.isEmpty())) {
			selected = subcs.get(0);
			SubcustomerUtil.addRoomToGroup(room, subcs.get(0).roomGroup());
		}
		return selected;
	}
}
