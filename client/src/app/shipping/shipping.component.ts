import {ChangeDetectorRef, Component, OnInit, ViewChild} from "@angular/core";
import {ActivatedRoute, Router} from "@angular/router";
import {Auth} from "../services/auth.service";
import {DSMService} from "../services/dsm.service";
import {IntervalObservable} from "rxjs/observable/IntervalObservable";
import {KitRequest} from "./shipping.model";
import {KitType} from "../utils/kit-type.model";
import {Utils} from "../utils/utils";
import {RoleService} from "../services/role.service";
import {ModalComponent} from "../modal/modal.component";
import {ComponentService} from "../services/component.service";
import {Statics} from "../utils/statics";
import {EasypostLabelRate} from "../utils/easypost-label-rate.model";
import {LabelSetting} from "../label-settings/label-settings.model";
import {Result} from "../utils/result.model";

@Component({
  selector: 'app-shipping',
  templateUrl: './shipping.component.html',
  styleUrls: ['./shipping.component.css']
})
export class ShippingComponent implements OnInit {

  @ViewChild(ModalComponent)
  public modal: ModalComponent;

  QUEUE: string = "queue";
  SENT: string = "sent";
  RECEIVED: string = "received";
  ERROR: string = "error";
  UPLOADED: string = "uploaded";
  OVERVIEW: string = "overview";
  DEACTIVATED: string = "deactivated";
  TRIGGERED: string = "triggered";

  EXPRESS: string = "express";
  NAME_LABELS: string = "nameLabels";

  shippingPage: string;

  allSelected: boolean = false;
  errorMessage: string;
  additionalMessage: string;

  selectedKitRequests: any[] = [];
  needsNameLabels: boolean = false;

  loading: boolean = false;

  kitTypes: Array<KitType> = [];
  kitType: KitType = null;

  kitRequests: KitRequest[] = [];

  isPrintButtonDisabled = true;

  sort_field: string = "default";
  sort_dir: string = "asc";

  kitRequest: KitRequest = null;
  deactivationReason: string = null;

  modalType: string;

  allSentSelected: boolean = false;
  isSentButtonDisabled = true;
  allowedToSeeInformation: boolean = false;

  public shortId: any = "";
  public shippingId: any = "";
  public externalOrderNumber: any = "";
  public externalOrderStatus: any = "";
  public reason: any = "";
  public trackingTo: any = "";
  public trackingReturn: any = "";
  public mfCode: any = "";
  public noReturn: any = "";
  public labelRate: EasypostLabelRate = null;

  labelSettings: LabelSetting[] = [];
  selectedSetting: LabelSetting;
  selectedLabel: string;
  labelNames: string[] = [];

  lastSelectedRow: number;
  selectRangeStart: number;
  selectRangeStop: number;

  kitsWithNoReturn: boolean = false;

  constructor(private route: ActivatedRoute, private router: Router, private dsmService: DSMService, private auth: Auth,
              private role: RoleService, private compService: ComponentService, private _changeDetectionRef: ChangeDetectorRef,
              private util: Utils) {
    if (!auth.authenticated()) {
      auth.logout();
    }
    this.route.queryParams.subscribe(params => {
      this.setShippingPage(this.router.url);
      let realm = params[DSMService.REALM] || null;
      if (realm != null && realm !== "") {
        //        this.compService.realmMenu = realm;
        this.checkRight();
      }
      else {
        this.additionalMessage = "Please select a realm";
      }
    });
  }

  ngOnInit() {
    if (localStorage.getItem(ComponentService.MENU_SELECTED_REALM) != null) {
      this.checkRight();
    }
    else {
      this.additionalMessage = "Please select a realm";
    }
    window.scrollTo(0,0);
  }

