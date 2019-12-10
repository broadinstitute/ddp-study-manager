import {Component, OnInit} from "@angular/core";
import {Auth} from "../services/auth.service";
import {DSMService} from "../services/dsm.service";
import {EmailSettings} from "./eel-setting.model";
import {Result} from "../utils/result.model";
import {ActivatedRoute} from "@angular/router";
import {ComponentService} from "../services/component.service";
import {Statics} from "../utils/statics";

@Component({
  selector: 'app-eel-setting',
  templateUrl: './eel-setting.component.html',
  styleUrls: ['./eel-setting.component.css']
})
export class EelSettingComponent implements OnInit {

  errorMessage: string;
  additionalMessage: string;

  loading: boolean = false;

  realm: string;

  emailSettings: Array<EmailSettings> = [];

  range: Array<number> = [];
  allowedToSeeInformation: boolean = false;

  constructor(private dsmService: DSMService, private auth: Auth, private compService: ComponentService, private route: ActivatedRoute) {
    for (let i = 1; i <= 30; i++) {
      this.range.push(i);
    }
    this.route.queryParams.subscribe(params => {
      this.realm = params[DSMService.REALM] || null;
      if (this.realm != null) {
        //        this.compService.realmMenu = this.realm;
        this.checkRight();
      }
    });
  }

  private checkRight() {
    this.allowedToSeeInformation = false;
    this.additionalMessage = null;
    let jsonData: any[];
    this.dsmService.getRealmsAllowed(Statics.EMAIL_EVENT).subscribe(
      data => {
        jsonData = data;
        jsonData.forEach((val) => {
          if (this.realm === val) {
            this.allowedToSeeInformation = true;
            this.loadEmailSettings();
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

  private loadEmailSettings() {
    this.emailSettings = [];
    this.loading = true;
    this.additionalMessage = null;
    let jsonData: any[];
    this.dsmService.getEmailSettings(this.realm).subscribe(
      data => {
        jsonData = data;
        // console.info(`received: ${JSON.stringify(data, null, 2)}`);
        jsonData.forEach((val) => {
          let event = EmailSettings.parse(val);
          this.emailSettings.push(event);
        });
        this.loading = false;
      },
      err => {
        if (err._body === Auth.AUTHENTICATION_ERROR) {
          this.auth.logout();
        }
        this.loading = false;
        this.errorMessage = "Error - Loading Email Settings\n " + err;
      }
    );
  }

  saveSettings() {
    if (this.realm != null) {
      this.loading = true;
      this.dsmService.saveEmailSettings(this.realm, JSON.stringify(this.emailSettings)).subscribe(
        data => {
          // console.log(`Deactivating kit request received: ${JSON.stringify(data, null, 2)}`);
          let result = Result.parse(data);
          if (result.code !== 200) {
            this.errorMessage = result.body;
          }
          else {
            this.errorMessage = "Saved email settings";
          }
          this.loading = false;
        },
        err => {
          if (err._body === Auth.AUTHENTICATION_ERROR) {
            this.auth.logout();
          }
          this.loading = false;
          this.errorMessage = "Error - Saving email settings\n" + err;
        }
      );
      window.scrollTo(0,0);
    }
  }
}
