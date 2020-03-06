import {Component, OnInit} from "@angular/core";
import {DrugList} from "./drug-list.model";
import {Auth} from "../services/auth.service";
import {DSMService} from "../services/dsm.service";
import {RoleService} from "../services/role.service";

@Component( {
  selector: "app-drug-list",
  templateUrl: "./drug-list.component.html",
  styleUrls: [ "./drug-list.component.css" ]
} )
export class DrugListComponent implements OnInit {

  errorMessage: string;
  additionalMessage: string;

  loading = false;
  drugList: DrugList[];

  notUniqueError = false;
  duplicatedNamesError = false;

  filterDisplayName: string = "";
  filterGenericName: string = "";
  filterBrandName: string = "";
  filterChemocat: string = "";
  filterChemoType: string = "";
  filterTreatmentType: string = "";
  filterChemotherapy: string = "";

  displayName: string = "";
  genericName: string = "";
  brandName: string = "";
  chemocat: string = "";
  chemoType: string = "";
  studyDrug: boolean = false;
  treatmentType: string = "";
  chemotherapy: string = "";
  active: boolean = false;

  constructor( private auth: Auth, private dsmService: DSMService, private role: RoleService ) {
  }

  ngOnInit() {
    this.getListOfDrugObjects();
  }

  getListOfDrugObjects() {
    this.loading = true;
    let jsonData: any[];
    this.dsmService.getDrugs().subscribe(
      data => {
        this.drugList = [];
        jsonData = data;
        // console.info(`received: ${JSON.stringify(data, null, 2)}`);
        jsonData.forEach( ( val ) => {
          let event = DrugList.parse( val );
          this.drugList.push( event );
        } );
        this.loading = false;
      },
      err => {
        if (err._body === Auth.AUTHENTICATION_ERROR) {
          this.auth.logout();
        }
        this.loading = false;
        this.errorMessage = "Error - Loading Drug List\nPlease contact your DSM developer";
      }
    );
  }

  hasRole(): RoleService {
    return this.role;
  }

  changed( index: number ) {
    this.drugList[ index ].changed = true;
  }

  addDrug() {
    let drug: DrugList = new DrugList( null, this.genericName, this.brandName, this.displayName,
      this.chemocat, this.chemoType, this.studyDrug, this.treatmentType, this.chemotherapy, this.active );
    drug.addedNew = true;
    this.drugList.push( drug );
    this.saveDrugList();
    this.genericName = "";
    this.brandName = "";
    this.displayName = "";
    this.chemocat = "";
    this.chemoType = "";
    this.studyDrug = false;
    this.treatmentType = "";
    this.chemotherapy = "";
    this.active = false;
  }

  goodNewDrug(): boolean {
    if (this.genericName == null || this.genericName === "" || this.displayName == null || this.displayName === "" ||
      this.chemocat == null || this.chemocat === "" || this.chemoType == null || this.chemoType === "" || this.treatmentType == null ||
      this.treatmentType === "" || this.chemotherapy == null || this.chemotherapy === "" || this.notUniqueError || this.duplicatedNamesError) {
      return false;
    }
    return true;
  }

  saveDrugList() {
    let foundError = false;
    let cleanedDrugs: Array<DrugList> = DrugList.removeUnchangedDrugs( this.drugList );
    for (let drug of cleanedDrugs) {
      let capitalizedDisplayName = drug.displayName.toUpperCase();
      drug.displayName = capitalizedDisplayName;
      if (drug.notUniqueError) {
        foundError = true;
      }
    }
    if (foundError) {
      this.additionalMessage = "Please fix errors first!";
    } else {
      this.loading = true;
      this.dsmService.saveDrugs( JSON.stringify( cleanedDrugs ) ).subscribe(
        data => {
          this.getListOfDrugObjects();
          this.loading = false;
          this.additionalMessage = "Data saved";
        },
        err => {
          if (err._body === Auth.AUTHENTICATION_ERROR) {
            this.auth.logout();
          }
          this.loading = false;
          this.additionalMessage = "Error - Saving Drug listings\nPlease contact your DSM developer";
        }
      );
    }
    window.scrollTo( 0, 0 );
  }

  checkDisplayName( index: number ) {
    if (index == null) {
      this.notUniqueError = false;
      let drug = this.drugList.find( drug => drug.displayName === this.displayName );
      if (drug != null) {
        this.notUniqueError = true;
      }
    }
    else {
      this.drugList[ index ].notUniqueError = false;
      for (let i = 0; i < this.drugList.length; i++) {
        if (i !== index) {
          if (this.drugList[ i ].displayName === this.drugList[ index ].displayName) {
            this.drugList[ index ].notUniqueError = true;
          }
        }
      }
    }
    return null;
  }

  // Validate against rows with generic & brand name values that are identical to another row
  checkDuplicatedNames( index: number ) {
    if (index == null) {
      this.duplicatedNamesError = false;
      let drug = this.drugList.find( drug => drug.genericName === this.genericName && drug.brandName === this.brandName );
      if (drug != null) {
        this.duplicatedNamesError = true;
      }
    }
    else {
      this.drugList[ index ].duplicatedNamesError = false;
      for (let i = 0; i < this.drugList.length; i++) {
        if (i !== index) {
          if (this.drugList[ i ].genericName === this.drugList[ index ].genericName && this.drugList[ i ].brandName === this.drugList[ index ].brandName) {
            this.drugList[ index ].duplicatedNamesError = true;
          }
        }
      }
    }
    return null;
  }
}
