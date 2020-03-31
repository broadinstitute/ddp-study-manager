import {Component, OnInit} from "@angular/core";
import {ActivityDefinition} from "../activity-data/models/activity-definition.model";
import {Option} from "../activity-data/models/option.model";
import {ParticipantColumn} from "../filter-column/models/column.model";
import {Filter} from "../filter-column/filter-column.model";
import {Participant} from "../participant-list/participant-list.model";
import {DSMService} from "../services/dsm.service";
import {Auth} from "../services/auth.service";
import {NameValue} from "../utils/name-value.model";
import {DDPInformation} from "./dashboard.model";
import {Utils} from "../utils/utils";
import {ActivatedRoute, Router} from "@angular/router";
import {Statics} from "../utils/statics";
import {Result} from "../utils/result.model";
import {ComponentService} from "../services/component.service";
import {RoleService} from "../services/role.service";

@Component( {
  selector: "app-dashboard",
  templateUrl: "./dashboard.component.html",
  styleUrls: [ "./dashboard.component.css" ]
} )
export class DashboardComponent implements OnInit {

  errorMessage: string;
  additionalMessage: string;

  Arr = Array;

  loadingDDPData: boolean = false;

  ddp: DDPInformation = null;

  MEDICALRECORDDASHBOARD: string = Statics.MEDICALRECORD_DASHBOARD;
  SHIPPINGDASHBOARD: string = Statics.SHIPPING_DASHBOARD;
  UNSENTOVERVIEW: string = Statics.UNSENT_OVERVIEW;

  dashboardVersion: string = "";

  startDate: string;
  endDate: string;

  allowedToSeeInformation: boolean = false;

  kitsNoLabel: boolean = false;
  allLabelTriggered: boolean = false;

  showMedicalRecordRequestDetails: boolean = false;
  showTissueRequestDetails: boolean = false;
  showTissueProblemDetails: boolean = false;
  showActivityDetails: boolean[] = [];

  activityKeys: string[] = [];

  isCreatingLabels: number = 0;

  dataSources = new Map( [
    [ "data", "ES" ],
    [ "p", "Participant" ],
    [ "m", "Medical Record" ],
    [ "oD", "Onc History" ],
    [ "t", "Tissue" ],
    [ "k", "Sample" ],
    [ "a", "Abstraction" ] ] );
  sourceColumns = {};
  downloadSource;
  hasESData = false;
  activityDefinitionList: ActivityDefinition[] = [];
  participantList: Participant[] = [];

  constructor( private dsmService: DSMService, private auth: Auth, private router: Router, private compService: ComponentService,
               private route: ActivatedRoute, private role: RoleService ) {
    if (!auth.authenticated()) {
      auth.logout();
    }
    this.route.queryParams.subscribe( params => {
      let realm = params[ DSMService.REALM ] || null;
      if (realm != null && auth.authenticated()) {
        this.getDashboardInformation( this.router.url );
        this.participantList = [];
      }
    } );
  }

  ngOnInit() {
    if (localStorage.getItem( ComponentService.MENU_SELECTED_REALM ) == null || localStorage.getItem( ComponentService.MENU_SELECTED_REALM ) === undefined) {
      this.additionalMessage = "Please select a realm";
    }
    else {
      if (this.auth.authenticated()) {
        this.getDashboardInformation( this.router.url );
      }
    }
  }

