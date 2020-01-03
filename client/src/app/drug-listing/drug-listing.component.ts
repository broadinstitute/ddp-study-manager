import { Component, OnInit } from '@angular/core';
import { DrugListing } from './drug-listing.model';
import { Auth } from '../services/auth.service';
import { DSMService } from '../services/dsm.service';
import {RoleService} from '../services/role.service';

@Component({
  selector: 'app-drug-listing',
  templateUrl: './drug-listing.component.html',
  styleUrls: ['./drug-listing.component.css']
})
export class DrugListingComponent implements OnInit {

  errorMessage: string;
  additionalMessage: string;

  loading = false;

  drugList: DrugListing[];

  constructor(private auth: Auth, private dsmService: DSMService, private role: RoleService) { }

  ngOnInit() {
    this.getListOfDrugObjects();
  }

  getListOfDrugObjects() {
    this.drugList = [];
    this.loading = true;
    let jsonData: any[];
    this.dsmService.getDrugListings().subscribe(
      data => {
        jsonData = data;
        // console.info(`received: ${JSON.stringify(data, null, 2)}`);
        jsonData.forEach((val) => {
          let event = DrugListing.parse(val);
          this.drugList.push(event);
        });
        this.addDrugListing();
        this.loading = false;
      },
      err => {
        if (err._body === Auth.AUTHENTICATION_ERROR) {
          this.auth.logout();
        }
        this.loading = false;
        this.errorMessage = 'Error - Loading Drug List\nPlease contact your DSM developer';
      }
    );
  }

  hasRole(): RoleService {
    return this.role;
  }

  check(index: number) {
    this.drugList[index].changed = true;

    // change from inactive to active (check updated value and set object value)
    if (this.drugList[index].active) {
            this.drugList[index].active = 1;
    }
    // change from active to inactive
    if (!this.drugList[index].active) {
      this.drugList[index].active = 0;
    }
    this.onChange(index);
  }

  onChange(index: number) {
    this.drugList[index].changed = true;
    if (index === this.drugList.length - 1) {
      this.addDrugListing()
    }
  }

  addDrugListing() {
    let labelSetting: DrugListing = new DrugListing(null, null, null, null,
      null, null, 0, null, null, 0, 0);
    labelSetting.addedNew = true;
    this.drugList.push(labelSetting);
  }

  saveDrugListings() {
    let foundError = false;
    let cleanedDrugs: Array<DrugListing> = DrugListing.removeUnchangedDrugListings(this.drugList);
    for (let drug of cleanedDrugs) {
      if (drug.notUniqueError || drug.chemoTypeLengthError || drug.treatmentTypeLengthError || drug.chemotherapyLengthError || drug.notCorrectTypeError) {
        foundError = true;
      }
    }
    if (foundError) {
      this.additionalMessage = 'Please fix errors first!';
    }
    else {
      this.loading = true;
      this.dsmService.saveDrugListings(JSON.stringify(cleanedDrugs)).subscribe(
        data => {
          this.getListOfDrugObjects();
          this.loading = false;
          this.additionalMessage = 'Data saved';
        },
        err => {
          if (err._body === Auth.AUTHENTICATION_ERROR) {
            this.auth.logout();
          }
          this.loading = false;
          this.additionalMessage = 'Error - Saving Drug entries\nPlease contact your DSM developer';
        }
      );
    }
    window.scrollTo(0, 0);
  }

  checkDisplayName(index: number) {
    this.drugList[index].notUniqueError = false;
    for (let i = 0; i < this.drugList.length; i++) {
      if (i !== index) {
        if (this.drugList[i].displayName === this.drugList[index].displayName) {
          this.drugList[index].notUniqueError = true;
        }
      }
    }
    return null;
  }

  // Validate Study Drug field (should be numeric)
  checkValueType(index: number) {
    this.drugList[index].notCorrectTypeError = false;
    if (isNaN(Number(this.drugList[index].studyDrug))) {
      this.drugList[index].notCorrectTypeError = true;
    }
    return null;
  }

  // Validate single-character fields: Chemo Type, Treatment Type, Chemotherapy
  checkValueLength(index: number) {
    this.drugList[index].chemoTypeLengthError = false;
    this.drugList[index].treatmentTypeLengthError = false;
    this.drugList[index].chemotherapyLengthError = false;

    if (this.drugList[index].chemoType.length > 1 ) {
      this.drugList[index].chemoTypeLengthError = true;
    }
    if (this.drugList[index].treatmentType.length > 1 ) {
      this.drugList[index].treatmentTypeLengthError = true;
    }
    if (this.drugList[index].chemotherapy.length > 1 ) {
      this.drugList[index].chemotherapyLengthError = true;
    }
    return null;
  }
}
