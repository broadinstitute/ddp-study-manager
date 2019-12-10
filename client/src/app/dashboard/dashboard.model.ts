import {NameValue} from "../utils/name-value.model";

export class DDPInformation {

  private salivaSent: number = 0;
  private bloodSent: number = 0;

  constructor( public ddpName: string, public sentCounters: Counter, public receivedCounters: Counter, public deactivatedCounters: NameValue,
               public dashboardValues: Object, public dashboardValuesDetailed: Object, public dashboardValuesPeriod: Object, public dashboardValuesPeriodDetailed: Object,
               public unsentExpressKits: Array<NameValue>, public kits: Array<KitDDPSummary> ) {
    this.ddpName = ddpName;
    this.sentCounters = sentCounters;
    this.receivedCounters = receivedCounters;
    this.deactivatedCounters = deactivatedCounters;
    this.dashboardValues = dashboardValues;
    this.dashboardValuesDetailed = dashboardValuesDetailed;
    this.dashboardValuesPeriod = dashboardValuesPeriod;
    this.dashboardValuesPeriodDetailed = dashboardValuesPeriodDetailed;
    this.unsentExpressKits = unsentExpressKits;
    this.kits = kits;
  }

  public unsentExpressName( index: number ): string {
    if (this.unsentExpressKits[ index ] != null && this.unsentExpressKits[ index ].name != null) {
      return this.unsentExpressKits[ index ].name.replace( "_", " " );
    }
    return this.unsentExpressKits[ index ].name;
  }

  static parse( json ): DDPInformation {
    return new DDPInformation( json.ddpName, json.sentCounters, json.receivedCounters, json.deactivatedCounters,
      json.dashboardValues, json.dashboardValuesDetailed, json.dashboardValuesPeriod, json.dashboardValuesPeriodDetailed, json.unsentExpressKits, json.kits );
  }
}

export class KitDDPSummary {
  constructor( public realm: string, public kitType: string, public kitsNoLabel: string, public kitsNoLabelMinDate: number, public kitsQueue: string, public kitsError: string ) {
    this.realm = realm;
    this.kitType = kitType;
    this.kitsNoLabel = kitsNoLabel;
    this.kitsNoLabelMinDate = kitsNoLabelMinDate;
    this.kitsQueue = kitsQueue;
    this.kitsError = kitsError;
  }
}

export class Counter {
  constructor( public kitType: string, public counters: Array<NameValue> ) {
    this.kitType = kitType;
    this.counters = counters;
  }
}

