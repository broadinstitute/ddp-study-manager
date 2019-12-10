import {Component, EventEmitter, Input, OnInit, Output} from "@angular/core";
import {TypeaheadMatch} from "ngx-bootstrap";

@Component( {
  selector: "app-field-typeahead",
  templateUrl: "./field-typeahead.component.html",
  styleUrls: [ "./field-typeahead.component.css" ]
} )
export class FieldTypeaheadComponent implements OnInit {

  @Input() dataSource: string[];
  @Input() drug: string;
  @Input() disabled: boolean;
  @Input() fieldName: string;
  @Output() drugSelected = new EventEmitter();

  constructor() {
  }

  ngOnInit() {
  }

  selectDrug( e: TypeaheadMatch ): void {
    this.drugSelected.emit( this.drug );
  }
}
