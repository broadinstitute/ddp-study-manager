import {Component, OnInit, ViewChild} from "@angular/core";
import {ActivatedRoute} from "@angular/router";

import {DSMService} from "../services/dsm.service";
import {Auth} from "../services/auth.service";
import {Utils} from "../utils/utils";
import {UploadParticipant, UploadResponse} from "./upload.model";
import {KitType} from "../utils/kit-type.model";
import {ModalComponent} from "../modal/modal.component";
import {ComponentService} from "../services/component.service";
import {Statics} from "../utils/statics";
import {FieldFilepickerComponent} from "../field-filepicker/field-filepicker.component";
import {Result} from "../utils/result.model";

@Component( {
  selector: "app-upload",
  templateUrl: "./upload.component.html",
  styleUrls: [ "./upload.component.css" ]
} )
export class UploadComponent implements OnInit {

  @ViewChild( ModalComponent )
  public modal: ModalComponent;

  @ViewChild( FieldFilepickerComponent )
  public filepicker: FieldFilepickerComponent;

  errorMessage: string;
  additionalMessage: string;

  loading: boolean = false;
  uploadPossible: boolean = false;

  kitTypes: Array<KitType> = [];
  kitType: KitType;

  file: File = null;

  failedParticipants: Array<UploadParticipant> = [];
  duplicateParticipants: Array<UploadParticipant> = [];
  specialKits: Array<UploadParticipant> = [];

  realmNameStoredForFile: string;
  allowedToSeeInformation: boolean = false;
  specialMessage: string;

  constructor( private dsmService: DSMService, private auth: Auth, private compService: ComponentService, private route: ActivatedRoute ) {
    if (!auth.authenticated()) {
      auth.logout();
    }
    this.route.queryParams.subscribe( params => {
      let realm = params[ DSMService.REALM ] || null;
      if (realm != null && realm !== "") {
        //        this.compService.realmMenu = realm;
        this.checkRight();
      }
    } );
  }

