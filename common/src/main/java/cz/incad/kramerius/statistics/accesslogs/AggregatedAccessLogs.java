package cz.incad.kramerius.statistics.accesslogs;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import cz.incad.kramerius.statistics.ReportedAction;
import cz.incad.kramerius.statistics.StatisticReport;
import cz.incad.kramerius.statistics.StatisticsAccessLog;
import cz.incad.kramerius.statistics.StatisticsAccessLogSupport;

import java.io.IOException;

public class AggregatedAccessLogs implements StatisticsAccessLog{


    @Inject
    @Named("database")
    StatisticsAccessLog databaseAccessLog;

    @Inject
    @Named("dnnt")
    StatisticsAccessLog dnntAccessLog;


    public AggregatedAccessLogs() {
    }

    @Override
    public void reportAccess(String pid, String streamName) throws IOException {
        databaseAccessLog.reportAccess(pid,streamName);
        dnntAccessLog.reportAccess(pid, streamName);
    }

    @Override
    public void reportAccess(String pid, String streamName, String actionName) throws IOException {
        databaseAccessLog.reportAccess(pid, streamName, actionName);
        dnntAccessLog.reportAccess(pid, streamName, actionName);
    }

    @Override
    public boolean isReportingAccess(String pid, String streamName) {
        return databaseAccessLog.isReportingAccess(pid, streamName);
    }

    @Override
    public void processAccessLog(ReportedAction reportedAction, StatisticsAccessLogSupport sup) {
        this.databaseAccessLog.processAccessLog(reportedAction, sup);
    }

    @Override
    public StatisticReport[] getAllReports() {
        return this.databaseAccessLog.getAllReports();
    }

    @Override
    public StatisticReport getReportById(String reportId) {
        return this.databaseAccessLog.getReportById(reportId);
    }
}
