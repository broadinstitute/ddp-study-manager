import {Component, EventEmitter, Input, OnInit, Output, ViewChild} from "@angular/core";
import {TabDirective} from "ngx-bootstrap";
import {ActivatedRoute, Router} from "@angular/router";
import {ActivityDefinition} from "../activity-data/models/activity-definition.model";
import {Participant} from "../participant-list/participant-list.model";

import {ComponentService} from "../services/component.service";
import {Auth} from "../services/auth.service";
import {DSMService} from "../services/dsm.service";
import {MedicalRecord} from "../medical-record/medical-record.model";
import {RoleService} from "../services/role.service";
import {Utils} from "../utils/utils";
import {Statics} from "../utils/statics";
import {OncHistoryDetail} from "../onc-history-detail/onc-history-detail.model";
import {ModalComponent} from "../modal/modal.component";
import {Tissue} from "../tissue/tissue.model";
import {DDPParticipantInformation} from "./participant-page.model";
import {Result} from "../utils/result.model";
import {NameValue} from "../utils/name-value.model";
import {Abstraction} from "../medical-record-abstraction/medical-record-abstraction.model";
import {AbstractionGroup, AbstractionWrapper} from "../abstraction-group/abstraction-group.model";
import {PatchUtil} from "../utils/patch.model";

var fileSaver = require( "file-saver/filesaver.js" );

@Component( {
  selector: "app-participant-page",
  templateUrl: "./participant-page.component.html",
  styleUrls: [ "./participant-page.component.css" ]
} )
export class ParticipantPageComponent implements OnInit {

  @ViewChild( ModalComponent )
  public universalModal: ModalComponent;

  @Input() parentList: string;
  @Input() participant: Participant;
  @Input() drugs: string[];
  @Input() cancers: string[];
  @Input() activeTab: string;
  @Input() activityDefinitions: Array<ActivityDefinition>;
  @Input() settings: {};
  @Input() oncHistoryId: string;
  @Input() mrId: string;
  @Output() leaveParticipant = new EventEmitter();

  participantExited: boolean = true;
  participantNotConsented: boolean = true;

  medicalRecord: MedicalRecord;
  oncHistoryDetail: OncHistoryDetail;

  errorMessage: string;
  additionalMessage: string;

  _showWarningModal = false;

  loadingParticipantPage: boolean = false;

  noteMedicalRecord: MedicalRecord;

  showParticipantRecord: boolean = false;
  showMedicalRecord: boolean = false;
  showTissue: boolean = false;
  isOncHistoryDetailChanged: boolean = false;
  disableTissueRequestButton: boolean = true;

  facilityName: string;

  warning: string;

  currentPatchField: string;
  patchFinished: boolean = true;

  fileListUsed: string[] = [];

  abstractionFields: Array<AbstractionGroup>;
  reviewFields: Array<AbstractionGroup>;
  qcFields: Array<AbstractionGroup>;
  finalFields: Array<AbstractionGroup>;

  gender: string;

  constructor( private auth: Auth, private compService: ComponentService, private dsmService: DSMService, private router: Router,
               private role: RoleService, private util: Utils, private route: ActivatedRoute ) {
    if (!auth.authenticated()) {
      auth.logout();
    }
    this.route.queryParams.subscribe( params => {
      let realm = params[ DSMService.REALM ] || null;
      if (realm != null) {
        //        this.compService.realmMenu = realm;
        this.leaveParticipant.emit( true );
      }
    } );
  }

  ngOnInit() {
    this.loadInstitutions();
    window.scrollTo( 0, 0 );
  }

  hasRole(): RoleService {
    return this.role;
  }

  getUtil(): Utils {
    return this.util;
  }

  getGroupHref( group: string ): string {
    return "#" + group;
  }