  private checkRight() {
    this.allowedToSeeInformation = false;
    this.additionalMessage = null;
    let jsonData: any[];
    this.dsmService.getRealmsAllowed( Statics.SHIPPING ).subscribe(
      data => {
        jsonData = data;
        jsonData.forEach( ( val ) => {
          if (localStorage.getItem( ComponentService.MENU_SELECTED_REALM ) === val) {
            this.allowedToSeeInformation = true;
            this.getPossibleKitType();
          }
        } );
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
    if (localStorage.getItem( ComponentService.MENU_SELECTED_REALM ) != null) {
      this.checkRight();
    }
    else {
      this.additionalMessage = "Please select a realm";
    }
    window.scrollTo( 0, 0 );
  }

  getPossibleKitType() {
    if (localStorage.getItem( ComponentService.MENU_SELECTED_REALM ) != null && localStorage.getItem( ComponentService.MENU_SELECTED_REALM ) !== "") {
      this.loading = true;
      let jsonData: any[];
      this.dsmService.getKitTypes( localStorage.getItem( ComponentService.MENU_SELECTED_REALM ) ).subscribe(
        data => {
          this.kitTypes = [];
          jsonData = data;
          jsonData.forEach( ( val ) => {
            let kitType = KitType.parse( val );
            this.kitTypes.push( kitType );
          } );
          // console.info(`${this.kitTypes.length} kit types received: ${JSON.stringify(data, null, 2)}`);
          this.loading = false;
        },
        err => {
          if (err._body === Auth.AUTHENTICATION_ERROR) {
            this.auth.logout();
          }
          this.loading = false;
          this.errorMessage = "Error - Loading kit types\n" + err;
        }
      );
    }
  }

  typeChecked( type: KitType ) {
    this.uploadPossible = false;
    if (type.selected) {
      this.kitType = type;
      this.uploadPossible = true;
    }
    else {
      this.kitType = null;
    }
    for (let kit of this.kitTypes) {
      if (kit !== type) {
        if (kit.selected) {
          kit.selected = false;
        }
      }
    }
  }

  upload() {
    this.errorMessage = null;
    this.additionalMessage = null;
    this.failedParticipants = [];
    this.loading = true;
    let jsonData: any[];
    this.dsmService.uploadTxtFile( localStorage.getItem( ComponentService.MENU_SELECTED_REALM ), this.kitType.name, this.file ).subscribe(
      data => {
        this.loading = false;
        // console.log(`received: ${JSON.stringify(data, null, 2)}`);
        if (typeof data === "string") {
          this.errorMessage = "Error - Uploading txt.\n" + data;
        }
        else {

          let result = Result.parse( data );
          if (result.code == 500) {
            this.additionalMessage = result.body;
          }
          else {
            let response: UploadResponse = UploadResponse.parse( data );
            response.invalidKitAddressList.forEach( ( val ) => {
              this.failedParticipants.push( UploadParticipant.parse( val ) );
            } );
            if (this.failedParticipants.length > 0) {
              this.errorMessage = "Participants uploaded.\nCouldn't create kit requests for " + this.failedParticipants.length + " participant(s)";
            }

            response.duplicateKitList.forEach( ( val ) => {
              this.duplicateParticipants.push( UploadParticipant.parse( val ) );
            } );
            response.specialKitList.forEach( ( val ) => {
              this.specialKits.push( UploadParticipant.parse( val ) );
            } );
            this.specialMessage = response.specialMessage;

            if (this.duplicateParticipants.length > 0 || this.specialKits.length > 0) {
              this.modal.show();
            }
            if (this.failedParticipants.length === 0 && this.duplicateParticipants.length === 0) {
              this.additionalMessage = "All participants were uploaded.";
              this.file = null;
              this.filepicker.unselectFile();
              this.realmNameStoredForFile = localStorage.getItem( ComponentService.MENU_SELECTED_REALM );
            }
          }
        }
      },
      err => {
        this.loading = false;
        if (err._body === Auth.AUTHENTICATION_ERROR) {
          this.auth.logout();
        }
        this.errorMessage = "Error - Uploading txt\n" + err;
      }
    );
  }

  downloadFailed() {
    if (this.failedParticipants != null && this.failedParticipants.length > 0 && this.realmNameStoredForFile != null &&
      this.kitType != null && this.kitType.name != null) {
      var fields = [ "altPID", "shortId", "firstName", "lastName", "street1", "street2",
        "city", "postalCode", "state", "country" ];
      var date = new Date();
      Utils.createCSV( fields, this.failedParticipants, "Upload " + this.realmNameStoredForFile + " " + this.kitType.name + " Failed " + Utils.getDateFormatted( date, Utils.DATE_STRING_CVS ) + Statics.CSV_FILE_EXTENSION );
      this.realmNameStoredForFile = null;
    }
    else {
      this.errorMessage = "Error - Couldn't save failed participant list";
    }
  }

  uploadDuplicate() {
    this.additionalMessage = null;
    this.errorMessage = null;
    this.loading = true;
    let array: UploadParticipant[] = [];
    for (let i = this.duplicateParticipants.length - 1; i >= 0; i--) {
      if (this.duplicateParticipants[ i ].selected) {
        array.push(this.duplicateParticipants[i]);
      }
    }
    for (let i = this.specialKits.length - 1; i >= 0; i--) {
      if (this.specialKits[ i ].selected) {
        array.push(this.specialKits[i]);
      }
    }
    let jsonParticipants = JSON.stringify( array );
    this.dsmService.uploadDuplicateParticipant( localStorage.getItem( ComponentService.MENU_SELECTED_REALM ), this.kitType.name, jsonParticipants ).subscribe(
      data => {
        this.loading = false;
        if (typeof data === "string") {
          this.errorMessage = "Error - Uploading duplicate.\n" + data;
        }
        else {
          this.additionalMessage = "All participants were uploaded.";
        }
      },
      err => {
        this.loading = false;
        if (err._body === Auth.AUTHENTICATION_ERROR) {
          this.auth.logout();
        }
        this.errorMessage = "Error - Uploading txt\n" + err;
      }
    );
    this.emptyUpload();
  }

  forgetDuplicate() {
    this.additionalMessage = "No duplicate kits uploaded";
    this.emptyUpload();
  }

  emptyUpload() {
    this.modal.hide();
    this.duplicateParticipants = [];
    this.specialKits = [];
    this.file = null;
    this.filepicker.unselectFile();
    this.realmNameStoredForFile = localStorage.getItem( ComponentService.MENU_SELECTED_REALM );
  }

  fileSelected( file: File ) {
    this.file = file;
  }

  getCompService(): ComponentService {
    return this.compService;
  }

}
