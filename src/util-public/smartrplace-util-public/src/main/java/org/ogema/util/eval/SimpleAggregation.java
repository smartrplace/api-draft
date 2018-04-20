package org.ogema.util.eval;

import java.util.Collection;

import org.smartrplace.remotesupervision.transfer.util.EvalSumUpCreator;

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
