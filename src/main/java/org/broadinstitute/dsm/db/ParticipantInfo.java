package org.broadinstitute.dsm.db;

import org.broadinstitute.dsm.db.structure.ColumnName;
import org.broadinstitute.dsm.statics.DBConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParticipantInfo {
    private static final Logger logger = LoggerFactory.getLogger(ParticipantInfo.class);

    private static final String SQL_SELECT_PARTICIPANT_INFO = "";

    private String ddpParticipantId;

    private final String firstName;

    private final String lastName;

    private final String middleName;

    private final String suffix;

    private final String title;

    private final String gender;

    private final String dateOfBirth;

    private final String language;

    private final String timeZone;

    private final String phoneH;

    private final String phoneW;

    private final String phoneM;

    private final String email;

    private final String altEmail;

    private final String addressP;

    private final String cityP;

    private final String stateP;

    private final String countryP;

    private final String zipP;

    private final String addressM;

    private final String cityM;

    private  final String stateM;

    private final String countryM;

    private final String zipM;

    private final String dnc;

    private final String dncComment;

    public ParticipantInfo(String ddpParticipantId, String firstName, String lastName, String middleName, String suffix, String title, String gender,
                           String dateOfBirth, String language, String timeZone, String phoneH, String phoneM, String phoneW, String email,
                           String altEmail, String addressP, String cityP, String stateP, String countryP, String zipP,
                           String addressM, String cityM, String stateM, String countryM, String zipM, String dnc, String dncComment){

        this.ddpParticipantId = ddpParticipantId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.middleName = middleName;
        this.suffix = suffix;
        this.title = title;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.language = language;
        this.timeZone = timeZone;
        this.phoneH = phoneH;
        this.phoneM = phoneM;
        this.phoneW = phoneW;
        this.email =email;
        this.altEmail =altEmail;
        this.addressP = addressP;
        this.cityP = cityP;
        this.stateP = stateP;
        this.countryP = countryP;
        this.zipP = zipP;
        this.addressM = addressM;
        this.cityM = cityM;
        this.stateM = stateM;
        this.countryM = countryM;
        this.zipM = zipM;
        this.dnc = dnc;
        this.dncComment = dncComment;
    }


}
