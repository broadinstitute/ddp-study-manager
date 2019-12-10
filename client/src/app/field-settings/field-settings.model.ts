import {Value} from "../utils/value.model";
import {FieldType} from "./field-type.model";

export class FieldSettings {
  changed: boolean = false;
  deleted: boolean = false;
  changedBy: string;

  addedNew: boolean = false;

  notUniqueError: boolean = false;
  spaceError: boolean = false;

  constructor(public fieldSettingId: string, public columnName: string, public columnDisplay: string, public fieldType: string,
              public displayType: string, public possibleValues: Value[]) {
    this.fieldSettingId = fieldSettingId;
    this.columnName = columnName;
    this.columnDisplay = columnDisplay;
    this.fieldType = fieldType;
    this.displayType = displayType;
    this.possibleValues = possibleValues;
  }

  static parse(json): FieldSettings {
    return new FieldSettings(json.fieldSettingId, json.columnName, json.columnDisplay, json.fieldType,
      json.displayType, json.hasOwnProperty("possibleValues") ? json.possibleValues : []);
  }

  static addSettingWithType(map: Map<string, Array<FieldSettings>>, setting: FieldSettings, type: FieldType) {
    if (map.has(type.name)) {
      map.get(type.name).push(setting);
    }
    else {
      let arr: Array<FieldSettings> = [];
      arr.push(setting);
      map.set(type.name, arr);
    }
  }

  static allowPossibleValues(setting: FieldSettings): boolean {
    return setting.displayType === "OPTIONS" || setting.displayType === "MULTI_OPTIONS";
  }

  static removeUnchangedFieldSettings(array: Array<FieldSettings>, selectedType: FieldType): Array<FieldSettings> {
    let cleanedFieldSettings: Array<FieldSettings> = [];
    for (let elem of array) {
      if (elem.changed && !(elem.addedNew && elem.deleted)) {
        if (elem.columnDisplay == null || elem.columnDisplay === "") {
          elem.columnDisplay = elem.columnName;
        }
        elem.fieldType = selectedType.tableAlias;
        if (elem.displayType === null || elem.displayType === "") {
          elem.displayType = "text";
        }
        let oldPVals: Array<Value> = elem.possibleValues;
        console.log(oldPVals);
        elem.possibleValues = [];
        if (this.allowPossibleValues(elem)) {
          // If possible values were specified and the display type is one that can have possible values, add valid possible values back
          if (oldPVals !== null && oldPVals !== undefined && oldPVals.length > 0) {
            oldPVals.forEach((item, index) => {
              if (item.value !== null && item.value !== undefined && item.value !== "" && item.value.trim() !== "") {
                elem.possibleValues.push(item);
              }
            });
            console.log(elem.possibleValues);
          }
          // If the display type is select or multiselect but no options are specified, change it to text
          if (elem.possibleValues.length < 1) {
            elem.displayType = "text";
          }
        }
        cleanedFieldSettings.push(elem);
      }
    }
    return cleanedFieldSettings;
  }
}
