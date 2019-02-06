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

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.smartrplace.util.directobjectgui.ObjectGUITableProvider;

import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;

public interface ResourceGUITableProvider<T extends Resource> extends ObjectGUITableProvider<T, T> {

	void addWidgets(final T object, final ResourceGUIHelper<T> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan);
/*	void addWidgets(final T object,
			final ResourceGUIHelper<T> vh, final String id, OgemaHttpRequest req, Row row,
			ApplicationManager appMan);*/
}
