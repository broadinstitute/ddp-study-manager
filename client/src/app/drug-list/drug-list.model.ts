export class DrugList {

  addedNew: boolean = false;
  changed: boolean = false;
  deleted: boolean = false;

  notUniqueError: boolean = false;

  constructor(public drugId: string, public genericName: string, public brandName: string, public displayName: string,
              public chemocat2: string, public chemoType: string, public studyDrug: number, public treatmentType: string,
              public chemotherapy: string, public active: number, public dateAdded: number) {
    this.drugId = drugId;
    this.genericName = genericName;
    this.brandName = brandName;
    this.displayName = displayName;
    this.chemocat2 = chemocat2;
    this.chemoType = chemoType;
    this.studyDrug = studyDrug;
    this.treatmentType = treatmentType;
    this.chemotherapy = chemotherapy;
    this.active = active;
    this.dateAdded = dateAdded;


    var dateAddedMillis = dateAdded * 1000;
    var dateAddedString = dateAddedMillis.toString(); // convert from epoch to current millis
    //var dateAddedDate = {{ dateAddedMillis | date }};
    // this.dateAdded = dateAddedString;

    // Test
    // {{ 1575887469 * 1000 | date }}

  }

  static parse(json): DrugList {
    return new DrugList(json.drugId, json.genericName, json.brandName, json.displayName, json.chemocat2, json.chemoType,
      json.studyDrug, json.treatmentType, json.chemotherapy, json.active, json.dateAdded);
  }
}
