import {Pipe, PipeTransform} from "@angular/core";

import {Utils} from "../utils/utils";
import {DatePipe} from "@angular/common";

@Pipe({
  name: "dateFormatPipe"
})
export class DateFormatPipe implements PipeTransform {
  transform(value: any, args?: any): any {
    console.log(value);
    if (value != null) {
      if (value.length == 10) {
        let tmp = Utils.getDate(value);
        if (tmp instanceof Date) {
          let t = Utils.getDateFormatted(tmp, args);
          return t;
        }
      }
      else if (value.length == 7) {
        return Utils.getPartialFormatDate(value, args);
      }
      else if (typeof value === "number") {
        let date =  new Date(value);
        // let s = date.getMonth() + 1 + "/" + date.getDate() + "/" + date.getFullYear();
        // console.log(s);
        return new DatePipe( "en-US" ).transform( date, Utils.DATE_STRING_IN_CVS );
      }
    }
    return value;
  }
}
