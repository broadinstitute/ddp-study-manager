import { Component, OnInit } from '@angular/core';
import { LabelSetting } from "./label-settings.model";
import { Auth } from "../services/auth.service";
import { DSMService } from "../services/dsm.service";

@Component({
  selector: 'app-label-settings',
  templateUrl: './label-settings.component.html',
  styleUrls: ['./label-settings.component.css']
})
export class LabelSettingsComponent implements OnInit {

  errorMessage: string;
  additionalMessage: string;

  loading: boolean = false;

  pageSettings: LabelSetting[];

  constructor(private auth: Auth, private dsmService: DSMService) { }

  ngOnInit() {
    this.getListOfLabelSettings();
  }

  getListOfLabelSettings() {
    this.pageSettings = [];
    this.loading = true;
    let jsonData: any[];
    this.dsmService.getLabelSettings().subscribe(
      data => {
        jsonData = data;
        // console.info(`received: ${JSON.stringify(data, null, 2)}`);
        jsonData.forEach((val) => {
          let event = LabelSetting.parse(val);
          this.pageSettings.push(event);
        });
        this.addPageSetting();
        this.loading = false;
      },
      err => {
        if (err._body === Auth.AUTHENTICATION_ERROR) {
          this.auth.logout();
        }
        this.loading = false;
        this.errorMessage = "Error - Loading Label Settings\nPlease contact your DSM developer";
      }
    );
  }

  check(index: number) {
    if (this.pageSettings[index].defaultPage) {
      for (var i = 0; i < this.pageSettings.length; i++) {
        if (i != index) {
          if (this.pageSettings[i].defaultPage) {
            this.pageSettings[i].defaultPage = false;
            this.pageSettings[i].changed = true;
          }
        }
      }
    }
    this.onChange(index);
  }

  onChange(index: number) {
    this.pageSettings[index].changed = true;
    if (index === this.pageSettings.length - 1) {
      this.addPageSetting()
    }
  }

  addPageSetting() {
    let labelSetting: LabelSetting = new LabelSetting(null, null, null, false,
      0,0,0,0,0,0,0);
    labelSetting.addedNew = true;
    this.pageSettings.push(labelSetting);
  }

  deletePageSetting(index: number) {
    this.pageSettings[index].deleted = true;
    this.onChange(index);
    this.saveSettings();
  }

  saveSettings() {
    let foundError: boolean = false;
    let cleanedSettings: Array<LabelSetting> = LabelSetting.removeUnchangedLabelSetting(this.pageSettings);
    for (let setting of cleanedSettings) {
      if (setting.notUniqueError) {
        foundError = true;
      }
    }
    if (foundError) {
      this.additionalMessage = "Please fix errors first!";
    }
    else {
      this.loading = true;
      this.dsmService.saveLabelSettings(JSON.stringify(cleanedSettings)).subscribe(
        data => {
          this.getListOfLabelSettings();
          this.loading = false;
          this.additionalMessage = "Data saved";
        },
        err => {
          if (err._body === Auth.AUTHENTICATION_ERROR) {
            this.auth.logout();
          }
          this.loading = false;
          this.additionalMessage = "Error - Saving label settings\nPlease contact your DSM developer";
        }
      );
    }
    window.scrollTo(0,0);
  }

  checkName(index: number) {
    this.pageSettings[index].notUniqueError = false;
    for (var i = 0; i < this.pageSettings.length; i++) {
      if (i != index) {
        if (this.pageSettings[i].name === this.pageSettings[index].name) {
          this.pageSettings[index].notUniqueError = true;
        }
      }
    }
    return null;
  }
}
