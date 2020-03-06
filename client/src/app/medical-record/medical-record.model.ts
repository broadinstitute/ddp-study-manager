import {OncHistoryDetail} from "../onc-history-detail/onc-history-detail.model";
import {FollowUp} from "../follow-up/follow-up.model";

export class MedicalRecord {

  private NEW: string = "New";
  private SENT: string = "Fax Sent";
  private RECEIVED: string = "MR Received";
  private UNABLE: string = "Unable To Obtain";
  private REMOVED: string = "Removed";
  private DUPLICATE: string = "Duplicate";
  private INTERNATIONAL: string = "International";

  private PHYSICIAN: string = "PHYSICIAN";
  private INSTITUTION: string = "INSTITUTION";
  private INITIALBIOPSY: string = "INITIAL_BIOPSY";

  public changedBy: string;

  constructor(public medicalRecordId?: string, public participantId?: string, public institutionId?: string, public ddpInstitutionId?: string, public name?: string,
              public contact?: string, public phone?: string, public fax?: string,
              public faxSent?: string, public faxSentBy?: string, public faxConfirmed?: string,
              public faxSent2?: string, public faxSent2By?: string, public faxConfirmed2?: string,
              public faxSent3?: string, public faxSent3By?: string, public faxConfirmed3?: string,
              public mrReceived?: string, public mrDocument?: string, public mrDocumentFileNames?: string,
              public mrProblem?: boolean, public mrProblemText?: string, public unableObtain?: boolean, public duplicate?: boolean,
              public international?: boolean, public crRequired?: boolean, public pathologyPresent?: string,
              public mrNotes?: string, public reviewMedicalRecord?: boolean, public type?: string, public nameDDP?: string,
              public institutionDDP?: string, public streetAddressDDP?: string, public cityDDP?: string, public stateDDP?: string,
              public isDeleted?: boolean, public oncHistoryDetail?: Array<OncHistoryDetail>, public followUps?: FollowUp[], public followUpRequired?: boolean,
              public followUpRequiredText?: string, public additionalValues?: {}, public mrUnableToObtainText?: string) {
    this.medicalRecordId = medicalRecordId;
    this.participantId = participantId;
    this.institutionId = institutionId;
    this.ddpInstitutionId = ddpInstitutionId;
    this.name = name;
    this.contact = contact;
    this.phone = phone;
    this.fax = fax;
    this.faxSent = faxSent;
    this.faxSentBy = faxSentBy;
    this.faxConfirmed = faxConfirmed;
    this.faxSent2 = faxSent2;
    this.faxSent2By = faxSent2By;
    this.faxConfirmed2 = faxConfirmed2;
    this.faxSent3 = faxSent3;
    this.faxSent3By = faxSent3By;
    this.faxConfirmed3 = faxConfirmed3;
    this.mrReceived = mrReceived;
    this.mrDocument = mrDocument;
    this.mrDocumentFileNames = mrDocumentFileNames;
    this.mrProblem = mrProblem;
    this.mrProblemText = mrProblemText;
    this.unableObtain = unableObtain;
    this.duplicate = duplicate;
    this.international = international;
    this.crRequired = crRequired;
    this.pathologyPresent = pathologyPresent;
    this.mrNotes = mrNotes;
    this.reviewMedicalRecord = reviewMedicalRecord;
    this.type = type;

    this.nameDDP = nameDDP;
    this.institutionDDP = institutionDDP;
    this.streetAddressDDP = streetAddressDDP;
    this.cityDDP = cityDDP;
    this.stateDDP = stateDDP;

    this.isDeleted = isDeleted;

    this.oncHistoryDetail = oncHistoryDetail;

    this.followUps = followUps == undefined ? [] : followUps;

    this.followUpRequired = followUpRequired;
    this.followUpRequiredText = followUpRequiredText;
    this.additionalValues = additionalValues;

    this.mrUnableToObtainText = mrUnableToObtainText;
  }

  get mrStatus(): string {
    return this.getMRStatus();
  }

