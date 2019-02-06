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
package org.smartrplace.util.directobjectgui;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;

import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;

public interface ObjectGUITableProvider<T, R extends Resource> {
	void addWidgets(final T object,
			final ObjectResourceGUIHelper<T, R> vh, final String id, OgemaHttpRequest req, Row row,
			ApplicationManager appMan);

	R getResource(T object, OgemaHttpRequest req);
}
