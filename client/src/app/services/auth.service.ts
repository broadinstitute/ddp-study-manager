import {Injectable} from "@angular/core";
import {Router} from "@angular/router";
import {Http, Headers, Response, RequestOptions} from "@angular/http";
import {Observable, Subject} from "rxjs";

import {SessionService} from "./session.service";
import {RoleService} from "./role.service";
import {DSMService} from "./dsm.service";
import {ComponentService} from "./component.service";
import {Statics} from "../utils/statics";
import {Subscription} from "rxjs/Subscription";

// Avoid name not found warnings
declare var Auth0Lock: any;
declare var DDP_ENV: any;

@Injectable()
export class Auth {

  static AUTH0_TOKEN_NAME = "auth_token";

  public static AUTHENTICATION_ERROR: string = "AUTHENTICATION_ERROR";

  private baseUrl = DDP_ENV.baseUrl;
  private authUrl = this.baseUrl + DSMService.UI + "auth0";

  private eventsSource = new Subject<string>();

  events = this.eventsSource.asObservable();

  kitDiscard = new Subject<string>();
  confirmKitDiscard = this.kitDiscard.asObservable();

  realmList: Array<string>;
  selectedRealm: string;

  loadRealmsSubscription: Subscription;

  // Configure Auth0
  lock = new Auth0Lock( DDP_ENV.auth0ClientKey, DDP_ENV.auth0Domain, {
    auth: {
      redirectUrl: window.location.origin + Statics.HOME_URL,
      responseType: "token"
    },
    languageDictionary: {
      title: "DDP Study Management"
    },
    theme: {
      logo: "/assets/images/logo-broad-institute.svg",
      primaryColor: "#5e7da4"
    },
    autoclose: true
    // rememberLastLogin: false,
  } );

  // Configure Auth0 for confirm kit discard
  lockForDiscard = new Auth0Lock( DDP_ENV.auth0ClientKey, DDP_ENV.auth0Domain, {
    auth: {
      responseType: "token",
      redirect: false,
      // sso: false,
      params: {prompt: "select_account"}
    },
    languageDictionary: {
      title: "Login to confirm"
    },
    theme: {
      logo: "",
      primaryColor: "#5e7da4"
    },
    autoclose: true,
    rememberLastLogin: false,
    allowSignUp: false,
    allowedConnections: [ "google-oauth2" ]
  } );

  constructor( private router: Router, private http: Http, private sessionService: SessionService, private role: RoleService,
               private compService: ComponentService, private dsmService: DSMService ) {
    // Add callback for lock `authenticated` event
    this.lock.on( "authenticated", ( authResult: any ) => {
      localStorage.setItem( Auth.AUTH0_TOKEN_NAME, authResult.idToken );
      let payload = {
        "token": authResult.idToken
      };
      this.doLogin( payload );
    } );
    this.lock.on( "authorization_error", ( authResult ) => {
      // console.log("user is not allowed to login ");
      this.eventsSource.next( "authorization_error" );
    } );

    this.lockForDiscard.on( "authenticated", ( authResult: any ) => {
      this.kitDiscard.next( authResult.idToken );
    } );
    this.lockForDiscard.on( "authorization_error", ( authResult ) => {
      // console.log(authResult);
      // console.log("user is not allowed to login ");
    } );
  }

  public authenticated() {
    // Check if there's an unexpired JWT
    // This searches for an item in localStorage with key == 'token'
    // return tokenNotExpired();
    return this.sessionService.isAuthenticated();
  };

  public logout() {
    // Remove token from localStorage
    // console.log("log out user and remove all items from local storage");
    localStorage.removeItem( Auth.AUTH0_TOKEN_NAME );
    localStorage.removeItem( SessionService.DSM_TOKEN_NAME );
    localStorage.removeItem( Statics.PERMALINK );
    localStorage.removeItem( ComponentService.MENU_SELECTED_REALM );
    localStorage.clear();
    this.sessionService.logout();
    this.selectedRealm = null;
    this.router.navigate( [ Statics.HOME_URL ] );
  }

