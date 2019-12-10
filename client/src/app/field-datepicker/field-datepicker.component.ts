import {Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges} from "@angular/core";
import {Utils} from "../utils/utils";
import {EstimatedDate} from "./field-datepicker.model";

@Component( {
  selector: "app-field-datepicker",
  templateUrl: "./field-datepicker.component.html",
  styleUrls: [ "./field-datepicker.component.css" ]
} )
export class FieldDatepickerComponent implements OnInit, OnChanges {

  @Input() dateString: string;
  @Input() disabled: boolean = false;
  @Input() colorDuringPatch: boolean = false;
  @Input() showTodayButton: boolean = true;
  @Input() showNAButton: boolean = false;
  @Input() showNotFoundButton: boolean = false;
  @Input() allowUnknownDay: boolean = false;
  @Input() dateFormat: string = Utils.DATE_STRING_IN_CVS;
  @Input() allowFutureDate: boolean = false;
  @Input() addCheckboxEstimated: boolean = false;
  @Input() showCalendarButton: boolean = true;
  @Input() fieldName: string;
  @Output() dateChanged = new EventEmitter();

  _dateString: string;
  error: string = null;
  estimated: boolean = false;
  datePicker: Date;
  showDatePicker: boolean = false;
  isNA: boolean = false;

  constructor( private util: Utils ) {
  }

  ngOnInit() {
    this.setInput();
  }

  ngOnChanges( changes: SimpleChanges ) {
    this.setInput();
  }

  setInput() {
    if (this.dateString != null && this.dateString !== "") {
      if (this.addCheckboxEstimated) {
        let estimatedDate = EstimatedDate.parse( this.dateString );
        if (estimatedDate != null) {
          this.handleInput( estimatedDate.dateString );
          this.estimated = estimatedDate.est;
        }
      }
      else {
        this.handleInput( this.dateString );
      }
    }
    else {
      this._dateString = "";
      this.estimated = false;
    }
  }

  private handleInput( dateString: string ) {
    if (dateString !== "N/A" && dateString !== "Not Found") {
      if (dateString != null) {
        let tmpDate: string = dateString;
        if (dateString.length === 7) {
          tmpDate = tmpDate + "-01";
          this._dateString = Utils.getPartialFormatDate( dateString, this.dateFormat )
        }
        else if (dateString.length === 4) {
          tmpDate = tmpDate + "-01-01";
          this._dateString = dateString;
        }
        else {
          this._dateString = Utils.getDateFormatted( this.datePicker, this.dateFormat );
        }
        this.datePicker = Utils.getDate( tmpDate );
      }
    }
    else {
      this._dateString = dateString;
    }
  }

  public check(): void {
    this.colorDuringPatch = true;
    if (this._dateString != null) {
      if (this._dateString === "") {
        this.emitDate( "" );
      }
      else {
        if (this._dateString !== "N/A" && this._dateString !== "Not Found") {
          let tmp = Utils.parseDate( this._dateString, this.dateFormat, this.allowUnknownDay );
          // console.log(tmp);
          if (tmp instanceof Date) {
            this.datePicker = tmp;
            if (this.datePicker != null) {
              this.emitDate( Utils.getDateFormatted( this.datePicker, Utils.DATE_STRING ) );
              this.colorDuringPatch = false;
              this.error = null;
            }
            else {
              this.error = this.dateFormat;
            }
          }
          else if (tmp === Utils.DATE_PARTIAL) {
            this.error = null;
            this.emitDate( Utils.getPartialDateFormatted( this._dateString, this.dateFormat ) );
            this.colorDuringPatch = false;
          }
          else if (tmp == null) {
            this.error = this.dateFormat;
          }
        }
      }
    }
    else {
      this.colorDuringPatch = false;
      this.error = null;
    }
  }

  public selectDate( event: any ): void {
    this.showDatePicker = false;
    this.datePicker = event;
    if (this.datePicker instanceof Date) {
      this._dateString = Utils.getDateFormatted( this.datePicker, this.dateFormat ); //show user date in the format from the user settings
      this.emitDate( Utils.getDateFormatted( this.datePicker, Utils.DATE_STRING ) );
    }
  }

  getUtil(): Utils {
    return this.util;
  }

  public setToday(): void {
    this.datePicker = new Date();
    this.showDatePicker = false;
    this.isNA = false;
    this._dateString = Utils.getDateFormatted( this.datePicker, this.dateFormat ); //show user date in the format from the user settings
    this.emitDate( Utils.getDateFormatted( this.datePicker, Utils.DATE_STRING ) );
  }

  public setNA(): void {
    this.isNA = true;
    this._dateString = "N/A";
    this.emitDate( this._dateString );
  }

  public setNotFound(): void {
    this.isNA = true;
    this._dateString = "Not Found";
    this.emitDate( this._dateString );
  }

  public closeCalendar(): void {
    this.showDatePicker = !this.showDatePicker;
  }

  private emitDate( dateString: string ) {
    if (this.addCheckboxEstimated) {
      this.dateChanged.emit( JSON.stringify( new EstimatedDate( dateString, this.estimated ) ) );
    }
    else {
      this.dateChanged.emit( dateString );
    }
  }

  estimatedChanged() {
    this._dateString = Utils.getDateFormatted( this.datePicker, this.dateFormat ); //show user date in the format from the user settings
    this.emitDate( Utils.getDateFormatted( this.datePicker, Utils.DATE_STRING ) );
  }

  getDateFormatPlaceholder( placeholder: string ) {
    return placeholder.toLowerCase();
  }

}