  private checkRight() {
    this.allowedToSeeInformation = false;
    this.additionalMessage = null;
    this.kitType = null;
    this.kitRequests = [];
    this.kitTypes = [];
    let jsonData: any[];
    this.dsmService.getRealmsAllowed(Statics.SHIPPING).subscribe(
      data => {
        jsonData = data;
        jsonData.forEach((val) => {
          if (localStorage.getItem(ComponentService.MENU_SELECTED_REALM) === val) {
            this.allowedToSeeInformation = true;
            this.getPossibleKitType();
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

    this.dsmService.getLabelSettings().subscribe(
      data => {
        jsonData = data;
        jsonData.forEach((val) => {
          this.labelSettings = [];
          this.labelNames = [];
          this.selectedSetting = null;
          this.selectedLabel = null;
          jsonData = data;
          jsonData.forEach((val) => {
            let labelSetting = LabelSetting.parse(val);
            if (labelSetting.defaultPage) {
              this.selectedSetting = labelSetting;
              this.selectedLabel = labelSetting.name;
            }
            this.labelNames.push(labelSetting.name);
            this.labelSettings.push(labelSetting);
          });
        });
      },
      err => {
        return null;
      }
    );
  }

  setShippingPage(url: string) {
    if (url.indexOf(Statics.SHIPPING_QUEUE) > -1) {
      this.shippingPage = this.QUEUE;
    }
    else if (url.indexOf(Statics.SHIPPING_SENT) > -1) {
      this.shippingPage = this.SENT;
    }
    else if (url.indexOf(Statics.SHIPPING_RECEIVED) > -1) {
      this.shippingPage = this.RECEIVED;
    }
    else if (url.indexOf(Statics.SHIPPING_ERROR) > -1) {
      this.shippingPage = this.ERROR;
    }
    else if (url.indexOf(Statics.SHIPPING_UPLOADED) > -1) {
      this.shippingPage = this.UPLOADED;
    }
    else if (url.indexOf(Statics.SHIPPING_OVERVIEW) > -1) {
      this.shippingPage = this.OVERVIEW;
    }
    else if (url.indexOf(Statics.SHIPPING_DEACTIVATED) > -1) {
      this.shippingPage = this.DEACTIVATED;
    }
    else if (url.indexOf(Statics.SHIPPING_TRIGGERED) > -1) {
      this.shippingPage = this.TRIGGERED;
    }
    else {
      this.errorMessage = "Error - Router has unknown url\nPlease contact your DSM developer";
    }
  }

  getRole(): RoleService {
    return this.role;
  }

  getPossibleKitType() {
  // console.log(this.realm);
    this.additionalMessage = null;
    this.kitRequests = [];
    let jsonData: any[];

    if (localStorage.getItem(ComponentService.MENU_SELECTED_REALM) != null && localStorage.getItem(ComponentService.MENU_SELECTED_REALM) !== "") {
      this.loading = true;
      this.dsmService.getKitTypes(localStorage.getItem(ComponentService.MENU_SELECTED_REALM)).subscribe(
        data => {
          this.kitTypes = [];
          jsonData = data;
          jsonData.forEach((val) => {
            let kitType = KitType.parse(val);
            this.kitTypes.push(kitType);
          });
          this.loading = false;
          // console.info(`${this.kitTypes.length} kit types received: ${JSON.stringify(data, null, 2)}`);
        },
        err => {
          if (err._body === Auth.AUTHENTICATION_ERROR) {
            this.auth.logout();
          }
          this.loading = false;
          this.additionalMessage = "Error - Loading kit types\n" + err;
        }
      );
      this.additionalMessage = null;
    }
    else {
      this.kitTypes = [];
      this.additionalMessage = "Please select a realm";
    }
  }

  typeChecked(type: KitType) {
    if (type.selected) {
      this.kitType = type;
      this.loadKitRequestData(this.kitType);
    }
    else {
      this.kitType = null;
      this.errorMessage = null;
      this.kitRequests = [];
    }
    for (let kit of this.kitTypes) {
      if (kit !== type) {
        if (kit.selected) {
          kit.selected = false;
        }
      }
    }
  }

  private loadKitRequestData(kitType: KitType) {
    this.allSelected = false;
    this.errorMessage = null;
    this.needsNameLabels = false;
    this.kitsWithNoReturn = false;
    this.loading = true;

    let jsonData: any[];
    if (localStorage.getItem(ComponentService.MENU_SELECTED_REALM) != null && localStorage.getItem(ComponentService.MENU_SELECTED_REALM) !== "") {
      this.dsmService.getKitRequests(localStorage.getItem(ComponentService.MENU_SELECTED_REALM), this.shippingPage, kitType.name).subscribe(
        data => {
          this.kitRequests = [];
          jsonData = data;
          jsonData.forEach((val) => {
            let kit = KitRequest.parse(val);
            if (kit.noReturn) {
              this.kitsWithNoReturn = true;
            }
            this.kitRequests.push(kit);
          });

          // console.log(`${this.kitRequests.length} KitRequest data received: ${JSON.stringify(data, null, 2)}`);
          this.loading = false;
        },
        err => {
          if (err._body === Auth.AUTHENTICATION_ERROR) {
            this.auth.logout();
          }
          this.loading = false;
          this.errorMessage = "Error - Loading kit request data\n" + err;
        }
      );
    }
    else {
      this.kitRequests = [];
      this.additionalMessage = "Please select a realm";
    }
  }

  selectSetting(event): void {
    this.selectedLabel = event;
    for (var i = 0; i < this.labelSettings.length; i++) {
      if (this.labelSettings[i].name === this.selectedLabel) {
        this.selectedSetting = this.labelSettings[i];
        break;
      }
    }
  }

  getSelectedList(target: string) {
    this.selectedKitRequests = KitRequest.removeUnselectedKitRequests(this.kitRequests);
    this._changeDetectionRef.detectChanges();
    this.printLabels(target);
  }

  public printLabels(target: string) {
    var printContents;
    if("error" === target) {
      printContents = document.getElementById("errorLabelDiv").innerHTML;
    }
    else {
      printContents = document.getElementById("labelDiv").innerHTML;
    }
    if(window){
      if (navigator.userAgent.toLowerCase().indexOf('chrome') > -1) {
        var popup = window.open('', '_blank',
          'width=800,height=600,scrollbars=no,menubar=no,toolbar=no,'
          +'location=no,status=no,titlebar=no');

        popup.window.focus();
        popup.document.write('<!DOCTYPE html><html><head>'
          + '<link rel="stylesheet" href="node_modules/bootstrap/dist/css/bootstrap.css" '
          + 'media="screen,print">'
          + '<link rel="stylesheet" href="style.css" media="screen,print">'
          + '<style type="text/css">'
          + 'body { margin:0; }'
          + '</style>'
          + '</head><body onload="window.print()"><div class="reward-body">'
          + printContents + '</div></html>');
        popup.document.close();

        //to check if the print window is still open, if it is closed, user should be navigated to scan page
        var subscription = IntervalObservable.create(500).subscribe(n => {
          if (popup == null || popup.window == null || popup.window.closed) {
            this.closedWindow();
            subscription.unsubscribe();
          }
        });
      }
    }
    return true;
  }

  private closedWindow() {
    this.selectedKitRequests = [];
    if (!this.kitType.manualSentTrack) {
      this.router.navigate([Statics.SCAN_URL]);
    }
    this.allSelected = false;
    this.setAllCheckboxes(false);
  }

  private setAllCheckboxes(selected: boolean) {
    this.needsNameLabels = false;
    if (selected && this.kitRequests[0] != null && this.kitRequests[0].nameLabel != null) {
      this.needsNameLabels = true;
    }

    this.isPrintButtonDisabled = !selected;
    for (var i = 0; i < this.kitRequests.length; i++) {
      this.kitRequests[i].isSelected = selected;
    }
    if (!selected) {
      this.isPrintButtonDisabled = true;
    }
  }

  allChecked() {
    if (this.allSelected) {
      this.setAllCheckboxes(true);
    }
    else{
      this.setAllCheckboxes(false);
    }
  }

  checkboxChecked() {
    this.needsNameLabels = false;
    if (this.kitRequests[0] != null && this.kitRequests[0].nameLabel != null) {
      this.needsNameLabels = true;
    }

    // find first selected to enable print button and check for name label
    // start from the beginning more likely that people select kits at the start of the list
    this.isPrintButtonDisabled = true;
    for (var i = 0; i < this.kitRequests.length; i++) {
      if (this.kitRequests[i].isSelected) {
        this.isPrintButtonDisabled = false;
        break;
      }
    }

    // find first unselected to set allSelected to false
    // start from the end more likely that at the end of list are kits not selected
    this.allSelected = true;
    for (var i = this.kitRequests.length - 1; i > 0; i--) {
      if (!this.kitRequests[i].isSelected) {
        this.allSelected = false;
        break;
      }
    }
  }

  shiftClick(pos: number, event: any) {
    if (event.shiftKey) {
      if (pos > this.lastSelectedRow) {
        this.selectRangeStop = pos;
      }
      else if (this.lastSelectedRow > pos) {
        this.selectRangeStop = this.selectRangeStart;
        this.selectRangeStart = pos;
      }
      //select all in the range
      for (var i = this.selectRangeStart; i < this.selectRangeStop +1; i++) {
        this.kitRequests[i].isSelected = true;
      }
    }
    else {
      //set ranges for shift select
      this.selectRangeStart = pos;
      this.selectRangeStop = pos;
      this.lastSelectedRow = pos;
    }
  }

  queueToPrint(): boolean {
    if (this.shippingPage === this.QUEUE
      ||  this.shippingPage === this.ERROR) {
      return true;
    }
    return false;
  }

  downloadReceivedData() {
    let sentColumnName: string = "DATE_BBSENT";
    let receivedColumnName: string = "DATE_BB_KITREC";
    if (this.kitType.name === "SALIVA") {
      sentColumnName = "DATE_SALIVA_SENT";
      receivedColumnName = "DATE_SALIVA_RECEIVED";
    }
    var fieldNames = ["realm", "DATSTAT_ALTPID", "shortID", "mfCode", sentColumnName, receivedColumnName];
    this.downloadKitList(fieldNames);
  }

  private downloadKitList(fieldNames: string[]){
    let map: {realm: string, participantId: string, shortID:string, mfCode: string, sent: string, received: string}[] = [];
    for (var i = 0; i < this.kitRequests.length; i++) {
      let sentDate: string = null;
      if (this.kitRequests[i].scanDate !== 0) {
        sentDate = Utils.getDateFormatted(new Date(this.kitRequests[i].scanDate), Utils.DATE_STRING_IN_CVS);
      }
      let receivedDate: string = null;
      if (this.kitRequests[i].receiveDate !== 0) {
        receivedDate = Utils.getDateFormatted(new Date(this.kitRequests[i].receiveDate), Utils.DATE_STRING_IN_CVS);
      }
      map.push({realm: this.kitRequests[i].realm,
        participantId: this.kitRequests[i].participantId,
        shortID: this.kitRequests[i].getID(),
        mfCode: this.kitRequests[i].kitLabel,
        sent: sentDate,
        received: receivedDate});
    }
    var fields = ["realm", "participantId", "shortID", "mfCode", "sent", "received"];
    var date = new Date();
    Utils.createCSV(fields, map, localStorage.getItem(ComponentService.MENU_SELECTED_REALM) + " Kits " + this.kitType.name + " " + Utils.getDateFormatted(date, Utils.DATE_STRING_CVS) + Statics.CSV_FILE_EXTENSION);
  }

  setKitRequest(kitRequest: KitRequest, modalType: string) {
    this.kitRequest = kitRequest;
    this.modalType = modalType;
    if (modalType !== this.DEACTIVATED) {
      this.dsmService.rateOfExpressLabel(this.kitRequest.dsmKitRequestId).subscribe(
        data => {
          // console.log(`Deactivating kit request received: ${JSON.stringify(data, null, 2)}`);
          if (data != null) {
            this.labelRate = EasypostLabelRate.parse(data);
            this.modal.show();
          }
          else {
            this.errorMessage = "Can't buy express label!";
          }
          this.loading = false;
        },
        err => {
          if (err._body === Auth.AUTHENTICATION_ERROR) {
            this.auth.logout();
          }
          this.loading = false;
          this.errorMessage = "Error - Buying express label\n" + err;
        }
      );
    }
  }

  deactivateKitRequest() {
    if (this.kitRequest != null && this.deactivationReason != null) {
      this.loading = true;
      let payload = {
        'reason': this.deactivationReason
      };
      // console.log(JSON.stringify(payload));
      this.dsmService.deactivateKitRequest(this.kitRequest.dsmKitRequestId, JSON.stringify(payload)).subscribe(
        data => {
          // console.log(`Deactivating kit request received: ${JSON.stringify(data, null, 2)}`);
          if (this.kitType != null) {
            this.loadKitRequestData(this.kitType);
          }
          this.loading = false;
        },
        err => {
          if (err._body === Auth.AUTHENTICATION_ERROR) {
            this.auth.logout();
          }
          this.loading = false;
          this.errorMessage = "Error - Deactivating kit request\n" + err;
        }
      );
      this.kitRequest = null;
      this.deactivationReason = null;
      this.modal.hide();
      window.scrollTo(0,0);
    }
  }

  generateExpressLabel() {
    if (this.kitRequest != null) {
      // console.log(JSON.stringify(payload));
      this.allSelected = false;
      this.errorMessage = null;
      this.loading = true;
      this.kitRequests = [];
      this.dsmService.expressLabel(this.kitRequest.dsmKitRequestId).subscribe(
        data => {
          // console.log(`Deactivating kit request received: ${JSON.stringify(data, null, 2)}`);
          if (this.kitType != null) {
            this.loadKitRequestData(this.kitType);
          }
          this.loading = false;
        },
        err => {
          if (err._body === Auth.AUTHENTICATION_ERROR) {
            this.auth.logout();
          }
          this.loading = false;
          this.errorMessage = "Error - Deactivating kit request\n" + err;
        }
      );
      this.kitRequest = null;
      this.deactivationReason = null;
      this.modal.hide();
      window.scrollTo(0,0);

    }
  }

  activateKitRequest(kitRequest: KitRequest) {
    this.dsmService.activateKitRequest(kitRequest.dsmKitRequestId).subscribe(
      data => {
        // console.log(`Deactivating kit request received: ${JSON.stringify(data, null, 2)}`);
        if (this.kitType != null) {
          this.loadKitRequestData(this.kitType);
        }
        this.loading = false;
      },
      err => {
        if (err._body === Auth.AUTHENTICATION_ERROR) {
          this.auth.logout();
        }
        this.loading = false;
        this.errorMessage = "Error - Deactivating kit request\n" + err;
      }
    );
  }

  closedNameModal() {
    this.modalType = this.DEACTIVATED
  }

  showNameModal() {
    this.modalType = this.NAME_LABELS;
  }

  allSentChecked() {
    if (this.allSentSelected) {
      this.setAllSentCheckboxes(true);
    }
    else{
      this.setAllSentCheckboxes(false);
    }
  }

  sentCheckboxChecked() {
    this.isSentButtonDisabled = true;
    this.allSentSelected = true;
    for (var i = 0; i < this.kitRequests.length; i++) {
      if (this.kitRequests[i].setSent) {
        this.isSentButtonDisabled = false;
      }
      else {
        this.allSentSelected = false;
      }
    }
  }

  private setAllSentCheckboxes(selected: boolean) {
    for (var i = 0; i < this.kitRequests.length; i++) {
      this.kitRequests[i].setSent = selected;
      this.isSentButtonDisabled = !selected;
    }
    if (!selected) {
      this.isSentButtonDisabled = true;
    }
  }

  setKitSent() {
    let map: { kit: string } [] = [];
    for (var i = 0; i < this.kitRequests.length; i++) {
      if (this.kitRequests[i].setSent) {
        map.push({kit: this.kitRequests[i].shippingId});
      }
    }
    this.dsmService.setKitSentRequest(JSON.stringify(map)).subscribe(
      data => {
        // console.log(`Deactivating kit request received: ${JSON.stringify(data, null, 2)}`);
        if (this.kitType != null) {
          this.loadKitRequestData(this.kitType);
        }
        this.loading = false;
      },
      err => {
        if (err._body === Auth.AUTHENTICATION_ERROR) {
          this.auth.logout();
        }
        this.loading = false;
        this.errorMessage = "Error - Deactivating kit request\n" + err;
      }
    );
  }

  public getTopMargin() {
    if (this.selectedSetting != null) {
      return this.selectedSetting.topMargin + "in";
    }
  }

  public getMarginBetweenTopBottom() {
    if (this.selectedSetting != null && this.selectedSetting.labelOnPage > 1) {
      var letter = 11.0;
      var space = letter - this.selectedSetting.topMargin - (this.selectedSetting.labelHeight * (this.selectedSetting.labelOnPage / 2)) - this.selectedSetting.bottomMargin;
      return space + "in";
    }
  }

  public getBottomMargin() {
    if (this.selectedSetting != null) {
      return this.selectedSetting.bottomMargin + "in";
    }
  }

  public getLabelHeight() {
    if (this.selectedSetting != null) {
      return this.selectedSetting.labelHeight + "in";
    }
  }

  triggerLabelCreation() {
    this.loading = true;
    let cleanedKits: Array<KitRequest> = KitRequest.removeUnselectedKitRequests(this.kitRequests);
    this.dsmService.singleKitLabel(JSON.stringify(cleanedKits)).subscribe(
      data => {
        let result = Result.parse(data);
        if (result.code === 200) {
          this.additionalMessage = "Triggered label creation";
          this.errorMessage = null;
          this.loadKitRequestData(this.kitType);
        }
        this.loading = false;
      },
      err => {
        if (err._body === Auth.AUTHENTICATION_ERROR) {
          this.auth.logout();
        }
        this.loading = false;
        this.errorMessage = "Error - Loading ddp information " + err;
      }
    );
  }

  reload() {
    this.loadKitRequestData(this.kitType);
  }

  realm(): string {
    return localStorage.getItem(ComponentService.MENU_SELECTED_REALM);
  }

  getUtil(): Utils {
    return this.util;
  }
}
