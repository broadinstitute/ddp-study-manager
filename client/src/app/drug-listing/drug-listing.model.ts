export class DrugListing {

  addedNew = false;
  changed = false;
  deleted = false;

  notUniqueError = false;
  notCorrectTypeError = false;
  chemoTypeLengthError = false;
  treatmentTypeLengthError = false;
  chemotherapyLengthError = false;


  constructor(public drugId: string, public genericName: string, public brandName: string, public displayName: string,
              public chemocat: string, public chemoType: string, public studyDrug: number, public treatmentType: string,
              public chemotherapy: string, public active: number, public dateAdded: number) {
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
    this.dateAdded = dateAdded;
  }

  static parse(json): DrugListing {
    return new DrugListing(json.drugId, json.genericName, json.brandName, json.displayName, json.chemocat, json.chemoType,
      json.studyDrug, json.treatmentType, json.chemotherapy, json.active, json.dateAdded);
  }

  // Submit button gives the full list of drugs from the page, so we want to trim down to only the subset that user updated
  // cleanedDrugList will be an array of DrugList components
  static removeUnchangedDrugListings(array: Array<DrugListing>): Array<DrugListing> {
    let cleanedDrugList: Array<DrugListing> = [];
    for (let drug of array) {
      if (drug.changed) {
        if (drug.displayName == null || drug.displayName === '') {
          // @TODO: also will need a dupe check here
          drug.displayName = drug.displayName;
        }
        cleanedDrugList.push(drug);
      }
    }
    return cleanedDrugList;
  }
}
