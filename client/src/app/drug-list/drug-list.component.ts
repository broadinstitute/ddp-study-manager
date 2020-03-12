import {Component, OnInit} from "@angular/core";
import {NameValue} from "../utils/name-value.model";
import {PatchUtil} from "../utils/patch.model";
import {Result} from "../utils/result.model";
import {Statics} from "../utils/statics";
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

  currentPatchField: string;
  currentPatchFieldRow: number;
  patchFinished: boolean = true;

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


  valueChanged( value: any, parameterName: string, index: number ) {
    let v;

    if (parameterName === "displayName") {
      this.checkDisplayName( index );
      if (this.drugList[ index ].notUniqueError) {
        return;
      }
    }
    else if (parameterName === "genericName" || parameterName === "brandName") {
      this.checkDuplicatedNames( index );
      if (this.drugList[ index ].duplicatedNamesError) {
        return;
      }
    }

    if (typeof value === "string") {
      this.drugList[ index ][ parameterName ] = value;
      v = value;
    }
    else if (value != null) {
      if (value.srcElement != null && typeof value.srcElement.value === "string") {
        v = value.srcElement.value;
      }
      else if (value.value != null) {
        v = value.value;
      }
      else if (value.checked != null) {
        v = value.checked;
      }
      else if (typeof value === "object") {
        v = JSON.stringify( value );
      }
    }
    if (v != null) {
      let patch1 = new PatchUtil( this.drugList[ index ].drugId, this.role.userMail(),
        {
          name: parameterName,
          value: v
        }, null, null, null, Statics.DRUG_ALIAS );
      let patch = patch1.getPatch();
      this.patchFinished = false;
      this.currentPatchField = parameterName;
      this.currentPatchFieldRow = index;
      this.patch( patch );
    }
  }

  patch( patch: any ) {
    this.dsmService.patchParticipantRecord( JSON.stringify( patch ) ).subscribe(// need to subscribe, otherwise it will not send!
      data => {
        let result = Result.parse( data );
        if (result.code !== 200) {
          this.additionalMessage = "Error - Saving Drug\nPlease contact your DSM developer";
        }
        this.patchFinished = true;
        this.currentPatchField = null;
        this.currentPatchFieldRow = null;
      },
      err => {
        if (err._body === Auth.AUTHENTICATION_ERROR) {
          this.auth.logout();
        }
      }
    );
  }

  addDrug() {
    let drug: DrugList = new DrugList( null, this.genericName, this.brandName, this.displayName,
      this.chemocat, this.chemoType, this.studyDrug, this.treatmentType, this.chemotherapy, this.active );
    drug.addedNew = true;
    this.saveDrugList( drug );
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

  saveDrugList( drug: DrugList ) {
    this.loading = true;
    this.dsmService.saveDrug( JSON.stringify( drug ) ).subscribe(
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
        this.additionalMessage = "Error - Saving Drug\nPlease contact your DSM developer";
      }
    );
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

  isPatchedCurrently( field: string, row: number ): boolean {
    if (this.currentPatchField === field && this.currentPatchFieldRow === row) {
      return true;
    }
    return false;
  }

  currentField( field: string, row: number ) {
    if (field != null || ( field == null && this.patchFinished )) {
      this.currentPatchField = field;
      this.currentPatchFieldRow = row;
    }
  }
}
