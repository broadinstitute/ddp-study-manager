import { Pipe, PipeTransform } from "@angular/core";
import { ParticipantExit } from "../participant-exit/participant-exit.model";

@Pipe({
  name: "participantExitSort"
})
export class ParticipantExitSortPipe implements PipeTransform {

  transform(array: ParticipantExit[], sort_field: string, sort_dir: string): ParticipantExit[] {
    array.sort((a, b) => {
      if (sort_field === "getID()") {
        if (typeof a.shortId === "string") {
          return 1;
        }
        if (typeof b.shortId === "string") {
          return -1;
        }
        if (a.shortId < b.shortId) {
          return sort_dir === "asc" ? -1 : 1;
        }
        else if (a.shortId > b.shortId) {
          return sort_dir === "asc" ? 1 : -1;
        }
        else {
          return 0;
        }
      }
    });
    return array;
  }
}
