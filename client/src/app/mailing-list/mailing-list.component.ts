import {Component, OnInit} from "@angular/core";
import {DSMService} from "../services/dsm.service";
import {Auth} from "../services/auth.service";
import {Result} from "../utils/result.model";
import {MailingListContact} from "./mailing-list.model";
import {Utils} from "../utils/utils";
import {RoleService} from "../services/role.service";
import {ComponentService} from "../services/component.service";
import {ActivatedRoute} from "@angular/router";
import {Statics} from "../utils/statics";

@Component( {
  selector: "app-mailing-list",
  templateUrl: "./mailing-list.component.html",
  styleUrls: [ "./mailing-list.component.css" ]
} )
export class MailingListComponent implements OnInit {

  realm: string;
  contactList: Array<MailingListContact> = [];

  loadingContacts: boolean = false;

  errorMessage: string;
  additionalMessage: string;

  constructor( private dsmService: DSMService, private auth: Auth, private role: RoleService, private compService: ComponentService,
               private route: ActivatedRoute ) {
    if (!auth.authenticated()) {
      auth.logout();
    }
    this.route.queryParams.subscribe( params => {
      // console.log(this.compService.realmMenu);
      this.realm = params[ DSMService.REALM ] || null;
      if (this.realm != null) {
        //        this.compService.realmMenu = this.realm;
        this.checkRight();
      }
    } );
  }

  private checkRight() {
    this.additionalMessage = null;
    this.contactList = [];
    this.getMailingList();
  }

  ngOnInit() {
    // console.log(this.compService.realmMenu);
    if (localStorage.getItem( ComponentService.MENU_SELECTED_REALM ) != null) {
      this.realm = localStorage.getItem( ComponentService.MENU_SELECTED_REALM );
      this.checkRight();
    }
    else {
      this.additionalMessage = "Please select a realm";
    }
    window.scrollTo( 0, 0 );
  }

  public getMailingList(): void {
    if (this.realm != null) {
      this.loadingContacts = true;
      let jsonData: any[];
      this.additionalMessage = null;
      this.dsmService.getMailingList( this.realm ).subscribe(
        data => {
          let result = Result.parse( data );
          if (result.code === 500) {
            this.errorMessage = "";
            this.additionalMessage = "You are not allowed to see information of the selected realm at that category";
          }
          else {
            this.contactList = [];
            jsonData = data;
            jsonData.forEach( ( val ) => {
              let contact = MailingListContact.parse( val );
              this.contactList.push( contact );
            } );
          }
          // console.info(`${this.contactList.length} contacts received: ${JSON.stringify(data, null, 2)}`);
          this.loadingContacts = false;
        },
        err => {
          if (err._body === Auth.AUTHENTICATION_ERROR) {
            this.auth.logout();
          }
          this.errorMessage = "Error - Loading contacts  " + err;
          this.loadingContacts = false;
        }
      );
    }
  }

  public downloadMailingList(): void {
    let map: { firstName: string, lastName: string, email: string, dateCreated: string }[] = [];
    for (var i = 0; i < this.contactList.length; i++) {
      let dateCreated: string = "-";
      if (this.contactList[ i ].dateCreated != null && this.contactList[ i ].dateCreated !== 0) {
        dateCreated = Utils.getDateFormatted( new Date( this.contactList[ i ].dateCreated * 1000 ), Utils.DATE_STRING_IN_CVS );
      }
      map.push( {
        firstName: this.contactList[ i ].firstName,
        lastName: this.contactList[ i ].lastName,
        email: this.contactList[ i ].email,
        dateCreated: dateCreated
      } );
    }
    var fields = [ "firstName", "lastName", "email", "dateCreated" ];
    var date = new Date();
    Utils.createCSV( fields, map, "MailingList " + this.realm + " " + Utils.getDateFormatted( date, Utils.DATE_STRING_CVS ) + Statics.CSV_FILE_EXTENSION );
  }

  hasRole(): RoleService {
    return this.role;
  }
}
