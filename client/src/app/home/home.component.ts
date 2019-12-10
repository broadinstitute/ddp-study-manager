import { Component, OnInit } from '@angular/core';
import { Auth } from "../services/auth.service";

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
})
export class HomeComponent implements OnInit {

  notAllowedToLogin = false;

  constructor(public auth: Auth) {
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

}
