package org.smartrplace.util.frontend.servlet;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.ogema.core.channelmanager.measurements.Value;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.smartrplace.util.frontend.servlet.UserServlet.JSONVarrRes;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider;

public abstract class ServletResourceListProvider<T extends Resource> implements ServletValueProvider {
	protected final String id;
	protected final ResourceList<T> res;
	protected final POSTMODE postMode;
	protected abstract JSONObject getJSON(T element);
	/** The return element is only relevant in POSTMODE ADD_ONLY.
	 * @param element the element to fill in mode REWRITE_COMPLETE_LIST. The parameter will be null in mode ADD_ONLY.
	 * In mode ADD_ONLY the resource has to be created by the method and returned by this method. This allows to
	 * perform additional checks if the creation is really possible and to create the resource with a defined
	 * name in the ResourceList.
	 * @param json
	 * @return irrelevant in POSTMODE REWRITE_COMPLETE_LIST. Otherwise the newly created resource shall
	 * 		be returned here or null if the creation of a new element is not possible with the input data
	 */
	protected abstract T setElementData(T element, JSONObject json, int index);	
	
	/** If the list is written and the new content is shorter than minimum size then the list will not
	 * be changed. This is not relevant for POSTMODE.ADDONLY
	 * @param json data to write
	 * @return
	 */
	protected int minimumSizeOfListAfterUpdate(JSONArray json) {
		return 0;
	}
	
	/** Overwrite to modify the array received before processing elements*/
	protected JSONArray checkArrayToWrite(JSONArray json) {
		return json;
	}

	public enum POSTMODE {
		/** In this mode every POST must contain the entire list with all data. The list will be
		 * rewritten containing the new data. Existing elements will be reused, but the content may
		 * be switched, so the names of the elements of the ResourceList should be irrelevant if this
		 * POSTMODE is used. Currently this is the default mode.
		 * TODO: Change implementation so that elements are re-identified according to name-subresource
		 * if available. This may also be used for an additional mode allowing to modify a single element
		 * with each POST as it is possible to add an elements with ADD_ONLY now.
		 */
		REWRITE_COMPLETE_LIST,
		/** The content of the POST request shall contain a single element in this mode, which will be
		 * added to the ResourceList. Modification or removal of existing elements is not possible via
		 * this provider in this POSTMODE
		 */
		ADD_ONLY
	}
	
	/** Construct ServletResourceListProvider
	 * 
	 * @param res resourcelist to be processed
	 * @param id key in the servlet result
	 */
	public ServletResourceListProvider(ResourceList<T> res, String id) {
		this(res, id, POSTMODE.REWRITE_COMPLETE_LIST);
	}
	public ServletResourceListProvider(ResourceList<T> res, String id, POSTMODE postMode) {
		this.res = res;
		this.id = id;
		this.postMode = postMode;
	}
	
	@Override
	public ValueMode getValueMode() {
		return ValueMode.JSON;
	}
	
	@Override
	public JSONVarrRes getJSON(String user, String key) {
		JSONArray result = new JSONArray();
		for(T el: res.getAllElements()) {
			JSONObject eljson = getJSON(el);
			if(eljson != null)
				result.put(eljson);
		}
		JSONObject res = new JSONObject();
		res.put(id, result);
		
		//TODO: Also support to return array
		JSONVarrRes realResult = new JSONVarrRes();
		realResult.result = res;
		return realResult;
	}
	
	@Override
	public Value getValue(String user, String key) {
		throw new IllegalStateException("Returns JSON");
	}

	@Override
	public synchronized void setValue(String user, String key, String value) {
		if(postMode == POSTMODE.REWRITE_COMPLETE_LIST) try  {
			JSONObject jsonobj = new JSONObject(value);
			JSONArray json = jsonobj.getJSONArray(id);
			json = checkArrayToWrite(json);
			final int len = json.length();
			if(len < res.size()) {
				int minSize = minimumSizeOfListAfterUpdate(json);
				if(len < minSize)
					return;
				List<T> allRes = res.getAllElements();
				for(int i=len; i<allRes.size(); i++) {
					allRes.get(i).delete();
				}
			} else if(len > res.size()) {
				for(int i=res.size(); i<len; i++) {
					res.add();
				}
			}
			for(int i=0; i<len; i++) {
				List<T> allRes = res.getAllElements();
				setElementData(allRes.get(i), (JSONObject) json.get(i), i);
			}
		} catch(NumberFormatException e) {
			throw new IllegalArgumentException(e);
		}
		else if(postMode == POSTMODE.ADD_ONLY) try {
			JSONObject jsonobj = new JSONObject(value);
			//T newEl = 
			setElementData(null, jsonobj, -1);
			//if(newEl == null)
			//	throw new IllegalArgumentException("Data not accepted for element creation!");
		} catch(NumberFormatException e) {
			throw new IllegalArgumentException(e);
		}
	}
}
