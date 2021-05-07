package org.smartrplace.os.access.api;

import java.nio.file.Path;
import java.util.List;

public interface LogFileParseService {
	/** Get all lines from a log file that match a given String
	 * 
	 * @param fileToRead file to read. The file must be opened and processed in text mode
	 * @param grepString String to be applied via grep
	 * @param startTime if the log files contain a time stamp (initial assumption: Only at the beginning) then
	 * 		only lines from the time stamp shall be processed. If files exist that can be identified to contain
	 * 		older data in the linux standard format (e.g. appending -1, -2) then also those files shall be
	 * 		taken into account
	 * @return list of lines matching the criteria
	 */
	List<String> getRelevantLines(Path fileToRead, String grepString, long startTime);
}
