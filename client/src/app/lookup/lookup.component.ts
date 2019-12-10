import {Component, EventEmitter, Input, OnInit, Output} from "@angular/core";

import {Lookup} from "./lookup.model";
import {DSMService} from "../services/dsm.service";
import {ComponentService} from "../services/component.service";
import {Statics} from "../utils/statics";

@Component({
  selector: "app-lookup",
  templateUrl: "./lookup.component.html",
  styleUrls: ["./lookup.component.css"]
})
export class LookupComponent implements OnInit {

  @Input() lookupValue: string;
  @Input() lookupType: string;
  @Input() disabled: boolean = false;
  @Input() fieldName: string;
  @Input() placeholder: string;
  @Input() largeInputField: boolean = false;
  @Input() multiLineInput: boolean = false;
  @Input() colorDuringPatch: boolean = false;

  @Output() lookupResponse = new EventEmitter();

  lookups: Array<Lookup> = [];
  currentPatchField: string;

  constructor(private dsmService: DSMService, private compService: ComponentService) {
  }

  ngOnInit() {
  }

  public checkForLookup(): Array<Lookup> {
    let jsonData: any[];
    if (this.lookupValue.trim() !== "") {
      if (!(this.lookupValue.length > 1 && this.lookups.length === 0)) {
        this.dsmService.lookupValue(this.lookupType, this.lookupValue, localStorage.getItem(ComponentService.MENU_SELECTED_REALM)).subscribe(// need to subscribe, otherwise it will not send!
          data => {
            // console.log(`received: ${JSON.stringify(data, null, 2)}`);
            this.lookups = [];
            jsonData = data;
            jsonData.forEach((val) => {
              let value = Lookup.parse(val);
              this.lookups.push(value);
            });
          },
          err => {
          }
        );
      }
    }
    return this.lookups;
  }

  public setLookup(object: Lookup): boolean {
    this.lookupResponse.next(object);
    this.lookups = [];
    return false;
  }

  public getStyleDisplay() {
    if (this.lookups.length > 0) {
      return Statics.DISPLAY_BLOCK;
    }
    else {
      return Statics.DISPLAY_NONE;
    }
  }

  public changeValue() {
    this.lookupResponse.next(this.lookupValue);
  }

  currentField(field: string) {
    this.currentPatchField = field;
  }

  isPatchedCurrently(field: string): boolean {
    if (this.currentPatchField === field) {
      return true;
    }
    return false;
  }
}
