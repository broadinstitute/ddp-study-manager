import {Injectable} from "@angular/core";
import {JwtHelper} from "angular2-jwt";

@Injectable()
export class SessionService {

  static DSM_TOKEN_NAME = "dsm_token";

  private jwtHelper: JwtHelper = new JwtHelper();
  private isLoggedIn: boolean = false;
  private authExpiration: Date;

  constructor() {
    this.isLoggedIn = this.getDSMToken() != null;
  }

  public setDSMToken( value: string ): void {
    let expirationDate: Date = this.jwtHelper.getTokenExpirationDate( value );
    this.authExpiration = expirationDate;
    this.isLoggedIn = true;
  }

  public getDSMToken(): string {
    return localStorage.getItem( SessionService.DSM_TOKEN_NAME );
  }

  public getAuthBearerHeaderValue() {
    return "Bearer " + this.getDSMToken();
  }

  public isAuthenticated(): boolean {
    return this.isLoggedIn;
  }

  public logout() {
    localStorage.removeItem( SessionService.DSM_TOKEN_NAME );
    this.isLoggedIn = false;
    localStorage.clear();
  }


  public getDSMClaims( value: string ): any {
    return this.jwtHelper.decodeToken( value );
  }
} 
