package org.ogema.externalviewer.extensions;

import java.util.Collection;
import java.util.List;

import org.ogema.externalviewer.extensions.IntervalConfiguration;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButton;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfiguration;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfigurationBuilder;
import de.iwes.widgets.reswidget.scheduleviewer.api.TimeSeriesFilter;

public class ScheduleViwerOpenUtil {
	private static final String fixedEvalProviderId = "extended-quality_eval_provider";
	public static interface SchedOpenDataProvider {
		List<TimeSeriesData> getData(OgemaHttpRequest req);
		IntervalConfiguration getITVConfiguration();		
	}
	
	//public static ScheduleViewerOpenButton getScheduleViewerOpenButton(WidgetPage<?> page, String widgetId,
	//		final SchedOpenDataProvider provider) {
	//	
	//}
	public static ScheduleViewerOpenButton getScheduleViewerOpenButton(OgemaWidget parent, String widgetId,
			String text, final SchedOpenDataProvider provider,
			final DefaultScheduleViewerConfigurationProviderExtended scheduleViewerExpertProvider, OgemaHttpRequest req) {
		ScheduleViewerOpenButtonEval schedOpenButtonEval = new ScheduleViewerOpenButtonEval(parent, widgetId, text,
				scheduleViewerExpertProvider.getConfigurationProviderId(),
				scheduleViewerExpertProvider, req) {
			private static final long serialVersionUID = 1L;

			@Override
			protected ScheduleViewerConfiguration getViewerConfiguration(long startTime, long endTime,
					List<Collection<TimeSeriesFilter>> programs) {
				final ScheduleViewerConfiguration viewerConfiguration =
						ScheduleViewerConfigurationBuilder.newBuilder().setPrograms(programs).
						setStartTime(startTime).setEndTime(endTime).setShowManipulator(true).
						setShowIndividualConfigBtn(false).setShowPlotTypeSelector(true).
						setShowManipulator(false).build();
					return viewerConfiguration;
			}

			@Override
			protected List<TimeSeriesData> getTimeseries(OgemaHttpRequest req) {
				return provider.getData(req);
			}

			@Override
			protected String getEvaluationProviderId(OgemaHttpRequest req) {
				return fixedEvalProviderId;
			}

			@Override
			protected IntervalConfiguration getITVConfiguration(OgemaHttpRequest req) {
				return provider.getITVConfiguration();
			}
		};
		schedOpenButtonEval.setNameProvider(null);
		return schedOpenButtonEval;
	}

	public static ScheduleViewerOpenButton getScheduleViewerOpenButton(WidgetPage<?> page, String widgetId,
			String text, final SchedOpenDataProvider provider,
			final DefaultScheduleViewerConfigurationProviderExtended scheduleViewerExpertProvider) {
		return getScheduleViewerOpenButton(page, widgetId, text, provider, scheduleViewerExpertProvider, false);
	}
	public static ScheduleViewerOpenButton getScheduleViewerOpenButton(WidgetPage<?> page, String widgetId,
			String text, final SchedOpenDataProvider provider,
			final DefaultScheduleViewerConfigurationProviderExtended scheduleViewerExpertProvider,
			boolean generateGraphImmediately) {
		ScheduleViewerOpenButtonEval schedOpenButtonEval = new ScheduleViewerOpenButtonEval(page, widgetId, text,
				scheduleViewerExpertProvider.getConfigurationProviderId(),
				scheduleViewerExpertProvider) {
			private static final long serialVersionUID = 1L;

			@Override
			protected ScheduleViewerConfiguration getViewerConfiguration(long startTime, long endTime,
					List<Collection<TimeSeriesFilter>> programs) {
				final ScheduleViewerConfiguration viewerConfiguration =
						ScheduleViewerConfigurationBuilder.newBuilder().setPrograms(programs).
						setStartTime(startTime).setEndTime(endTime).setShowManipulator(true).
						setShowIndividualConfigBtn(false).setShowPlotTypeSelector(true).
						setShowManipulator(false).build();
					return viewerConfiguration;
			}

			@Override
			protected List<TimeSeriesData> getTimeseries(OgemaHttpRequest req) {
				return provider.getData(req);
			}

			@Override
			protected String getEvaluationProviderId(OgemaHttpRequest req) {
				return fixedEvalProviderId;
			}

			@Override
			protected IntervalConfiguration getITVConfiguration(OgemaHttpRequest req) {
				return provider.getITVConfiguration();
			}
			
			@Override
			boolean generateGraphImmediately() {
				return generateGraphImmediately;
			}
		};
		schedOpenButtonEval.setNameProvider(null);
		return schedOpenButtonEval;
	}
}
