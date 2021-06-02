package org.broadinstitute.dsm.route.upstracking;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.db.dao.ups.UPSActivityDao;
import org.broadinstitute.dsm.db.dao.ups.UPSPackageDao;
import org.broadinstitute.dsm.db.dao.ups.UPShipmentDao;
import org.broadinstitute.dsm.db.dto.ups.UPSActivityDto;
import org.broadinstitute.dsm.db.dto.ups.UPSPackageDto;
import org.broadinstitute.dsm.db.dto.ups.UPShipmentDto;
import org.broadinstitute.dsm.model.ups.UPSActivity;
import org.broadinstitute.dsm.security.RequestHandler;
import spark.Request;
import spark.Response;

public class UpsTrackingRoute extends RequestHandler {


    @Override
    protected Object processRequest(Request request, Response response, String userId) throws Exception {
        String dsmKitRequestId = request.queryMap("kitRequestId").value();
        if (StringUtils.isBlank(dsmKitRequestId)) {
            throw new IllegalArgumentException("dsm kit request id is not provided");
        }
        UPShipmentDao upShipmentDao = new UPShipmentDao();
        UPSPackageDao upsPackageDao = new UPSPackageDao();
        UPSActivityDao upsActivityDao = new UPSActivityDao();
        Optional<UPShipmentDto> upshipmentByDsmKitRequestId =
                upShipmentDao.getUpshipmentByDsmKitRequestId(Integer.parseInt(dsmKitRequestId));
        List<UPSActivity> upsActivities = new ArrayList<>();
        upshipmentByDsmKitRequestId.ifPresent(upShipmentDto -> {
            List<UPSPackageDto> upsPackagesByShipmentId = upsPackageDao.getUpsPackageByShipmentId(upShipmentDto.getUpsShipmentId());
            List<Integer> upsPackageIds = upsPackagesByShipmentId.stream()
                    .map(UPSPackageDto::getUpsPackageId)
                    .collect(Collectors.toList());
            List<UPSActivityDto> upsActivitiesByUpsPackageIds = upsActivityDao.getUpsActivitiesByUpsPackageIds(upsPackageIds);
            UPSActivity upsActivity = new UPSActivity();
            upsActivities.addAll(upsActivity.processActivities(upsActivitiesByUpsPackageIds));
        });
        return upsActivities;
    }
}
