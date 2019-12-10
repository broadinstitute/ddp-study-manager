package org.broadinstitute.dsm.model;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class DashboardInformation {

    private static final Logger logger = LoggerFactory.getLogger(DashboardInformation.class);

    public static final String TISSUE_REVIEW = "review";
    public static final String TISSUE_NO = "no";
    public static final String TISSUE_HOLD = "hold";
    public static final String TISSUE_REQUEST = "request";
    public static final String TISSUE_RETURN = "returned";


    private String ddpName;

    private List<KitCounter> sentCounters;
    private List<KitCounter> receivedCounters;
    private List<NameValue> deactivatedCounters;
    private List<NameValue> unsentExpressKits;
    private List<KitDDPSummary> kits;
    private Map<String, Integer> dashboardValues;
    private Map<String, Integer> dashboardValuesDetailed;
    private Map<String, Integer> dashboardValuesPeriod;
    private Map<String, Integer> dashboardValuesPeriodDetailed;

    public DashboardInformation(List<NameValue> unsentExpressKits, List<KitDDPSummary> kits) {
        this.unsentExpressKits = unsentExpressKits;
        this.kits = kits;
    }

    public DashboardInformation(Map<String, Integer> dashboardValues, Map<String, Integer> dashboardValuesDetailed,
                                Map<String, Integer> dashboardValuesPeriod, Map<String, Integer> dashboardValuesPeriodDetailed) {
        this.dashboardValues = dashboardValues;
        this.dashboardValuesDetailed = dashboardValuesDetailed;
        this.dashboardValuesPeriod = dashboardValuesPeriod;
        this.dashboardValuesPeriodDetailed = dashboardValuesPeriodDetailed;
    }

    public DashboardInformation(String ddpName, List<KitCounter> sentCounters,
                                List<KitCounter> receivedCounters,
                                List<NameValue> deactivatedCounters) {
        this.ddpName = ddpName;
        this.sentCounters = sentCounters;
        this.receivedCounters = receivedCounters;
        this.deactivatedCounters = deactivatedCounters;
    }

    public static class KitCounter {
        private String kitType;
        private ArrayList<NameValue> counters;

        public KitCounter(String kitType, ArrayList<NameValue> counters) {
            this.kitType = kitType;
            this.counters = counters;
        }
    }

//    public static void countMedicalRecords(ParticipantD participant, ResultSet rs, DDPInstance instance, long start, long end) throws SQLException {
//        boolean duplicate = rs.getBoolean(DBConstants.DUPLICATE);
    //        boolean unableObtain = rs.getBoolean(DBConstants.MR_UNABLE_OBTAIN);
//        boolean mrProblem = rs.getBoolean(DBConstants.MR_PROBLEM);
//        boolean international = rs.getBoolean(DBConstants.INTERNATIONAL);
//        boolean crRequired = rs.getBoolean(DBConstants.CR_REQUIRED);
//        boolean fuRequired = rs.getBoolean(DBConstants.FOLLOWUP_REQUIRED);
//        if (!MedicalRecordUtil.NOT_SPECIFIED.equals(rs.getString(DBConstants.TYPE))) {
//            //count all cr required
//            if (crRequired) {
//                participant.setPaperCRRequired(incrementCounter(participant.getPaperCRRequired()));
//            }
//            if(fuRequired){
//                participant.setFollowupRequiredMedicalRecord(incrementCounter(participant.getFollowupRequiredMedicalRecord()));
//            }
//            //count all mr with problem
//            if (mrProblem) {
//                participant.setMedicalRecordWithProblem(incrementCounter(participant.getMedicalRecordWithProblem()));
//            }
//
//            //count duplicate and unable to obtain mr
//            if (duplicate || unableObtain) {
//                if (duplicate) {
//                    participant.setDuplicateMedicalRecord(incrementCounter(participant.getDuplicateMedicalRecord()));
//                }
//                if (unableObtain) {
//                    participant.setUnableToObtainMedicalRecord(incrementCounter(participant.getUnableToObtainMedicalRecord()));
//                }
//            }
//            else {
//                //count status of mr only if mr is not duplicate or unable to obtain
//                String faxSent = rs.getString(DBConstants.FAX_SENT);
//                String mrReceived = rs.getString(DBConstants.MR_RECEIVED);
//                //get total number of all requested
//                if (StringUtils.isNotBlank(faxSent)) {
//                    participant.setRequestedMedicalRecord(incrementCounter(participant.getRequestedMedicalRecord()));
//                    //get count for the selected period of time
//                    if (start != -1 && end != -1) {
//                        long sentDate = SystemUtil.getLongFromDateString(faxSent);
//                        if (start <= sentDate && sentDate <= end) {
//                            participant.setRequestedMedicalRecordInPeriod(incrementCounter(participant.getRequestedMedicalRecordInPeriod()));
//                        }
//                    }
//                }
//
//                //count received mr
//                if (StringUtils.isNotBlank(mrReceived)) {
//                    participant.setReceivedMedicalRecord(incrementCounter(participant.getReceivedMedicalRecord()));
//                    //get count for the selected period of time
//                    if (start != -1 && end != -1) {
//                        long sentDate = SystemUtil.getLongFromDateString(mrReceived);
//                        if (start <= sentDate && sentDate <= end) {
//                            participant.setReceivedMedicalRecordInPeriod(incrementCounter(participant.getReceivedMedicalRecordInPeriod()));
//                        }
//                    }
//                }
//                //count sent mr which are not yet received
//                else if (StringUtils.isNotBlank(faxSent)) {
//                    participant.setSentMedicalRecordRequest(incrementCounter(participant.getSentMedicalRecordRequest()));
//                    //get count for the selected period of time
//                    if (start != -1 && end != -1) {
//                        long sentDate = SystemUtil.getLongFromDateString(faxSent);
//                        if (start <= sentDate && sentDate <= end) {
//                            participant.setSentMedicalRecordRequestInPeriod(incrementCounter(participant.getSentMedicalRecordRequestInPeriod()));
//                        }
//                    }
//                    if (instance.getDaysMrAttentionNeeded() != 0) {
//                        if (SystemUtil.getDaysTillNow(faxSent) > instance.getDaysMrAttentionNeeded()) {
//                            participant.setMrNeedsAttention(true);
//                        }
//                    }
//                }
//                else {
//                    //count international mr not as new mr
//                    if (international) {
//                        participant.setInternationalMedicalRecord(incrementCounter(participant.getInternationalMedicalRecord()));
//                    }
//                    //count new mr
//                    else {
//                        participant.setNewMedicalRecord(incrementCounter(participant.getNewMedicalRecord()));
//                    }
//                }
//            }
//            countFollowUpMedicalRecords(participant, rs, instance, start, end);
//        }
//    }
//
//    private static void countFollowUpMedicalRecords(Participant participant, ResultSet rs, DDPInstance instance, long start, long end) throws SQLException {
//        FollowUp[] followUps = new Gson().fromJson(rs.getString(DBConstants.FOLLOW_UP_REQUESTS), FollowUp[].class);
//        if (!MedicalRecordUtil.NOT_SPECIFIED.equals(rs.getString(DBConstants.TYPE)) && followUps != null) {
//            //count all followups
//            for (FollowUp followUp : followUps) {
//                String received = followUp.getFReceived();
//                String sent1 = followUp.getFRequest1();
//                String sent2 = followUp.getFRequest2();
//                String sent3 = followUp.getFRequest3();
//                //get total number of all requested
//                if (StringUtils.isNotBlank(sent1)) {
//                    participant.setFollowedUpMedicalRecord((incrementCounter(participant.getFollowedUpMedicalRecord())));
//                    if (start != -1 && end != -1) {
//                        if (StringUtils.isNotBlank(sent1)) {
//                            long sentDate = SystemUtil.getLongFromDateString(sent1);
//                            if (start <= sentDate && sentDate <= end) {
//                                participant.setFollowedUpMedicalRecordInPeriod(incrementCounter(participant.getFollowedUpMedicalRecordInPeriod()));
//                            }
//                        }
////                        if (StringUtils.isNotBlank(sent2)) {
////                            long sentDate = SystemUtil.getLongFromDateString(sent2);
////                            if (start <= sentDate && sentDate <= end) {
////                                participant.setFollowedUpMedicalRecordInPeriod(incrementCounter(participant.getFollowedUpMedicalRecordInPeriod()));
////                            }
////                        }
////                        if (StringUtils.isNotBlank(sent3)) {
////                            long sentDate = SystemUtil.getLongFromDateString(sent3);
////                            if (start <= sentDate && sentDate <= end) {
////                                participant.setFollowedUpMedicalRecordInPeriod(incrementCounter(participant.getFollowedUpMedicalRecordInPeriod()));
////                            }
////                        }
//                    }
//                }
//                if (StringUtils.isNotBlank(received)) {
//                    participant.setReceivedFollowedUpMedicalRecord((incrementCounter(participant.getReceivedFollowedUpMedicalRecord())));
//                    long sentDate = SystemUtil.getLongFromDateString(received);
//                    if (start <= sentDate && sentDate <= end) {
//                        participant.setReceivedFollowedUpMedicalRecordInPeriod(incrementCounter(participant.getReceivedFollowedUpMedicalRecordInPeriod()));
//                    }
//                }
//
//            }
//        }
//    }
//
//    public static void countTissues(Participant participant, ResultSet rs, DDPInstance instance, long start, long end) throws SQLException {
//        boolean deleted = rs.getBoolean(DBConstants.DELETED);
//        String tissueProblemOption = rs.getString(DBConstants.TISSUE_PROBLEM_OPTION);
//        boolean problem = StringUtils.isNotBlank(tissueProblemOption);
//        //only count if oncHist was not deleted!
//        if (!deleted) {
//            //count all mr with problem
//            if (problem) {
//                participant.setTissueWithProblem(incrementCounter(participant.getTissueWithProblem()));
//            }
//            //count unable to obtain
//            else {
//                //count status of oncHist only if oncHist is not unable to obtain
//                String faxSent = rs.getString(DBConstants.DDP_ONC_HISTORY_DETAIL_ALIAS + "." + DBConstants.FAX_SENT);
//                String tissueReceived = rs.getString(DBConstants.TISSUE_RECEIVED);
//                String request = rs.getString(DBConstants.REQUEST);
//                //                get total number of all requested
//                if (StringUtils.isNotBlank(faxSent)) {
//                    participant.setRequestedTissue(incrementCounter(participant.getRequestedTissue()));
//                    //get count for the selected period of time
//                    if (start != -1 && end != -1) {
//                        long sentDate = SystemUtil.getLongFromDateString(faxSent);
//                        if (start <= sentDate && sentDate <= end) {
//                            participant.setRequestedTissueInPeriod(incrementCounter(participant.getRequestedTissueInPeriod()));
//                        }
//                    }
//                }
//                if (StringUtils.isNotBlank(tissueReceived)) {
//                    participant.setReceivedTissue(incrementCounter(participant.getReceivedTissue()));
//                    //get count for the selected period of time
//                    if (start != -1 && end != -1) {
//                        long sentDate = SystemUtil.getLongFromDateString(tissueReceived);
//                        if (start <= sentDate && sentDate <= end) {
//                            participant.setReceivedTissueInPeriod(incrementCounter(participant.getReceivedTissueInPeriod()));
//                        }
//                    }
//                }
//                //count sent oncHist which are not yet received
//                else if (StringUtils.isNotBlank(faxSent)) {
//                    participant.setSentTissueRequest(incrementCounter(participant.getSentTissueRequest()));
//                    //get count for the selected period of time
//                    if (start != -1 && end != -1) {
//                        long sentDate = SystemUtil.getLongFromDateString(faxSent);
//                        if (start <= sentDate && sentDate <= end) {
//                            participant.setSentTissueRequestInPeriod(incrementCounter(participant.getSentTissueRequestInPeriod()));
//                        }
//                    }
//                    if (instance.getDaysTissueAttentionNeeded() != 0) {
//                        if (SystemUtil.getDaysTillNow(faxSent) > instance.getDaysTissueAttentionNeeded()) {
//                            participant.setTissueNeedsAttention(true);
//                        }
//                    }
//                }
//                if (TISSUE_REVIEW.equals(request)) {
//                    participant.setTissueRequestNeedReview(incrementCounter(participant.getTissueRequestNeedReview()));
//                }
//                else if (TISSUE_NO.equals(request)) {
//                    participant.setDoNotRequestTissue(incrementCounter(participant.getDoNotRequestTissue()));
//                }
//                else if (TISSUE_HOLD.equals(request)) {
//                    participant.setHoldTissueRequest(incrementCounter(participant.getHoldTissueRequest()));
//                }
//                else if (TISSUE_REQUEST.equals(request)) {
//                    participant.setRequestTissue(incrementCounter(participant.getRequestTissue()));
//                }
//
//            }
//        }
//    }
//
//    public static void countTissuesReturned(Participant participant, ResultSet rs,
//                                            long start, long end) throws SQLException {
//        boolean deleted = rs.getBoolean(DBConstants.DELETED);
//        String tissue_problem_option = rs.getString(DBConstants.TISSUE_PROBLEM_OPTION);
//        boolean problem = StringUtils.isNotBlank(tissue_problem_option);
//        if (!deleted && !problem) {
//            //count only if there wasn't any problems with it
//            String tissueReturned = rs.getString(DBConstants.TISSUE_RETURN_DATE);
//            //count returned Tissue
//            if (StringUtils.isNotBlank(tissueReturned)) {
//                participant.setReturnedTissue(incrementCounter(participant.getReturnedTissue()));
//                //get count for the selected period of time
//                if (start != -1 && end != -1) {
//                    long returnDate = SystemUtil.getLongFromDateString(tissueReturned);
//                    if (start <= returnDate && returnDate <= end) {
//                        participant.setTissueReturnedInPeriod(incrementCounter(participant.getTissueReturnedInPeriod()));
//                    }
//                }
//            }
//        }
//    }
//
//    public static void dashboardMedicalRecordCount(@NonNull SummaryMedicalRecordTissue summaryMedicalRecordTissue, Participant participant) {
//        if (participant != null) {
//            //counts for medical record
//            if (participant.getSentMedicalRecordRequest() != 0) {
//                summaryMedicalRecordTissue.setFaxSentParticipants(incrementCounter(summaryMedicalRecordTissue.getFaxSentParticipants()));
//                summaryMedicalRecordTissue.setFaxSentInstitutions(summaryMedicalRecordTissue.getFaxSentInstitutions() + participant.getSentMedicalRecordRequest());
//            }
//            if (participant.getReceivedMedicalRecord() != 0) {
//                summaryMedicalRecordTissue.setMrReceivedParticipants(incrementCounter(summaryMedicalRecordTissue.getMrReceivedParticipants()));
//                summaryMedicalRecordTissue.setMrReceivedInstitutions(summaryMedicalRecordTissue.getMrReceivedInstitutions() + participant.getReceivedMedicalRecord());
//            }
//            if (participant.getRequestedMedicalRecord() != 0) {
//                summaryMedicalRecordTissue.setMrRequestedParticipants(incrementCounter(summaryMedicalRecordTissue.getMrRequestedParticipants()));
//                summaryMedicalRecordTissue.setMrRequestedInstitutions(summaryMedicalRecordTissue.getMrRequestedInstitutions() + participant.getRequestedMedicalRecord());
//            }
//            if (participant.getDuplicateMedicalRecord() != 0) {
//                summaryMedicalRecordTissue.setDuplicateParticipants(incrementCounter(summaryMedicalRecordTissue.getDuplicateParticipants()));
//                summaryMedicalRecordTissue.setDuplicateInstitutions(summaryMedicalRecordTissue.getDuplicateInstitutions() + participant.getDuplicateMedicalRecord());
//            }
//            if (participant.getMedicalRecordWithProblem() != 0) {
//                summaryMedicalRecordTissue.setProblemMRParticipants(incrementCounter(summaryMedicalRecordTissue.getProblemMRParticipants()));
//                summaryMedicalRecordTissue.setProblemMRInstitutions(summaryMedicalRecordTissue.getProblemMRInstitutions() + participant.getMedicalRecordWithProblem());
//            }
//            if (participant.getInternationalMedicalRecord() != 0) {
//                summaryMedicalRecordTissue.setInternationalParticipants(incrementCounter(summaryMedicalRecordTissue.getInternationalParticipants()));
//                summaryMedicalRecordTissue.setInternationalInstitutions(summaryMedicalRecordTissue.getInternationalInstitutions() + participant.getInternationalMedicalRecord());
//            }
//            if (participant.getFollowupRequiredMedicalRecord() != 0) {
//                summaryMedicalRecordTissue.setFollowupReqMRForParticipants(incrementCounter(summaryMedicalRecordTissue.getFollowupReqMRForParticipants()));
//                summaryMedicalRecordTissue.setFollowupReqMRForInstitutions(summaryMedicalRecordTissue.getFollowupReqMRForInstitutions() + participant.getFollowupRequiredMedicalRecord() );
//            }
//            if (participant.getUnableToObtainMedicalRecord() != 0) {
//                summaryMedicalRecordTissue.setUnableToObtainMRForParticipants(incrementCounter(summaryMedicalRecordTissue.getUnableToObtainMRForParticipants()));
//                summaryMedicalRecordTissue.setUnableToObtainMRForInstitutions(summaryMedicalRecordTissue.getUnableToObtainMRForInstitutions() + participant.getUnableToObtainMedicalRecord());
//            }
//            if (participant.getPaperCRRequired() != 0) {
//                summaryMedicalRecordTissue.setPaperCRParticipants(incrementCounter(summaryMedicalRecordTissue.getPaperCRParticipants()));
//                summaryMedicalRecordTissue.setPaperCRInstitutions(summaryMedicalRecordTissue.getPaperCRInstitutions() + participant.getPaperCRRequired());
//            }
//            if (participant.getSentMedicalRecordRequestInPeriod() != 0) {
//                summaryMedicalRecordTissue.setFaxSentParticipantsPeriod(incrementCounter(summaryMedicalRecordTissue.getFaxSentParticipantsPeriod()));
//                summaryMedicalRecordTissue.setFaxSentInstitutionsPeriod(summaryMedicalRecordTissue.getFaxSentInstitutionsPeriod() + participant.getSentMedicalRecordRequestInPeriod());
//            }
//            if (participant.getReceivedMedicalRecordInPeriod() != 0) {
//                summaryMedicalRecordTissue.setMrReceivedParticipantsPeriod(incrementCounter(summaryMedicalRecordTissue.getMrReceivedParticipantsPeriod()));
//                summaryMedicalRecordTissue.setMrReceivedInstitutionsPeriod(summaryMedicalRecordTissue.getMrReceivedInstitutionsPeriod() + participant.getReceivedMedicalRecordInPeriod());
//            }
//            if (participant.getRequestedMedicalRecordInPeriod() != 0) {
//                summaryMedicalRecordTissue.setMrRequestedParticipantsPeriod(incrementCounter(summaryMedicalRecordTissue.getMrRequestedParticipantsPeriod()));
//                summaryMedicalRecordTissue.setMrRequestedInstitutionsPeriod(summaryMedicalRecordTissue.getMrRequestedInstitutionsPeriod() + participant.getRequestedMedicalRecordInPeriod());
//            }
//            if (participant.getFollowedUpMedicalRecord() != 0) {
//                summaryMedicalRecordTissue.setMrFollowUpSentParticipant(incrementCounter(summaryMedicalRecordTissue.getMrFollowUpSentParticipant()));
//                summaryMedicalRecordTissue.setMrFollowUpSentInstitutions(summaryMedicalRecordTissue.getMrFollowUpSentInstitutions() + participant.getFollowedUpMedicalRecord());
//
//            }
//            if (participant.getFollowedUpMedicalRecordInPeriod() != 0) {
//                summaryMedicalRecordTissue.setMrFollowUpSentParticipantInPeriod(incrementCounter(summaryMedicalRecordTissue.getMrFollowUpSentParticipantInPeriod()));
//                summaryMedicalRecordTissue.setMrFollowUpSentInstitutionsInPeriod(summaryMedicalRecordTissue.getMrFollowUpSentInstitutionsInPeriod() + participant.getFollowedUpMedicalRecordInPeriod());
//            }
//            if (participant.getReceivedFollowedUpMedicalRecord() != 0) {
//                summaryMedicalRecordTissue.setMrReceivedFollowUpParticipant(incrementCounter(summaryMedicalRecordTissue.getMrReceivedFollowUpParticipant()));
//                summaryMedicalRecordTissue.setMrReceivedFollowUpInstitutions(summaryMedicalRecordTissue.getMrReceivedFollowUpInstitutions() + participant.getReceivedFollowedUpMedicalRecord());
//            }
//            if (participant.getReceivedFollowedUpMedicalRecordInPeriod() != 0) {
//                summaryMedicalRecordTissue.setMrReceivedFollowUpParticipantInPeriod(incrementCounter(summaryMedicalRecordTissue.getMrReceivedFollowUpParticipantInPeriod()));
//                summaryMedicalRecordTissue.setMrReceivedFollowUpInstitutionsInPeriod(summaryMedicalRecordTissue.getMrReceivedFollowUpInstitutionsInPeriod() + participant.getReceivedFollowedUpMedicalRecordInPeriod());
//
//            }
//        }
//    }
//
//    public static void dashboardTissueCount(@NonNull SummaryMedicalRecordTissue summaryMedicalRecordTissue, Participant participant) {
//        if (participant != null) {
//            //counts for tissue
//            if (participant.getSentTissueRequest() != 0) {
//                summaryMedicalRecordTissue.setFaxSentTissueParticipants(incrementCounter(summaryMedicalRecordTissue.getFaxSentTissueParticipants()));
//                summaryMedicalRecordTissue.setFaxSentTissue(summaryMedicalRecordTissue.getFaxSentTissue() + participant.getSentTissueRequest());
//            }
//            if (participant.getReceivedTissue() != 0) {
//                summaryMedicalRecordTissue.setReceivedTissueParticipants(incrementCounter(summaryMedicalRecordTissue.getReceivedTissueParticipants()));
//                summaryMedicalRecordTissue.setReceivedTissue(summaryMedicalRecordTissue.getReceivedTissue() + participant.getReceivedTissue());
//            }
//            if (participant.getRequestedTissue() != 0) {
//                summaryMedicalRecordTissue.setRequestedTissueParticipants(incrementCounter(summaryMedicalRecordTissue.getRequestedTissueParticipants()));
//                summaryMedicalRecordTissue.setRequestedTissue(summaryMedicalRecordTissue.getRequestedTissue() + participant.getRequestedTissue());
//            }
//            if (participant.getTissueWithProblem() != 0) {
//                summaryMedicalRecordTissue.setTissueWithProblemForParticipants(incrementCounter(summaryMedicalRecordTissue.getTissueWithProblemForParticipants()));
//                summaryMedicalRecordTissue.setTissueWithProblem(summaryMedicalRecordTissue.getTissueWithProblem() + participant.getTissueWithProblem());
//            }
//            if (participant.getRequestTissue() != 0) {
//                summaryMedicalRecordTissue.setTissueToRequestParticipants(incrementCounter(summaryMedicalRecordTissue.getTissueToRequestParticipants()));
//                summaryMedicalRecordTissue.setTissueToRequest(summaryMedicalRecordTissue.getTissueToRequest() + participant.getRequestTissue());
//            }
//            if (participant.getSentTissueRequestInPeriod() != 0) {
//                summaryMedicalRecordTissue.setFaxSentTissueParticipantsPeriod(incrementCounter(summaryMedicalRecordTissue.getFaxSentTissueParticipantsPeriod()));
//                summaryMedicalRecordTissue.setFaxSentTissuePeriod(summaryMedicalRecordTissue.getFaxSentTissuePeriod() + participant.getSentTissueRequestInPeriod());
//            }
//            if (participant.getReceivedTissueInPeriod() != 0) {
//                summaryMedicalRecordTissue.setReceivedTissueParticipantsPeriod(incrementCounter(summaryMedicalRecordTissue.getReceivedTissueParticipantsPeriod()));
//                summaryMedicalRecordTissue.setReceivedTissuePeriod(summaryMedicalRecordTissue.getReceivedTissuePeriod() + participant.getReceivedTissueInPeriod());
//            }
//            if (participant.getRequestedTissueInPeriod() != 0) {
//                summaryMedicalRecordTissue.setRequestedTissueParticipantsPeriod(incrementCounter(summaryMedicalRecordTissue.getRequestedTissueParticipantsPeriod()));
//                summaryMedicalRecordTissue.setRequestedTissuePeriod(summaryMedicalRecordTissue.getRequestedTissuePeriod() + participant.getRequestedTissueInPeriod());
//            }
//            if (participant.getReturnedTissue() != 0) {
//                summaryMedicalRecordTissue.setTissueReturnedParticipants(incrementCounter(summaryMedicalRecordTissue.getTissueReturnedParticipants()));
//                summaryMedicalRecordTissue.setTissueReturned(summaryMedicalRecordTissue.getTissueReturned() + participant.getReturnedTissue());
//
//            }
//            if (participant.getTissueReturnedInPeriod() != 0) {
//                summaryMedicalRecordTissue.setTissueReturnedParticipantsPeriod(incrementCounter(summaryMedicalRecordTissue.getTissueReturnedParticipantsPeriod()));
//                summaryMedicalRecordTissue.setTissueReturnedInPeriod(summaryMedicalRecordTissue.getTissueReturnedInPeriod() + participant.getTissueReturnedInPeriod());
//            }
//        }
//    }
//
//    private static int incrementCounter(int previousValue) {
//        return previousValue + 1;
//    }
}
