import {Injectable} from "@angular/core";
import {Router, CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, ActivatedRoute} from "@angular/router";
import {Auth} from "../services/auth.service";
import {SessionService} from "../services/session.service";
import {ComponentService} from "../services/component.service";
import {DSMService} from "../services/dsm.service";
import {Statics} from "../utils/statics";

@Injectable()
export class AuthGuard implements CanActivate {

  constructor( private router: Router, private auth: Auth, private route: ActivatedRoute,
               private compService: ComponentService ) {
  }

  canActivate( route: ActivatedRouteSnapshot, state: RouterStateSnapshot ) {
    if (localStorage.getItem( Auth.AUTH0_TOKEN_NAME ) && localStorage.getItem( SessionService.DSM_TOKEN_NAME )) {
      // logged in so return true
      return true;
    }

    if (state.url !== Statics.HOME_URL && !this.auth.authenticated()) {
      if (state.url.indexOf( Statics.PERMALINK_URL ) === 0) {
        var link;
        if (state.url.indexOf( "participantList" ) > -1 ||
          state.url.indexOf( Statics.MEDICALRECORD ) > -1) {
          let realm: string = route.queryParams[ DSMService.REALM ];
          link = {link: state.url, realm: realm};
        }
        else if (state.url.indexOf( Statics.SHIPPING ) > -1) {
          let target: string = route.queryParams[ DSMService.TARGET ];
          link = {link: state.url, target: target};
        }
        else {
          link = {link: state.url};
        }
        localStorage.setItem( Statics.PERMALINK, JSON.stringify( link ) );
      }
    }
    return false;
  }
}
