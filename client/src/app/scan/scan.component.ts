import {ChangeDetectorRef, Component, OnInit} from "@angular/core";
import {ScanPairComponent} from "../scan-pair/scan-pair.component";
import {ScanPair, ScanValue} from "./scan.model";
import {DSMService} from "../services/dsm.service";
import {ActivatedRoute, Router} from "@angular/router";
import {ScanError} from "./error.model";
import {Auth} from "../services/auth.service";
import {Statics} from "../utils/statics";
import {ScanValueComponent} from "../scan-value/scan-value.component";
import {ComponentService} from "../services/component.service";

@Component({
  selector: 'app-scan',
  templateUrl: './scan.component.html',
  styleUrls: ['./scan.component.css'],
})
export class ScanComponent implements OnInit {

  scanPairs: Array<ScanPairComponent> = [];
  private scanPairsValue: Array<ScanPair> = [];
  private scanErrors: Array<ScanError> = [];

  duplicateDetected: boolean = false;

  additionalMessage: string;

  scanTracking: boolean = false;
  leftPlaceholder: string = "Kit Label";
  rightPlaceholder: string = "DSM Label";

  scanReceived: boolean = false;
  scanValues: Array<ScanValueComponent> = [];
  private singleScanValues: Array<ScanValue> = [];

  constructor(private _changeDetectionRef: ChangeDetectorRef, private dsmService: DSMService, private router: Router,
              private auth: Auth, private route: ActivatedRoute, private compService: ComponentService) {
    if (!auth.authenticated()) {
      auth.logout();
    }
    this.route.queryParams.subscribe(params => {
      this.scanTracking = params[DSMService.SCAN_TRACKING] || false;
      this.scanReceived = params[DSMService.SCAN_RECEIVED] || false;
      this.changePlaceholder();
      this.createNewComponents();
      this.additionalMessage = null;
      let realm = params[DSMService.REALM] || null;
      if (realm != null && realm !== "") {
        //        this.compService.realmMenu = realm;
      }
    });
  }

  private changePlaceholder() {
    if (this.scanTracking) {
      this.leftPlaceholder = "Tracking Label";
      this.rightPlaceholder = "Kit Label";
    }
    else if (this.scanReceived) {
      this.leftPlaceholder = "SM-ID";
    }
    else {
      this.leftPlaceholder = "Kit Label";
      this.rightPlaceholder = "DSM Label";
    }
  }

  public scanDone(arg) { //arg[0] = leftvalue (ddpLabel), arg[1] = rightvalue (kitLabel) and arg[2] = position
    if (arg.length === 3) {
      if (!this.checkIfKitLabelChanged(arg[0], arg[1], arg[2])) {
        this.scanPairsValue.push(new ScanPair(arg[0], arg[1]));
        this.addNewScanPair();
        this._changeDetectionRef.detectChanges();
      }
    }
  }

  private checkIfKitLabelChanged(left: string, right: string, position: number): boolean {
    for (let i = 0; i < this.scanPairsValue.length; i++) {
      if (this.scanPairsValue[i].leftValue === left && i === position) {
        this.scanPairsValue[i].rightValue = right;
        return true;
      }
    }
    return false;
  }

  public validateRightValue(position: number): boolean {
    if (this.scanPairsValue.length > 0 && this.scanPairsValue[position] != null) {
      return this.validateValue(this.scanPairsValue[position].rightValue, position, false);
    }
    return false;
  }

  public validateLeftValue(position: number): boolean {
    if (this.scanPairsValue.length > 0 && this.scanPairsValue[position] != null) {
      return this.validateValue(this.scanPairsValue[position].leftValue, position, true);
    }
    return false;
  }

  private validateValue(labelValue: string, position: number, isLeft: boolean): boolean {
    let isDuplicate = false;
    for (let i = 0; i < this.scanPairsValue.length; i++) {
      if (i != position) {
        if (this.scanPairsValue[position].leftValue != null && labelValue === this.scanPairsValue[i].leftValue
          || this.scanPairsValue[position].rightValue != null && labelValue === this.scanPairsValue[i].rightValue){
          isDuplicate = true;
        }
      }
      else {
        if (isLeft) {
          if (this.scanPairsValue[position].rightValue != null && labelValue === this.scanPairsValue[i].rightValue){
            isDuplicate = true;
          }
        }
        else {
          if (this.scanPairsValue[position].leftValue != null && labelValue === this.scanPairsValue[i].leftValue){
            isDuplicate = true;
          }
        }
      }
    }
    return isDuplicate;
  }

