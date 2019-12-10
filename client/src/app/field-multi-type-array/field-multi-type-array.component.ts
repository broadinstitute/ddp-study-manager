import {Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges, ViewChild} from "@angular/core";
import {ModalComponent} from "../modal/modal.component";
import {Value} from "../utils/value.model";

@Component( {
  selector: "app-field-multi-type-array",
  templateUrl: "./field-multi-type-array.component.html",
  styleUrls: [ "./field-multi-type-array.component.css" ]
} )
export class FieldMultiTypeArrayComponent implements OnInit, OnChanges {

  @ViewChild( ModalComponent )
  public universalModal: ModalComponent;

  @Input() possibleValues: Value[];
  @Input() jsonArray: any;
  @Input() disabled: boolean = false;
  @Input() fieldName: string;
  @Input() drugs: string[];
  @Input() cancers: string[];
  @Input() finished: boolean;
  @Output() changes = new EventEmitter();

  multiTypes = [];
  nope: boolean = false;

  constructor() {
  }

  ngOnInit() {
    this.setMultiTypes();
  }

  ngOnChanges( changes: SimpleChanges ) {
    this.setMultiTypes();
  }

  setMultiTypes() {
    if (this.jsonArray !== "no") {
      if (this.jsonArray != null && this.jsonArray !== "") {
        this.multiTypes = JSON.parse( this.jsonArray );
      }
      else {
        this.multiTypes = [];
        let multiType = {};
        if (this.possibleValues != null) {
          this.possibleValues.forEach( value => {
            multiType[ value.value ] = null;
          } );
          this.multiTypes.push( multiType );
        }
      }
    }
    else {
      this.nope = true;
    }
  }

  valuesChanged( change: any, fieldName: string, index: number ) {
    if (fieldName === "no" && change.checked != null) {
      if (this.nope) {
        //if there is already medication added double check that they want to remove it!
        if (this.jsonArray != null && this.jsonArray !== "no" && this.jsonArray !== "") {
          this.universalModal.show();
        }
        else {
          this.changeToNothing();
        }
      }
      else {
        this.jsonArray = "";
        this.multiTypes = [];
        let multiType = {};
        if (this.possibleValues != null) {
          this.possibleValues.forEach( value => {
            multiType[ value.value ] = null;
          } );
          this.multiTypes.push( multiType );
        }
      }
    }
    else {
      if (change == null) {
        if (this.multiTypes instanceof Array) {
          this.multiTypes.splice( index, 1 );
        }
      }
      else if (typeof change === "string") {
        if (typeof this.multiTypes === "string") {
          this.multiTypes = [];
        }
        this.multiTypes[ index ] = JSON.parse( change );
      }
      if (this.multiTypes instanceof Array && this.multiTypes.length < 1) {
        this.changes.emit( "" );
      }
      else {
        this.jsonArray = JSON.stringify( this.multiTypes );
        this.changes.emit( this.jsonArray );
      }
    }
  }

  addAction() {
    if (this.multiTypes == null || typeof this.multiTypes === "string") {
      this.multiTypes = [];
    }
    let multiType = {};
    if (this.possibleValues != null) {
      this.possibleValues.forEach( value => {
        multiType[ value.value ] = null;
      } );
      this.multiTypes.push( multiType );
    }
  }

  changeToNothing() {
    this.jsonArray = this.nope ? "no" : "";
    this.changes.emit( this.jsonArray );
    this.universalModal.hide();
  }

  abort() {
    this.nope = false;
  }

}
