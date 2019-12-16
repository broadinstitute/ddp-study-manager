import {Injectable} from "@angular/core";
import {Headers, Http, RequestOptions, Response, ResponseContentType, URLSearchParams} from "@angular/http";
import {JwtHelper} from "angular2-jwt";
import {Observable} from "rxjs";
import {SessionService} from "./session.service";
import {RoleService} from "./role.service";
import {Router} from "@angular/router";
import {Statics} from "../utils/statics";
import {ComponentService} from "./component.service";
import {Abstraction} from "../medical-record-abstraction/medical-record-abstraction.model";
import {ViewFilter} from "../filter-column/models/view-filter.model";
import {Filter} from "../filter-column/filter-column.model";

declare var DDP_ENV: any;

@Injectable()
export class DSMService {

  public static UI: string = "ui/";

  public static REALM: string = "realm";
  public static TARGET: string = "target";
  public static SCAN_TRACKING: string = "scanTracking";
  public static SCAN_RECEIVED: string = "scanReceived";

  private baseUrl = DDP_ENV.baseUrl;

  constructor( private http: Http, private sessionService: SessionService, private role: RoleService, private router: Router ) {
  }

  public transferScan( scanTracking: boolean, json: string ) {
    let url = this.baseUrl + DSMService.UI;
    if (scanTracking) {
      url += "trackingScan";
    }
    else {
      url += "finalScan";
    }
    let map: { name: string, value: any }[] = [];
    map.push( {name: "userId", value: this.role.userID()} );
    return this.http.post( url, json, this.buildQueryHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public setKitReceivedRequest( json: string ) {
    let url = this.baseUrl + DSMService.UI + "receivedKits";
    let map: { name: string, value: any }[] = [];
    map.push( {name: "userId", value: this.role.userID()} );
    return this.http.post( url, json, this.buildQueryHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public setKitSentRequest( json: string ) {
    let url = this.baseUrl + DSMService.UI + "sentKits";
    let map: { name: string, value: any }[] = [];
    map.push( {name: "userId", value: this.role.userID()} );
    return this.http.post( url, json, this.buildQueryHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public getKitRequests( realm: string, target: string, name: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "kitRequests";
    let map: { name: string, value: any }[] = [];
    map.push( {name: DSMService.REALM, value: realm} );
    map.push( {name: DSMService.TARGET, value: target} );
    map.push( {name: "kitType", value: name} );
    return this.http.get( url, this.buildQueryHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public getFiltersForUserForRealm( realm: string, parent: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "getFilters";
    let map: { name: string, value: any }[] = [];
    let userId = this.role.userID();
    map.push( {name: DSMService.REALM, value: realm} );
    map.push( {name: "userId", value: userId} );
    map.push( {name: "parent", value: parent} );
    return this.http.get( url, this.buildQueryHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public setDefaultFilter( json: string, filterName: string, parent: string, realm ) {
    let url = this.baseUrl + DSMService.UI + "defaultFilter";
    let map: { name: string, value: any }[] = [];
    map.push({name: "filterName", value: filterName});
    map.push({name: "parent", value: parent});
    map.push({name: "userId", value: this.role.userID()});
    map.push({name: "userMail", value: this.role.userMail()});
    map.push({name: DSMService.REALM, value: realm});
    return this.http.patch(url, json, this.buildQueryHeader(map)).map((res: Response) => res.json()).catch(this.handleError);
  }


  public filterData( realm: string, json: string, parent: string, defaultFilter: boolean ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "filterList";
    let map: { name: string, value: any }[] = [];
    map.push( {name: DSMService.REALM, value: realm} );
    map.push( {name: "parent", value: parent} );
    map.push( {name: "userId", value: this.role.userID()} );
    map.push( {name: "userMail", value: this.role.userMail()} );
    map.push( {name: "defaultFilter", value: defaultFilter == true ? "1" : defaultFilter != null ? "0" : ""} );
    return this.http.patch( url, json, this.buildQueryHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public saveCurrentFilter( json: string, realm: string, parent: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "saveFilter";
    let userId = this.role.userID();
    let map: { name: string, value: any }[] = [];
    map.push( {name: DSMService.REALM, value: realm} );
    map.push( {name: "parent", value: parent} );
    map.push( {name: "userId", value: userId} );
    return this.http.patch( url, json, this.buildQueryHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public applyFilter( json: ViewFilter, realm: string, parent: string, filterQuery: string ): Observable<any> {
    if (json != null && json.filters != null) {
      for (let filter of json.filters) {
        if (filter.type === Filter.OPTION_TYPE) {
          filter.selectedOptions = filter.getSelectedOptionsName();
        }
      }
    }
    let url = this.baseUrl + DSMService.UI + "applyFilter";
    let userId = this.role.userID();
    let map: { name: string, value: any }[] = [];
    map.push( {name: DSMService.REALM, value: realm} );
    map.push( {name: "userId", value: userId} );
    map.push( {name: "parent", value: parent} );
    if (filterQuery != null) {
      map.push( {name: "filterQuery", value: filterQuery} );
    }
    else if (json == null || json.filters == undefined || json.filters == null) {
      map.push( {name: "filterName", value: json == null ? null : json.filterName} );
    }
    else {
      map.push( {name: "filters", value: JSON.stringify( json.filters )} );
    }
    return this.http.get( url, this.buildQueryHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public getParticipantData( realm: string, ddpParticipantId: string, parent: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "getParticipant";
    let map: { name: string, value: any }[] = [];
    let userId = this.role.userID();
    map.push( {name: DSMService.REALM, value: realm} );
    map.push( {name: "ddpParticipantId", value: ddpParticipantId} );
    map.push( {name: "userId", value: userId} );
    map.push( {name: "parent", value: parent} );
    return this.http.get( url, this.buildQueryHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public getSettings( realm: string, parent: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "displaySettings/" + realm;
    let map: { name: string, value: any }[] = [];
    map.push( {name: "userId", value: this.role.userID()} );
    map.push( {name: "parent", value: parent} );
    return this.http.get( url, this.buildQueryHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  // returns a comma-separated list of drug display names
  public getDrugs(): Observable<any> {
    let url = this.baseUrl + DSMService.UI + 'drugs';
    return this.http.get( url, this.buildHeader() ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  // returns drug list entries with all fields
  public getFullDrugData(): Observable<any> {
    let url = this.baseUrl + DSMService.UI + 'druglistEntries';
    return this.http.get( url, this.buildHeader() ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public saveDruglistEntries( json: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + 'druglistEntries';
    return this.http.patch( url, json, this.buildHeader() ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public getMedicalRecordData( realm: string, ddpParticipantId: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "institutions";
    let json = {
      ddpParticipantId: ddpParticipantId,
      realm: realm,
      userId: this.role.userID()
    };
    return this.http.post( url, JSON.stringify( json ), this.buildHeader() ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public assignParticipant( realm: string, assignMR: boolean, assignTissue: boolean, json: string ) {
    let url = this.baseUrl + DSMService.UI + "assignParticipant";
    let map: { name: string, value: any }[] = [];
    map.push( {name: DSMService.REALM, value: realm} );
    if (assignMR) {
      map.push( {name: "assignMR", value: assignMR} );
    }
    if (assignTissue) {
      map.push( {name: "assignTissue", value: assignTissue} );
    }
    return this.http.patch( url, json, this.buildQueryHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public patchParticipantRecord( json: string ) {
    let url = this.baseUrl + DSMService.UI + "patch";
    return this.http.patch( url, json, this.buildHeader() ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public downloadCoverPDFs( ddpParticipantId: string, medicalRecordId: string, startDate: string, endDate: string, notesCb: boolean,
                            treatmentCb: boolean, pathologyCb: boolean, operativeCb: boolean, referralsCb: boolean, exchangeCb: boolean,
                            geneticCb: boolean, realm: string ) {
    let url = this.baseUrl + DSMService.UI + "downloadPDF/cover/" + medicalRecordId;
    let map: { name: string, value: any }[] = [];
    map.push( {name: DSMService.REALM, value: realm} );
    let json = {
      ddpParticipantId: ddpParticipantId,
      startDate: startDate,
      endDate: endDate,
      notesCb: notesCb,
      treatmentCb: treatmentCb,
      pathologyCb: pathologyCb,
      operativeCb: operativeCb,
      referralsCb: referralsCb,
      exchangeCb: exchangeCb,
      geneticCb: geneticCb,
      userId: this.role.userID()
    };
    // console.log(json);
    return this.http.post( url, JSON.stringify( json ), this.buildQueryPDFHeader( map ) ).map( ( res: Response ) => res.blob() ).catch( this.handleError );
  }

  public downloadPDF( ddpParticipantId: string, realm: string, configName: string ) {
    let url = this.baseUrl + DSMService.UI + "downloadPDF/pdf";
    let map: { name: string, value: any }[] = [];
    map.push( {name: DSMService.REALM, value: realm} );
    let json = {
      ddpParticipantId: ddpParticipantId,
      configName: configName,
      userId: this.role.userID()
    };
    return this.http.post( url, JSON.stringify( json ), this.buildQueryPDFHeader( map ) ).map( ( res: Response ) => res.blob() ).catch( this.handleError );
  }

  public downloadConsentPDFs( ddpParticipantId: string, realm: string ) {
    let url = this.baseUrl + DSMService.UI + "downloadPDF/consentpdf";
    let map: { name: string, value: any }[] = [];
    map.push( {name: DSMService.REALM, value: realm} );
    let json = {
      ddpParticipantId: ddpParticipantId,
      userId: this.role.userID()
    };
    return this.http.post( url, JSON.stringify( json ), this.buildQueryPDFHeader( map ) ).map( ( res: Response ) => res.blob() ).catch( this.handleError );
  }

  public downloadReleasePDFs( ddpParticipantId: string, realm: string ) {
    let url = this.baseUrl + DSMService.UI + "downloadPDF/releasepdf";
    let map: { name: string, value: any }[] = [];
    map.push( {name: DSMService.REALM, value: realm} );
    let json = {
      ddpParticipantId: ddpParticipantId,
      userId: this.role.userID()
    };
    return this.http.post( url, JSON.stringify( json ), this.buildQueryPDFHeader( map ) ).map( ( res: Response ) => res.blob() ).catch( this.handleError );
  }

  public downloadTissueRequestPDFs( ddpParticipantId: string, map: { name: string, value: any }[] ) {
    let url = this.baseUrl + DSMService.UI + "downloadPDF/requestpdf";
    let json = {
      ddpParticipantId: ddpParticipantId,
      userId: this.role.userID()
    };
    return this.http.post( url, JSON.stringify( json ), this.buildQueryPDFHeader( map ) ).map( ( res: Response ) => res.blob() ).catch( this.handleError );
  }

  public getParticipant( participantId: string, realm: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "participant/" + participantId;
    let map: { name: string, value: any }[] = [];
    map.push( {name: DSMService.REALM, value: realm} );
    return this.http.get( url, this.buildQueryHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public getMedicalRecord( participantId: string, institutionId: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "participant/" + participantId + "/institution/" + institutionId;
    return this.http.get( url, this.buildHeader() ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public getMedicalRecordLog( medicalRecordId: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "medicalRecord/" + medicalRecordId + "/log";
    return this.http.get( url, this.buildHeader() ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public saveMedicalRecordLog( medicalRecordId: string, json: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "medicalRecord/" + medicalRecordId + "/log";
    return this.http.patch( url, json, this.buildHeader() ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public getMedicalRecordDashboard( realm: string, startDate: string, endDate: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "ddpInformation/" + startDate + "/" + endDate;
    let map: { name: string, value: any }[] = [];
    map.push( {name: DSMService.REALM, value: realm} );
    map.push( {name: "userId", value: this.role.userID()} );
    return this.http.get( url, this.buildQueryHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public getShippingReportOverview(): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "sampleReport";
    let map: { name: string, value: any }[] = [];
    map.push( {name: "userId", value: this.role.userID()} );
    return this.http.get( url, this.buildQueryHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public getShippingReport( startDate: string, endDate: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "sampleReport/" + startDate + "/" + endDate;
    let map: { name: string, value: any }[] = [];
    map.push( {name: "userId", value: this.role.userID()} );
    return this.http.get( url, this.buildQueryHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public getShippingOverview(): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "ddpInformation";
    let map: { name: string, value: any }[] = [];
    map.push( {name: "userId", value: this.role.userID()} );
    return this.http.get( url, this.buildQueryHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public getShippingDashboard( realm: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "ddpInformation";
    let map: { name: string, value: any }[] = [];
    map.push( {name: DSMService.REALM, value: realm} );
    map.push( {name: "userId", value: this.role.userID()} );
    return this.http.get( url, this.buildQueryHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public getKit( field: string, value: string, realms: string[] ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "searchKit";
    let map: { name: string, value: any }[] = [];
    map.push( {name: "field", value: field} );
    map.push( {name: "value", value: value} );
    for (var i of realms) {
      map.push( {name: "realm", value: i} );
    }
    return this.http.get( url, this.buildQueryHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public lookupValue( lookupType: string, lookupValue: string, realm: string ): Observable<any> {
    if (lookupType === "mrContact" || lookupType === "tSite") {
      return this.lookup( lookupType, lookupValue, null, null );
    }
    else if (lookupType === "tHistology" || lookupType === "tFacility" || lookupType === "tType") {
      return this.lookup( lookupType, lookupValue, realm, null );
    }
  }

  public lookupCollaboratorId( lookupType: string, participantId: string, shortId: string, realm: string ): Observable<any> {
    return this.lookup( lookupType, participantId, realm, shortId );
  }

  public lookup( field: string, lookupValue: string, realm: string, shortId: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "lookup";
    let map: { name: string, value: any }[] = [];
    map.push( {name: "field", value: field} );
    map.push( {name: "value", value: lookupValue} );
    if (realm != null) {
      map.push( {name: "realm", value: realm} );
    }
    if (shortId != null) {
      map.push( {name: "shortId", value: shortId} );
    }
    return this.http.get( url, this.buildQueryHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public getMailingList( realm: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "mailingList/" + realm;
    return this.http.get( url, this.buildHeader() ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public getRealmsAllowed( menu: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "realmsAllowed";
    let map: { name: string, value: any }[] = [];
    if (menu != null) {
      map.push( {name: "menu", value: menu} );
    }
    map.push( {name: "userId", value: this.role.userID()} );
    return this.http.get( url, this.buildQueryHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public getKitTypes( realm: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "kitTypes/" + realm;
    let map: { name: string, value: any }[] = [];
    map.push( {name: "userId", value: this.role.userID()} );
    return this.http.get( url, this.buildQueryHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public uploadTxtFile( realm: string, kitType: string, file: File ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "kitUpload";
    let map: { name: string, value: any }[] = [];
    map.push( {name: DSMService.REALM, value: realm} );
    map.push( {name: "kitType", value: kitType} );
    map.push( {name: "userId", value: this.role.userID()} );
    return this.http.post( url, file, this.buildQueryUploadHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public uploadNdiFile( file: File ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "ndiRequest";
    let map: { name: string, value: any }[] = [];
    map.push( {name: "userId", value: this.role.userID()} );
    return this.http.post( url, file, this.buildQueryUploadHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public uploadDuplicateParticipant( realm: string, kitType: string, jsonParticipants: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "kitUpload";
    let map: { name: string, value: any }[] = [];
    map.push( {name: DSMService.REALM, value: realm} );
    map.push( {name: "kitType", value: kitType} );
    map.push( {name: "userId", value: this.role.userID()} );
    map.push( {name: "uploadDuplicate", value: true} );
    return this.http.post( url, jsonParticipants, this.buildQueryUploadHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public kitLabel( realm: string, kitType: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "kitLabel";
    let map: { name: string, value: any }[] = [];
    map.push( {name: DSMService.REALM, value: realm} );
    map.push( {name: "kitType", value: kitType} );
    map.push( {name: "userId", value: this.role.userID()} );
    return this.http.post( url, null, this.buildQueryHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public singleKitLabel( kitJson: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "kitLabel";
    let map: { name: string, value: any }[] = [];
    map.push( {name: "userId", value: this.role.userID()} );
    return this.http.post( url, kitJson, this.buildQueryHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public getLabelCreationStatus(): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "kitLabel";
    return this.http.get( url, this.buildHeader() ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public exitParticipant( json: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "exitParticipant";
    return this.http.post( url, json, this.buildHeader() ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public getPossibleSurveys( realm: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "triggerSurvey/" + realm;
    return this.http.get( url, this.buildHeader() ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public getSurveyStatus( realm: string, survey: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "triggerSurvey/" + realm;
    let map: { name: string, value: any }[] = [];
    map.push( {name: "surveyName", value: survey} );
    return this.http.get( url, this.buildQueryHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public triggerSurvey( realm: string, surveyName: string, surveyType: string, comment: string, isFileUpload: boolean, payload: any ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "triggerSurvey";
    let map: { name: string, value: any }[] = [];
    map.push( {name: DSMService.REALM, value: realm} );
    map.push( {name: "surveyName", value: surveyName} );
    map.push( {name: "surveyType", value: surveyType} );
    map.push( {name: "userId", value: this.role.userID()} );
    map.push( {name: "comment", value: comment} );
    map.push( {name: "isFileUpload", value: isFileUpload} );
    return this.http.post( url, payload, this.buildQueryUploadHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public triggerAgain( realm: string, surveyName: string, surveyType: string, comment: string, jsonParticipants: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "triggerSurvey";
    let map: { name: string, value: any }[] = [];
    map.push( {name: DSMService.REALM, value: realm} );
    map.push( {name: "surveyName", value: surveyName} );
    map.push( {name: "surveyType", value: surveyType} );
    map.push( {name: "userId", value: this.role.userID()} );
    map.push( {name: "comment", value: comment} );
    map.push( {name: "triggerAgain", value: true} );
    map.push( {name: "isFileUpload", value: false} );
    return this.http.post( url, jsonParticipants, this.buildQueryHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public getPossibleEventTypes( realm: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "eventTypes/" + realm;
    return this.http.get( url, this.buildHeader() ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public getPossiblePDFs( realm: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "pdfs/" + realm;
    return this.http.get( url, this.buildHeader() ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public getSkippedParticipantEvents( realm: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "participantEvents/" + realm;
    return this.http.get( url, this.buildHeader() ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public skipEvent( json: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "skipEvent";
    return this.http.post( url, json, this.buildHeader() ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public getExitedParticipants( realm: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "exitParticipant/" + realm;
    return this.http.get( url, this.buildHeader() ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public getKitExitedParticipants( realm: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "discardSamples";
    let map: { name: string, value: any }[] = [];
    map.push( {name: DSMService.REALM, value: realm} );
    return this.http.get( url, this.buildQueryHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public setKitDiscardAction( realm: string, json: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "discardSamples";
    let map: { name: string, value: any }[] = [];
    map.push( {name: DSMService.REALM, value: realm} );
    return this.http.patch( url, json, this.buildQueryHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public showUpload( realm: string, json: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "showUpload";
    let map: { name: string, value: any }[] = [];
    map.push( {name: DSMService.REALM, value: realm} );
    return this.http.patch( url, json, this.buildQueryPDFHeader( map ) ).map( ( res: Response ) => res.blob() ).catch( this.handleError );
  }

  public setKitDiscarded( realm: string, json: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "discardSamples";
    let map: { name: string, value: any }[] = [];
    map.push( {name: DSMService.REALM, value: realm} );
    map.push( {name: "userId", value: this.role.userID()} );
    return this.http.patch( url, json, this.buildQueryHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public uploadFile( realm: string, kitDiscardId: string, pathName: string, payload: File ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "discardUpload";
    let map: { name: string, value: any }[] = [];
    map.push( {name: DSMService.REALM, value: realm} );
    map.push( {name: "kitDiscardId", value: kitDiscardId} );
    map.push( {name: pathName, value: payload.name} );
    map.push( {name: "userId", value: this.role.userID()} );
    return this.http.post( url, payload, this.buildQueryUploadHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public deleteFile( realm: string, kitDiscardId: string, pathName: string, path: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "discardUpload";
    let map: { name: string, value: any }[] = [];
    map.push( {name: DSMService.REALM, value: realm} );
    map.push( {name: "kitDiscardId", value: kitDiscardId} );
    map.push( {name: "delete", value: true} );
    map.push( {name: pathName, value: path} );
    map.push( {name: "userId", value: this.role.userID()} );
    return this.http.post( url, null, this.buildQueryHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public saveNote( realm: string, kitDiscardId: string, note: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "discardUpload";
    let map: { name: string, value: any }[] = [];
    map.push( {name: DSMService.REALM, value: realm} );
    map.push( {name: "kitDiscardId", value: kitDiscardId} );
    map.push( {name: "note", value: note} );
    map.push( {name: "userId", value: this.role.userID()} );
    return this.http.post( url, null, this.buildQueryHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public confirm( realm: string, payload: String ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "discardConfirm";
    let map: { name: string, value: any }[] = [];
    map.push( {name: DSMService.REALM, value: realm} );
    return this.http.post( url, payload, this.buildQueryHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public deactivateKitRequest( kitRequestId: string, json: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "deactivateKit/" + kitRequestId;
    let map: { name: string, value: any }[] = [];
    map.push( {name: "userId", value: this.role.userID()} );
    return this.http.patch( url, json, this.buildQueryHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public expressLabel( kitRequestId: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "expressKit/" + kitRequestId;
    let map: { name: string, value: any }[] = [];
    map.push( {name: "userId", value: this.role.userID()} );
    return this.http.patch( url, null, this.buildQueryHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public rateOfExpressLabel( kitRequestId: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "expressKit/" + kitRequestId;
    let map: { name: string, value: any }[] = [];
    map.push( {name: "userId", value: this.role.userID()} );
    return this.http.get( url, this.buildQueryHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public activateKitRequest( kitRequestId: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "activateKit/" + kitRequestId;
    return this.http.patch( url, null, this.buildHeader() ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public saveUserSettings( json: string ) {
    let url = this.baseUrl + DSMService.UI + "userSettings";
    let map: { name: string, value: any }[] = [];
    map.push( {name: "userId", value: this.role.userID()} );
    return this.http.patch( url, json, this.buildQueryHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public getEmailEventData( source: string, target: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "emailEvent/" + source;
    let map: { name: string, value: any }[] = [];
    if (target != null && target !== "") {
      map.push( {name: DSMService.TARGET, value: target} );
    }
    return this.http.get( url, this.buildQueryHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public getEmailSettings( source: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "emailSettings/" + source;
    return this.http.get( url, this.buildHeader() ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public saveEmailSettings( source: string, json: string ) {
    let url = this.baseUrl + DSMService.UI + "emailSettings/" + source;
    return this.http.patch( url, json, this.buildHeader() ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public followUpEmailEvent( source: string, json: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "followUpEmailEvent/" + source;
    let map: { name: string, value: any }[] = [];
    map.push( {name: "userId", value: this.role.userID()} );
    return this.http.patch( url, json, this.buildQueryHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public getFieldSettings( source: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "fieldSettings/" + source;
    return this.http.get( url, this.buildHeader() ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public saveFieldSettings( source: string, json: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "fieldSettings/" + source;
    return this.http.patch( url, json, this.buildHeader() ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public applyDestructionPolicyToAll( source: string, json: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "institutions";
    return this.http.patch( url, json, this.buildHeader() ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public getMedicalRecordAbstractionFormControls( realm: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "abstractionformcontrols";
    let map: { name: string, value: any }[] = [];
    map.push( {name: DSMService.REALM, value: realm} );
    return this.http.get( url, this.buildQueryHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public saveMedicalRecordAbstractionFormControls( realm: string, json: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "abstractionformcontrols";
    let map: { name: string, value: any }[] = [];
    map.push( {name: DSMService.REALM, value: realm} );
    return this.http.patch( url, json, this.buildQueryHeader( map ) ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public getAbstractionValues( realm: string, ddpParticipantId: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "abstraction";
    let json = {
      ddpParticipantId: ddpParticipantId,
      realm: realm
    };
    return this.http.post( url, JSON.stringify( json ), this.buildHeader() ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public changeMedicalRecordAbstractionStatus( realm: string, ddpParticipantId: string, status: string, abstraction: Abstraction ) {
    let url = this.baseUrl + DSMService.UI + "abstraction";
    let json = {
      ddpParticipantId: ddpParticipantId,
      realm: realm,
      status: status,
      userId: this.role.userID(),
      abstraction: abstraction
    };
    return this.http.post( url, JSON.stringify( json ), this.buildHeader() ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public getLabelSettings(): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "labelSettings";
    return this.http.get( url, this.buildHeader() ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  public saveLabelSettings( json: string ): Observable<any> {
    let url = this.baseUrl + DSMService.UI + "labelSettings";
    return this.http.patch( url, json, this.buildHeader() ).map( ( res: Response ) => res.json() ).catch( this.handleError );
  }

  private handleError( error: any ) {
    console.error( "ERROR: " + JSON.stringify( error ) ); // log to console instead
    return Observable.throw( error );
  }

  private buildHeader(): RequestOptions {
    return new RequestOptions( {headers: this.buildJsonAuthHeader(), withCredentials: true} );
  }

  private buildPDFHeader(): RequestOptions {
    return new RequestOptions( {
      headers: this.buildJsonAuthHeader(),
      withCredentials: true,
      responseType: ResponseContentType.Blob
    } );
  }

  private buildQueryPDFHeader( map: any[] ): RequestOptions {
    let params: URLSearchParams = new URLSearchParams();
    for (let i in map) {
      params.append( map[ i ].name, map[ i ].value );
    }
    return new RequestOptions( {
      headers: this.buildJsonAuthHeader(),
      withCredentials: true,
      responseType: ResponseContentType.Blob,
      search: params
    } );
  }

  private buildQueryHeader( map: any[] ): RequestOptions {
    let params: URLSearchParams = new URLSearchParams();
    for (let i in map) {
      params.append( map[ i ].name, map[ i ].value );
    }
    return new RequestOptions( {headers: this.buildJsonAuthHeader(), withCredentials: true, search: params} );
  }

  private buildQueryUploadHeader( map: any[] ): RequestOptions {
    let params: URLSearchParams = new URLSearchParams();
    for (let i in map) {
      params.append( map[ i ].name, map[ i ].value );
    }
    return new RequestOptions( {headers: this.uploadHeader(), withCredentials: true, search: params} );
  }

  private buildJsonAuthHeader(): Headers {
    if (this.checkCookieBeforeCall()) {
      let headers = new Headers( {"Content-Type": "application/json", "Accept": "application/json"} );
      headers.append( "Authorization", this.sessionService.getAuthBearerHeaderValue() );
      return headers;
    }
  }

  private uploadHeader(): Headers {
    if (this.checkCookieBeforeCall()) {
      let headers = new Headers( {"Content-Type": "multipart/form-data", "Accept": "application/json"} );
      headers.append( "Authorization", this.sessionService.getAuthBearerHeaderValue() );
      return headers;
    }
  }

  private checkCookieBeforeCall(): boolean {
    if (this.sessionService.getDSMToken() == null || this.sessionService.getDSMToken() == undefined) {
      let jwtHelper: JwtHelper = new JwtHelper();
      let expirationDate: Date = jwtHelper.getTokenExpirationDate( this.sessionService.getDSMToken() );
      let myDate = new Date();
      if (expirationDate <= myDate) {
        // Remove token from localStorage
        // console.log("log out user and remove all items from local storage");
        localStorage.removeItem( "auth_token" );
        localStorage.removeItem( SessionService.DSM_TOKEN_NAME );
        localStorage.removeItem( Statics.PERMALINK );
        localStorage.removeItem( ComponentService.MENU_SELECTED_REALM );
        this.sessionService.logout();
        this.router.navigate( [ Statics.HOME_URL ] );
        return false;
      }
    }
    return true;
  }
}
