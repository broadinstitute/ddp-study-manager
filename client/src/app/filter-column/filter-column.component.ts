import {Component, Input, OnInit} from "@angular/core";
import {MdRadioChange} from "@angular/material";
import {Filter} from "./filter-column.model";

@Component( {
  selector: "app-filter-column",
  templateUrl: "./filter-column.component.html",
  styleUrls: [ "./filter-column.component.css" ]
} )
export class FilterColumnComponent implements OnInit {

  @Input() dataFilter: Filter;
  @Input() editable: boolean = true;

  showOptions: boolean = false;
  selected: number;

  constructor() {
  }

  ngOnInit() {
    if (this.dataFilter.singleOption) {
      for (let [ key, value ] of Object.entries( this.dataFilter.selectedOptions )) {
        if (value) {
          this.selected = Number(key);
          break;
        }
      }
      console.log(this.selected);
    }
  }

  dateChange( value: any, number: number ) {
    let v;
    if (typeof value === "string") {
      this.dataFilter[ "value" + number ] = value;
    }
    else {
      if (value.srcElement != null && typeof value.srcElement.value === "string") {
        v = value.srcElement.value;
        this.dataFilter[ "value" + number ] = v;
      }
    }
  }

  changeValue( value, dataFilter: Filter ) {
    if (value.checked) {
      dataFilter.value1 = true;
      dataFilter.value2 = false;
    }
    else {
      dataFilter.value1 = false;
    }
  }

  changeValue2( value, dataFilter: Filter ) {
    if (value.checked) {
      dataFilter.value1 = false;
      dataFilter.value2 = true;
    }
    else {
      dataFilter.value2 = false;
    }
  }

  hasSelectedOption( dataFilter: Filter ) {
    if (dataFilter.type !== Filter.OPTION_TYPE) {
      return false;
    }
    for (let o of dataFilter.selectedOptions) {
      if (o) {
        return true;
      }
    }
    return false;
  }

  radioChange( event: MdRadioChange ) {
    //deselect all values
    for (let i in this.dataFilter.selectedOptions) {
      if (this.dataFilter.selectedOptions[ i ]) {
        this.dataFilter.selectedOptions[ i ] = false;
      }
    }

    //set value to selected
    this.dataFilter.selectedOptions[ event.value ] = true;
  }


}
