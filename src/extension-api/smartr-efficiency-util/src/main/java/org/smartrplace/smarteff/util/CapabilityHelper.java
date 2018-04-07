package org.smartrplace.smarteff.util;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.model.Resource;
import org.smartrplace.extensionservice.ExtensionCapabilityPublicData.EntryType;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration.Cardinality;

public class CapabilityHelper {
	public static final String ERROR_START = "ERROR: ";
	
	public static String getnewDecoratorName(String baseName, Resource parent) {
		return getnewDecoratorName(baseName, parent, "_");
	}
	public static String getnewDecoratorName(String baseName, Resource parent, String separator) {
		int i=0;
		String name = baseName+separator+i;
		while(parent.getSubResource(name) != null) {
			i++;
			name = baseName+separator+i;
		}
		return name;
	}

	@SafeVarargs
	public static List<EntryType> getStandardEntryTypeList(Class<? extends Resource>... types) {
		List<EntryType> result = new ArrayList<>();
		for(Class<? extends Resource> t: types) {
			EntryType r = getEntryType(t);
			result.add(r);
		}
		return result ;
	}
	
	/*@SafeVarargs
	public static List<EntryType> getStandardEntryTypeList(ApplicationManagerSPExt appManExt, Class<? extends Resource>... types) {
		List<EntryType> result = new ArrayList<>();
		for(Class<? extends Resource> t: types) {
			EntryType r = getEntryType(appManExt.getTypeDeclaration(t));
			result.add(r);
		}
		return result ;
	}*/

	private static EntryType getEntryType(Class<? extends Resource> type) {
		return new EntryType() {

			@Override
			public Class<? extends Resource> getType() {
				return type;
			}

			@Override
			public Cardinality getCardinality() {
				return Cardinality.SINGLE_VALUE_REQUIRED;
			}
			
		};		
	}}
