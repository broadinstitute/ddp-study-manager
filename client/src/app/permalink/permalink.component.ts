import {Component, OnInit} from "@angular/core";
import {Participant} from "../participant-list/participant-list.model";
import {DSMService} from "../services/dsm.service";
import {Auth} from "../services/auth.service";
import {ActivatedRoute, Router} from "@angular/router";
import {ComponentService} from "../services/component.service";
import {MedicalRecord} from "../medical-record/medical-record.model";
import {Statics} from "../utils/statics";

@Component( {
  selector: "app-permalink",
  templateUrl: "./permalink.component.html",
  styleUrls: [ "./permalink.component.css" ]
} )
export class PermalinkComponent implements OnInit {

  participant: Participant;
  medicalRecord: MedicalRecord;

  constructor( private router: Router, private route: ActivatedRoute, private dsmService: DSMService, private auth: Auth,
               private compService: ComponentService ) {
    if (!auth.authenticated()) {
      auth.logout();
    }
  }

  ngOnInit() {
    let realm: string;
    this.route.queryParams.forEach( ( p ) => {
      realm = p[ DSMService.REALM ];
      //      this.compService.realmMenu = realm;
    } );
    if (this.router.url.indexOf( "/participantList" ) > -1) {
      this.gotToParticipant( realm );
    }
    if (this.router.url.indexOf( Statics.MEDICALRECORD ) > -1) {
      let participantId: string;
      let medicalRecordId: string;
      this.route.params.forEach( ( p ) => {
        participantId = p[ "participantid" ];
        medicalRecordId = p[ "medicalrecordid" ];
      } );
      this.goToMedicalRecord( participantId, medicalRecordId );
    }
  }

  public gotToParticipant( realm: string ) {
    this.router.navigate( [ "/participantList" ] );
  }

  public goToMedicalRecord( participantId: string, medicalRecordId: string ) {
    this.participant = null;
    this.medicalRecord = null;
    if (medicalRecordId != null && medicalRecordId !== "" && participantId != null && participantId !== "") {
      this.dsmService.getParticipant(participantId, localStorage.getItem(ComponentService.MENU_SELECTED_REALM)).subscribe(
        data => {
          // console.info(`participant data received: ${JSON.stringify(data, null, 2)}`);
          let participant = Participant.parse( data );
          this.participant = participant;

          if (this.medicalRecord != null && this.participant != null) {
            this.router.navigate( [ Statics.MEDICALRECORD_URL ] );
          }
        },
        err => {
          if (err._body === Auth.AUTHENTICATION_ERROR) {
            this.auth.logout();
          }
          throw "Error loading institutions" + err;
        }
      );

      this.dsmService.getMedicalRecord( participantId, medicalRecordId ).subscribe(
        data => {
          // console.info(`institution data received: ${JSON.stringify(data, null, 2)}`);
          let medicalRecord = MedicalRecord.parse( data );
          this.medicalRecord = medicalRecord;

          if (this.medicalRecord != null && this.participant != null) {
            this.router.navigate( [ Statics.MEDICALRECORD_URL ] );
          }
        },
        err => {
          if (err._body === Auth.AUTHENTICATION_ERROR) {
            this.auth.logout();
          }
          throw "Error loading medical record data" + err;
        }
      );
    }
    else {
      this.router.navigate( [ Statics.HOME_URL ] );
    }
  }
}
