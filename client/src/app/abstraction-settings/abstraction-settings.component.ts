import {ChangeDetectorRef, Component, OnInit} from "@angular/core";
import {ActivatedRoute} from "@angular/router";

import {DSMService} from "../services/dsm.service";
import {Auth} from "../services/auth.service";
import {RoleService} from "../services/role.service";
import {ComponentService} from "../services/component.service";
import {Statics} from "../utils/statics";
import {AbstractionGroup} from "../abstraction-group/abstraction-group.model";

@Component({
  selector: 'app-abstraction-settings',
  templateUrl: './abstraction-settings.component.html',
  styleUrls: ['./abstraction-settings.component.css']
})
export class AbstractionSettingsComponent implements OnInit {

  errorMessage: string;
  additionalMessage: string;

  loading: boolean = false;
  saving: boolean = false;

  realmsAllowed: Array<string> = [];
  realm: string;

  abstractionFormControls: Array<AbstractionGroup>;
  allowedToSeeInformation: boolean = false;
  isViewHelp: boolean = true;

  constructor(private _changeDetectionRef: ChangeDetectorRef, private dsmService: DSMService, private auth: Auth, private role: RoleService, private compService: ComponentService,
              private route: ActivatedRoute) {
    if (!auth.authenticated()) {
      auth.logout();
    }
    this.route.queryParams.subscribe(params => {
      this.realm = params[DSMService.REALM] || null;
      if (this.realm != null) {
        //        this.compService.realmMenu = this.realm;
        this.checkRight();
      }
    });
  }

  ngOnInit() {
    if (localStorage.getItem(ComponentService.MENU_SELECTED_REALM) != null) {
      this.realm = localStorage.getItem(ComponentService.MENU_SELECTED_REALM);
      this.checkRight();
    }
    else {
      this.additionalMessage = "Please select a realm";
    }
    window.scrollTo(0,0);
  }

  private checkRight() {
    this.allowedToSeeInformation = false;
    this.additionalMessage = null;
    let jsonData: any[];
    this.dsmService.getRealmsAllowed(Statics.MEDICALRECORD).subscribe(
      data => {
        jsonData = data;
        jsonData.forEach((val) => {
          if (this.realm === val) {
            this.allowedToSeeInformation = true;
            this.loadAbstractionFormControls();
          }
        });
        if (!this.allowedToSeeInformation) {
          this.additionalMessage = "You are not allowed to see information of the selected realm at that category";
        }
      },
      err => {
        return null;
      }
    );
  }

  loadAbstractionFormControls() {
    this.loading = true;
    let jsonData: any[];
    this.dsmService.getMedicalRecordAbstractionFormControls(localStorage.getItem(ComponentService.MENU_SELECTED_REALM)).subscribe(
      data => {
        this.abstractionFormControls = [];
        jsonData = data;
        jsonData.forEach((val) => {
          let medicalRecordAbstractionGroup = AbstractionGroup.parse(val);
          this.abstractionFormControls.push(medicalRecordAbstractionGroup);
        });
        this.loading = false;
      },
      err => {
        if (err._body === Auth.AUTHENTICATION_ERROR) {
          this.auth.logout();
          this.loading = false;
        }
        this.errorMessage = "Error - Loading medical record abstraction form controls\n " + err;
      }
    );
  }

  saveSettings() {
    if (this.realm != null) {
      let foundError: boolean = false;
      this.loading = true;
      let cleanedSettings: Array<AbstractionGroup> = AbstractionGroup.removeUnchangedSetting(this.abstractionFormControls);
      for (let setting of cleanedSettings) {
        if (setting.notUniqueError) {
          foundError = true;
        }
      }
      if (foundError) {
        this.additionalMessage = "Please change duplicates first!";
        this.loading = false;
      }
      else {
        this.dsmService.saveMedicalRecordAbstractionFormControls(localStorage.getItem(ComponentService.MENU_SELECTED_REALM), JSON.stringify(cleanedSettings, (key, value) => {
          if (value !== null) return value
        })).subscribe(
          data => {
            this.loadAbstractionFormControls();
          },
          err => {
            if (err._body === Auth.AUTHENTICATION_ERROR) {
              this.auth.logout();
              this.loading = false;
            }
            this.errorMessage = "Error - Loading medical record abstraction form controls\n " + err;
          }
        );
      }
      window.scrollTo(0,0);
    }
  }

  doNothing() { //needed for the menu, otherwise page will refresh!
    return false;
  }

}
