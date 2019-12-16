import { Component, OnInit } from '@angular/core';
import { DrugList } from './drug-list.model';
import { Auth } from '../services/auth.service';
import { DSMService } from '../services/dsm.service';
import {RoleService} from '../services/role.service';

@Component({
  selector: 'app-drug-list',
  templateUrl: './drug-list.component.html',
  styleUrls: ['./drug-list.component.css']
})
export class DrugListComponent implements OnInit {

  errorMessage: string;
  additionalMessage: string;

  loading = false;

  drugList: DrugList[];

  constructor(private auth: Auth, private dsmService: DSMService, private role: RoleService) { }

  ngOnInit() {
    this.getListOfDrugObjects();
  }

  getListOfDrugObjects() {
    this.drugList = [];
    this.loading = true;
    let jsonData: any[];
    this.dsmService.getDruglistEntries().subscribe(
      data => {
        jsonData = data;
        // console.info(`received: ${JSON.stringify(data, null, 2)}`);
        jsonData.forEach((val) => {
          let event = DrugList.parse(val);
          this.drugList.push(event);
        });
        this.addDrugListEntry();
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

  // @TODO: Come back to this when we get to inline edit
  check(index: number) {
    // if (this.drugList[index].defaultPage) {
    //   for (var i = 0; i < this.drugList.length; i++) {
    //     if (i != index) {
    //       if (this.drugList[i].defaultPage) {
    //         this.drugList[i].defaultPage = false;
    //         this.drugList[i].changed = true;
    //       }
    //     }
    //   }
    // }
    this.onChange(index);
  }

  onChange(index: number) {
    this.drugList[index].changed = true;
    if (index === this.drugList.length - 1) {
      this.addDrugListEntry()
    }
  }

  addDrugListEntry() {
    let labelSetting: DrugList = new DrugList(null, null, null, null,
      null, null, 0, null, null, 0, 0);
    labelSetting.addedNew = true;
    this.drugList.push(labelSetting);
  }

  updateActiveFlag(index: number) {
    this.drugList[index].deleted = true;
    this.onChange(index);
    this.saveDruglistEntries();
  }

  saveDruglistEntries() {
    let foundError = false;
    let cleanedDrugs: Array<DrugList> = DrugList.removeUnchangedDruglistEntries(this.drugList);
    for (let drug of cleanedDrugs) {
      if (drug.notUniqueError) {
        foundError = true;
      }
    }
    if (foundError) {
      this.additionalMessage = 'Please fix errors first!';
    }
    else {
      this.loading = true;
      this.dsmService.saveDruglistEntries(JSON.stringify(cleanedDrugs)).subscribe(
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
}
