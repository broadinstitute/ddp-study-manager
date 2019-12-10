import { Pipe, PipeTransform } from "@angular/core";
import { SurveyStatus } from "../survey/survey.model";

@Pipe({
  name: "surveySort",
  // pure: false
})
export class SurveySortPipe implements PipeTransform {

  transform(array: SurveyStatus[]): SurveyStatus[] {
    var reA = /[^a-zA-Z-]/g;
    var reN = /[^0-9- ]/g;

    array.sort((a, b) => {
      if (a.surveyInfo.participantId != null && b.surveyInfo.participantId != null) {
        var aA = a.surveyInfo.participantId.replace(reA, "");
        var bA = b.surveyInfo.participantId.replace(reA, "");
        if (aA === bA) {
          var aN = parseInt(a.surveyInfo.participantId.replace(reN, ""), 10);
          var bN = parseInt(b.surveyInfo.participantId.replace(reN, ""), 10);
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
