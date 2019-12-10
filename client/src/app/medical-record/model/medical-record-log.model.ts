export class MedicalRecordLog {

  static DATA_REVIEW: string = "DATA_REVIEW";

  constructor(public medicalRecordLogId: string, public date: string, public comments: string, public type: string) {
    this.medicalRecordLogId = medicalRecordLogId;
    this.date = date;
    this.comments = comments;
    this.type = type;
  }

  static parse(json): MedicalRecordLog {
    return new MedicalRecordLog(json.medicalRecordLogId, json.date, json.comments, json.type);
  }
}