  private loadDDPData( startDate: string, endDate: string, version: string ) {
    if (localStorage.getItem( ComponentService.MENU_SELECTED_REALM ) != null) {
      this.allowedToSeeInformation = false;
      this.loadingDDPData = true;
      this.ddp = null;
      if (version === Statics.MEDICALRECORD_DASHBOARD) {
        let jsonData: any[];
        this.dsmService.getRealmsAllowed( Statics.MEDICALRECORD ).subscribe(
          data => {
            jsonData = data;
            jsonData.forEach( ( val ) => {
              if (localStorage.getItem( ComponentService.MENU_SELECTED_REALM ) === val) {
                this.allowedToSeeInformation = true;
                this.dsmService.getMedicalRecordDashboard( localStorage.getItem( ComponentService.MENU_SELECTED_REALM ), startDate, endDate ).subscribe(
                  data => {
                    let result = Result.parse( data );
                    if (result.code != null && result.code !== 200) {
                      this.errorMessage = "Error - Getting all participant numbers\nPlease contact your DSM developer";
                    }
                    else {
                      this.ddp = DDPInformation.parse( data );
                      this.activityKeys = this.getActivityKeys( this.ddp.dashboardValues );
                      this.activityKeys.forEach( value => this.showActivityDetails[ value ] = false );
                      this.getSourceColumnsFromFilterClass();
                      this.loadSettings();
                    }
                    this.loadingDDPData = false;
                  },
                  err => {
                    if (err._body === Auth.AUTHENTICATION_ERROR) {
                      this.auth.logout();
                    }
                    this.loadingDDPData = false;
                    this.errorMessage = "Error - Loading ddp information\nPlease contact your DSM developer";
                  }
                );
              }
            } );
            if (!this.allowedToSeeInformation) {
              this.additionalMessage = "You are not allowed to see information of the selected realm at that category";
              this.loadingDDPData = false;
            }
            else {
              this.additionalMessage = null;
            }
          },
          err => {
            return null;
          }
        );
      }
      else if (version === Statics.SHIPPING_DASHBOARD) {
        let jsonData: any[];
        this.dsmService.getRealmsAllowed( Statics.SHIPPING ).subscribe(
          data => {
            jsonData = data;
            jsonData.forEach( ( val ) => {
              if (localStorage.getItem( ComponentService.MENU_SELECTED_REALM ) === val) {
                this.allowedToSeeInformation = true;
                this.dsmService.getShippingDashboard( localStorage.getItem( ComponentService.MENU_SELECTED_REALM ) ).subscribe(
                  data => {
                    this.ddp = DDPInformation.parse( data );
                    this.loadingDDPData = false;
                  },
                  err => {
                    if (err._body === Auth.AUTHENTICATION_ERROR) {
                      this.auth.logout();
                    }
                    this.errorMessage = "Error - Loading ddp information\nPlease contact your DSM developer";
                    this.loadingDDPData = false;
                  }
                );
              }
            } );
            if (!this.allowedToSeeInformation) {
              this.additionalMessage = "You are not allowed to see information of the selected realm at that category";
              this.loadingDDPData = false;
            }
            else {
              this.additionalMessage = null;
            }
            this.loadingDDPData = false;
          },
          err => {
            return null;
          }
        );
      }
      else {
        this.errorMessage = "Error - Router has wrong url ";
      }
    }
    else {
      this.additionalMessage = "Please select a realm";
    }
  }

  getDashboardInformation( url: string ) {
    this.loadingDDPData = true;
    if (url.indexOf( Statics.MEDICALRECORD_DASHBOARD ) > -1) {
      this.dashboardVersion = Statics.MEDICALRECORD_DASHBOARD;
      this.loadDDPSummary();
    }
    else if (url.indexOf( Statics.SHIPPING_DASHBOARD ) > -1) {
      this.dashboardVersion = Statics.SHIPPING_DASHBOARD;
      this.loadDDPSummary();
    }
    else if (url.indexOf( Statics.UNSENT_OVERVIEW ) > -1) {
      this.dashboardVersion = Statics.UNSENT_OVERVIEW;
      this.creatingLabelStatus();
      this.loadUnsentOverview();
    }
    else {
      this.errorMessage = "Error - Router has wrong url\nPlease contact your DSM developer";
    }
  }

  creatingLabelStatus() {
    this.dsmService.getLabelCreationStatus().subscribe(
      data => {
        let result = Result.parse( data );
        if (result.code == 200 && result.body != null && result.body !== "0") {
          this.isCreatingLabels = Number( result.body );
        }
        else {
          this.isCreatingLabels = 0;
        }
      }
    );
  }

  loadDDPSummary() {
    if (localStorage.getItem( ComponentService.MENU_SELECTED_REALM ) != null) {
      let start = new Date();
      start.setDate( start.getDate() - 7 );
      this.startDate = Utils.getFormattedDate( start );
      let end = new Date();
      this.endDate = Utils.getFormattedDate( end );
      this.loadDDPData( this.startDate, this.endDate, this.dashboardVersion );
      window.scrollTo( 0, 0 );
    }
    else {
      this.loadingDDPData = false;
      this.additionalMessage = "Please select a realm";
    }
  }

