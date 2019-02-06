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
package org.smartrplace.util.eval;

import java.util.Collection;


/**Aggregates according to modes as defined in {@link EvalSumUpCreator}
 * 
 * @author dnestle
 *
 */
public abstract class SimpleAggregation<T extends Object> {
	protected abstract float getValue(T element);
	
	public float getAggregatedValue(Collection<T> sourceList, int mode) {
		float sumUp;
		switch(mode) {
		case 1: 
			sumUp = Float.MAX_VALUE;
			break;
		case 2: 
			sumUp = -Float.MAX_VALUE;
			break;
		default:
			sumUp = 0;
			break;
		}
		int count = 0;
		for(T s: sourceList) {
			float val = getValue(s);
			switch(mode) {
			case 1: //minimum
				if(val < sumUp) sumUp = val;
				break;
			case 2: //maximum
				if(val > sumUp) sumUp = val;
				break;
			default: //average, integral
				sumUp += val;
				count++;
				break;
			}
		}
		if(mode == 3) {//average
			sumUp /= count;
		}
		return sumUp;
	}
}
