package org.smartrplace.smarteff.admin.protect;

import java.util.List;

import org.smartrplace.extenservice.resourcecreate.ExtensionPageSystemAccessForCreate;
import org.smartrplace.extenservice.resourcecreate.ExtensionPageSystemAccessForPageOpening;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.ExtensionResourceType;
import org.smartrplace.extensionservice.ExtensionUserDataNonEdit;

public class ExtensionResourceAccessInitDataImpl implements ExtensionResourceAccessInitData {
	private final int entryTypeIdx;
	private final List<ExtensionResourceType> entryResources;
	private final ExtensionResourceType userData;
	private final ExtensionUserDataNonEdit userDataNonEdit;
	private final ExtensionPageSystemAccessForPageOpening systemAccess;
	
	public ExtensionResourceAccessInitDataImpl(int entryTypeIdx, List<ExtensionResourceType> entryResources,
			ExtensionResourceType userData, ExtensionUserDataNonEdit userDataNonEdit,
			ExtensionPageSystemAccessForPageOpening systemAccess) {
		this.entryTypeIdx = entryTypeIdx;
		this.entryResources = entryResources;
		this.userData = userData;
		this.userDataNonEdit = userDataNonEdit;
		this.systemAccess = systemAccess;
	}

	@Override
	public int entryTypeIdx() {
		return entryTypeIdx;
	}

	@Override
	public List<ExtensionResourceType> entryResources() {
		return entryResources;
	}

	@Override
	public ExtensionResourceType userData() {
		return userData;
	}

	@Override
	public ExtensionUserDataNonEdit userDataNonEdit() {
		return userDataNonEdit;
	}

	@Override
	public ExtensionPageSystemAccessForCreate systemAccess() {
		if(!(systemAccess instanceof ExtensionPageSystemAccessForCreate))
			throw new IllegalStateException("Page without valid configID only supports systeAccessForPageOpening!");
		return (ExtensionPageSystemAccessForCreate) systemAccess;
	}
	@Override
	public ExtensionPageSystemAccessForPageOpening systemAccessForPageOpening() {
		return systemAccess;
	}
}