  private loadInstitutions() {
    if (this.participant.data != null) {
      if (this.participant.data.status.indexOf( Statics.EXITED ) == -1) {
        this.participantExited = false;
      }
      if (this.participant.data.status.indexOf( Statics.CONSENT_SUSPENDED ) == -1) {
        this.participantNotConsented = false;
      }
      //if surveys is null then it is a gen2 participant > go and get institution information
      if (this.participant.data.activities == null) {
        if (this.participant.data.dsm == null) {
          this.participant.data.dsm = {};
        }
        this.loadingParticipantPage = true;
        let ddpParticipantId = this.participant.data.profile[ "guid" ];
        this.dsmService.getMedicalRecordData(localStorage.getItem(ComponentService.MENU_SELECTED_REALM), ddpParticipantId).subscribe(
          data => {
            let ddpInformation = DDPParticipantInformation.parse( data );
            if (ddpInformation != null) {
              console.log( ddpInformation );
              this.participant.data.dsm[ "dateOfBirth" ] = ddpInformation.dob;
              let tmp = ddpInformation.dateOfDiagnosis;
              if (tmp != null && tmp.indexOf( "/" ) > -1) {
                this.participant.data.dsm[ "diagnosisMonth" ] = tmp.split( "/" )[ 0 ];
                this.participant.data.dsm[ "diagnosisYear" ] = tmp.split( "/" )[ 1 ];
              }
              this.participant.data.dsm[ "hasConsentedToBloodDraw" ] = false;
              this.participant.data.dsm[ "hasConsentedToTissueSample" ] = false;
              if (ddpInformation.drawBloodConsent === 1) {
                this.participant.data.dsm[ "hasConsentedToBloodDraw" ] = true;
              }
              if (ddpInformation.tissueSampleConsent === 1) {
                this.participant.data.dsm[ "hasConsentedToTissueSample" ] = true;
              }

              let counterReceived: number = 0;
              let medicalRecords = this.participant.medicalRecords;
              for (let mr of medicalRecords) {
                if (mr.mrDocumentFileNames != null) {
                  let files = mr.mrDocumentFileNames.split( /[\s,|;]+/ );
                  for (let file of files) {
                    if (this.fileListUsed.indexOf( file ) == -1) {
                      this.fileListUsed.push( file );
                    }
                  }
                }
                if (mr.crRequired) {
                  this.showParticipantRecord = true;
                }
                for (let inst of ddpInformation.institutions) {
                  if (inst.id === mr.ddpInstitutionId) {
                    mr.type = inst.type;
                    mr.institutionDDP = inst.institution;
                    mr.nameDDP = inst.physician;
                    mr.streetAddressDDP = inst.streetAddress;
                    mr.cityDDP = inst.city;
                    mr.stateDDP = inst.state;
                    mr.isDeleted = false;
                    break;
                  }
                }
                //add that here in case a mr was received but participant object does not know it
                let receivedMedicalRecord = -1;
                if (receivedMedicalRecord < 1) {
                  if (mr.mrReceived != null && mr.mrReceived !== "") {
                    counterReceived = counterReceived + 1;
                  }
                }
                if (counterReceived > 0) {
                  if (this.hasRole().isAbstracter() || this.hasRole().isQC()) {
                    this.loadAbstractionValues();
                  }
                }
              }
              this.addEmptyOncHistoryRow();
            }
            else {
              this.additionalMessage = "No additional information from the DDP was received";
            }
            this.loadingParticipantPage = false;
          },
          err => {
            if (err._body === Auth.AUTHENTICATION_ERROR) {
              this.auth.logout();
            }
            this.loadingParticipantPage = false;
            this.additionalMessage = "Error - Saving loading participant information\nPlease contact your DSM developer";
          }
        );
      }
      else {// don't need to load institution data
        let counterReceived: number = 0;
        let medicalRecords = this.participant.medicalRecords;
        for (let mr of medicalRecords) {
          if (mr.mrDocumentFileNames != null) {
            let files = mr.mrDocumentFileNames.split( /[\s,|;]+/ );
            for (let file of files) {
              if (this.fileListUsed.indexOf( file ) == -1) {
                this.fileListUsed.push( file );
              }
            }
          }
          if (mr.crRequired) {
            this.showParticipantRecord = true;
          }
          //add that here in case a mr was received but participant object does not know it
          let receivedMedicalRecord = -1;
          if (receivedMedicalRecord < 1) {
            if (mr.mrReceived != null && mr.mrReceived !== "") {
              counterReceived = counterReceived + 1;
            }
          }
          if (counterReceived > 0) {
            if (this.hasRole().isAbstracter() || this.hasRole().isQC()) {
              this.loadAbstractionValues();
            }
          }
        }
        if (this.participant.participant != null) {
          this.addEmptyOncHistoryRow();
        }
      }
    }
  }

