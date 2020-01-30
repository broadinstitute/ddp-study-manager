import {AbstractionGroup} from "../abstraction-group/abstraction-group.model";
import {Abstraction} from "../medical-record-abstraction/medical-record-abstraction.model";
import {MedicalRecord} from "../medical-record/medical-record.model";
import {OncHistoryDetail} from "../onc-history-detail/onc-history-detail.model";
import {Sample} from "./models/sample.model";
import {Data} from "./models/data.model";
import {ParticipantDSMInformation} from "./models/participant.model";

export class Participant {

  isSelected: boolean = false;

  constructor( public data: Data, public participant: ParticipantDSMInformation, public medicalRecords: Array<MedicalRecord>,
               public kits: Array<Sample>, public oncHistoryDetails: Array<OncHistoryDetail>, public abstractionActivities: Array<Abstraction>,
               public abstractionSummary: Array<AbstractionGroup>,
               public abstraction?: Abstraction, public review?: Abstraction, public qc?: Abstraction, public finalA?: Abstraction ) {
    this.data = data;
    this.participant = participant;
    this.medicalRecords = medicalRecords;
    this.kits = kits;
    this.oncHistoryDetails = oncHistoryDetails;
    this.abstractionActivities = abstractionActivities;
    this.abstractionSummary = abstractionSummary;
    this.abstraction = abstraction;
    this.review = review;
    this.qc = qc;
    this.finalA = finalA;
  }

  getSampleStatus(): string {
    let status: string = "";
    if (this.kits != null && this.kits.length > 0) {
      for (let sample of this.kits) {
        status += sample.kitType + ": " + sample[ "sampleQueue" ] + "\n";
      }
    }
    return status;
  }

  getProcessStatus(): string {
    if (this.abstraction.aStatus === "not_started") {
      return "Not Started";
    }
    else if (this.abstraction.aStatus === "in_progress" || this.abstraction.aStatus === "clear"
      || this.review.aStatus === "in_progress" || this.review.aStatus === "clear") {
      return "In Progress";
    }
    else if (this.qc.aStatus === "in_progress") {
      return "QC in progress";
    }
    else if (this.abstraction.aStatus === "done" && this.review.aStatus === "done" && this.qc.aStatus !== "done") {
      return "v";
    }
    else if (this.qc.aStatus === "done") {
      return "Finished";
    }
    return "";
  }

  getAbstractionStatus(): string {
    let status: string = "";
    if (this.abstraction.aStatus === "not_started") {
      status += "First Abstraction not started\n";
    }
    else if (this.abstraction.aStatus === "in_progress") {
      status += "First Abstraction started\n";
    }
    else if (this.abstraction.aStatus === "done") {
      status += "First Abstraction finished\n";
    }
    else if (this.abstraction.aStatus === "clear") {
      status += "First Abstraction not finished\n";
    }
    if (this.review.aStatus === "not_started") {
      status += "Second Abstraction not started\n";
    }
    if (this.review.aStatus === "in_progress") {
      status += "Second Abstraction started\n";
    }
    else if (this.review.aStatus === "clear") {
      status += "Second Abstraction not finished\n";
    }
    else if (this.review.aStatus === "done") {
      status += "Second Abstraction finished\n";
    }
    return status;
  }

  getQCStatus(): string {
    let status: string = "";
    if (this.qc.aStatus === "not_started") {
      status += "QC not started\n";
    }
    else if (this.qc.aStatus === "in_progress") {
      status += "QC started\n";
    }
    else if (this.qc.aStatus === "done") {
      status += "QC finished\n";
    }
    return status;
  }

  static parse( json ): Participant {
    let jsonData: any[];
    let medicalRecords: Array<MedicalRecord> = [];
    jsonData = json.medicalRecords;
    if (jsonData != null) {
      jsonData.forEach( ( val ) => {
        let medicalRecord = MedicalRecord.parse( val );
        medicalRecords.push( medicalRecord );
      } );
    }

    let oncHistoryDetails: Array<OncHistoryDetail> = [];
    jsonData = json.oncHistoryDetails;
    if (jsonData != null) {
      jsonData.forEach( ( val ) => {
        let oncHistory = OncHistoryDetail.parse( val );
        oncHistoryDetails.push( oncHistory );
      } );
    }

    let samples: Array<Sample> = [];
    jsonData = json.kits;
    if (jsonData != null) {
      jsonData.forEach( ( val ) => {
        let sample = Sample.parse( val );
        samples.push( sample );
      } );
    }

    let abstractionSummary: Array<AbstractionGroup> = [];
    jsonData = json.abstractionSummary;
    if (jsonData != null) {
      jsonData.forEach( ( val ) => {
        let abstractionGroup = AbstractionGroup.parse( val );
        abstractionSummary.push( abstractionGroup );
      } );
    }

    let data: Data = null;
    if (json.data != null) {
      data = Data.parse( json.data );
    }

    let abstraction: Abstraction = new Abstraction( null, null, null, "abstraction", "not_started", null, null, null );
    let review: Abstraction = new Abstraction( null, null, null, "review", "not_started", null, null, null );
    let qc: Abstraction = new Abstraction( null, null, null, "qc", "not_started", null, null, null );
    let finalA: Abstraction = new Abstraction( null, null, null, "final", "not_started", null, null, null );

    jsonData = json.abstractionActivities;
    if (jsonData != null) {
      jsonData.forEach( ( val ) => {
        let a = Abstraction.parse( val );
        if (a.activity === "abstraction") {
          abstraction = a;
        }
        else if (a.activity === "review") {
          review = a;
        }
        else if (a.activity === "qc") {
          qc = a;
        }
        else if (a.activity === "final") {
          finalA = a;
        }
      } );
    }

    let participant: ParticipantDSMInformation = null;
    jsonData = json.participant;
    if (jsonData != null) {
      participant = ParticipantDSMInformation.parse( jsonData );
    }

    return new Participant( data, participant, medicalRecords, samples, oncHistoryDetails, json.abstractionActivities, abstractionSummary, abstraction, review, qc, finalA );
  }
}
