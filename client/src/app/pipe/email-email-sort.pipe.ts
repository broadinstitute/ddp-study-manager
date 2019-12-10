import { Pipe, PipeTransform } from "@angular/core";

import { Email } from "../email-event/email-event.model";

@Pipe({
  name: "emailEmailSort"
})
export class EmailEmailSortPipe implements PipeTransform {

  transform(array: Email[]): Email[] {
    var reA = /[^a-zA-Z-]/g;
    var reN = /[^0-9- ]/g;

    array.sort((a, b) => {
      if (a.email != null && b.email != null) {
        var aA = a.email.replace(reA, "");
        var bA = b.email.replace(reA, "");
        if (aA === bA) {
          var aN = parseInt(a.email.replace(reN, ""), 10);
          var bN = parseInt(b.email.replace(reN, ""), 10);
          return aN === bN ? 0 : aN > bN ? 1 : -1;
        }
        else {
          return aA > bA ? 1 : -1;
        }
      }
    });
    return array;
  }
}
