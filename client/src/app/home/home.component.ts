import {Component, OnInit} from "@angular/core";
import {Auth} from "../services/auth.service";
import {RoleService} from "../services/role.service";

@Component( {
  selector: "app-home",
  templateUrl: "./home.component.html"
} )
export class HomeComponent implements OnInit {

  notAllowedToLogin = false;

  constructor( public auth: Auth, private role: RoleService ) {
    auth.events.subscribe(
      e => {
        if (e === Auth.AUTHENTICATION_ERROR) {
          this.notAllowedToLogin = true;
        }
      }
    );
  }

  ngOnInit() {
  }

  hasRole(): RoleService {
    return this.role;
  }

}
