import {Component, EventEmitter, Input, OnInit, Output} from "@angular/core";
import {Value} from "../utils/value.model";

@Component( {
  selector: "app-field-multi-type",
  templateUrl: "./field-multi-type.component.html",
  styleUrls: [ "./field-multi-type.component.css" ]
} )
export class FieldMultiTypeComponent implements OnInit {

  @Input() disabled: boolean = false;
  @Input() json: string;
  @Input() values: Value[];
  @Input() multiType: any;
  @Input() fieldName: string;
  @Input() drugs: string[];
  @Input() cancers: string[];
  @Input() patchFinished: boolean;
  @Output() multiTypeChanged = new EventEmitter();

  _multiType: { [ k: string ]: string | string[] } = {};
  showDelete: boolean = false;
  currentPatchField: string;
  _other: { [ k: string ]: string } = {};

  constructor() {
  }

  ngOnInit() {
    this.setOtherOptionText();
  }

  setOtherOptionText() {
    if (this.multiType != null) {
      this.showDelete = true;
      this._multiType = this.multiType;
      if (this.values != null) {
        this.values.forEach( value => {
            if (( value.type === "multi_options" || value.type2 === "multi_options" )) {
              let v: any = this.multiType[ value.value ];
              if (typeof v === "string" && !v.startsWith( "[" )) {
                //type was changed to multi_select afterwards
                let array: string[] = [];
                array.push( v );
                this._multiType[ value.value ] = array;
                if (array.includes( "other" )) {
                  this._other[ value.value ] = "";
                }
              }
              else if (v instanceof Array) {
                this._multiType[ value.value ] = v;
                if (v.includes( "other" )) {
                  this._other[ value.value ] = "";
                }
              }
              else {
                let array: string[] = JSON.parse( v );
                this._multiType[ value.value ] = array;
              }
            }
            else if (( value.type === "options" || value.type2 === "options" )) {
              this.setOther( value.value );
            }
          }
        );
      }
    }
    else {
      if (this.values != null) {
        if (this.json != null) {
          this._multiType = JSON.parse( this.json );
        }
        this.values.forEach( value => {
          if (value.type === "multi_options" || value.type2 === "multi_options") {
            if (this.json != null) {
              let o: any = JSON.parse( this.json );
              this._multiType[ value.value ] = o[ value.value ];
              if (this._multiType[ value.value ] != null && this._multiType[ value.value ].includes( "other" )) {
                this._other[ value.value ] = "";
                if (this._multiType[ "other" ] != null) {
                  this._other[ value.value ] = String( this._multiType[ "other" ] );
                }
              }
            }
            else {
              let array: string[] = [];
              this._multiType[ value.value ] = array;
            }
          }
          else if (( value.type === "options" || value.type2 === "options" )) {
            this.setOther( value.value );
          }
        } );
      }
    }
  }

  setOther( value: string ) {
    if (this._multiType[ "other" ] != null && this._multiType[ "other" ] instanceof Array) {
      for (let other of this._multiType[ "other" ]) {
        if (other.hasOwnProperty( value )) {
          if (other[ value ] != null && other[ value ] != "") {
            this._other[ value ] = other[ value ];
          }
        }
      }
    }
    else if (this._multiType[ value ] === "other") {
      this._other[ value ] = "";
    }
    else {
      this._other[ value ] = null;
    }
  }

  multiTypeValueChanged( value: any, multiType: {}, displayName: string, val: Value ) {
    this.patchFinished = false;
    let v;
    if (typeof value === "string") {
      v = value;
    }
    else {
      if (value.srcElement != null && typeof value.srcElement.value === "string") {
        v = value.srcElement.value;
      }
      else if (value.value != null) {
        v = value.value;
      }
      else if (value.checked != null) {
        v = value.checked;
      }
      else if (value.source.value != null && value.source.selected) {
        v = value.source.value;
      }
    }
    multiType[ displayName ] = v;
    this.currentPatchField = displayName;

    if (displayName === "other") {
      let array: { [ k: string ]: string }[] = [];
      for (let source of Object.keys( this._other )) {
        let other: { [ k: string ]: string } = {};
        other[ source ] = this._other[ source ];
        array.push( other );
      }
      multiType[ "other" ] = array;
    }
    else if (val.type === "multi_options" || val.type2 === "multi_options" || val.type === "options" || val.type2 === "options") {
      if (multiType[ displayName ].indexOf( "other" ) > -1) {
        if (this._other[ val.value ] !== "" && this._other[ val.value ] != null) {
          let array: { [ k: string ]: string }[] = [];
          for (let source of Object.keys( this._other )) {
            let other: { [ k: string ]: string } = {};
            other[ source ] = this._other[ source ];
            array.push( other );
          }
          multiType[ "other" ] = array;
        }
        else {
          this._other[ val.value ] = "";
        }
      }
      else {
        this._other[ val.value ] = null;
        // delete multiType[ "other" ];
      }
    }
    this.multiTypeChanged.emit( JSON.stringify( multiType ) );
  }

  delete() {
    this.multiTypeChanged.emit( null );
  }

  isPatchedCurrently( field: string ): boolean {
    if (this.currentPatchField === field && !this.patchFinished) {
      return true;
    }
    return false;
  }

  currentField( field: string ) {
    if (field != null || ( field == null && this.patchFinished )) {
      this.currentPatchField = field;
    }
  }
}
