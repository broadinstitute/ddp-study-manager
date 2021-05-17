package org.broadinstitute.dsm.route.dynamicdashboard;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.dao.dashboardsettings.DashboardSettingsDao;
import org.broadinstitute.dsm.db.dao.ddp.instance.DDPInstanceDao;
import org.broadinstitute.dsm.db.dto.dashboardsettings.DashboardSettingsDto;
import org.broadinstitute.dsm.model.dynamicdashboard.DisplayType;
import org.broadinstitute.dsm.model.dynamicdashboard.FilterType;
import org.broadinstitute.dsm.model.dynamicdashboard.Statistic;
import org.broadinstitute.dsm.model.dynamicdashboard.StatisticsCreator;
import org.broadinstitute.dsm.model.dynamicdashboard.StatisticFor;
import org.broadinstitute.dsm.model.dynamicdashboard.StatisticPayload;
import org.broadinstitute.dsm.model.dynamicdashboard.StatisticResult;
import org.broadinstitute.dsm.security.RequestHandler;
import spark.QueryParamsMap;
import spark.Request;
import spark.Response;

public class StatisticDataRoute extends RequestHandler {


    @Override
    protected Object processRequest(Request request, Response response, String userId) throws Exception {
        QueryParamsMap queryParamsMap = request.queryMap();
        String realm = queryParamsMap.get("realm").value();
        if (realm == null) {
            throw new RuntimeException("Realm is not provided");
        }
        int from = Integer.parseInt(queryParamsMap.get("from").value());
        int to = Integer.parseInt(queryParamsMap.get("to").value());
        DDPInstance ddpInstanceByRealm = DDPInstanceDao.getDDPInstanceByRealm(realm);
        DashboardSettingsDao dashboardSettingsDao = new DashboardSettingsDao();
        List<DashboardSettingsDto> dashboardSettingsByInstanceId =
                dashboardSettingsDao.getDashboardSettingsByInstanceId(Integer.parseInt(ddpInstanceByRealm.getDdpInstanceId()));

        List<StatisticPayload> statisticPayloads = new ArrayList<>();
        for (DashboardSettingsDto dashboardSetting: dashboardSettingsByInstanceId) {
            StatisticPayload.Builder statisticPayloadBuilder = extractStatisticPayloadFromDashboardSetting(dashboardSetting);
            StatisticPayload statisticPayload = statisticPayloadBuilder
                    .withFrom(from)
                    .withTo(to)
                    .withRealm(realm)
                    .build();
            statisticPayloads.add(statisticPayload);
        }
        List<StatisticResult> result = new ArrayList<>();
        Statistic statistic;
        for (StatisticPayload statisticPayload: statisticPayloads) {
            StatisticsCreator statisticFactory = new StatisticsCreator();
            statistic = statisticFactory.makeStatistic(statisticPayload);
            result.add(
                    statistic.filter(statisticPayload)
            );
        }
        return result;
    }

    private StatisticPayload.Builder extractStatisticPayloadFromDashboardSetting(DashboardSettingsDto dashboardSetting) {
        DisplayType displayType = DisplayType.valueOf(dashboardSetting.getDisplayType());
        StatisticFor statisticFor = StatisticFor.valueOf(dashboardSetting.getStatisticFor());
        FilterType filterType = FilterType.valueOf(dashboardSetting.getFilterType());
        List possibleValues = new Gson().fromJson(dashboardSetting.getPossibleValues(), List.class);
        String displayText = dashboardSetting.getDisplayText();
        return new StatisticPayload.Builder(displayType, statisticFor, filterType)
                .withDisplayText(displayText)
                .withPossibleValues(possibleValues);
    }
}
