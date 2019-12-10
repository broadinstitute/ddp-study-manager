import {Component, OnInit, ViewChild} from "@angular/core";
import {FieldFilepickerComponent} from "../field-filepicker/field-filepicker.component";
import {DSMService} from "../services/dsm.service";
import {Auth} from "../services/auth.service";
import {ComponentService} from "../services/component.service";
import {ActivatedRoute} from "@angular/router";
import {Response} from "@angular/http";

var fileSaver = require("file-saver/filesaver.js");

@Component({
  selector: "app-ndiupload",
  templateUrl: "./ndiupload.component.html",
  styleUrls: ["./ndiupload.component.css"]
})
export class NDIUploadComponent implements OnInit {


  @ViewChild(FieldFilepickerComponent)
  public filepicker: FieldFilepickerComponent;

  errorMessage: string;
  additionalMessage: string;

  loading: boolean = false;


  file: File = null;

  allowedToSeeInformation: boolean = false;

  constructor(private dsmService: DSMService, private auth: Auth, private compService: ComponentService, private route: ActivatedRoute) {
    if (!auth.authenticated()) {
      auth.logout();
    }
  }


  ngOnInit() {
    window.scrollTo(0, 0);
  }

  upload() {
    this.errorMessage = null;
    this.additionalMessage = null;
    this.loading = true;
    this.dsmService.uploadNdiFile(this.file).subscribe(
      data => {
        this.errorMessage ="";
          this.loading = false;
        //        console.log(`received: ${JSON.stringify(data, null, 2)}`);
        this.downloadFile(data);
      },
      err => {
        //        console.log(`received***: ${JSON.stringify(err, null, 2)}`);
        this.loading = false;
        if (err._body === Auth.AUTHENTICATION_ERROR) {
          this.auth.logout();
        }
        this.errorMessage = "Error - Uploading txt\n" + err._body;
      }
    );
  }


  downloadFile(data: Response) {
    var blob = new Blob([data], {type: "text/plain;charset=utf-8"});
    fileSaver.saveAs(blob, "output" + ".txt");
  }


  fileSelected(file: File) {
    this.file = file;
  }

  getCompService(): ComponentService {
    return this.compService;
  }
}

