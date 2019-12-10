import { Pipe, PipeTransform } from "@angular/core";
import { NameValue } from "../utils/name-value.model";

@Pipe({
  name: "dashboardDateSort"
})
export class DashboardDateSortPipe implements PipeTransform {

  transform(array: NameValue[], args: string): NameValue[] {
    var reA = /[^a-zA-Z]/g;
    var reN = /[^0-9]/g;

    array.sort((a, b) => {
      var aA = a.name.replace(reA, "");
      var bA = b.name.replace(reA, "");
      if(aA === bA) {
        var aN = parseInt(a.name.replace(reN, ""), 10);
        var bN = parseInt(b.name.replace(reN, ""), 10);
        return aN === bN ? 0 : aN > bN ? -1 : 1;
      } else {
        return aA > bA ? -1 : 1;
      }
    });
    return array;
  }
}
