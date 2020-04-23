import {Injectable} from "@angular/core";
import {AccessRole} from "../utils/access-role.model";
import {Access} from "../utils/access.model";
import {SessionService} from "./session.service";
import {UserSetting} from "../user-setting/user-setting.model";

@Injectable()
export class RoleService {

  private _noPermissions: boolean = true;
  private _isShipping: boolean = false;
  private _isMRRequesting: boolean = false;
  private _isMRView: boolean = false;
  private _isMailingList: boolean = false;
  private _isUpload: boolean = false;
  private _isExitParticipant: boolean = false;
  private _isDeactivation: boolean = false;
  private _isReceiving: boolean = false;
  private _isExpressKit: boolean = false;
  private _isTriggeringSurveyCreation: boolean = false;
  private _isSkipParticipant: boolean = false;
  private _isDiscardingSamples: boolean = false;
  private _isSampleListView: boolean = false;
  private _isDownloadPDF: boolean = false;
  private _isDownloadNDI: boolean = false;
  private _noTissueRequest: boolean = false;
  private _fieldSettings: boolean = false;
  private _isAbstracter: boolean = false;
  private _isQC: boolean = false;
  private _isAbstractionAdmin: boolean = false;
  private _canEditDrugList: boolean = false;

  private _userId: string;
  private _user: string;
  private _userEmail: string;
  private _userSetting: UserSetting;

  constructor( private sessionService: SessionService ) {
    let token: string = this.sessionService.getDSMToken();
    this.setRoles( token );
  }

  public setRoles( token: string ) {
    if (token != null) {
      var obj: any = this.sessionService.getDSMClaims( token );
      let access: Access;
      if (obj.USER_ACCESS_ROLE != null && obj.USER_ACCESS_ROLE !== "null") {
        access = Access.parse( JSON.parse( obj.USER_ACCESS_ROLE ) );
      }
      if (access != null && access.accessRoles != null) {
        access.accessRoles.forEach( ( accessRole: AccessRole ) => {
          if (accessRole != null && accessRole.permissions != null) {
            accessRole.permissions.forEach( ( permission: string ) => {
              this._noPermissions = false;
              console.log( permission );
              // only special kit_shipping_xxx rights should get added here, not the overall only kit_shipping_view
              if (permission.startsWith( "kit:shipping" ) && permission !== "kit:view") {
                this._isShipping = true;
              }
              else if (permission === "mr:request") {
                this._isMRRequesting = true;
              }
              else if (permission === "mr:view") {
                this._isMRView = true;
              }
              else if (permission === "mailingList_view") {
                this._isMailingList = true;
              }
              else if (permission === "kit:upload") {
                this._isUpload = true;
              }
              else if (permission === "participant:exit") {
                this._isExitParticipant = true;
              }
              else if (permission === "kit:deactivation") {
                this._isDeactivation = true;
              }
              else if (permission === "kit:receiving") {
                this._isReceiving = true;
              }
              else if (permission === "kit:express") {
                this._isExpressKit = true;
              }
              else if (permission === "participant:survey") {
                this._isTriggeringSurveyCreation = true;
              }
              else if (permission === "participant:event") {
                this._isSkipParticipant = true;
              }
              else if (permission === "discard_sample") {
                this._isDiscardingSamples = true;
              }
              else if (permission === "kit:view") {
                this._isSampleListView = true;
              }
              else if (permission === "participant:pdf") {
                this._isDownloadPDF = true;
              }
              else if (permission === "ndi_download") {
                this._isDownloadNDI = true;
              }
              else if (permission === "mr_no_request_tissue") {
                this._noTissueRequest = true;
              }
              else if (permission === "study:fields") {
                this._fieldSettings = true;
              }
              else if (permission === "mr:abstraction") {
                this._isAbstracter = true;
              }
              else if (permission === "mr:qc") {
                this._isQC = true;
              }
              else if (permission === "study:abstraction") {
                this._isAbstractionAdmin = true;
              }
              else if (permission === "drug_list_edit") {
                this._canEditDrugList = true;
              }
            } );
          }
        } );
      }
      let userSettings: any = obj.USER_SETTINGS;
      if (userSettings != null && userSettings !== "null") {
        this._userSetting = UserSetting.parse( JSON.parse( userSettings ) );
      }
      this._userId = obj.USER_ID;
      this._user = obj.USER_NAME;
      this._userEmail = obj.USER_MAIL;
    }
  }

  public userID() {
    return this._userId;
  }

  public userMail() {
    return this._userEmail;
  }

  public getUserName() {
    return this._user;
  }

  public hasNoPermissions() {
    return this._noPermissions;
  }

  public allowedToHandleSamples() {
    return this._isShipping;
  }

  public allowedToViewMedicalRecords() {
    return this._isMRView;
  }

  public allowedToViewMailingList() {
    return this._isMailingList;
  }

  public allowedToUploadKits() {
    return this._isUpload;
  }

  public allowedToExitParticipant() {
    return this._isExitParticipant;
  }

  public allowedToDeactivateKits() {
    return this._isDeactivation;
  }

  public allowedToViewReceivingPage() {
    return this._isReceiving;
  }

  public allowedToCreateExpressLabels() {
    return this._isExpressKit;
  }

  public allowedToCreateSurveys() {
    return this._isTriggeringSurveyCreation;
  }

  public allowedToSkipParticipantEvents() {
    return this._isSkipParticipant;
  }

  public allowedToDiscardSamples() {
    return this._isDiscardingSamples;
  }

  public allowToViewSampleLists() {
    return this._isSampleListView;
  }

  public allowedToDownloadPDF() {
    return this._isDownloadPDF;
  }

  public allowedToDownloadNDI() {
    return this._isDownloadNDI;

  }

  public prohibitedToRequestTissue() {
    return this._noTissueRequest;
  }

  public allowedToChangeFieldSettings() {
    return this._fieldSettings;
  }

  public isAbstracter() {
    return this._isAbstracter;
  }

  public isQC() {
    return this._isQC;
  }

  public isAbstractionAdmin() {
    return this._isAbstractionAdmin;
  }

  public getUserSetting(): UserSetting {
    return this._userSetting;
  }

  public setUserSetting( userSettings: UserSetting ) {
    this._userSetting = userSettings;
  }

  public allowedToEditDrugList() {
    return this._canEditDrugList;
  }
}
