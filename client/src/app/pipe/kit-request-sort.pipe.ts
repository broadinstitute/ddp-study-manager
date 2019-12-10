import { Pipe, PipeTransform } from "@angular/core";
import { KitRequest } from "../shipping/shipping.model";

@Pipe({
  name: "kitRequestSort"
})
export class KitRequestSortPipe implements PipeTransform {

  transform(array: KitRequest[], sort_field: string, sort_dir: string): KitRequest[] {
    var reA = /[^a-zA-Z]/g;
    var reN = /[^0-9]/g;

    array.sort((a, b) => {
      var asc = 1;
      if (sort_dir !== "asc") {
        asc = -1;
      }
      if (sort_field === "default") {
        if (a != null && b != null && a.getID() != null && b.getID() != null) {
          var aA = a.getID().replace(reA, "");
          var bA = b.getID().replace(reA, "");

          if (aA === bA) {
            var aN = parseInt(a.getID().replace(reN, ""), 10);
            var bN = parseInt(b.getID().replace(reN, ""), 10);
            if (a.express && b.express) {
              return aN === bN ? 0 : aN > bN ? 1 * asc : -1 * asc;
            }
            if (a.express) {
              return -1;
            }
            if (b.express) {
              return 1;
            }
            return aN === bN ? 0 : aN > bN ? 1 * asc : -1 * asc;
          }
          else {
            if (a.express) {
              return -1;
            }
            if (b.express) {
              return 1;
            }
            return aA > bA ? 1 * asc : -1 * asc;
          }
        }
      }
    });
    return array;
  }
}
