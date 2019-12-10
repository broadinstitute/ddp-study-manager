import {Component, OnInit} from "@angular/core";
import {ActivatedRoute, Router} from "@angular/router";

import {DSMService} from "../services/dsm.service";
import {Auth} from "../services/auth.service";
import {ParticipantExit} from "./participant-exit.model";
import {RoleService} from "../services/role.service";
import {Statics} from "../utils/statics";
import {ComponentService} from "../services/component.service";
import {DiscardSample} from "../discard-sample/discard-sample.model";

@Component({
  selector: 'app-participant-exit',
  templateUrl: './participant-exit.component.html',
  styleUrls: ['./participant-exit.component.css']
})
export class ParticipantExitComponent implements OnInit {

  errorMessage: string;
  additionalMessage: string;
  loading: boolean = false;

  realm: string;

  participantId: string = null;
  deletedFromDDP: boolean = false;

  exitedParticipants: Array<ParticipantExit> = [];
  allowedToSeeInformation: boolean = false;

  kits: Array<DiscardSample> = null;

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
    this.exitedParticipants = [];
    let jsonData: any[];
    this.dsmService.getRealmsAllowed(Statics.PARTICIPANT_EXIT).subscribe(
      data => {
        jsonData = data;
        jsonData.forEach((val) => {
          if (this.realm === val) {
            this.allowedToSeeInformation = true;
            this.getExitedParticipants();
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

  exitParticipant() {
    if (this.realm != null && this.participantId !== "") {
      this.loading = true;
      let jsonData: any[];
      let participantExit: ParticipantExit = new ParticipantExit(this.realm, this.participantId, this.role.userID(), null, null, null, !this.deletedFromDDP);
      // console.log(JSON.stringify(participantExit));
      this.dsmService.exitParticipant(JSON.stringify(participantExit)).subscribe(// need to subscribe, otherwise it will not send!
        data => {
          // console.info(`received: ${JSON.stringify(data, null, 2)}`);
          this.kits = [];
          jsonData = data;
          jsonData.forEach((val) => {
            let kit = DiscardSample.parse(val);
            this.kits.push(kit);
          });
          this.participantId = null;
          this.getExitedParticipants();
        },
        err => {
          if (err._body === Auth.AUTHENTICATION_ERROR) {
            this.router.navigate([Statics.HOME_URL]);
          }
          this.errorMessage = "Error - Failed to exit participant";
          this.loading = false;
        }
      );
    }
    else {
      this.errorMessage = "Please select a realm and enter the AltPID for the participant you want to exit.";
    }
  }

  saveStatusKits(kit: DiscardSample) {
    let payload = {
      'kitDiscardId': kit.kitDiscardId,
      'action': kit.action
    };
    this.dsmService.setKitDiscardAction(this.realm, JSON.stringify(payload)).subscribe(
      data => {
        // console.info(`received: ${JSON.stringify(data, null, 2)}`);
        this.additionalMessage = "Changed status";
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

  private getExitedParticipants() {
    if (this.realm != null) {
      this.errorMessage = null;
      this.additionalMessage = null;
      this.loading = true;
      let jsonData: any[];
      this.dsmService.getExitedParticipants(this.realm).subscribe(
        data => {
          this.exitedParticipants = [];
          // console.info(`received: ${JSON.stringify(data, null, 2)}`);
          jsonData = data;
          jsonData.forEach((val) => {
            let exitParticipant = ParticipantExit.parse(val);
            this.exitedParticipants.push(exitParticipant);
          });
          this.loading = false;
        },
        err => {
          if (err._body === Auth.AUTHENTICATION_ERROR) {
            this.auth.logout();
          }
          this.loading = false;
          this.errorMessage = "Error - Loading list of exited participants\nPlease contact your DSM developer";
        }
      );
    }
  }

}
