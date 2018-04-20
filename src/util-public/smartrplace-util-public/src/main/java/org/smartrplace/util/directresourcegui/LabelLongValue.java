package org.smartrplace.util.directresourcegui;

public class LabelLongValue extends LabelValue {
	public LabelLongValue(long value) {
		this.value = value;
	}
	public LabelLongValue(String alternativeText) {
		this.alternativeText = alternativeText;
	}

	public long value;
}