  public removeScanPair(position: number) {
    this.scanPairs.splice(position, 1);
    this.scanPairsValue.splice(position, 1);
  }

  private addNewScanPair() {
    let newRow = new ScanPairComponent();
    this.scanPairs.push(newRow);
  }

  ngOnInit() {
    this.additionalMessage = null;
    this.createNewComponents();
  }

  createNewComponents() {
    this.scanPairsValue = [];
    this.scanPairs = [];
    this.scanValues = [];
    this.singleScanValues = [];
    if (this.scanTracking) {
      this.addNewScanPair();
      if (this.scanPairs.length < 1) {
        let newScanPair = new ScanPairComponent();
        this.scanPairs.push(newScanPair);
      }
    }
    else if (this.scanReceived) {
      this.addNewSingleScan();
      if (this.scanValues.length < 1) {
        let newScanValue = new ScanValueComponent();
        this.scanValues.push(newScanValue);
      }
    }
    else {
      this.addNewScanPair();
      if (this.scanPairs.length < 1) {
        let newScanPair = new ScanPairComponent();
        this.scanPairs.push(newScanPair);
      }
    }
  }

  public savePairs() {
    if (this.scanPairsValue.length > 0) {
      this.duplicateDetected = false;
      for (let i = 0; i < this.scanPairsValue.length; i++) {
        if (this.validateValue(this.scanPairsValue[i].leftValue, i, true)) {
          this.duplicateDetected = true;
          break;
        }
        if (this.validateValue(this.scanPairsValue[i].rightValue, i, false)) {
          this.duplicateDetected = true;
          break;
        }
      }

      if (!this.duplicateDetected) {
        let jsonData: any[];
        this.scanErrors = [];
        this.dsmService.transferScan(this.scanTracking, JSON.stringify(this.scanPairsValue)).subscribe(// need to subscribe, otherwise it will not send!
          data => {
            let failedSending: boolean = false;
            jsonData = data;
            jsonData.forEach((val) => {
              this.scanErrors.push(ScanError.parse(val));
              failedSending = true;
            });
            if (failedSending) {
              this.removeSuccessfulScans();
              this.additionalMessage = "Error - Failed to save all changes";
            }
            else {
              this.scanPairsValue = [];
              this.scanPairs = [];
              this.addNewScanPair();
              this.additionalMessage =  "Data saved";
            }
          },
          err => {
            if (err._body === Auth.AUTHENTICATION_ERROR) {
              this.router.navigate([Statics.HOME_URL]);
            }
            this.additionalMessage = "Error - Failed to save data";
          }
        );
      }
    }
  }

  private removeSuccessfulScans(){
    for (let i = this.scanPairsValue.length-1; i >= 0; i--) {
      let found: boolean = false;
      for (let j = this.scanErrors.length-1; j >= 0; j--) {
        if (this.scanPairsValue[i].rightValue === this.scanErrors[j].kit) {
          found = true;
        }
      }
      if (!found) {
        this.scanPairs.splice(i, 1);
        this.scanPairsValue.splice(i, 1);
      }
    }
  }

  private removeSuccessfulSingleScans(){
    for (let i = this.singleScanValues.length-1; i >= 0; i--) {
      let found: boolean = false;
      for (let j = this.scanErrors.length-1; j >=0; j--) {
        if (this.singleScanValues[i].kit === this.scanErrors[j].kit) {
          found = true;
        }
      }
      if (!found) {
        this.scanValues.splice(i, 1);
        this.singleScanValues.splice(i, 1);
      }
    }
  }

  public checkSendStatus(position: number): boolean {
      if (this.scanPairsValue.length > 0 && this.scanPairsValue[position] != null
        && this.scanPairsValue[position].rightValue != null
        && this.scanErrors.length > 0) {
        for (let i = 0; i < this.scanErrors.length; i++) {
          if (this.scanPairsValue[position].rightValue === this.scanErrors[i].kit) {
            return true;
          }
        }
        return false;
      }
    return false;
  }

