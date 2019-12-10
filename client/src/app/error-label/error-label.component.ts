import { Component, Input, AfterViewChecked } from '@angular/core';
import { Address } from "../address/address.model";
import {LabelSetting} from "../label-settings/label-settings.model";

declare var JsBarcode: any;

@Component({
  selector: 'app-error-label',
  templateUrl: './error-label.component.html',
  styleUrls: ['./error-label.component.css']
})
export class ErrorLabelComponent implements AfterViewChecked {

  @Input() address: Address;
  @Input() shippingId: string;
  @Input() labelSetting: LabelSetting;

  constructor() {
  }

  ngAfterViewChecked() {
    JsBarcode('#' + this.shippingId).init();
  }

  public getLabelHeight() {
    if (this.labelSetting != null) {
      //making it a little smaller than it could be
      return (this.labelSetting.labelHeight - 0.2) + "in";
    }
  }

  public getFullLabelHeight() {
    if (this.labelSetting != null) {
      //making it a little smaller than it could be
      return this.labelSetting.labelHeight + "in";
    }
  }

  public getLabelWidth() {
    if (this.labelSetting != null) {
      //making it a little smaller than it could be
      return (this.labelSetting.labelWidth - 0.2) + "in";
    }
  }

  public getFullLabelWidth() {
    if (this.labelSetting != null) {
      //making it a little smaller than it could be
      return this.labelSetting.labelWidth + "in";
    }
  }

  public getLeftMargin() {
    if (this.labelSetting != null) {
      return this.labelSetting.leftMargin + "in";
    }
  }

  public getMarginBetweenLeftRight() {
    if (this.labelSetting != null && this.labelSetting.labelOnPage > 1) {
      var letter = 8.5
      var space = letter - this.labelSetting.leftMargin - (this.labelSetting.labelWidth * (this.labelSetting.labelOnPage / 2)) - this.labelSetting.rightMargin;
      return space + "in";
    }
  }
}
