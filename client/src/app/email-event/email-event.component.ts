import {Component, OnInit} from "@angular/core";
import {ActivatedRoute, Router} from "@angular/router";

import {DSMService} from "../services/dsm.service";
import {Auth} from "../services/auth.service";
import {EmailEvent} from "./email-event.model";
import {RoleService} from "../services/role.service";
import {Utils} from "../utils/utils";
import {EmailEventRawData} from "./email-event-raw-data.model";
import {Statics} from "../utils/statics";
import {ComponentService} from "../services/component.service";

@Component({
  selector: 'app-email-event',
  templateUrl: './email-event.component.html',
  styleUrls: ['./email-event.component.css']
})
export class EmailEventComponent implements OnInit {

  FOLLOW_UP: string = "followup";
  EVENT: string = "";

  errorMessage: string;
  additionalMessage: string;

  private loading: boolean = false;

  realm: string;

  emailEvents: Array<EmailEvent> = [];
  noEmailData: boolean = false;

  page: string;

  showPopUp: boolean = false;
  allowedToSeeInformation: boolean = false;

  constructor(private dsmService: DSMService, private auth: Auth, private role: RoleService, private route: ActivatedRoute,
              private router: Router, private compService: ComponentService) {
    if (!auth.authenticated()) {
      auth.logout();
    }
    this.route.queryParams.subscribe(params => {
      this.setPage(this.router.url);
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
            this.loadEventData();
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

  setPage(url: string) {
    if (url.indexOf(Statics.EMAIL_EVENT_FOLLOW_UP) > -1) {
      this.page = this.FOLLOW_UP;
    }
    else if (url.indexOf(Statics.EMAIL_EVENT) > -1) {
      this.page = this.EVENT;
    }
    else {
      this.errorMessage = "Error - Router has unknown url\nPlease contact your DSM developer";
    }
  }

  hasRole(): RoleService {
    return this.role;
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

  private loadEventData() {
    this.emailEvents = [];
    this.loading = true;
    this.additionalMessage = null;
    let jsonData: any[];
    this.dsmService.getEmailEventData(this.realm, this.page).subscribe(
      data => {
        jsonData = data;
        // console.info(`received: ${JSON.stringify(data, null, 2)}`);
        jsonData.forEach((val) => {
          this.emailEvents.push(EmailEvent.parse(val));
        });
        if (this.emailEvents.length < 1) {
          this.noEmailData = true;
        }
        else {
          this.noEmailData = false;
        }
        this.loading = false;
        this.sortFollowupArray();
      },
      err => {
        if (err._body === Auth.AUTHENTICATION_ERROR) {
          this.auth.logout();
        }
        this.loading = false;
        this.errorMessage = "Error - Loading participant\n " + err;
      }
    );
  }

  csvDownload(): void {
    var date = new Date();
    var fields = ["template", "name", "email", "event", "time"];
    let rawData: Array<EmailEventRawData>  = [];
    this.emailEvents.forEach((valEEL) => {
      valEEL.emails.forEach((valEmail) => {
        valEmail.events.forEach((valEvent) => {
          rawData.push(new EmailEventRawData(valEEL.templateId, valEEL.name, valEmail.email, valEvent.event, Utils.getDateFormatted(new Date(valEvent.timestamp*1000), Utils.DATE_STRING_IN_EVENT_CVS), valEvent.url));
        });
      });
    });
    Utils.createCSV(fields, rawData, "All Email Events "  + this.realm + " " + Utils.getDateFormatted(date, Utils.DATE_STRING_CVS) + Statics.CSV_FILE_EXTENSION)
  }

  csvFollowUpDownload(): void {
    var date = new Date();
    var fields = ["email", "template", "name", "event", "time"];
    let rawData: Array<EmailEventRawData>  = [];
    this.emailEvents.forEach((valEEL) => {
      valEEL.templates.forEach((valTemplate) => {
        valTemplate.events.forEach((valEvent) => {
          rawData.push(new EmailEventRawData(valTemplate.templateId, valTemplate.name, valEEL.email, valEvent.event, Utils.getDateFormatted(new Date(valEvent.timestamp*1000), Utils.DATE_STRING_IN_EVENT_CVS), valEvent.url));
        });
      });
    });
    Utils.createCSV(fields, rawData, "Follow-up Email Events "  + this.realm + " " + Utils.getDateFormatted(date, Utils.DATE_STRING_CVS) + Statics.CSV_FILE_EXTENSION)
  }

  saveFollowUp(){
    this.loading = true;
    let followedUpEmailEvents: Array<EmailEvent> = [];
    this.emailEvents.forEach((valEEL) => {
      var alreadyAdded: boolean = false;
      valEEL.templates.forEach((valTemplate) => {
        if (!alreadyAdded) {
          if (valTemplate.followUpChanged) {
            followedUpEmailEvents.push(valEEL);
            alreadyAdded = true;
          }
        }
      });
    });
    // console.log(`send changes to dsm: ${JSON.stringify(followedUpEmailEvents, null, 2)}`);
    this.dsmService.followUpEmailEvent(this.realm, JSON.stringify(followedUpEmailEvents)).subscribe(// need to subscribe, otherwise it will not send!
      data => {
        this.additionalMessage =  "Data saved";
      },
      err => {
        if (err._body == Auth.AUTHENTICATION_ERROR) {
          this.router.navigate([Statics.HOME_URL]);
        }
        this.additionalMessage =  "Error - Saving follow-up dates\n" + err;
      }
    );
    this.loading = false;
    window.scrollTo(0,0);
  }


  followUpChanged(date: string, emailEvent: EmailEvent, indexTemplate: number) {
    emailEvent.templates[indexTemplate].followUpDateString = date;
    emailEvent.templates[indexTemplate].followUpChanged = true;
  }

  sortFollowupArray(): Array<EmailEvent> {
    var reA = /[^a-zA-Z-]/g;
    var reN = /[^0-9- ]/g;

      this.emailEvents.sort((a: EmailEvent, b: EmailEvent) => {
        if (this.page === this.FOLLOW_UP) {
          if (a.email != null && b.email != null) {
            var aA = a.email.replace(reA, "");
            var bA = b.email.replace(reA, "");
            if (aA === bA) {
              var aN = parseInt(a.email.replace(reN, ""), 10);
              var bN = parseInt(b.email.replace(reN, ""), 10);
              return aN === bN ? 0 : aN > bN ? 1 : -1;
            }
            else {
              return aA > bA ? 1 : -1;
            }
          }
        }
        else {
          if (a.workflowId < 0) {
            return -1;
          }
          return a.workflowId > b.workflowId ? 1 : -1;
        }
      });
    return this.emailEvents;
  }
}

