import { Pipe, PipeTransform } from "@angular/core";
import { OncHistoryDetail } from "../onc-history-detail/onc-history-detail.model";

@Pipe({
  name: "oncHistoryDetailSort",
})
export class OncHistoryDetailSortPipe implements PipeTransform {

  transform(array: OncHistoryDetail[], trigger: number): OncHistoryDetail[] {
    var reA = /[^a-zA-Z]/g;
    var reN = /[^0-9]/g;

    array.sort((a, b) => {
      if (a.datePX != null && b.datePX != null && a.datePX != undefined && b.datePX != undefined) {
        var aA = a.datePX.replace(reA, "");
        var bA = b.datePX.replace(reA, "");
        if (aA === bA) {
          var aN = parseInt(a.datePX.replace(reN, ""), 10);
          var bN = parseInt(b.datePX.replace(reN, ""), 10);
          return aN === bN ? 0 : aN > bN ? 1 : -1;
        }
        else {
          return aA > bA ? 1 : -1;
        }
      }
      if (b.oncHistoryDetailId == null) {
        return -1;
      }
      if (a.datePX == null) {
        return 1;
      }
      if (b.datePX == null) {
        return -1;
      }
    });
    return array;
  }
}
