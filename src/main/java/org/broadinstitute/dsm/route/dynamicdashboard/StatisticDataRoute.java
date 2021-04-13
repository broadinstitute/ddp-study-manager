package org.broadinstitute.dsm.route.dynamicdashboard;

import java.util.ArrayList;
import java.util.List;

import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.dao.dashboardsettings.DashboardSettingsDao;
import org.broadinstitute.dsm.db.dao.ddp.instance.DDPInstanceDao;
import org.broadinstitute.dsm.db.dto.dashboardsettings.DashboardSettingsDto;
import org.broadinstitute.dsm.model.dynamicdashboard.DisplayType;
import org.broadinstitute.dsm.model.dynamicdashboard.StatisticFor;
import org.broadinstitute.dsm.model.dynamicdashboard.StatisticPayload;
import org.broadinstitute.dsm.model.dynamicdashboard.StatisticResult;
import org.broadinstitute.dsm.model.dynamicdashboard.counter.Counter;
import org.broadinstitute.dsm.model.dynamicdashboard.counter.CounterResult;
import org.broadinstitute.dsm.model.dynamicdashboard.counter.participant.ParticipantCounter;
import org.broadinstitute.dsm.security.RequestHandler;
import spark.QueryParamsMap;
import spark.Request;
import spark.Response;

public class StatisticDataRoute extends RequestHandler {


    @Override
    protected Object processRequest(Request request, Response response, String userId) throws Exception {
        QueryParamsMap queryParamsMap = request.queryMap();
        String realm = queryParamsMap.get("realm").value();
        DDPInstance ddpInstanceByRealm = DDPInstanceDao.getDDPInstanceByRealm(realm);
        DashboardSettingsDao dashboardSettingsDao = new DashboardSettingsDao();
        List<DashboardSettingsDto> dashboardSettingsByInstanceId =
                dashboardSettingsDao.getDashboardSettingsByInstanceId(Integer.parseInt(ddpInstanceByRealm.getDdpInstanceId()));

        List<StatisticPayload> statisticPayloads = new ArrayList<>();
        for (DashboardSettingsDto dashboardSetting: dashboardSettingsByInstanceId) {
            DisplayType displayType = DisplayType.valueOf(dashboardSetting.getDisplayType());
            StatisticFor statisticFor = StatisticFor.valueOf(dashboardSetting.getStatisticFor());
            String filterType = dashboardSetting.getFilterType();
            statisticPayloads.add(
                    new StatisticPayload(displayType, statisticFor, filterType)
            );
        }
        List<StatisticResult> result = new ArrayList<>();
        Counter counter = new ParticipantCounter();
        for (StatisticPayload statisticPayload: statisticPayloads) {
            result.add(
                    counter.filter(statisticPayload)
            );
        }
        return result;
    }
}
