import { Value } from "../utils/value.model";

export class AbstractionField {

  newAdded: boolean = false;
  deleted: boolean = false;
  changed: boolean = false;
  notUniqueError: boolean = false;
  viewHelpText: boolean = false;

  constructor(public medicalRecordAbstractionFieldId: number, public displayName: string, public type: string, public helpText: string, public orderNumber: number,
              public possibleValues: Value[], public additionalType: string, public hide: boolean,
              public fieldValue: AbstractionFieldValue, public qcWrapper: QCWrapper) { //last line are value fields!
    this.medicalRecordAbstractionFieldId = medicalRecordAbstractionFieldId;
    this.displayName = displayName;
    this.helpText = helpText;
    this.type = type;
    this.orderNumber = orderNumber;
    this.possibleValues = possibleValues;
    this.additionalType = additionalType;
    this.fieldValue = fieldValue;
    this.qcWrapper = qcWrapper;
  }

  static parse(json): AbstractionField {
    let fValue: AbstractionFieldValue = json.fieldValue;
    if (fValue == undefined || fValue == null) {
      fValue = new AbstractionFieldValue(null, null, "", 0,"","", false, false,null, "");
    }
    let wrapper: QCWrapper = json.qcWrapper;
    if (wrapper == undefined || wrapper == null) {
      let fieldValue: AbstractionFieldValue = new AbstractionFieldValue(null, null, "", 0,"", "", false, false, null, "");
      wrapper = new QCWrapper(fieldValue, fieldValue, false, false);
    }
    return new AbstractionField(json.medicalRecordAbstractionFieldId, json.displayName, json.type, json.helpText, json.orderNumber, json.possibleValues, json.additionalType, json.hide, fValue, wrapper);
  }

  static removeUnchangedSetting(array: Array<AbstractionField>): Array<AbstractionField> {
    let cleanedSettings: Array<AbstractionField> = [];
    for (let setting of array) {
      if (setting.changed) {
        if (!(setting.newAdded && setting.deleted)) {
          if (setting.possibleValues != null && setting.possibleValues.length > 0) {
            setting.possibleValues.forEach( (item, index) => {
              if (item.value == null || item.value === "") setting.possibleValues.splice(index,1);
            });
          }
          cleanedSettings.push(setting);
        }
      }
    }
    return cleanedSettings;
  }
}

export class AbstractionFieldValue {
  constructor (public medicalRecordAbstractionFieldId: number, public primaryKeyId: number, public value: string | string[], public valueCounter: number,
               public note: string,public question: string, public noData: boolean, public doubleCheck: boolean, public filePage: string,
               public fileName: string) {
    this.medicalRecordAbstractionFieldId = medicalRecordAbstractionFieldId;
    this.primaryKeyId = primaryKeyId;
    this.value = value;
    this.valueCounter = valueCounter;
    this.note = note;
    this.question = question;
    this.noData = noData;
    this.doubleCheck = doubleCheck;
    this.filePage = filePage;
    this.fileName = fileName;
  }

  static parse(json): AbstractionFieldValue {
    return new AbstractionFieldValue(json.medicalRecordAbstractionFieldId, json.primaryKeyId, json.value, json.valueCounter, json.note, json.question,
      json.noData, json.doubleCheck, json.filePage, json.fileName);
  }
}

export class QCWrapper {
  constructor (public abstraction: AbstractionFieldValue, public review: AbstractionFieldValue, public equals: Boolean, public check: Boolean) {
    this.abstraction = abstraction;
    this.review = review;
    this.equals = equals;
    this.check = check;
  }

  static parse(json): QCWrapper {
    return new QCWrapper(json.abstraction, json.review, json.equals, json.check);
  }
}
