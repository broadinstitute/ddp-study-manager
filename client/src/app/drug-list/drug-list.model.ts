export class DrugList {

  addedNew = false;
  changed = false;
  deleted = false;

  notUniqueError = false;
  duplicatedNamesError = false;

  constructor( public drugId: string, public genericName: string, public brandName: string, public displayName: string,
               public chemocat: string, public chemoType: string, public studyDrug: boolean, public treatmentType: string,
               public chemotherapy: string, public active: boolean ) {
    this.drugId = drugId;
    this.genericName = genericName;
    this.brandName = brandName;
    this.displayName = displayName;
    this.chemocat = chemocat;
    this.chemoType = chemoType;
    this.studyDrug = studyDrug;
    this.treatmentType = treatmentType;
    this.chemotherapy = chemotherapy;
    this.active = active;
  }

  static parse( json ): DrugList {
    return new DrugList( json.drugId, json.genericName, json.brandName, json.displayName, json.chemocat, json.chemoType,
      json.studyDrug, json.treatmentType, json.chemotherapy, json.active );
  }

  // Submit button gives the full list of drugs from the page, so we want to trim down to only the subset that user updated
  // cleanedDrugList will be an array of DrugList components
  static removeUnchangedDrugs( array: Array<DrugList> ): Array<DrugList> {
    let cleanedDrugList: Array<DrugList> = [];
    for (let drug of array) {
      if (drug.changed || drug.addedNew) {
        if (drug.displayName == null || drug.displayName === "") {
          drug.displayName = drug.displayName;
        }
        cleanedDrugList.push( drug );
      }
    }
    return cleanedDrugList;
  }
}