  loadUnsentOverview() {
    this.loadingDDPData = true;
    this.additionalMessage = "";
    this.allowedToSeeInformation = true;
    this.dsmService.getShippingOverview().subscribe(
      data => {
        this.kitsNoLabel = false;
        this.ddp = DDPInformation.parse( data );
        for (let kit of this.ddp.kits) {
          if (kit.kitsNoLabel !== "0") {
            this.kitsNoLabel = true;
            break;
          }
        }
        this.loadingDDPData = false;
      },
      err => {
        if (err._body === Auth.AUTHENTICATION_ERROR) {
          this.auth.logout();
        }
        this.loadingDDPData = false;
        this.errorMessage = "Error - Loading ddp information " + err;
      }
    );
  }

  public reload(): void {
    this.ddp = null;
    this.loadDDPData( this.startDate, this.endDate, this.dashboardVersion );
  }

  startChanged( date: string ) {
    this.startDate = date;
  }

  endChanged( date: string ) {
    this.endDate = date;
  }

  triggerLabelCreation( realm: string, kitType: string ) {
    this.loadingDDPData = true;
    if (realm == null && kitType == null) {
      this.allLabelTriggered = true;
    }
    else {
      this.allLabelTriggered = false;
    }
    this.dsmService.kitLabel( realm, kitType ).subscribe(
      data => {
        let result = Result.parse( data );
        if (result.code === 200) {
          this.additionalMessage = "Triggered label creation";
          this.errorMessage = null;
        }
        this.loadingDDPData = false;
      },
      err => {
        if (err._body === Auth.AUTHENTICATION_ERROR) {
          this.auth.logout();
        }
        this.loadingDDPData = false;
        this.errorMessage = "Error - Loading ddp information " + err;
      }
    );
  }

  getRole(): RoleService {
    return this.role;
  }

  reloadUnsent() {
    this.loadUnsentOverview();
  }

  getActivityKeys( o: Object ) {
    let keys = Object.keys( o ).filter( obj => obj.startsWith( "activity" ) && !obj.endsWith( "completed" ) && obj.indexOf( "." ) == obj.lastIndexOf( "." ) );
    keys.sort( ( a, b ) => {
      if (a.toLowerCase() < b.toLowerCase()) {
        return -1;
      }
      else if (a.toLowerCase() > b.toLowerCase()) {
        return 1;
      }
      else {
        return 0;
      }
    } );
    return keys;
  }

  getActivityVersionKeys( o: Object, activity: string ) {
    let keys = Object.keys( o ).filter( obj => obj.startsWith( "activity" ) && !obj.endsWith( "completed" ) && obj.indexOf( "." ) != obj.lastIndexOf( "." )
      && obj.indexOf( activity ) > -1 );
    keys.sort( ( a, b ) => {
      if (a.toLowerCase() < b.toLowerCase()) {
        return -1;
      }
      else if (a.toLowerCase() > b.toLowerCase()) {
        return 1;
      }
      else {
        return 0;
      }
    } );
    return keys;
  }

  getHighestFollowUp() {
    let allFollowUpSent = Object.keys( this.ddp.dashboardValues ).filter( obj => obj.startsWith( "followUpSent" ) );
    let allFollowUpIndexes = allFollowUpSent.map( obj => {
      return obj.split( "." ) [ 1 ];
    } );
    let uniqueIndexes = Array.from( new Set( allFollowUpIndexes ) );
    let maxIndex = Math.max.apply( null, uniqueIndexes.map( o => {
      return Number( o );
    } ) );
    return maxIndex;
  }

  getActivityName( dashboardKey: String ) {
    if (dashboardKey !== "" && dashboardKey != null && dashboardKey.indexOf( "." ) > -1) {
      return dashboardKey.split( "." )[ 1 ];
    }
  }

  getActivityVersion( dashboardKey: String ) {
    if (dashboardKey !== "" && dashboardKey != null && dashboardKey.indexOf( "." ) > -1) {
      return dashboardKey.split( "." )[ 2 ];
    }
  }

