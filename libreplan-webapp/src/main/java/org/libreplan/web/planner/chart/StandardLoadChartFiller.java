package org.libreplan.web.planner.chart;

import static java.util.Arrays.asList;

import java.util.Collections;
import java.util.SortedMap;

import org.joda.time.LocalDate;
import org.libreplan.business.planner.chart.ILoadChartData;
import org.libreplan.business.workingday.EffortDuration;
import org.zkoss.ganttz.util.Interval;

public abstract class StandardLoadChartFiller extends LoadChartFiller {

    @Override
    protected ChartSeries[] getPlotInfo(Interval interval) {
        final ILoadChartData data = getDataOn(interval);

        ChartSeries plotInfoLoad = createPlotinfoFromDurations(getLoad(data));
        plotInfoLoad.setFillColor(COLOR_ASSIGNED_LOAD);
        plotInfoLoad.setLineWidth(0);

        ChartSeries plotInfoMax = createPlotinfoFromDurations(getCalendarMaximumAvailability(data));
        plotInfoMax.setLineColor(COLOR_CAPABILITY_LINE);
        plotInfoMax.setFillColor("#FFFFFF");
        plotInfoMax.setLineWidth(2);

        ChartSeries plotInfoOverload = createPlotinfoFromDurations(getOverload(data));
        plotInfoOverload.setFillColor(COLOR_OVERLOAD);
        plotInfoOverload.setLineWidth(0);

        return new ChartSeries[] { plotInfoOverload, plotInfoMax, plotInfoLoad };
    }

    protected abstract ILoadChartData getDataOn(Interval interval);

    protected LocalDate getStart(LocalDate explicitlySpecifiedStart, Interval interval) {
        return explicitlySpecifiedStart == null
                ? interval.getStart()
                : Collections.max(asList(explicitlySpecifiedStart, interval.getStart()));
    }

    @SuppressWarnings("unchecked")
    protected LocalDate getEnd(LocalDate explicitlySpecifiedEnd, Interval interval) {
        return explicitlySpecifiedEnd == null
                ? interval.getFinish()
                : Collections.min(asList(explicitlySpecifiedEnd, interval.getFinish()));
    }

    private SortedMap<LocalDate, EffortDuration> getLoad(ILoadChartData data) {
        return groupAsNeededByZoom(data.getLoad());
    }

    private SortedMap<LocalDate, EffortDuration> getOverload(ILoadChartData data) {
        return groupAsNeededByZoom(data.getOverload());
    }

    private SortedMap<LocalDate, EffortDuration> getCalendarMaximumAvailability(ILoadChartData data) {
        return groupAsNeededByZoom(data.getAvailability());
    }

}
