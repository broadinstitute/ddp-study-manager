import {Component, OnInit} from "@angular/core";
import {Utils} from "../utils/utils";
import {Auth} from "../services/auth.service";
import {DSMService} from "../services/dsm.service";
import {ShippingReport} from "./shipping-report.model";
import {KitType} from "../utils/kit-type.model";
import {Result} from "../utils/result.model";
import {Statics} from "../utils/statics";

@Component( {
  selector: "app-shipping-report",
  templateUrl: "./shipping-report.component.html",
  styleUrls: [ "./shipping-report.component.css" ]
} )
export class ShippingReportComponent implements OnInit {

  errorMessage: string;
  additionalMessage: string;
  allowedToSeeInformation: boolean = false;
  loadingReport: boolean = false;
  startDate: string;
  endDate: string;
  reportData: any[];

  constructor( private dsmService: DSMService, private auth: Auth ) {
    if (!auth.authenticated()) {
      auth.logout();
    }
  }

  ngOnInit() {
    this.additionalMessage = "";
    this.errorMessage = "";
    let start = new Date();
    start.setDate( start.getDate() - 7 );
    this.startDate = Utils.getFormattedDate( start );
    let end = new Date();
    this.endDate = Utils.getFormattedDate( end );
    this.reload();
  }

  private loadReport( startDate: string, endDate: string ) {
    this.loadingReport = true;
    let jsonData: any[];
    this.dsmService.getShippingReport( startDate, endDate ).subscribe(
      data => {
        let result = Result.parse( data );
        if (result.code === 500) {
          this.allowedToSeeInformation = false;
          this.errorMessage = "";
          this.additionalMessage = "You are not allowed to see information of the selected realm at that category";
        }
        else {
          this.allowedToSeeInformation = true;
          this.reportData = [];
          let result = Result.parse( data );
          if (result.code != null && result.code !== 200) {
            this.errorMessage = "Error - Loading Sample Report\nPlease contact your DSM developer";
          }
          else {
            jsonData = data;
            jsonData.forEach( ( val ) => {
              let kitType = ShippingReport.parse( val );
              this.reportData.push( kitType );
            } );
          }
        }
        this.loadingReport = false;
      },
      err => {
        if (err._body === Auth.AUTHENTICATION_ERROR) {
          this.auth.logout();
        }
        this.loadingReport = false;
        this.errorMessage = "Error - Loading Sample Report\nPlease contact your DSM developer";
      }
    );
  }

  public reload(): void {
    this.loadReport( this.startDate, this.endDate );
  }

  startChanged( date: string ) {
    this.startDate = date;
  }

  endChanged( date: string ) {
    this.endDate = date;
  }

  downloadReport() {
    let jsonData: any[];
    let downloadData: any[] = [];
    this.dsmService.getShippingReportOverview().subscribe(
      data => {
        // console.info(`received: ${JSON.stringify(data, null, 2)}`);
        let result = Result.parse( data );
        if (result.code != null && result.code !== 200) {
          this.errorMessage = "Error - Downloading Sample Report\nPlease contact your DSM developer";
        }
        else {
          jsonData = data;
          jsonData.forEach( ( val ) => {
            let kitType = ShippingReport.parse( val );
            downloadData.push( kitType );
          } );
          this.saveReport( downloadData );
        }
      },
      err => {
        if (err._body === Auth.AUTHENTICATION_ERROR) {
          this.auth.logout();
        }
        this.loadingReport = false;
        this.errorMessage = "Error - Loading Sample Report\nPlease contact your DSM developer";
      }
    );
  }

  saveReport( downloadData: any[] ) {
    let map: { kitType: string, month: string, ddpName: string, sent: number, received: number }[] = [];
    if (downloadData != null) {
      for (var i = 0; i < downloadData.length; i++) {
        if (downloadData[ i ].summaryKitTypeList != null) {
          for (var j = 0; j < downloadData[ i ].summaryKitTypeList.length; j++) {
            map.push( {
              kitType: downloadData[ i ].summaryKitTypeList[ j ].kitType,
              month: downloadData[ i ].summaryKitTypeList[ j ].month,
              ddpName: downloadData[ i ].ddpName,
              sent: downloadData[ i ].summaryKitTypeList[ j ].sent,
              received: downloadData[ i ].summaryKitTypeList[ j ].received
            } );
          }
        }
      }
      if (map.length > 0) {
        var fields = [ {label: "Material Type", value: "kitType"},
          {label: "Month", value: "month"},
          {label: "Project", value: "ddpName"},
          {label: "Number of Samples Shipped", value: "sent"},
          {label: "Number of Samples Received", value: "received"} ];
        var date = new Date();
        Utils.createCSV( fields, map, "GP_Report_" + Utils.getDateFormatted( date, Utils.DATE_STRING_CVS ) + Statics.CSV_FILE_EXTENSION );
      }
    }
  }
}
