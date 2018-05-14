package org.ogema.util.directresourcegui.kpi;

import org.ogema.model.alignedinterval.StatisticalAggregation;
import org.smartrplace.analysis.backup.parser.api.GatewayBackupAnalysis;

import de.iwes.timeseries.eval.api.ResultType;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;

public class KPIResultType extends KPIStatisticsUtil {

	public final boolean isHeader;

 	public KPIResultType(ResultType resultType, GaRoSingleEvalProvider provider, StatisticalAggregation sAgg,
			boolean forceCalculations, GatewayBackupAnalysis gatewayParser) {
		super(resultType, provider, sAgg, forceCalculations, gatewayParser);
		this.isHeader = false;
	}

	/** Use this constructor only to generate a header line object*/
	public KPIResultType(boolean isHeader) {
		super(null, null, null, false, null);
		this.isHeader = isHeader;
	}

}