  getUniqueKitTypes() {
    let allKits = Object.keys( this.ddp.dashboardValues ).filter( obj => obj.startsWith( "kit." ) );
    let kitTypes = allKits.map( obj => {
      return obj.split( "." ) [ 1 ];
    } );
    return Array.from( new Set( kitTypes ) );
  }

  getPercentage( of: object, from: object ): number {
    return Math.round( ( Number( of ) / Number( from ) ) * 100 * 100 ) / 100;
  }

  getSourceColumnsFromFilterClass() {
    this.dataSources.forEach( ( value: string, key: string ) => {
      this.sourceColumns[ key ] = [];
    } );
    for (let filter of Filter.ALL_COLUMNS) {
      if (filter.participantColumn.tableAlias === "o" || filter.participantColumn.tableAlias === "ex"
        || filter.participantColumn.tableAlias === "r") {
        if (this.sourceColumns[ "p" ] == null) {
          this.sourceColumns[ "p" ] = [];
        }
        this.sourceColumns[ "p" ].push( filter );
      }
      if (filter.participantColumn.tableAlias === "inst") {
        if (this.sourceColumns[ "m" ] == null) {
          this.sourceColumns[ "m" ] = [];
        }
        this.sourceColumns[ "p" ].push( filter );
      }
      if (this.sourceColumns[ filter.participantColumn.tableAlias ] != null && this.sourceColumns[ filter.participantColumn.tableAlias ] != undefined) {
        this.sourceColumns[ filter.participantColumn.tableAlias ].push( filter );
      }
    }
  }

  loadSettings() {
    let jsonData: any;
    this.dsmService.getSettings( localStorage.getItem( ComponentService.MENU_SELECTED_REALM ), "participantList" ).subscribe(
      data => {
        this.activityDefinitionList = [];
        jsonData = data;
        if (jsonData.fieldSettings != null) {
          Object.keys( jsonData.fieldSettings ).forEach( ( key ) => {
            jsonData.fieldSettings[ key ].forEach( ( fieldSetting ) => {
              if (this.sourceColumns[ key ] == null || this.sourceColumns[ key ] == undefined) {
                this.sourceColumns[ key ] = [];
              }
              this.sourceColumns[ key ].push( new Filter( new ParticipantColumn( fieldSetting.columnDisplay, fieldSetting.columnName, key ), Filter.ADDITIONAL_VALUE_TYPE, [], new NameValue( fieldSetting.columnName, null ) ) );
            } );
          } );
        }

        if (jsonData.activityDefinitions != null) {
          Object.keys( jsonData.activityDefinitions ).forEach( ( key ) => {
            let activityDefinition: ActivityDefinition = ActivityDefinition.parse( jsonData.activityDefinitions[ key ] );
            this.activityDefinitionList.push( activityDefinition );
            let possibleColumns: Array<Filter> = [];
            possibleColumns.push( new Filter( new ParticipantColumn( "Survey Created", "createdAt", activityDefinition.activityCode, null, true ), Filter.DATE_TYPE ) );
            possibleColumns.push( new Filter( new ParticipantColumn( "Survey Completed", "completedAt", activityDefinition.activityCode, null, true ), Filter.DATE_TYPE ) );
            possibleColumns.push( new Filter( new ParticipantColumn( "Survey Last Updated", "lastUpdatedAt", activityDefinition.activityCode, null, true ), Filter.DATE_TYPE ) );
            possibleColumns.push( new Filter( new ParticipantColumn( "Survey Status", "status", activityDefinition.activityCode, null, true ), Filter.OPTION_TYPE, [ new NameValue( "COMPLETE", "Done" ), new NameValue( "CREATED", "Not Started" ), new NameValue( "IN_PROGRES", "In Progress" ) ] ) );
            if (activityDefinition != null && activityDefinition.questions != null) {
              for (let question of activityDefinition.questions) {
                if (question.stableId != null) {
                  let questionDefinition = activityDefinition.getQuestionDefinition( question.stableId );
                  if (questionDefinition != null && questionDefinition.questionText != null && questionDefinition.questionText !== "") {
                    let options: Array<NameValue> = null;
                    let type: string = questionDefinition.questionType;
                    if (questionDefinition.questionType === "PICKLIST") {
                      options = new Array<NameValue>();
                      type = Filter.OPTION_TYPE;
                      questionDefinition.options.forEach( ( value: Option ) => {
                        options.push( new NameValue( value.optionStableId, value.optionText ) );
                      } );
                    }
                    let filter = new Filter( new ParticipantColumn( questionDefinition.questionText, question.stableId, activityDefinition.activityCode, null, true ), type, options );
                    possibleColumns.push( filter );
                  }
                }
              }
              this.dataSources.set( activityDefinition.activityCode, activityDefinition.activityName );
              this.sourceColumns[ activityDefinition.activityCode ] = possibleColumns;
            }
          } );
        }
        this.getParticipantData();
      },
      err => {
        if (err._body === Auth.AUTHENTICATION_ERROR) {
          this.auth.logout();
        }
        throw "Error - Loading display settings" + err;
      }
    );

  }

