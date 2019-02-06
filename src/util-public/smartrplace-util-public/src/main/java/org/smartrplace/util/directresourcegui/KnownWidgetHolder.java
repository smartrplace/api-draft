/**
 * ﻿Copyright 2018 Smartrplace UG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.smartrplace.util.directresourcegui;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.emptywidget.EmptyWidget;

public class KnownWidgetHolder<T> extends EmptyWidget {

	private static final long serialVersionUID = 1L;

	public KnownWidgetHolder(WidgetPage<?> page, String id) {
		super(page, id);
	}
	
	@Override
	public KnownWidgetHolderData<T> createNewSession() {
		return new KnownWidgetHolderData<T>(this);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public KnownWidgetHolderData<T> getData(OgemaHttpRequest req) {
		return (KnownWidgetHolderData<T>) super.getData(req);
	}
	
}