  addEmptyOncHistoryRow() {
    if (this.participant.data.dsm[ "hasConsentedToTissueSample" ]) {
      let hasEmptyOncHis = false;
      for (let oncHis of this.participant.oncHistoryDetails) {
        if (oncHis.oncHistoryDetailId === null) {
          hasEmptyOncHis = true;
        }
        else {
          //get gender information
          if (this.gender === null || this.gender == undefined) {
            this.gender = oncHis.gender;
          }
          else {
            if (this.gender != oncHis.gender && oncHis.gender != undefined && oncHis.gender != null) {
              console.log( this.gender );
              console.log( oncHis.gender );
              this.gender = "Discrepancy in gender between the different oncHistories";
            }
          }
        }
        if (oncHis.tissues == null) {
          let tissues: Array<Tissue> = [];
          tissues.push( new Tissue( null, oncHis.oncHistoryDetailId, null, null, null, null,
            null, null, null, null, null, null, null, null
            , null, null, null, null, null, null, null, null, null,
            null, null, null, null ) );
          oncHis.tissues = tissues;
        }
        else if (oncHis.tissues.length < 1) {
          oncHis.tissues.push( new Tissue( null, oncHis.oncHistoryDetailId, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null
            , null, null, null, null, null, null, null,
            null, null, null, null ) );
        }
      }
      if (!hasEmptyOncHis) {
        let tissues: Array<Tissue> = [];
        tissues.push( new Tissue( null, null, null, null, null, null, null,
          null, null, null, null, null, null, null, null, null,
          null, null, null, null, null, null, null,
          null, null, null, null ) );

        let oncHis = new OncHistoryDetail( this.participant.participant.participantId,
          null, null, null, null, null, null, null, null, null, null,
          null, null, null, null, null, null, null, null,
          null, null, null, null, null, null, tissues, null, null, null,
          null );
        this.participant.oncHistoryDetails.push( oncHis );
      }
    }
  }

  openMedicalRecord( medicalRecord: MedicalRecord ) {
    if (medicalRecord != null) {
      this.medicalRecord = medicalRecord;
      this.showMedicalRecord = true;
    }
  }

  valueChanged( value: any, parameterName: string, tableAlias: string ) {
    let v;
    if (parameterName === "additionalValues") {
      v = JSON.stringify( value );
    }
    else if (typeof value === "string") {
      this.participant.participant[ parameterName ] = value;
      v = value;
    }
    else {
      if (value.srcElement != null && typeof value.srcElement.value === "string") {
        v = value.srcElement.value;
      }
      else if (value.checked != null) {
        v = value.checked;
      }
    }
    if (v !== null) {
      let participantId = this.participant.participant.participantId;
      let patch1 = new PatchUtil( participantId, this.role.userMail(),
        {name: parameterName, value: v}, null, null, null, tableAlias );
      let patch = patch1.getPatch();
      this.currentPatchField = parameterName;
      this.patchFinished = false;
      console.log( JSON.stringify( patch ) );
      this.dsmService.patchParticipantRecord( JSON.stringify( patch ) ).subscribe(// need to subscribe, otherwise it will not send!
        data => {
          let result = Result.parse( data );
          if (result.code === 200 && result.body != null) {
            let jsonData: any[] = JSON.parse( result.body );
            jsonData.forEach( ( val ) => {
              let nameValue = NameValue.parse( val );
              this.participant.participant[ nameValue.name ] = nameValue.value;
            } );
          }
          this.currentPatchField = null;
          this.patchFinished = true;
          this.additionalMessage = null;
        },
        err => {
          if (err._body === Auth.AUTHENTICATION_ERROR) {
            this.router.navigate( [ Statics.HOME_URL ] );
          }
          this.additionalMessage = "Error - Saving changed field \n" + err;
        }
      );
    }
  }

