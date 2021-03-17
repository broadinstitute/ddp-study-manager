package org.broadinstitute.dsm.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.Data;
import lombok.NonNull;
import org.broadinstitute.dsm.db.dto.participant.data.ParticipantDataDto;

@Data
public class NewParticipantData {

    private long dataId;
    private String ddpParticipantId;
    private long ddpInstanceId;
    private String fieldTypeId;
    private Map<String, String> data;

    public NewParticipantData() {}

    public NewParticipantData(long participantDataId, String ddpParticipantId, long ddpInstanceId, String fieldTypeId,
                              Map<String, String> data) {
        this.dataId = participantDataId;
        this.ddpParticipantId = ddpParticipantId;
        this.ddpInstanceId = ddpInstanceId;
        this.fieldTypeId = fieldTypeId;
        this.data = data;
    }

    public static NewParticipantData parseDto(@NonNull ParticipantDataDto participantDataDto) {
        return new NewParticipantData(
                participantDataDto.getParticipantDataId(),
                participantDataDto.getDdpParticipantId(),
                participantDataDto.getDdpInstanceId(),
                participantDataDto.getFieldTypeId(),
                new Gson().fromJson(participantDataDto.getData(), new TypeToken<Map<String, String>>() {}.getType())
        );
    }

    public static List<NewParticipantData> parseDtoList(@NonNull List<ParticipantDataDto> participantDataDtoList) {
        List<NewParticipantData> participantData = new ArrayList<>();
        participantDataDtoList.forEach(dto -> participantData.add(new NewParticipantData(
                dto.getParticipantDataId(),
                dto.getDdpParticipantId(),
                dto.getDdpInstanceId(),
                dto.getFieldTypeId(),
                new Gson().fromJson(dto.getData(), new TypeToken<Map<String, String>>() {}.getType())
        )));
        return participantData;
    }
}
