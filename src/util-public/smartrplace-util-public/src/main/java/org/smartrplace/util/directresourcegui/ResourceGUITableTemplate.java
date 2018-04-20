package org.smartrplace.util.directresourcegui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.smartrplace.util.directobjectgui.ApplicationManagerMinimal;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.resource.widget.table.DefaultResourceRowTemplate;
import de.iwes.widgets.resource.widget.table.ResourceTable;

public abstract class ResourceGUITableTemplate<T extends Resource> extends DefaultResourceRowTemplate<T> {
	private static final long IDLE_TIME_TO_CLEANUP = 60000;
	public ResourceGUIHelper<T> mhInit = null;
	private boolean isInInit = false;
	private final ApplicationManager appMan;
	private final ApplicationManagerMinimal appManMinimal;
	private long getFrameworkTime() {
		if(appMan != null) return appMan.getFrameworkTime();
		return appManMinimal.getFrameworkTime();
	}
	protected final WidgetPage<?> page;
	public interface TableProvider<P extends Resource> {
		ResourceTable<P> getTable(OgemaHttpRequest req);
	}
	protected final TableProvider<T> tableProvider;
	
	//TODO: We sometimes get 2 or 3 onGET calls for the same object on the same request, which leads to a Widget with
	//   id already exists exception (IllegalArgumentException)
	@Deprecated
	Map<T, Set<String>> objectsInitialized = new HashMap<>();
	@Deprecated
	long lastAccessTime = -1;
	
	public ResourceGUITableTemplate(WidgetPage<?> page,
			Class<T> resourceType, ApplicationManager appMan)  {
		this(page, resourceType, appMan, null);
	}
	public ResourceGUITableTemplate(WidgetPage<?> page,
			Class<T> resourceType, ApplicationManager appMan, ApplicationManagerMinimal appManMin)  {
		this.appMan = appMan;
		this.appManMinimal = appManMin;
		this.page = page;
		this.tableProvider = null;
		
		init(resourceType);
	}
	public ResourceGUITableTemplate(TableProvider<T> tableProvider,
			Class<T> resourceType, ApplicationManager appMan)  {
		this(tableProvider, resourceType, appMan, null);
	}
	public ResourceGUITableTemplate(TableProvider<T> tableProvider,
			Class<T> resourceType, ApplicationManager appMan, ApplicationManagerMinimal appManMin)  {
		this.appMan = appMan;
		this.appManMinimal = appManMin;
		this.page = null;
		this.tableProvider = tableProvider;
		
		init(resourceType);
	}
	
	private void init(Class<T> resourceType) {
		/*ApplicationManager myAppMan = DirectGUIExtendedApp.getApplicationManager();
		EvalCollection ec = EvalHelper.getEvalCollection(myAppMan);
		T sampleResource = ec.getSubResource("sampleForInit_"+resourceType.getSimpleName(), resourceType);*/
		T sampleResource = ResourceHelper.getSampleResource(resourceType);
		isInInit = true;
		addRow(sampleResource, null);
		isInInit = false;		
	}
	
	/**Replacement for {@link #addRow(Resource, OgemaHttpRequest)} that provides {@link ResourceGUIHelper}
	 * 
	 * @param object resource for the row
	 * @param vh
	 * @param id lineId
	 * @param req
	 * @return
	 */
	protected abstract Row addRow(T object, ResourceGUIHelper<T> vh, String id, OgemaHttpRequest req);
	/**Overwrite to add entries into header map that are not generated by ValueReceiverHelper*/
	//protected void addToHeader(LinkedHashMap<String,Object> map) {}

	public class InitResult {
		public ResourceGUIHelper<T> vh;
		public String id;
	}
	public InitResult initRow(T object, OgemaHttpRequest req) {
		InitResult result = new InitResult();
		
		if(isInInit) {
			if(tableProvider != null) {
				ResourceTable<T> table = tableProvider.getTable(req);
				mhInit = result.vh = new ResourceGUIHelper<T>(table, req, (T)null, appMan, false);
			} else
				mhInit = result.vh = new ResourceGUIHelper<T>(page, (T)null, appMan, false);
			result.id = "";
		}
		else {
			if(tableProvider != null) {
				ResourceTable<T> table = tableProvider.getTable(req);
				result.vh = new ResourceGUIHelper<T>(table, req, object, appMan, false);
			} else
				result.vh = new ResourceGUIHelper<T>(page, object, appMan, false);
			result.id = getLineId(object);
		}
		return result;
	}
	
	public Map<String,Object> getHeader() {
		if(mhInit != null) {
			LinkedHashMap<String,Object> map2 = mhInit.getHeader();
			//addToHeader(map2);
			return map2;
		}
		throw new IllegalStateException("mhInit must be initialized before calling getHeader!");
	}
	
	@Override
	public Row addRow(T object, OgemaHttpRequest req) {
		if(req != null) {
			if((lastAccessTime > 0)&&(getFrameworkTime() - lastAccessTime > IDLE_TIME_TO_CLEANUP)) {
				objectsInitialized.clear();
			} else {
				if(objectsInitialized.get(object) != null && objectsInitialized.get(object).contains(req.getSessionId()))
					return null;
			}
			lastAccessTime = getFrameworkTime();
			Set<String> sessions = objectsInitialized.get(object);
			if(sessions == null) {
				sessions = new HashSet<>();
				objectsInitialized.put(object, sessions);
			}
			sessions.add(req.getSessionId());
		}
		InitResult initRow = initRow(object, req);
		return addRow(object, initRow.vh, initRow.id, req);
	}
}