  oncHistoryValueChanged( value: any, parameterName: string, oncHis: OncHistoryDetail ) {
    let v;
    if (typeof value === "string") {
      oncHis[ parameterName ] = value;
      v = value;
    }
    else if (value != null) {
      if (value.srcElement != null && typeof value.srcElement.value === "string") {
        v = value.srcElement.value;
      }
      else if (value.value != null) {
        v = value.value;
      }
      else if (value.checked != null) {
        v = value.checked;
      }
    }
    if (v !== null) {

      let patch1 = new PatchUtil( oncHis.oncHistoryDetailId, this.role.userMail(),
        {name: parameterName, value: v}, null, "participantId", oncHis.participantId, Statics.ONCDETAIL_ALIAS );
      let patch = patch1.getPatch();
      this.patchFinished = false;
      this.currentPatchField = parameterName;
      this.dsmService.patchParticipantRecord( JSON.stringify( patch ) ).subscribe(// need to subscribe, otherwise it will not send!
        data => {
          let result = Result.parse( data );
          if (result.code === 200 && result.body != null) {
            let jsonData: any[] = JSON.parse( result.body );
            if (jsonData instanceof Array) {
              jsonData.forEach( ( val ) => {
                let nameValue = NameValue.parse( val );
                console.log( nameValue );
                oncHis[ nameValue.name.substr( 3 ) ] = nameValue.value;
              } );
            }
          }
          this.patchFinished = true;
          this.currentPatchField = null;
        },
        err => {
          if (err._body === Auth.AUTHENTICATION_ERROR) {
            this.router.navigate( [ Statics.HOME_URL ] );
          }
        }
      );
    }
  }

  public leavePage(): boolean {
    this.participant = null;
    this.medicalRecord = null;
    this.compService.justReturning = true;
    this.leaveParticipant.emit( true );
    return false;
  }

  openTissue( object: any ) {
    console.info( object );
    if (object != null) {
      console.info( "yes" );
      this.oncHistoryDetail = object;
      this.showTissue = true;
    }
  }

  onOncHistoryDetailChange() {
    this.isOncHistoryDetailChanged = true;
  }

  doRequest() {
    let requestOncHistoryList: Array<OncHistoryDetail> = [];
    for (let oncHis of this.participant.oncHistoryDetails) {
      if (oncHis.selected) {
        requestOncHistoryList.push( oncHis );
      }
    }
    this.downloadRequestPDF( requestOncHistoryList );
    this.disableTissueRequestButton = true;
    this._showWarningModal = false;
    this.universalModal.hide();
  }

