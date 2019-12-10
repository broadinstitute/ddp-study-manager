import {Pipe, PipeTransform} from "@angular/core";

import {Utils} from "../utils/utils";

@Pipe({
  name: "dateFormatPipe"
})
export class DateFormatPipe implements PipeTransform {
  transform(value: any, args?: any): any {
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
    }
    return value;
  }
}
