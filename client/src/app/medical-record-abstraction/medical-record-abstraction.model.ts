export class Abstraction {

  colorNotFinished: boolean;

  constructor(public medicalRecordAbstractionActivityId: number, public participantId: string, public user: string, public activity: string,
              public aStatus: string, public startDate: number, public lastChanged: number, public filesUsed: string) {
    this.medicalRecordAbstractionActivityId = medicalRecordAbstractionActivityId;
    this.participantId = participantId;
    this.user = user;
    this.activity = activity;
    this.aStatus = aStatus;
    this.startDate = startDate;
    this.lastChanged = lastChanged;
    this.filesUsed = filesUsed;
  }

  static parse(json): Abstraction {
    return new Abstraction(json.medicalRecordAbstractionActivityId, json.participantId, json.user, json.activity, json.aStatus, json.startDate, json.lastChanged, json.filesUsed);
  }
}
