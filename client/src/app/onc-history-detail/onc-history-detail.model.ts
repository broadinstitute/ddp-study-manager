import {Tissue} from "../tissue/tissue.model";

export class OncHistoryDetail {

  changed: boolean = false;
  selected: boolean = false;
  deleted: boolean = false;

  changedBy: string;

  constructor(public participantId: string, public oncHistoryDetailId: string, public medicalRecordId: string, public datePX: string, public typePX: string,
              public locationPX: string, public histology: string, public accessionNumber: string, public facility: string,
              public fPhone: string, public fFax: string, public oncHisNotes: string, public request: string,
              public tFaxSent: string, public tFaxSentBy: string, public tFaxConfirmed: string,
              public tFaxSent2: string, public tFaxSent2By: string, public tFaxConfirmed2: string,
              public tFaxSent3: string, public tFaxSent3By: string, public tFaxConfirmed3: string,
              public tissueReceived: string, public gender: string,
              public additionalValues: {}, public tissues: Array<Tissue>,
              public tissueProblemOption: string, public destructionPolicy: string, public unableToObtain: boolean, public numberOfRequests) {
    this.participantId = participantId;
    this.oncHistoryDetailId = oncHistoryDetailId;
    this.medicalRecordId = medicalRecordId;
    this.datePX = datePX;
    this.typePX = typePX;
    this.locationPX = locationPX;
    this.histology = histology;
    this.accessionNumber = accessionNumber;
    this.facility = facility;
    this.fPhone = fPhone;
    this.fFax = fFax;
    this.oncHisNotes = oncHisNotes;
    this.request = request;
    this.tFaxSent = tFaxSent;
    this.tFaxSentBy = tFaxSentBy;
    this.tFaxConfirmed = tFaxConfirmed;
    this.tFaxSent2 = tFaxSent2;
    this.tFaxSent2By = tFaxSent2By;
    this.tFaxConfirmed2 = tFaxConfirmed2;
    this.tFaxSent3 = tFaxSent3;
    this.tFaxSent3By = tFaxSent3By;
    this.tFaxConfirmed3 = tFaxConfirmed3;
    this.tissueReceived = tissueReceived;
    this.gender = gender;
    this.additionalValues = additionalValues;
    this.tissues = tissues;
    this.tissueProblemOption = tissueProblemOption;
    this.destructionPolicy = destructionPolicy;
    this.unableToObtain = unableToObtain;
  }

  static parse(json): OncHistoryDetail {
    let jsonData: any[];
    let tissues: Array<Tissue> = [];
    jsonData = json.tissues;
    if (jsonData != null && jsonData != undefined) {
      jsonData.forEach((val) => {
        let tissue = Tissue.parse(val);
        tissues.push(tissue);
      });
    }

    let data = json.additionalValues;
    let additionalValues = {};
    if (data != null) {
      data = "{" + data.substring(1, data.length - 1) + "}";
      additionalValues = JSON.parse(data);
    }

    return new OncHistoryDetail(json.participantId, json.oncHistoryDetailId, json.medicalRecordId, json.datePX, json.typePX, json.locationPX,
      json.histology, json.accessionNumber, json.facility, json.fPhone, json.fFax, json.oncHisNotes, json.request,
      json.tFaxSent, json.tFaxSentBy, json.tFaxConfirmed,
      json.tFaxSent2, json.tFaxSent2By, json.tFaxConfirmed2,
      json.tFaxSent3, json.tFaxSent3By, json.tFaxConfirmed3,
      json.tissueReceived, json.gender, additionalValues, tissues,
      json.tissueProblemOption, json.destructionPolicy, json.unableToObtain, json.numberOfRequests);
  }
}