  requestTissue() {
    this._showWarningModal = true;
    this.warning = null;
    let doIt: boolean = true;
    let firstOncHis: OncHistoryDetail = null;
    for (let oncHis of this.participant.oncHistoryDetails) {
      if (oncHis.selected) {
        if (firstOncHis == null) {
          firstOncHis = oncHis;
          this.facilityName = firstOncHis.facility;
        }
        if (typeof firstOncHis.facility === "undefined" || firstOncHis.facility == null) {
          this.warning = "Facility is empty";
          doIt = false;
        }
        else if (this.participant.kits != null) {
          //no samples for pt
          let kitReturned: boolean = false;
          for (let kit of this.participant.kits) {
            if (kit.receiveDate != 0) {
              kitReturned = true;
              break;
            }
          }
          if (!kitReturned) {
            doIt = false;
            this.warning = "No samples returned for participant yet";
          }
        }
        else {
          if (firstOncHis.facility !== oncHis.facility ||
            firstOncHis.fPhone !== oncHis.fPhone ||
            firstOncHis.fFax !== oncHis.fFax) {
            doIt = false;
            this.warning = "Tissues are not from the same facility";
          }
        }
      }
    }
    if (doIt && this.facilityName != null) {
      this.doRequest();
    }
    else {
      this.universalModal.show();
    }
  }

  onOncHistoryDetailSelectionChange() {
    let oneSelected: boolean = false;
    let oncHistories = this.participant.oncHistoryDetails;
    if (oncHistories != null) {
      for (let oncHis of oncHistories) {
        if (oncHis.selected) {
          oneSelected = true;
          break;
        }
      }
    }
    this.disableTissueRequestButton = !oneSelected;
  }

  openNoteModal( item: MedicalRecord ) {
    this.noteMedicalRecord = item;
  }

  saveNote() {
    let patch1 = new PatchUtil( this.noteMedicalRecord.medicalRecordId, this.role.userMail(),
      {name: "mrNotes", value: this.noteMedicalRecord.mrNotes}, null, null, null, Statics.MR_ALIAS );
    let patch = patch1.getPatch();


    this.dsmService.patchParticipantRecord( JSON.stringify( patch ) ).subscribe(// need to subscribe, otherwise it will not send!
      data => {
        console.info( `response saving data: ${JSON.stringify( data, null, 2 )}` );
      },
      err => {
        if (err._body === Auth.AUTHENTICATION_ERROR) {
          this.router.navigate( [ Statics.HOME_URL ] );
        }
        this.additionalMessage = "Error - Saving paper C/R changes \n" + err;
      }
    );
    this.noteMedicalRecord = null;
    this.universalModal.hide();
  }

  downloadRequestPDF( requestOncHistoryList: Array<OncHistoryDetail> ) {
    let map: { name: string, value: any }[] = [];
    map.push({name: DSMService.REALM, value: localStorage.getItem(ComponentService.MENU_SELECTED_REALM)});
    for (let onc of requestOncHistoryList) {
      map.push( {name: "requestId", value: onc.oncHistoryDetailId} );
    }
    let ddpParticipantId = this.participant.participant.ddpParticipantId;
    this.dsmService.downloadTissueRequestPDFs( ddpParticipantId, map ).subscribe(
      data => {
        var date = new Date();
        this.downloadFile( data, "_TissueRequest_" + this.facilityName + "_" + Utils.getDateFormatted( date, Utils.DATE_STRING_CVS ) );

        let oncHistories = this.participant.oncHistoryDetails;
        if (oncHistories != null) {
          for (let oncHis of oncHistories) {
            if (oncHis.selected) {
              let date = new Date();
              if (oncHis.tFaxSent == null) {
                oncHis.tFaxSent = Utils.getFormattedDate( date );
                oncHis.tFaxSentBy = this.role.userID();
                this.oncHistoryValueChanged( oncHis.tFaxSent, "tFaxSent", oncHis );
              }
              else if (oncHis.tFaxSent2 == null) {
                oncHis.tFaxSent2 = Utils.getFormattedDate( date );
                oncHis.tFaxSent2By = this.role.userID();
                this.oncHistoryValueChanged( oncHis.tFaxSent2, "tFaxSent2", oncHis );
              }
              else {
                oncHis.tFaxSent3 = Utils.getFormattedDate( date );
                oncHis.tFaxSent3By = this.role.userID();
                this.oncHistoryValueChanged( oncHis.tFaxSent3, "tFaxSent3", oncHis );
              }
              oncHis.changedBy = this.role.userMail();
              oncHis.changed = true;
              oncHis.selected = false;
              this.isOncHistoryDetailChanged = true;
            }
          }
          this.facilityName = null;
        }
      },
      err => {
        if (err._body === Auth.AUTHENTICATION_ERROR) {
          this.router.navigate( [ Statics.HOME_URL ] );
        }
        this.disableTissueRequestButton = false;
        this.additionalMessage = "Error - Downloading pdf file\nPlease contact your DSM developer";
      }
    );
    window.scrollTo( 0, 0 );
  }

