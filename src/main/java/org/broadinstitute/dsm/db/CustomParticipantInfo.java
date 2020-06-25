package org.broadinstitute.dsm.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomParticipantInfo {

    private static final Logger logger = LoggerFactory.getLogger(CustomParticipantInfo.class);

    private static final String SQL_SELECT_CUSTOM_PARTICIPANT_INFO = "";

    private String ddpParticipantId;

    private final String eligibility;

    private final String drFirstName;

    private final String drLastName;

    private final String drSuffix;

    private final String drAddress;
    private final String drPhoneNumber;

    private final String drEmail;
    private final Boolean careAtBrisbaneAustralia;
    private final Boolean careAtShebaMedicalCenterIsrael;
    private final Boolean careAtJohnsHopkins;
    private final Boolean careAtJohannWolfgangGoetheUniversityGermany;
    private final Boolean careAtNottinghamCityHospitalUK;
    private final String ageOfDeath;
    private final String dateOfDeath;
    private final String causeOfDeath;
    private final String deathNotes;

    private final String genomeStudyCollaboratorId;
    private final String consentedForGenomeStudy;
    private final String genomeStudyDateOfConsent;
    private final String ethnicity;
    private final String hasASibling;
    private final String genomeStudyShippingAddress;
    private final String sampleKitStatus;
    private final String salivaBarCode;
    private final String sampleKitTrackingNumber;
    private final String sampleKitShippingDate;
    private final String participantReceivedDate;
    private final String sampleKitReceivedBackDate;
    private final String dateOfCompletingSequencing;
    private final String dateOfDataReleasedInRepository;
    private final String genomeStudyNotes;


    public CustomParticipantInfo(String eligibility, String drFirstName, String drLastName, String drSuffix,
                                 String drAddress, String drPhoneNumber, String drEmail, String ageOfDeath,
                                 String dateOfDeath, String causeOfDeath, String deathNotes,
                                 Boolean careAtBrisbaneAustralia, Boolean careAtShebaMedicalCenterIsrael,
                                 Boolean careAtJohnsHopkins, Boolean careAtJohannWolfgangGoetheUniversityGermany,
                                 Boolean careAtNottinghamCityHospitalUK,
                                  String genomeStudyCollaboratorId,
                                          String consentedForGenomeStudy,
                                          String genomeStudyDateOfConsent,
                                          String Ethnicity,
                                          String hasASibling,
                                          String genomeStudyShippingAddress,
                                          String sampleKitStatus,
                                          String salivaBarCode,
                                          String sampleKitTrackingNumber,
                                          String sampleKitShippingDate,
                                          String participantReceivedDate,
                                          String sampleKitReceivedBackDate,
                                          String dateOfCompletingSequencing,
                                          String dateOfDataReleasedInRepository,
                                          String genomeStudyNotes
                                 ) {
        this.eligibility = eligibility;
        this.drFirstName = drFirstName;
        this.drLastName = drLastName;
        this.drSuffix = drSuffix;
        this.drAddress = drAddress;
        this.drPhoneNumber = drPhoneNumber;
        this.drEmail = drEmail;
        this.ageOfDeath = ageOfDeath;
        this.dateOfDeath = dateOfDeath;
        this.causeOfDeath = causeOfDeath;
        this.deathNotes = deathNotes;
        this.careAtBrisbaneAustralia = careAtBrisbaneAustralia;
        this.careAtShebaMedicalCenterIsrael = careAtShebaMedicalCenterIsrael;
        this.careAtJohnsHopkins = careAtJohnsHopkins;
        this.careAtJohannWolfgangGoetheUniversityGermany = careAtJohannWolfgangGoetheUniversityGermany;
        this.careAtNottinghamCityHospitalUK = careAtNottinghamCityHospitalUK;
        this.genomeStudyCollaboratorId = genomeStudyCollaboratorId;
        this.consentedForGenomeStudy =  consentedForGenomeStudy;
        this.genomeStudyDateOfConsent = genomeStudyDateOfConsent;
        this.ethnicity = Ethnicity;
        this.hasASibling = hasASibling;
        this.genomeStudyShippingAddress = genomeStudyShippingAddress;
        this.sampleKitStatus = sampleKitStatus;
        this.salivaBarCode = salivaBarCode;
        this.sampleKitTrackingNumber = sampleKitTrackingNumber;
        this.sampleKitShippingDate = sampleKitShippingDate;
        this.participantReceivedDate = participantReceivedDate;
        this.sampleKitReceivedBackDate = sampleKitReceivedBackDate;
        this.dateOfCompletingSequencing = dateOfCompletingSequencing;
        this.dateOfDataReleasedInRepository = dateOfDataReleasedInRepository;
        this.genomeStudyNotes = genomeStudyNotes;
    }
}
