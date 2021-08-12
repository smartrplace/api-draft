package org.smartrplace.tissue.util.logconfig;

import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.model.connections.ElectricityConnection;
import org.ogema.recordeddata.DataRecorder;
import org.ogema.timeseries.eval.simple.mon.TimeseriesSimpleProcUtil;
import org.ogema.timeseries.eval.simple.mon.TimeseriesSimpleProcUtilBase;
import org.slf4j.Logger;
import org.smartrplace.apps.hw.install.prop.ViaHeartbeatSchedules;

/** Management of Virtual sensors for a certain application<br>
 * Note that virtual sensors are created with resources, but time series are only stored as memory time series. Only
 * when transferred via heartbeat then schedules are created on the server side. Evaluations and alarming relying on
 * historical data should access this via the datapoint to also get time series for virtual datapoints. Note that
 * the simpler version of providing virtual datapoints is the calculation of gateway-wide KPIs. This generally
 * implies call of {@link ViaHeartbeatSchedules#registerDatapointForHeartbeatDp2Schedule(Datapoint, Integer)}.
 * There is a good example for this in KPILocInit. Usually no resources are created for such KPIs on the gateway.<br>
 * All such types require an instance of TimeseriesSimpleProcUtilBase, which can either by a standard {@link TimeseriesSimpleProcUtil} or
 * a {@link TimeseriesSimpleProcUtil3}. The latter is recommended for any new implementations.<br>
 * <br>
 * Standard virtual sensors:<br>
 * Usually for each device type an instance of VirtualSensorKPIMgmt is created that either overwrites getAndConfigureValueResourceSingle or
 * getAndConfigureValueResource. The actual generation of the virtual datapoint in the getDatapoints() method of a
 * device handler is done via {@link #addVirtualDatapoint(List, String, Resource, long, boolean, boolean, List)}. In the
 * getAndConfigureValueResource method the actual creation of the virtual datapoint and the creation of the
 * SingleValueResource representing the virtual datapoint must be made. If the virtual sensor data shall be 
 * transferred to a superior via heartbeat-schedule this must also be registered here.<br>
 * Important: Collecting gateways find devices in the serverMirror structure and process them via the DeviceHandler.
 * So any virtual sensor for a device is generated on the collecting gateway, a transfer via heartbeat is
 * not necessary. As the computing power on a collecting gateway is usually sufficient for several, but no
 * a very large number of sub-gateways this is the preferred option. This also saves traffic via a mobile connection.
 * For superior servers that do not activate sub gateway devices virtual sensor calculation shall take place on
 * the gateway, data shall be transferred via heartbeat.<br>
 * [Unsure as the registration is done by addVirtualDatapoint: Note that if a SingleValueResource
 * is created and the path of this resource is requested via heartbeat by the server then no separate registration
 * on the client side is necessary (but of course on the server).]<br>
 * See GatewayDeviceHandler as latest example,
 * also ESE_ElConnBoxDeviceHandler (simplest) or DeviceHandlerMQTT_ElecConnBox can be used.<br>
 * For some virtual sensor types the datapoint creation and heartbeat-schedule registration can be done by static methods e.g. in this class. Currently there are only two types with
 * some options:<br>
 * - {@link #registerEnergySumDatapointOverSubPhasesFromDay(ElectricityConnection, AggregationMode, TimeseriesSimpleProcUtilBase, DatapointService, Datapoint)}<br>
 * - {@link #registerEnergySumDatapointFromDay(List, AggregationMode, TimeseriesSimpleProcUtilBase, String, boolean, DatapointService, Datapoint, List)}<br>
 * For simple virtual datapoints it should be no problem to generate datapoints from TimeseriesSimpleProcUtil3 and schedule transfer via 
 * ViaHeartbeatSchedules#registerDatapointForHeartbeatDp2Schedule directly, though.<br>
 * In both cases if heartbeat-schedule transfer is used it has to be registered with remote device processing below
 * DPManServer also, see SPSKPI_Init.createVirtualEnergySensorOnSuperior for an example.
 * 
 * <br>
 * Collecting Gateways:<br>
 * Devices shown in Installation&Setup on the collecting gateway refer to the serverMirror sub device. For the datapoints
 * refering to these resources the remote slotsDb is used and these can be used in any charts by this. Applications
 * using getHistoricalData() would fail, though.<br>
 * Should be deprecated: Here the plots do
 * not provide any reasonable data. You have to use the Charts app to get the real plot data of the devices from the
 * sub gateways.<br>
 * Collecting Gateways calculate virtual sensors on the collecting gateway, but the slotsDB source is stored in the
 * directory of the sub gateway. TODO: This leads to the effect that on superior e.g. a Iotawatt device is shown twice,
 * the real slotsDB source is shown in the sub gateway device plot, the virtual sensor data is shown in the collecting
 * gateway device plot.<br>
 * <br>
 * KPIs:<br>
 * KPIs are usually not part of specific devices, but calculated for an entire gateway. So the schedules are stored
 * in serverMirror/_gw<gwId>/gw. To access the schedule content on the superior gateway you have to use the gateway KPI plots for the respective
 * collecting gateway. There is a link in the TsAny plot page of superior, which is usually almost empty.<br>
 * Registration of KPIs on the server is usually done in methods calling SKSKPI_Init#registerKPIorVirtualSensorSchedule,
 * usually at the end of DpManServer#init.<br>
 * On the gateway charts for KPIs can be generated or they can be added to any gateway-wide virtual device offered by the
 * PST device handler.
 * 
 * @author dnestle
 *
 */
public abstract class VirtualSensorKPIMgmt extends VirtualSensorKPIMgmtBase<Resource, SingleValueResource> {

	public VirtualSensorKPIMgmt(TimeseriesSimpleProcUtilBase util, Logger logger,
			DatapointService dpService) {
		super(util, logger, dpService);
	}
	public VirtualSensorKPIMgmt(TimeseriesSimpleProcUtilBase util, DataRecorder dataRecorder, Logger logger,
			DatapointService dpService) {
		super(util, dataRecorder, logger, dpService);
	}
}
