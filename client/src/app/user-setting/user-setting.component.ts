import {Component, OnInit} from "@angular/core";
import {RoleService} from "../services/role.service";
import {UserSetting} from "./user-setting.model";
import {DSMService} from "../services/dsm.service";
import {Result} from "../utils/result.model";
import {Auth} from "../services/auth.service";
import {ActivatedRoute, Router} from "@angular/router";
import {Statics} from "../utils/statics";
import {Utils} from "../utils/utils";
import {ComponentService} from "../services/component.service";

@Component({
  selector: "app-user-setting",
  templateUrl: "./user-setting.component.html",
  styleUrls: ["./user-setting.component.css"],
})
export class UserSettingComponent implements OnInit {

  errorMessage: string;
  additionalMessage: string;

  saving: boolean = false;

  userSetting: UserSetting = null;
  tissueListFilterNames: string[] = [];
  participantListFilterNames: string[] = [];
  loading = true;

  constructor(private route: ActivatedRoute, private role: RoleService, private dsmService: DSMService, private router: Router, private auth: Auth, private util: Utils,
              private compService: ComponentService) {
    if (!auth.authenticated()) {
      auth.logout();
    }
    this.additionalMessage = null;
    this.route.queryParams.subscribe(params => {
      let realm = params[DSMService.REALM] || null;
      if (realm != null) {
        //        this.compService.realmMenu = realm;
        this.additionalMessage = null;
        this.checkRight();
      }
    });
    window.scrollTo(0, 0);
  }

  ngOnInit() {
    this.additionalMessage = null;
    if (localStorage.getItem(ComponentService.MENU_SELECTED_REALM) == null || localStorage.getItem(ComponentService.MENU_SELECTED_REALM) === undefined) {
      this.additionalMessage = "Please select a realm";
    }
    else {
      this.checkRight();
      // this.userSetting = this.role.getUserSetting();
      // console.log(this.userSetting.defaultTissueFilter);
    }
    window.scrollTo(0, 0);
  }

  hasRole(): RoleService {
    return this.role;
  }

  private checkRight() {
    let jsonData: any[];
    let allowedToSeeInformation = false;
    this.dsmService.getRealmsAllowed(Statics.MEDICALRECORD).subscribe(
      data => {
        jsonData = data;
        jsonData.forEach((val) => {
          if (localStorage.getItem(ComponentService.MENU_SELECTED_REALM) === val) {
            allowedToSeeInformation = true;
            this.getSavedFilters();
            this.userSetting = this.role.getUserSetting();
            console.log(this.userSetting.defaultTissueFilter);
            this.loading = false;
            this.errorMessage = null;
            this.additionalMessage = null;
          }
        });
        if (!allowedToSeeInformation) {
          this.loading = true;
          this.compService.customViews = null;
          this.errorMessage = "You are not allowed to see information of the selected realm at that category";
        }
      },
      err => {
        // this.loadingParticipants = null;
        return null;
      },
    );

  }

  saveUserSettings() {
    this.saving = true;
    this.dsmService.saveUserSettings(JSON.stringify(this.userSetting)).subscribe(// need to subscribe, otherwise it will not send!
      data => {
        this.additionalMessage = "";
        console.log(`received: ${JSON.stringify(data, null, 2)}`);
        let result = Result.parse(data);
        if (result.code !== 200) {
          this.additionalMessage = result.body;
        }
        else {
          this.role.setUserSetting(this.userSetting);
        }
        this.saving = false;
      },
      err => {
        if (err._body === Auth.AUTHENTICATION_ERROR) {
          this.router.navigate([Statics.HOME_URL]);
        }
        this.additionalMessage = "Error - Saving user settings\n" + err;
        this.saving = false;
      },
    );
  }

  setDefaultFilter(value, parent: string) {
    let filterName;
    console.log(value);
    if (typeof value === "string") {
      filterName = value;
    }
    else {
      filterName = value.value;
    }
    console.log(filterName);
    if (filterName === undefined) {
      filterName = "";
    }
    let json = {
      user: this.role.userMail(),
    };
    let jsonString = JSON.stringify(json);
    this.saving = true;
    this.dsmService.setDefaultFilter(jsonString, filterName, parent, localStorage.getItem(ComponentService.MENU_SELECTED_REALM)).subscribe(
      data => {
        this.additionalMessage = "";
        if (parent === "participantList") {
          this.role.getUserSetting().defaultParticipantFilter = filterName;
          this.userSetting.defaultParticipantFilter = filterName;
        }
        else if (parent === "tissueList") {
          this.role.getUserSetting().defaultTissueFilter = filterName;
          this.userSetting.defaultTissueFilter = filterName;
        }
        console.log(data);
        this.saving = false;
      },
      err => {
        if (err._body === Auth.AUTHENTICATION_ERROR) {
          this.router.navigate([Statics.HOME_URL]);
        }
        this.additionalMessage = "Error - Saving user settings\n" + err;
        this.saving = false;

      },
    );
  }

  getSavedFilters() {
    this.dsmService.getFiltersForUserForRealm(localStorage.getItem(ComponentService.MENU_SELECTED_REALM), null).subscribe(data => {
        this.tissueListFilterNames = [];
        this.participantListFilterNames = [];
        let jsonData = data;
        jsonData.forEach((val) => {
          if (val.parent === "tissueList") {
            this.tissueListFilterNames.push(val.filterName);
          }
          else if (val.parent === "participantList") {
            this.participantListFilterNames.push(val.filterName);
          }
        });
      },
      err => {
      });
  }

}
