import {Component, OnInit} from "@angular/core";
import {ActivatedRoute, Router} from "@angular/router";
import {ComponentService} from "../services/component.service";
import {DSMService} from "../services/dsm.service";
import {Auth} from "../services/auth.service";
import {RoleService} from "../services/role.service";
import {Statics} from "../utils/statics";
import {DiscardSample} from "./discard-sample.model";

@Component({
  selector: 'app-discard-sample',
  templateUrl: './discard-sample.component.html',
  styleUrls: ['./discard-sample.component.css']
})
export class DiscardSampleComponent implements OnInit {

  errorMessage: string;
  additionalMessage: string;
  loading: boolean = false;

  realm: string;
  allowedToSeeInformation: boolean = false;

  samples: Array<DiscardSample> = [];

  constructor(private dsmService: DSMService, private auth: Auth, private router: Router, private role: RoleService,
              private compService: ComponentService, private route: ActivatedRoute) {
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

  private checkRight() {
    this.allowedToSeeInformation = false;
    this.additionalMessage = null;
    this.samples = [];
    let jsonData: any[];
    this.dsmService.getRealmsAllowed(Statics.DISCARD_SAMPLES).subscribe(
      data => {
        jsonData = data;
        jsonData.forEach((val) => {
          if (this.realm === val) {
            this.allowedToSeeInformation = true;
            this.getSamples();
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

  openSample(sample: DiscardSample) {
    if (sample != null && sample.receivedDate != 0 && sample.action !== "hold") {
      this.compService.discardSample = sample;
      this.router.navigate(["/discardSample"]);
    }
  }

  private getSamples() {
    if (this.realm != null) {
      this.errorMessage = null;
      this.additionalMessage = null;
      this.loading = true;
      let jsonData: any[];
      this.dsmService.getKitExitedParticipants(this.realm).subscribe(
        data => {
          this.samples = [];
          // console.info(`received: ${JSON.stringify(data, null, 2)}`);
          jsonData = data;
          jsonData.forEach((val) => {
            let sample = DiscardSample.parse(val);
            this.samples.push(sample);
          });
          this.loading = false;
        },
        err => {
          if (err._body === Auth.AUTHENTICATION_ERROR) {
            this.auth.logout();
          }
          this.loading = false;
          this.errorMessage = "Error - Loading list of samples of exited participants\nPlease contact your DSM developer";
        }
      );
    }
  }

  hasRole(): RoleService {
    return this.role;
  }

  triggerAction(index: number) {
    if (this.realm != null) {
      this.errorMessage = null;
      this.additionalMessage = null;
      this.loading = true;
      let payload = {
        'kitRequestId': this.samples[index].kitRequestId,
        'kitDiscardId': this.samples[index].kitDiscardId,
        'action': this.samples[index].action
      };
      this.dsmService.setKitDiscardAction(this.realm, JSON.stringify(payload)).subscribe(
        data => {
          console.info(`received: ${JSON.stringify(data, null, 2)}`);
          this.getSamples();
        },
        err => {
          if (err._body === Auth.AUTHENTICATION_ERROR) {
            this.auth.logout();
          }
          this.loading = false;
          this.errorMessage = "Error - Loading list of samples of exited participants\nPlease contact your DSM developer";
        }
      );
    }
  }
}
