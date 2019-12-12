export class DrugList {

  addedNew = false;
  changed = false;
  deleted = false;

  notUniqueError = false;

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
  }

  static parse(json): DrugList {
    return new DrugList(json.drugId, json.genericName, json.brandName, json.displayName, json.chemocat2, json.chemoType,
      json.studyDrug, json.treatmentType, json.chemotherapy, json.active, json.dateAdded);
  }
}
