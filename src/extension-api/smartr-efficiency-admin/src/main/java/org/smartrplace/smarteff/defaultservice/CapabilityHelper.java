package org.smartrplace.smarteff.defaultservice;

import java.util.ArrayList;
import java.util.List;

import org.smartrplace.extensionservice.ExtensionCapabilityPublicData.EntryType;
import org.smartrplace.extensionservice.ExtensionResourceType;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration.Cardinality;

public class CapabilityHelper {
	@SafeVarargs
	public static List<EntryType> getStandardEntryTypeList(Class<? extends ExtensionResourceType>... types) {
		List<EntryType> result = new ArrayList<>();
		for(Class<? extends ExtensionResourceType> t: types) {
			EntryType r = new EntryType() {

				@Override
				public Class<? extends ExtensionResourceType> getType() {
					return t;
				}

				@Override
				public Cardinality getCardinality() {
					return Cardinality.SINGLE_VALUE_REQUIRED;
				}
				
			};
			result.add(r);
		}
		return result ;
	}
}