// export class MedicalRecordTissueDashboardInfo {
//   constructor( public summaryKitTypeList: SummaryKitType[], public numberOfParticipants: number,
//                public faxSentParticipants: number, public faxSentParticipantsPeriod: number,
//                public faxSentInstitutions: number, public faxSentInstitutionsPeriod: number,
//                public mrReceivedParticipants: number, public mrReceivedParticipantsPeriod: number,
//                public mrReceivedInstitutions: number, public mrReceivedInstitutionsPeriod: number,
//                public mrRequestedParticipants: number, public mrRequestedParticipantsPeriod: number,
//                public mrRequestedInstitutions: number, public mrRequestedInstitutionsPeriod: number,
//                public internationalParticipants: number, public internationalInstitutions: number,
//                public duplicateParticipants: number, public duplicateInstitutions: number,
//                public problemMRParticipants: number, public problemMRInstitutions: number,
//                public unableToObtainMRForParticipants: number, public unableToObtainMRForInstitutions: number,
//                public paperCRParticipants: number, public paperCRInstitutions: number,
//                public exitedParticipants: number,
//                public faxSentTissue: number, public faxSentTissueParticipants: number,
//                public receivedTissue: number, public receivedTissueParticipants: number,
//                public requestedTissue: number, public requestedTissueParticipants: number,
//                public tissueToRequest: number, public tissueToRequestParticipants: number,
//                public tissueWithProblem: number, public tissueWithProblemForParticipants: number,
//                public faxSentTissuePeriod: number, public faxSentTissueParticipantsPeriod: number,
//                public receivedTissuePeriod: number, public receivedTissueParticipantsPeriod: number,
//                public requestedTissuePeriod: number, public requestedTissueParticipantsPeriod: number,
//                public tissueReturned: number, public tissueReturnedParticipants: number,
//                public tissueReturnedInPeriod: number, public tissueReturnedParticipantsPeriod: number,
//                public mrFollowUpSentParticipant: number,
//                public mrFollowUpSentParticipantInPeriod: number,
//                public mrFollowUpSentInstitutions: number,
//                public mrFollowUpSentInstitutionsInPeriod: number,
//                public mrReceivedFollowUpParticipant: number,
//                public mrReceivedFollowUpParticipantInPeriod: number,
//                public mrReceivedFollowUpInstitutions: number,
//                public mrReceivedFollowUpInstitutionsInPeriod: number,
//                public followupReqMRForParticipants: number, public followupReqMRForInstitutions: number ) {
//     this.summaryKitTypeList = summaryKitTypeList;
//     this.numberOfParticipants = numberOfParticipants;
//     this.faxSentParticipantsPeriod = faxSentParticipantsPeriod;
//     this.faxSentInstitutions = faxSentInstitutions;
//     this.faxSentInstitutionsPeriod = faxSentInstitutionsPeriod;
//     this.mrReceivedParticipants = mrReceivedParticipants;
//     this.mrReceivedParticipantsPeriod = mrReceivedParticipantsPeriod;
//     this.mrReceivedInstitutions = mrReceivedInstitutions;
//     this.mrRequestedParticipants = mrRequestedParticipants;
//     this.mrRequestedParticipantsPeriod = mrRequestedParticipantsPeriod;
//     this.mrRequestedInstitutions = mrRequestedInstitutions;
//     this.mrRequestedInstitutionsPeriod = mrRequestedInstitutionsPeriod;
//     this.mrReceivedInstitutionsPeriod = mrReceivedInstitutionsPeriod;
//     this.internationalParticipants = internationalParticipants;
//     this.internationalInstitutions = internationalInstitutions;
//     this.duplicateParticipants = duplicateParticipants;
//     this.duplicateInstitutions = duplicateInstitutions;
//     this.problemMRParticipants = problemMRParticipants;
//     this.problemMRInstitutions = problemMRInstitutions;
//     this.unableToObtainMRForParticipants = unableToObtainMRForParticipants;
//     this.unableToObtainMRForInstitutions = unableToObtainMRForInstitutions;
//     this.paperCRParticipants = paperCRParticipants;
//     this.paperCRInstitutions = paperCRInstitutions;
//     this.exitedParticipants = exitedParticipants;
//     this.faxSentTissue = faxSentTissue;
//     this.faxSentTissueParticipants = faxSentTissueParticipants;
//     this.receivedTissue = receivedTissue;
//     this.receivedTissueParticipants = receivedTissueParticipants;
//     this.requestedTissue = requestedTissue;
//     this.requestedTissueParticipants = requestedTissueParticipants;
//     this.tissueToRequest = tissueToRequest;
//     this.tissueToRequestParticipants = tissueToRequestParticipants;
//     this.tissueWithProblem = tissueWithProblem;
//     this.tissueWithProblemForParticipants = tissueWithProblemForParticipants;
//     this.faxSentTissuePeriod = faxSentTissuePeriod;
//     this.faxSentTissueParticipantsPeriod = faxSentTissueParticipantsPeriod;
//     this.receivedTissuePeriod = receivedTissuePeriod;
//     this.receivedTissueParticipantsPeriod = receivedTissueParticipantsPeriod;
//     this.requestedTissuePeriod = requestedTissuePeriod;
//     this.requestedTissueParticipantsPeriod = requestedTissueParticipantsPeriod;
//     this.tissueReturned = tissueReturned;
//     this.tissueReturnedParticipants = tissueReturnedParticipants;
//     this.tissueReturnedInPeriod = tissueReturnedInPeriod;
//     this.tissueReturnedParticipantsPeriod = tissueReturnedParticipantsPeriod;
//     this.mrFollowUpSentParticipant = mrFollowUpSentParticipant;
//     this.mrFollowUpSentParticipantInPeriod = mrFollowUpSentParticipantInPeriod;
//     this.mrFollowUpSentInstitutions = mrFollowUpSentInstitutions;
//     this.mrFollowUpSentInstitutionsInPeriod = mrFollowUpSentInstitutionsInPeriod;
//     this.mrReceivedFollowUpParticipant = mrReceivedFollowUpParticipant;
//     this.mrReceivedFollowUpParticipantInPeriod = mrReceivedFollowUpParticipantInPeriod;
//     this.mrReceivedFollowUpInstitutions = mrReceivedFollowUpInstitutions;
//     this.mrReceivedFollowUpInstitutionsInPeriod = mrReceivedFollowUpInstitutionsInPeriod;
//     this.followupReqMRForInstitutions = followupReqMRForInstitutions;
//     this.followupReqMRForParticipants = followupReqMRForParticipants;
//   }
// }
//
// export class SummaryKitType {
//   constructor( public kitType: string, public sent: number, public received: number,
//                public sentPeriod: number, public receivedPeriod: number ) {
//     this.kitType = kitType;
//     this.sent = sent;
//     this.received = received;
//     this.sentPeriod = sentPeriod;
//     this.receivedPeriod = receivedPeriod;
//
//   }
// }
