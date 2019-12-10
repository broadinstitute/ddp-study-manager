import {MedicalRecord} from "../medical-record/medical-record.model";
import {ParticipantDSMInformation} from "../participant-list/models/participant.model";

export class RawData {

  constructor( public participant: ParticipantDSMInformation, public medicalRecord: MedicalRecord ) {
    this.participant = participant;
    this.medicalRecord = medicalRecord;
  }

  static parse( json ): RawData {
    return new RawData( json.participant, json.medicalRecord );
  }
}