  public setLeftValue(arg) { //arg[0] = dsmValue and arg[1] = position
    if (arg.length === 2) {
      if (arg[1] < this.scanPairsValue.length) {
        this.scanPairsValue[arg[1]].leftValue = arg[0];
      }
    }
  }

  public singleValueScanDone(arg) { //arg[0] = singleValue (SM-ID) and arg[1] = position
    if (arg.length === 2) {
      if (!this.checkIfSingleValueChanged(arg[0], arg[1])) {
        this.singleScanValues.push(new ScanValue(arg[0]));
        this.addNewSingleScan();
        this._changeDetectionRef.detectChanges();
      }
    }
  }

  private checkIfSingleValueChanged(value: string, position: number): boolean {
    for (let i = 0; i < this.singleScanValues.length; i++) {
      if (this.singleScanValues[i].kit === value && i === position) {
        return true;
      }
    }
    return false;
  }

  private addNewSingleScan() {
    let newRow = new ScanValueComponent();
    this.scanValues.push(newRow);
  }

  public removeScanValue(position: number) {
    this.scanValues.splice(position, 1);
    this.singleScanValues.splice(position, 1);
  }

  validateSingleScan(position: number): boolean {
    let isDuplicate = false;
    for (let i = 0; i < this.singleScanValues.length - 1; i++) {
      if (i != position) {
        if (this.singleScanValues[position] != null && this.singleScanValues[i] != null
          && this.singleScanValues[position].kit != null && this.singleScanValues[i].kit != null
          && this.singleScanValues[position].kit === this.singleScanValues[i].kit){
          isDuplicate = true;
        }
      }
    }
    return isDuplicate;
  }

  public checkSingleScanSendStatus(position: number): boolean {
    if (this.singleScanValues.length > 0 && this.singleScanValues[position] != null
      && this.singleScanValues[position].kit != null
      && this.scanErrors.length > 0) {
      for (let i = 0; i < this.scanErrors.length; i++) {
        if (this.singleScanValues[position].kit === this.scanErrors[i].kit) {
          return true;
        }
      }
      return false;
    }
    return false;
  }

  public saveValues() {
    if (this.singleScanValues.length > 0) {
      this.duplicateDetected = false;
      for (let i = 0; i < this.singleScanValues.length; i++) {
        if (this.validateSingleScan(i)) {
          this.duplicateDetected = true;
          break;
        }
      }

      if (!this.duplicateDetected) {
        let jsonData: any[];
        this.scanErrors = [];
        this.dsmService.setKitReceivedRequest(JSON.stringify(this.singleScanValues)).subscribe(// need to subscribe, otherwise it will not send!
          data => {
            let failedSending: boolean = false;
            jsonData = data;
            jsonData.forEach((val) => {
              this.scanErrors.push(ScanError.parse(val));
              failedSending = true;
            });
            if (failedSending) {
              this.removeSuccessfulSingleScans();
              this.additionalMessage = "Error - Failed to save all changes";
            }
            else {
              this.scanValues = [];
              this.singleScanValues = [];
              this.addNewSingleScan();
              this.additionalMessage =  "Data saved";
            }
          },
          err => {
            if (err._body === Auth.AUTHENTICATION_ERROR) {
              this.router.navigate([Statics.HOME_URL]);
            }
            this.additionalMessage = "Error - Failed to save data";
          }
        );
      }
    }
  }

  public getError(position: number): string {
    for (let j = this.scanErrors.length - 1; j >= 0; j--) {
      if (this.scanPairsValue[position] != null && this.scanPairsValue[position].rightValue === this.scanErrors[j].kit) {
        return this.scanErrors[j].error;
      }
    }
    return null;
  }

  public getSingleError(position: number): string {
    for (let j = this.scanErrors.length - 1; j >= 0; j--) {
      if (this.singleScanValues[position] != null && this.singleScanValues[position].kit === this.scanErrors[j].kit) {
        return this.scanErrors[j].error;
      }
    }
    return null;
  }
}

