/**
 * Copyright 2009 - 2016
 *
 * Fraunhofer-Gesellschaft zur Förderung der angewandten Wissenschaften e.V.
 *
 * Fraunhofer IWES
 *
 * All Rights reserved
 */
package de.iwes.widgets.object.widget.init;

import de.iwes.widgets.api.extended.plus.InitWidget;
import de.iwes.widgets.api.extended.plus.SelectorTemplate;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.emptywidget.EmptyData;
import de.iwes.widgets.html.emptywidget.EmptyWidget;

/**
 * A widget that does not provide any Html, but can extract parameters from the page URL, and select a corresponding resource
 * of the specified type. This can then be used to initialize other widgets on the page.
 *
 * @param <R>
 */
// TODO move to widget-collections, let ResourceInitSingleEmpty and Pattern... inherit from this, rename to TemplateInitSingle?
public abstract class ObjectInitSingleEmpty<R>
	extends EmptyWidget implements InitWidget, SelectorTemplate<R> {

	private static final long serialVersionUID = 1L;
//	private final AppstoreGuiDevController app;
	private R defaultSelected = null;
	//private final boolean allowInactive;
	
	public ObjectInitSingleEmpty(WidgetPage<?> page, String id) {
		super(page, id);
	}

	/*public InitSingleEmpty(WidgetPage<?> page, String id, boolean globalWidget, boolean allowInactive, ApplicationManager am) {
		super(page, id, globalWidget);
		Objects.requireNonNull(am);
		this.am=am;
		this.allowInactive = allowInactive;
	}
	
	public InitSingleEmpty(OgemaWidget parent, String id, boolean allowInactive, OgemaHttpRequest req, ApplicationManager am) {
		super(parent,id, req);
		Objects.requireNonNull(am);
		this.am =am;
		this.allowInactive = allowInactive;
	}*/
	
	public class PatternInitSingleEmptyOptions extends EmptyData {
		
		private volatile R selectedResource;

		public PatternInitSingleEmptyOptions(ObjectInitSingleEmpty<R> empty) {
			super(empty);
		}

		public R getSelectedItem() {
			return selectedResource;
		}

		public void selectItem(R item) {
			this.selectedResource = item;
		}
		
	}

	@Override
	public PatternInitSingleEmptyOptions createNewSession() {
		return new PatternInitSingleEmptyOptions(this);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public PatternInitSingleEmptyOptions getData(OgemaHttpRequest req) {
		return (PatternInitSingleEmptyOptions) super.getData(req);
	}
	
	@Override
	protected void setDefaultValues(EmptyData opt) {
		super.setDefaultValues(opt);
		@SuppressWarnings("unchecked")
		PatternInitSingleEmptyOptions opt2= (PatternInitSingleEmptyOptions) opt;
		opt2.selectItem(defaultSelected);
	}
	
	@Override
	public R getSelectedItem(OgemaHttpRequest req) {
		return getData(req).getSelectedItem();
	}

	@Override
	public void selectItem(R item, OgemaHttpRequest req) {
		getData(req).selectItem(item);
	}

	@Override
	public void selectDefaultItem(R item) {
		this.defaultSelected = item;
	}

	// select the resource
	@Override
	public abstract void init(OgemaHttpRequest req);
}
