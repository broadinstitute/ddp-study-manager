// import { Pipe, PipeTransform } from "@angular/core";
// import { Participant } from "../participant/participant.model";
//
// @Pipe({
//   name: "participantSort",
//   // pure: false
// })
// export class ParticipantSortPipe implements PipeTransform {
//
//   transform(array: Participant[], sort_field: string, sort_dir: string): Participant[] {
//     array.sort((a, b) => {
//       if (a === null || a[sort_field] == undefined || a[sort_field] === null) {
//         return 1;
//       }
//       else if (b === null || b[sort_field] == undefined || b[sort_field] === null) {
//         return -1;
//       }
//       else if (sort_field === "shortId") {
//         return parseInt(a[sort_field]) < parseInt(b[sort_field]) ? -1 : 1;
//       }
//       else {
//         if (a[sort_field].toLowerCase() < b[sort_field].toLowerCase()) {
//           return -1;
//         }
//         else if (a[sort_field].toLowerCase() > b[sort_field].toLowerCase()) {
//           return 1;
//         }
//         else {
//           return 0;
//         }
//       }
//     });
//     return array;
//   }
// }
