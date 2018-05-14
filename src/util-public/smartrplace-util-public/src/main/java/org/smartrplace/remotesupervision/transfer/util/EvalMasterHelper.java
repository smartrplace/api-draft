package org.smartrplace.remotesupervision.transfer.util;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.model.eval.EvaluationConfig;
import org.ogema.model.gateway.master.GatewayInfo;
import org.ogema.model.gateway.remotesupervision.GatewayTransferInfo;
import org.ogema.model.gateway.remotesupervision.SingleValueTransferInfo;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.ogema.util.eval.ContinuousEvalCreator;

import de.iwes.tools.statistics.StatisticalAggregationCallbackTemplate;
import de.iwes.tools.statistics.StatisticalAggregationProvider;
import de.iwes.tools.statistics.StatisticalMinMaxProvider;
import de.iwes.tools.statistics.StatisticalProviderMeterCount;
import de.iwes.util.timer.AbsoluteTiming;

public class EvalMasterHelper {
	//TODO: Update init-strategy for evaluation. Note that the initStatProviders methods contain
	//evaluation implementation code!
	//Finding the FloatResource that here is svti.value could be done differently via pattern on a client
	public static void initGatewayInfo(ResourceList<EvaluationConfig> evaluations, final GatewayTransferInfo gti, 
			ApplicationManager am, final ResourceList<GatewayTransferInfo> clientList) {
		//TODO: In a more generic implementation this would have to be found via a REST-Pattern
		checkCounterTransferData("sensorActorEvalConfig/homematicSensorActivity", gti, am, clientList);
		checkCounterTransferData("semaFTConfigConfig/heatingPoints", gti, am, clientList);
		evaluations.activate(true);		
	}
	
	public static void initCentralGatewayInfo(GatewayInfo central, final ResourceList<GatewayTransferInfo> clientList,
			ApplicationManager am) {
		ResourceList<EvaluationConfig> evaluations = central.evaluations().create();
		evaluations.getSubResource("aggregatedData", GatewayInfo.class).create();
	}
	
	/** Create statistical base provider suitable for the input. In contrast to {@link createAggregationEval}
	 * (see below) it suuports also StatisticalMinMaxProvider and StatisticalAggregationProvider, not only
	 * StatisticalProviderMeterCount. For StatisticalProviderMeterCount it only support standard mode, though,
	 * whereas createAggregationEval performs an integration(?).
	 * 
	 * @param id
	 * @param gti
	 * @param appMan
	 * @param clientList
	 * @return
	 */
	public static ContinuousEvalCreator<EvaluationConfig> checkCounterTransferData(final String id,
			GatewayTransferInfo gti, ApplicationManager appMan, final ResourceList<GatewayTransferInfo> clientList) {
		SingleValueTransferInfo svti = EvalSetup.getTransferData(id, gti.valueData());
		if(svti != null) {
			ContinuousEvalCreator<EvaluationConfig> deviceEval =
					new ContinuousEvalCreator<EvaluationConfig>(id, gti.masterInformation().evaluations(), EvaluationConfig.class,
					"gateway-master-v2",
					new int[]{AbsoluteTiming.TEN_SECOND, AbsoluteTiming.DAY}, true, (FloatResource)svti.value(), appMan) {
				@Override
				protected void initStatProviders() {
					int mode = EvalSetup.getMode(id);
					switch(mode) {
					case 1:
					case 2:
						providers.add(new StatisticalMinMaxProvider(dataSource, evalConfig.destinationStat(),
								appMan, null, mode));
						break;
					case 3:
						providers.add(new StatisticalAggregationProvider(dataSource, evalConfig.destinationStat(),
								appMan, null));
						break;
					default:
						providers.add(new StatisticalProviderMeterCount(dataSource, evalConfig.destinationStat(),
								appMan, null));
					}
				}
			};
			//TODO: we need another TimeResource for the underlying AbsoluteTimer for this operation. 
			//TODO: For now just commented ou
			//if(clientList != null)
			//	createAggregationEval(id, gti.masterInformation().evaluations(), clientList, appMan);
			return deviceEval;
		}
		return null;
	}
	
	/** Create statistical StatisticalProviderMeterCount for a location inside the GatewayTransferInfo
	 * received on the master
	 * 
	 * @param location client location (typically real location, could also be a virtual location)
	 * @param evaluations typically GateWayInfo.evaluations
	 * @param clientList
	 * @param appMan
	 * @return
	 */
	public static ContinuousEvalCreator<EvaluationConfig> createAggregationEval(final String location,
			ResourceList<EvaluationConfig> evaluations, final ResourceList<GatewayTransferInfo> clientList,
			ApplicationManager appMan) {
		return new ContinuousEvalCreator<EvaluationConfig>(location, evaluations,
				EvaluationConfig.class, "gateway-master-v2",
				new int[]{AbsoluteTiming.TEN_SECOND, AbsoluteTiming.DAY}, true, null, appMan) {
			@Override
			protected void initStatProviders() {
				providers.add(new StatisticalProviderMeterCount(dataSource, evalConfig.destinationStat(),
						appMan, new StatisticalAggregationCallbackTemplate() {
					@Override
					public float valueChanged(FloatResource value, FloatResource primaryAggregation,
							long valueEndInstant, long timeStep) {
						float sum = 0;
						for(GatewayTransferInfo gw: clientList.getAllElements()) {
							SingleValueTransferInfo svti = EvalSetup.getTransferData(location, gw.valueData());
							if(svti != null) {
								sum += ValueResourceUtils.getFloatValue(svti.value());
							}									
						}
						return sum;
					}
				}));
			}
		};
	}
}