  downloadFile( data: Response, type: string ) {
    var blob = new Blob( [ data ], {type: "application/pdf"} );

    let shortId = this.participant.data.profile[ "hruid" ];

    fileSaver.saveAs( blob, shortId + type + Statics.PDF_FILE_EXTENSION );
  }

  getButtonColorStyle( notes: string ): string {
    if (notes != null && notes !== "") {
      return "primary";
    }
    return "basic";
  }

  onSelect( data: TabDirective, tabName: string ): void {
    if (data instanceof TabDirective) {
      // this.selectedTabTitle = data.heading;
      this.activeTab = tabName;
    }
  }

  tabActive( tab: string ): boolean {
    if (this.activeTab === tab) {
      return true;
    }
    return false;
  }

  isPatchedCurrently( field: string ): boolean {
    if (this.currentPatchField === field) {
      return true;
    }
    return false;
  }

  currentField( field: string ) {
    if (field != null || ( field == null && this.patchFinished )) {
      this.currentPatchField = field;
    }
  }

  getInstitutionCount(): number {
    let counter = 0;
    let medicalRecords = this.participant.medicalRecords;
    for (let med of medicalRecords) {
      if (med.showInstitution()) {
        counter = counter + 1;
      }
    }
    return counter;
  }

  getBloodConsent(): boolean {
    return this.participant.data.dsm[ "hasConsentedToBloodDraw" ];
  }

  getTissueConsent(): boolean {
    return this.participant.data.dsm[ "hasConsentedToTissueSample" ];
  }

  private loadAbstractionValues() {
    if (this.participant.participant != null && this.participant.participant.ddpParticipantId != null) {
      // this.summaryFields = [];
      this.loadingParticipantPage = true;
      let ddpParticipantId = this.participant.participant.ddpParticipantId;
      this.dsmService.getAbstractionValues(localStorage.getItem(ComponentService.MENU_SELECTED_REALM), ddpParticipantId).subscribe(
        data => {
          let jsonData: any | any[];
          if (data != null) {
            jsonData = AbstractionWrapper.parse( data );
            if (jsonData.finalFields != null) {
              this.finalFields = [];
              jsonData.finalFields.forEach( ( val ) => {
                let abstractionFieldValue = AbstractionGroup.parse( val );
                this.finalFields.push( abstractionFieldValue );
              } );
            }
            else {
              if (jsonData.abstraction != null) {
                this.abstractionFields = [];
                jsonData.abstraction.forEach( ( val ) => {
                  let abstractionFieldValue = AbstractionGroup.parse( val );
                  this.abstractionFields.push( abstractionFieldValue );
                } );
              }
              if (jsonData.review != null) {
                this.reviewFields = [];
                jsonData.review.forEach( ( val ) => {
                  let abstractionFieldValue = AbstractionGroup.parse( val );
                  this.reviewFields.push( abstractionFieldValue );
                } );
              }
              if (jsonData.qc != null) {
                this.qcFields = [];
                jsonData.qc.forEach( ( val ) => {
                  let abstractionFieldValue = AbstractionGroup.parse( val );
                  this.qcFields.push( abstractionFieldValue );
                } );
              }
            }
          }
          this.loadingParticipantPage = false;
        },
        err => {
          if (err._body === Auth.AUTHENTICATION_ERROR) {
            this.auth.logout();
          }
        }
      );
    }
  }

