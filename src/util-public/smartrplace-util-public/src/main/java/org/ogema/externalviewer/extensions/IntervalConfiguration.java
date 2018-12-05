package org.ogema.externalviewer.extensions;

/** Configuration of evaluation intervals*/
public class IntervalConfiguration {
	public long start = 0;
	public long end = 0;
	/** If multiStart is not null, also multiEnd must exist and have the same array length.
	 * In this case start and end are not used.
	 */
	public long[] multiStart = null;
	public long[] multiEnd = null;
	/** These suffixes will be checked when searching for non-existing file names for
	 * result creation
	 */
	public String[] multiFileSuffix = null;
}

