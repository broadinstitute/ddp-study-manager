import {Injectable} from "@angular/core";
import {SessionService} from "./session.service";
import {UserSetting} from "../user-setting/user-setting.model";

@Injectable()
export class RoleService {

  private _isShipping: boolean = false;
  private _isMRRequesting: boolean = false;
  private _isMRView: boolean = false;
  private _isMailingList: boolean = false;
  private _isUpload: boolean = false;
  private _isExitParticipant: boolean = false;
  private _isDeactivation: boolean = false;
  private _isViewingEEL: boolean = false;
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

  private _userId: string;
  private _user: string;
  private _userEmail: string;
  private _userSetting: UserSetting;

  constructor(private sessionService: SessionService) {
    let token: string = this.sessionService.getDSMToken();
    this.setRoles(token);
  }

  public setRoles(token: string) {
    if (token != null) {
      var obj: any = this.sessionService.getDSMClaims(token);
      let accessRoles: string = obj.USER_ACCESS_ROLE;
      if (accessRoles != null) {
        let roles: string[] = JSON.parse(accessRoles);
        for (let entry of roles) {
          // only special kit_shipping_xxx rights should get added here, not the overall only kit_shipping_view
          if (entry.startsWith("kit_shipping") && entry !== "kit_shipping_view") {
            this._isShipping = true;
          }
          else if (entry === "mr_request") {
            this._isMRRequesting = true;
          }
          else if (entry === "mr_view") {
            this._isMRView = true;
          }
          else if (entry === "mailingList_view") {
            this._isMailingList = true;
          }
          else if (entry === "kit_upload") {
            this._isUpload = true;
          }
          else if (entry === "participant_exit") {
            this._isExitParticipant = true;
          }
          else if (entry === "kit_deactivation") {
            this._isDeactivation = true;
          }
          else if (entry === "eel_view") {
            this._isViewingEEL = true;
          }
          else if (entry === "kit_receiving") {
            this._isReceiving = true;
          }
          else if (entry === "kit_express") {
            this._isExpressKit = true;
          }
          else if (entry === "survey_creation") {
            this._isTriggeringSurveyCreation = true;
          }
          else if (entry === "participant_event") {
            this._isSkipParticipant = true;
          }
          else if (entry === "discard_sample") {
            this._isDiscardingSamples = true;
          }
          else if (entry === "kit_shipping_view") {
            this._isSampleListView = true;
          }
          else if (entry === "pdf_download") {
            this._isDownloadPDF = true;
          }
          else if (entry === "ndi_download") {
            this._isDownloadNDI = true;
          }
          else if (entry === "mr_no_request_tissue") {
            this._noTissueRequest = true;
          }
          else if (entry === "field_settings") {
            this._fieldSettings = true;
          }
          else if (entry === "mr_abstracter") {
            this._isAbstracter = true;
          }
          else if (entry === "mr_qc") {
            this._isQC = true;
          }
          else if (entry === "mr_abstraction_admin") {
            this._isAbstractionAdmin = true;
          }
        }
      }
      let userSettings: any = obj.USER_SETTINGS;
      if (userSettings != null && userSettings !== "null") {
        this._userSetting = UserSetting.parse(JSON.parse(userSettings));
      }
      this._userId = obj.USER_ID;
      this._user = obj.USER_NAME;
      this._userEmail = obj.USER_MAIL;
      // console.log(obj);
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

  public allowedToViewEELData() {
    return this._isViewingEEL;
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

  public setUserSetting(userSettings: UserSetting) {
    this._userSetting = userSettings;
  }
}