  lockParticipant( abstraction: Abstraction ) {
    let ddpParticipantId = this.participant.participant.ddpParticipantId;
    this.dsmService.changeMedicalRecordAbstractionStatus(localStorage.getItem(ComponentService.MENU_SELECTED_REALM), ddpParticipantId, "in_progress", abstraction).subscribe(// need to subscribe, otherwise it will not send!
      data => {
        let result = Result.parse( data );
        if (result.code != 200) {
          this.additionalMessage = "Couldn't lock participant";
        }
        else {
          if (result.code === 200 && result.body != null) {
            let jsonData: any | any[] = JSON.parse( result.body );
            let abstraction: Abstraction = Abstraction.parse( jsonData );
            this.participant[ abstraction.activity ] = abstraction;
            this.activeTab = abstraction.activity;
          }
          else {
            this.additionalMessage = "Error";
          }
        }
      },
      err => {
        if (err._body === Auth.AUTHENTICATION_ERROR) {
          this.router.navigate( [ Statics.HOME_URL ] );
        }
      }
    );
  }

  breakLockParticipant( abstraction: Abstraction ) {
    let ddpParticipantId = this.participant.participant.ddpParticipantId;
    this.dsmService.changeMedicalRecordAbstractionStatus(localStorage.getItem(ComponentService.MENU_SELECTED_REALM), ddpParticipantId, "clear", abstraction).subscribe(// need to subscribe, otherwise it will not send!
      data => {
        let result = Result.parse( data );
        if (result.code != 200) {
          this.additionalMessage = "Couldn't break lock of participant";
        }
        else {
          if (result.code === 200 && result.body != null) {
            let jsonData: any | any[] = JSON.parse( result.body );
            let abstraction: Abstraction = Abstraction.parse( jsonData );
            this.participant[ abstraction.activity ] = abstraction;
          }
          else {
            this.additionalMessage = "Error";
          }
        }
      },
      err => {
        if (err._body === Auth.AUTHENTICATION_ERROR) {
          this.router.navigate( [ Statics.HOME_URL ] );
        }
      }
    );
  }

  submitParticipant( abstraction: Abstraction ) {
    let ddpParticipantId = this.participant.participant.ddpParticipantId;
    this.dsmService.changeMedicalRecordAbstractionStatus(localStorage.getItem(ComponentService.MENU_SELECTED_REALM), ddpParticipantId, "submit", abstraction).subscribe(// need to subscribe, otherwise it will not send!
      data => {
        let result = Result.parse( data );
        if (result.code != 200 && result.body != null) {
          this.additionalMessage = result.body;
          abstraction.colorNotFinished = true;
        }
        else if (result.code === 200 && result.body != null) {
          let jsonData: any | any[] = JSON.parse( result.body );
          let abstraction: Abstraction = Abstraction.parse( jsonData );
          this.participant[ abstraction.activity ] = abstraction;
          this.additionalMessage = null;
          abstraction.colorNotFinished = false;
        }
        else {
          this.errorMessage = "Something went wrong! Please contact your DSM developer";
        }

        window.scrollTo( 0, 0 );
      },
      err => {
        if (err._body === Auth.AUTHENTICATION_ERROR) {
          this.router.navigate( [ Statics.HOME_URL ] );
        }
      }
    );
  }

