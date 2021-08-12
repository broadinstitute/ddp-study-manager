package org.broadinstitute.dsm.route.participant;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.db.dao.ddp.instance.DDPInstanceDao;
import org.broadinstitute.dsm.db.dto.ddp.instance.DDPInstanceDto;
import org.broadinstitute.dsm.model.elasticsearch.ElasticSearch;
import org.broadinstitute.dsm.security.RequestHandler;
import org.broadinstitute.dsm.statics.RoutePath;
import spark.QueryParamsMap;
import spark.Request;
import spark.Response;

public class ParticipantsSizeRoute extends RequestHandler {


    @Override
    protected Object processRequest(Request request, Response response, String userId) throws Exception {

        QueryParamsMap queryParamsMap = request.queryMap();

        String realm = queryParamsMap.get(RoutePath.REALM).value();

        if (StringUtils.isBlank(realm)) throw new IllegalArgumentException("realm cannot be empty");

        DDPInstanceDto ddpInstanceDto = new DDPInstanceDao().getDDPInstanceByInstanceName(realm).orElseThrow();

        ElasticSearch elasticSearch = new ElasticSearch();

        return elasticSearch.getParticipantsSize(ddpInstanceDto.getEsParticipantIndex());
    }
}
