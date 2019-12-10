import { Pipe, PipeTransform } from "@angular/core";

import { Event } from "../email-event/email-event.model";

@Pipe({
  name: "emailEventSort"
})
export class EmailEventSortPipe implements PipeTransform {

  transform(array: Event[]): Event[] {

    array.sort((a, b) => {
      if (a.timestamp != null && b.timestamp != null) {
        return a.timestamp > b.timestamp ? 1 : -1;
      }
    });
    return array;
  }
}
