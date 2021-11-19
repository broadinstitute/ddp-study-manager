package org.broadinstitute.dsm.db.dto.onchistory;

public class OncHistoryDto {

    private int oncHistoryId;
    private int participantId;
    private String created;
    private String reviewed;

    public OncHistoryDto(int oncHistoryId, int participantId, String created, String reviewed) {
        this.oncHistoryId = oncHistoryId;
        this.participantId = participantId;
        this.created = created;
        this.reviewed = reviewed;
    }
}