  dataExport( source: string ) {
    this.loadingDDPData = true;
    if (source != null) {
      let date = new Date();
      let columns = {};
      this.dataSources.forEach( ( value: string, key: string ) => {
        if (this.sourceColumns[ key ] != null && this.sourceColumns[ key ].length != 0) {
          columns[ key ] = this.sourceColumns[ key ];
        }
      } );
      if (this.participantList.length != 0) {
        if (source === "m") {
          Utils.downloadCurrentData( this.participantList, [ [ "data", "data" ], [ "participant", "p" ], [ "medicalRecords", "m" ] ], columns, "Participants-MR-" + Utils.getDateFormatted( date, Utils.DATE_STRING_CVS ) + Statics.CSV_FILE_EXTENSION );
          this.downloadDone();
        }
        else if (source === "oD") {
          Utils.downloadCurrentData( this.participantList, [ [ "data", "data" ], [ "participant", "p" ], [ "oncHistoryDetails", "oD", "tissues", "t" ] ], columns, "Participants-OncHistory-" + Utils.getDateFormatted( date, Utils.DATE_STRING_CVS ) + Statics.CSV_FILE_EXTENSION );
          this.downloadDone();
        }
        else if (source === "k") {
          Utils.downloadCurrentData( this.participantList, [ [ "data", "data" ], [ "participant", "p" ], [ "kits", "k" ] ], columns, "Participants-Sample-" + Utils.getDateFormatted( date, Utils.DATE_STRING_CVS ) + Statics.CSV_FILE_EXTENSION );
          this.downloadDone();
        }
        else if (source === "a") {
          //TODO add final abstraction values to download
          Utils.downloadCurrentData( this.participantList, [ [ "data", "data" ], [ "participant", "p" ], [ "abstractionActivities", "a" ] ], columns, "Participants-Abstraction-" + Utils.getDateFormatted( date, Utils.DATE_STRING_CVS ) + Statics.CSV_FILE_EXTENSION );
          this.downloadDone();
        }
        else {
          Utils.downloadCurrentData( this.participantList, [ [ "data", "data" ], [ source, source ] ], columns, "Participants-" + source + Utils.getDateFormatted( date, Utils.DATE_STRING_CVS ) + Statics.CSV_FILE_EXTENSION, true );
          this.downloadDone();
        }
      }
    }
  }

  getParticipantData() {
    this.dsmService.applyFilter( null, localStorage.getItem( ComponentService.MENU_SELECTED_REALM ), "participantList", null ).subscribe(
      data => {
        let jsonData: any[];
        this.participantList = [];
        jsonData = data;
        jsonData.forEach( ( val ) => {
          let participant = Participant.parse( val );
          this.participantList.push( participant );
        } );
      },
      err => {
        if (err._body === Auth.AUTHENTICATION_ERROR) {
          this.auth.logout();
        }
        this.loadingDDPData = false;
        this.errorMessage = "Error - Downloading Participant List, Please contact your DSM developer";
      }
    );
  }

  downloadDone() {
    this.loadingDDPData = false;
    this.errorMessage = null;
  }

  getSourceKeys(): string[] {
    let keys = [];
    this.dataSources.forEach( ( value: string, key: string ) => {
      if (key !== "data" && key !== "p" && key !== "t") {
        keys.push( key );
      }
    } );
    return keys;
  }

}
