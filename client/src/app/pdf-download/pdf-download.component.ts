import {Component, OnInit} from "@angular/core";
import {RoleService} from "../services/role.service";
import {DSMService} from "../services/dsm.service";
import {Auth} from "../services/auth.service";
import {ComponentService} from "../services/component.service";
import {ActivatedRoute, Router} from "@angular/router";
import {Statics} from "../utils/statics";
import {Response} from "@angular/http";

var fileSaver = require('file-saver/filesaver.js');

@Component({
  selector: 'app-pdf-download',
  templateUrl: './pdf-download.component.html',
  styleUrls: ['./pdf-download.component.css']
})
export class PdfDownloadComponent implements OnInit {

  errorMessage: string;
  additionalMessage: string;
  loading: boolean = false;

  realm: string;

  participantId: string = null;
  allowedToSeeInformation: boolean = false;

  possiblePDFs: Array<String> = [];
  selectedPDF: string;
  downloading: boolean = false;

  constructor(private dsmService: DSMService, private auth: Auth, private router: Router, private role: RoleService,
              private compService: ComponentService, private route: ActivatedRoute) {
    if (!auth.authenticated()) {
      auth.logout();
    }
    this.route.queryParams.subscribe(params => {
      this.realm = params[DSMService.REALM] || null;
      if (this.realm != null) {
        this.errorMessage = null;
        //        this.compService.realmMenu = this.realm;
        this.checkRight();
      }
    });
  }

  ngOnInit() {
    if (localStorage.getItem(ComponentService.MENU_SELECTED_REALM) != null) {
      this.realm = localStorage.getItem(ComponentService.MENU_SELECTED_REALM);
      this.checkRight();
    }
    else {
      this.additionalMessage = "Please select a realm";
    }
    window.scrollTo(0,0);
  }

  private checkRight() {
    this.additionalMessage = null;
    this.allowedToSeeInformation = false;
    let jsonData: any[];
    this.dsmService.getRealmsAllowed(Statics.PDF_DOWNLOAD_MENU).subscribe(
      data => {
        jsonData = data;
        jsonData.forEach((val) => {
          if (this.realm === val) {
            this.getListOfPossiblePDFs();
            this.allowedToSeeInformation = true;
          }
        });
        if (!this.allowedToSeeInformation) {
          this.additionalMessage = "You are not allowed to see information of the selected realm at that category";
        }
      },
      err => {
        return null;
      }
    );
  }

  getListOfPossiblePDFs() {
    if (this.realm !== "") {
      this.errorMessage = null;
      this.additionalMessage = null;
      this.loading = true;
      let jsonData: any[];
      this.dsmService.getPossiblePDFs(this.realm).subscribe(
        data => {
          this.possiblePDFs = [];
          // console.info(`received: ${JSON.stringify(data, null, 2)}`);
          jsonData = data;
          jsonData.forEach((val) => {
            let role: string = val;
            var roleParts : string[] = role.split("_");
            if (roleParts.length == 3) {
              this.possiblePDFs.push(roleParts[2]);
            }
          });
          this.loading = false;
        },
        err => {
          if (err._body === Auth.AUTHENTICATION_ERROR) {
            this.auth.logout();
          }
          this.loading = false;
          this.errorMessage = "Error - Loading list of event types\nPlease contact your DSM developer";
        }
      );
    }
  }

  downloadPDF() {
    this.errorMessage = null;
    this.additionalMessage = null;
    if (this.selectedPDF != null && this.selectedPDF !== "") {
      this.loading = true;
      if (this.selectedPDF === "consent") {
        this.downloadConsentPDFs()
      }
      else if (this.selectedPDF === "release") {
        this.downloadReleasePDFs()
      }
    }
  }

  downloadConsentPDFs() {
    this.downloading = true;
    this.dsmService.downloadConsentPDFs(this.participantId, this.realm).subscribe(
      data => {
        console.info(data);
        this.downloadFile(data, "_Consent");
        this.downloading = false;
        this.loading = false;
      },
      err => {
        if (err._body === Auth.AUTHENTICATION_ERROR) {
          this.router.navigate([Statics.HOME_URL]);
        }
        this.additionalMessage =  "Error - Downloading consent pdf file\nPlease contact your DSM developer";
        this.downloading = false;
        this.loading = false;
      }
    );
  }

  downloadReleasePDFs() {
    this.downloading = true;
    this.dsmService.downloadReleasePDFs(this.participantId, this.realm).subscribe(
      data => {
        this.downloadFile(data, "_Release");
        this.downloading = false;
        this.loading = false;
      },
      err => {
        if (err._body === Auth.AUTHENTICATION_ERROR) {
          this.router.navigate([Statics.HOME_URL]);
        }
        this.additionalMessage =  "Error - Downloading release pdf file\nPlease contact your DSM developer";
        this.downloading = false;
        this.loading = false;
      }
    );
  }

  downloadFile(data: Response, type: string){
    var blob = new Blob([data], { type: 'application/pdf' });
    fileSaver.saveAs(blob,  this.participantId  + type + Statics.PDF_FILE_EXTENSION);
  }

}