  public doLogin( authPayload: any ) {
    let dsmObservable = this.http.post( this.authUrl, authPayload, this.buildHeaders() )
      .map( ( res: Response ) => res.json() )
      .catch( this.handleError );


    let dsmResponse: any;

    dsmObservable.subscribe(
      response => dsmResponse = response,
      err => {
      },
      () => {
        let dsmToken = dsmResponse.dsmToken;
        localStorage.setItem( SessionService.DSM_TOKEN_NAME, dsmToken );
        this.sessionService.setDSMToken( dsmToken );
        this.role.setRoles( dsmToken );

        this.realmList = [];
        this.getRealmList();

        var link: any = JSON.parse( localStorage.getItem( Statics.PERMALINK ) );
        //get rid of localStorage of url
        //navigate to original url
        if (link != null && link.link != null) {
          if (link.link.indexOf( "participantList" ) > -1) {
            let realmName: string = link.realm;
            this.router.navigate( [ Statics.PERMALINK + "/participantList" ], {queryParams: {realm: realmName}} );
          }
          else if (link.link.indexOf( Statics.SHIPPING ) > -1) {
            let target: string = link.target;
            this.router.navigate( [ Statics.PERMALINK + Statics.SHIPPING_URL ], {queryParams: {target: target}} );
          }
          else if (link.link.indexOf( Statics.MEDICALRECORD ) > -1) {
            let realmName: string = link.realm;
            this.router.navigate( [ link.link ], {queryParams: {realm: realmName}} );
          }
          else {
            this.router.navigate( [ link.link ] );
          }
          localStorage.removeItem( Statics.PERMALINK );
        }
        else {
          this.redirect();
        }
      }
    );
  }

  public buildHeaders(): RequestOptions {
    let headers = new Headers( {"Content-Type": "application/json", "Accept": "application/json"} );
    headers.append( "Authorization", this.sessionService.getAuthBearerHeaderValue() );
    return new RequestOptions( {headers: headers, withCredentials: true} );
  }

  private handleError( error: any ) {
    // In a real world app, we might use a remote logging infrastructure
    // We'd also dig deeper into the error to get a better message
    let errMsg = ( error.message ) ? error.message :
      error.status ? `${error.status} - ${error.statusText}` : "Server error";
    console.error( errMsg ); // log to console instead
    return Observable.throw( errMsg );
  }

  private redirect() {
    if (this.role.allowedToViewMedicalRecords()) {
      this.router.navigate( [ Statics.MEDICALRECORD_DASHBOARD_URL ] );
    }
    else {
      if (this.role.allowedToHandleSamples() || this.role.allowToViewSampleLists()) {
        this.router.navigate( [ Statics.SHIPPING_DASHBOARD_URL ], {queryParams: {target: Statics.UNSENT}} );
      }
      else {
        if (this.role.allowedToViewReceivingPage()) {
          this.router.navigate( [ Statics.SCAN_URL ], {queryParams: {scanReceived: true}} );
        }
        else {
          this.router.navigate( [ Statics.HOME_URL ] );
        }
      }
    }
  }

  getRealmList() {
    if (this.realmList == undefined || this.realmList == null || this.realmList.length == 0) {
      let jsonData: any[];
      this.realmList = [];


      if (this.loadRealmsSubscription != null) {
        this.loadRealmsSubscription.unsubscribe();
      }
      this.loadRealmsSubscription = this.dsmService.getRealmsAllowed( null ).subscribe(
        data => {
          jsonData = data;
          jsonData.forEach( ( val ) => {
            this.realmList.push( val );
          } );
          // console.info(`received: ${JSON.stringify(data, null, 2)}`);
        }
      );
    }
  }

  selectRealm( newValue ) {
    if (newValue !== "") {
      this.selectedRealm = newValue;
      localStorage.setItem( ComponentService.MENU_SELECTED_REALM, this.selectedRealm );
      let nav = this.router.url;
      if (this.router.url.indexOf( "?" ) > -1) {
        nav = this.router.url.slice( 0, this.router.url.indexOf( "?" ) );
      }
      this.router.navigate( [ nav ], {queryParams: {realm: this.selectedRealm}} );
    }
    else {
      localStorage.removeItem( ComponentService.MENU_SELECTED_REALM );
      this.router.navigate( [ Statics.HOME_URL ] );
    }
  }
}