  getMRStatus(): string {
    if (this.duplicate) {
      return this.DUPLICATE;
    }
    else if (this.unableObtain) {
      return this.UNABLE;
    }
    else if (this.international) {
      return this.INTERNATIONAL;
    }
    else {
      if (this.mrReceived != null && this.mrReceived !== "") {
        return this.RECEIVED;
      }
      else if (this.faxSent != null && this.faxSent !== "") {
        return this.SENT;
      }
      else {
        return this.NEW;
      }
    }
  }

  getName(): string {
    if (this.isDeleted) {
      return this.REMOVED;
    }
    else {
      if (this.name != null && this.name != "") {
        return this.name;
      }
      else {
        if (this.PHYSICIAN === this.type) {
          return this.nameDDP;
        }
        else {
          return this.institutionDDP;
        }
      }
    }
  }

  getType(): string {
    if (this.PHYSICIAN === this.type) {
      return "Physician";
    }
    else if (this.INSTITUTION === this.type) {
      return "Institution";
    }
    else if (this.INITIALBIOPSY === this.type) {
      return "Initial Biopsy";
    }
    return "";
  }

  getFollowUpValue( name: string, i: number ): any {
    if (i > this.followUps.length) {
      return null;
    }
    return this.followUps[ i ][ name ];
  }

  showInstitution(): boolean {
    if (this.PHYSICIAN === this.type || this.INSTITUTION === this.type || this.INITIALBIOPSY === this.type) {
      return true;
    }
    return false;
  }

  getParticipantEnteredAddress(): string {
    let address: string = "";
    if (this.nameDDP) {
      address += this.nameDDP;
    }
    if (this.institutionDDP) {
      address += this.addLineBreak( address );
      address += this.institutionDDP;
    }
    if (this.streetAddressDDP) {
      address += this.addLineBreak( address );
      address += this.streetAddressDDP;
    }
    if (this.cityDDP && this.stateDDP) {
      //both are not empty, so add them both
      address += this.addLineBreak( address );
      address += this.cityDDP + " " + this.stateDDP;
    }
    else {
      //one is not empty, so add that one
      if (this.cityDDP) {
        address += this.addLineBreak( address );
        address += this.cityDDP;
      }
      if (this.stateDDP) {
        address += this.addLineBreak( address );
        address += this.stateDDP;
      }
    }
    return address;
  }

  addLineBreak( address: string ): string {
    if (address) {
      return "\n";
    }
    return "";
  }

  static parse( json ): MedicalRecord {
    let result: FollowUp[] = [];
    let jsonArray = json.followUps;
    if (jsonArray !== undefined) {
      jsonArray.forEach( function ( json ) {
        result.push( new FollowUp(
          json.fRequest1 == undefined ? null : json.fRequest1,
          json.fRequest2 == undefined ? null : json.fRequest2,
          json.fRequest3 == undefined ? null : json.fRequest3,
          json.fReceived == undefined ? null : json.fReceived ) );
      } );
    }
    let data = json.additionalValues;
    let additionalValues = {};
    if (data != null) {
      data = "{" + data.substring(1, data.length - 1) + "}";
      additionalValues = JSON.parse(data);
    }
    return new MedicalRecord( json.medicalRecordId, json.participantId, json.institutionId, json.ddpInstitutionId, json.name,
      json.contact, json.phone, json.fax,
      json.faxSent, json.faxSentBy, json.faxConfirmed,
      json.faxSent2, json.faxSent2By, json.faxConfirmed2,
      json.faxSent3, json.faxSent3By, json.faxConfirmed3,
      json.mrReceived, json.mrDocument, json.mrDocumentFileNames,
      json.mrProblem, json.mrProblemText, json.unableObtain, json.duplicate, json.international, json.crRequired,
      json.pathologyPresent,
      json.mrNotes, json.reviewMedicalRecord, json.type, json.nameDDP,
      json.institutionDDP, json.streetAddressDDP, json.cityDDP, json.stateDDP,
      json.isDeleted, json.oncHistoryDetails, result,
      json.followUpRequired, json.followUpRequiredText, additionalValues );
  }


}