  abstractionFilesUsedChanged( abstraction: Abstraction ) {
    this.currentPatchField = "filesUsed";
    this.patchFinished = false;
    let ddpParticipantId = this.participant.participant.ddpParticipantId;
    this.dsmService.changeMedicalRecordAbstractionStatus(localStorage.getItem(ComponentService.MENU_SELECTED_REALM), ddpParticipantId, null, abstraction).subscribe(// need to subscribe, otherwise it will not send!
      data => {
        let result = Result.parse( data );
        if (result.code != 200 && result.body != null) {
          this.additionalMessage = result.body;
        }
        else if (result.code === 200 && result.body != null) {
          let jsonData: any | any[] = JSON.parse( result.body );
          let abstraction: Abstraction = Abstraction.parse( jsonData );
          this.participant[ abstraction.activity ] = abstraction;
          this.getFileList( abstraction );
          this.additionalMessage = null;
          this.currentPatchField = null;
          this.patchFinished = true;
        }
        else {
          this.errorMessage = "Something went wrong! Please contact your DSM developer";
        }
      },
      err => {
        if (err._body === Auth.AUTHENTICATION_ERROR) {
          this.router.navigate( [ Statics.HOME_URL ] );
        }
      }
    );
  }

  addFileToParticipant( fileName: string, abstraction: Abstraction ) {
    if (abstraction.filesUsed != null && abstraction.filesUsed !== "") {
      let found: boolean = false;
      let files = abstraction.filesUsed.split( /[\s,|;]+/ );
      for (let file of files) {
        if (file === fileName) {
          found = true;
          break;
        }
      }
      if (!found) {
        abstraction.filesUsed = abstraction.filesUsed.concat( ", " + fileName );
        this.abstractionFilesUsedChanged( abstraction );
      }
    }
    else {
      abstraction.filesUsed = fileName;
      this.abstractionFilesUsedChanged( abstraction );
    }
    return false;
  }

  getFileList( abstraction: Abstraction ) {
    if (abstraction.filesUsed != null && abstraction.filesUsed !== "") {
      let files = abstraction.filesUsed.split( /[\s,|;]+/ );
      for (let file of files) {
        if (this.fileListUsed.indexOf( file ) == -1) {
          this.fileListUsed.push( file );
        }
      }
    }
    this.fileListUsed.sort( ( one, two ) => ( one > two ? 1 : -1 ) );
  }

  getMedicalRecordName( name: string, ddpInstitutionId: string ) {
    if (this.participant.data != null && this.participant.data.profile != null && this.participant.data.medicalProviders != null) {
      let medicalProvider = this.participant.data.medicalProviders.find( medicalProvider => {
        return medicalProvider.guid === ddpInstitutionId;
      } );
      if (name) {
        return name;
      }
      else {
        if (medicalProvider != null) {
          if ("PHYSICIAN" === medicalProvider.institutionType) {
            return medicalProvider.physicianName;
          }
          else {
            return medicalProvider.institutionName;
          }
        }
      }
    }
  }

  getActivityDefinition( code: string ) {
    if (this.activityDefinitions != null) {
      return this.activityDefinitions.find( x => x.activityCode === code );
    }
    return null;
  }

  onAdditionalValueChange( evt: any, colName: string ) {
    let v;
    if (typeof evt === "string") {
      v = evt;
    }
    else {
      if (evt.srcElement != null && typeof evt.srcElement.value === "string") {
        v = evt.srcElement.value;
      }
      else if (evt.value != null) {
        v = evt.value;
      }
      else if (evt.checked != null) {
        v = evt.checked;
      }
    }
    if (v !== null) {
      if (this.participant.participant.additionalValues != null) {
        this.participant.participant.additionalValues[ colName ] = v;
      }
      else {
        let addArray = {};
        addArray[ colName ] = v;
        this.participant.participant.additionalValues = addArray;
      }
      this.valueChanged( this.participant.participant.additionalValues, "additionalValues", "r" );
    }
  }

  //display additional value
  getAdditionalValue( colName: string ): string {
    if (this.participant.participant.additionalValues != null) {
      if (this.participant.participant.additionalValues[ colName ] != undefined && this.participant.participant.additionalValues[ colName ] != null) {
        return this.participant.participant.additionalValues[ colName ];
      }
    }
    return null;
  }
}
