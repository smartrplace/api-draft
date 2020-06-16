package org.ogema.devicefinder.util;

import org.ogema.devicefinder.api.DatapointDesc.ScalingProvider;

public class FactorScale implements ScalingProvider {
	public final float factor;

	public FactorScale(float factor) {
		this.factor = factor;
	}

	@Override
	public float getStdVal(float rawValue, Long timeStamp) {
		return factor*rawValue;
	}
}
