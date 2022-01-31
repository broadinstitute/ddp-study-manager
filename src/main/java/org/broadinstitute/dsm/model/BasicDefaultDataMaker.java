package org.broadinstitute.dsm.model;

import java.util.List;
import java.util.Optional;

import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.dao.bookmark.BookmarkDao;
import org.broadinstitute.dsm.db.dao.ddp.instance.DDPInstanceDao;
import org.broadinstitute.dsm.db.dao.ddp.participant.ParticipantDataDao;
import org.broadinstitute.dsm.db.dao.settings.FieldSettingsDao;
import org.broadinstitute.dsm.db.dto.settings.FieldSettingsDto;
import org.broadinstitute.dsm.model.defaultvalues.Defaultable;
import org.broadinstitute.dsm.model.elastic.search.ElasticSearchParticipantDto;
import org.broadinstitute.dsm.model.rgp.AutomaticProbandDataCreator;
import org.broadinstitute.dsm.model.settings.field.FieldSettings;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BasicDefaultDataMaker implements Defaultable {
    protected static final Logger logger = LoggerFactory.getLogger(AutomaticProbandDataCreator.class);
    private final FieldSettings fieldSettings = new FieldSettings();
    private final BookmarkDao bookmarkDao = new BookmarkDao();
    private final ParticipantDataDao participantDataDao = new ParticipantDataDao();
    private final DDPInstanceDao ddpInstanceDao = new DDPInstanceDao();
    protected DDPInstance instance;

    private boolean setDefaultProbandData(Optional<ElasticSearchParticipantDto> maybeParticipantESData) {
        if (maybeParticipantESData.isEmpty()) {
            logger.warn("Could not create proband/self data, participant ES data is null");
            return false;
        }
        List<FieldSettingsDto> fieldSettingsDtosByOptionAndInstanceId =
                FieldSettingsDao.of().getOptionAndRadioFieldSettingsByInstanceId(Integer.parseInt(instance.getDdpInstanceId()));

        return maybeParticipantESData
                .map(elasticSearch -> extractAndInsertProbandFromESData(instance, elasticSearch, fieldSettingsDtosByOptionAndInstanceId))
                .orElse(false);
    }

    protected abstract boolean extractAndInsertProbandFromESData(DDPInstance instance, ElasticSearchParticipantDto esData,
                                                                 List<FieldSettingsDto> fieldSettingsDtosByOptionAndInstanceId);

    @Override
    public boolean generateDefaults(String studyGuid, String participantId) {
        String esParticipantIndex = ddpInstanceDao.getEsParticipantIndexByStudyGuid(studyGuid)
                .orElse("");
        Optional<ElasticSearchParticipantDto> maybeParticipantESDataByParticipantId =
                ElasticSearchUtil.getParticipantESDataByParticipantId(esParticipantIndex, participantId);
        instance = DDPInstance.getDDPInstance(studyGuid);
        return setDefaultProbandData(maybeParticipantESDataByParticipantId);
    }
}
