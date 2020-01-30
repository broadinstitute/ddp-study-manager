export class ParticipantDSMInformation {

  constructor( public participantId: string, public ddpParticipantId: string, public realm: string,
               public assigneeMr: string, public assigneeTissue: string, public createdOncHistory: string, public reviewedOncHistory: string,
               public paperCRSent: string, public paperCRReceived: string, public ptNotes: string, public minimalMR: boolean, public abstractionReady: boolean,
               public mrNeedsAttention: boolean, public tissueNeedsAttention: boolean, public exitDate: number, public additionalValues: {} ) {
    this.participantId = participantId;
    this.ddpParticipantId = ddpParticipantId;
    this.realm = realm;
    this.assigneeMr = assigneeMr;
    this.assigneeTissue = assigneeTissue;
    this.createdOncHistory = createdOncHistory;
    this.reviewedOncHistory = reviewedOncHistory;
    this.paperCRSent = paperCRSent;
    this.paperCRReceived = paperCRReceived;
    this.ptNotes = ptNotes;
    this.minimalMR = minimalMR;
    this.abstractionReady = abstractionReady;
    this.mrNeedsAttention = mrNeedsAttention;
    this.tissueNeedsAttention = tissueNeedsAttention;
    this.exitDate = exitDate;
    this.additionalValues = additionalValues;
  }

  static parse( json ): ParticipantDSMInformation {
    let data = json.additionalValues;
    let additionalValues = {};
    if (data != null) {
      data = "{" + data.substring(1, data.length - 1) + "}";
      additionalValues = JSON.parse(data);
    }
    return new ParticipantDSMInformation( json.participantId, json.ddpParticipantId, json.realm, json.assigneeMr, json.assigneeTissue,
      json.createdOncHistory, json.reviewedOncHistory, json.paperCRSent, json.paperCRReceived,
      json.ptNotes, json.minimalMR, json.abstractionReady, json.mrNeedsAttention, json.tissueNeedsAttention, json.exitDate, additionalValues );
  }


}
