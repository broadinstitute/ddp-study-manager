import { Pipe, PipeTransform } from "@angular/core";
import { SurveyStatus } from "../survey/survey.model";

@Pipe({
  name: "surveyFilter",
  // pure: false
})
export class SurveyFilterPipe implements PipeTransform {

  transform(array: SurveyStatus[], filterParticipantId:string, filterShortId: string, filterReason: string, filterUser:string): SurveyStatus[] {
    if (filterParticipantId != "") {
      array = array.filter( row => row.surveyInfo.participantId != null && row.surveyInfo.participantId.indexOf(filterParticipantId) > -1);
    }
    if (filterShortId != "") {
      array = array.filter( row => row.surveyInfo.shortId != null && row.surveyInfo.shortId.indexOf(filterShortId) > -1
        || row.surveyInfo.legacyShortId != null && row.surveyInfo.legacyShortId.indexOf(filterShortId) > -1);
    }
    if (filterReason != "") {
      array = array.filter( row => row.reason != null && row.reason.toLowerCase().indexOf(filterReason.toLowerCase()) > -1);
    }
    if (filterUser != "") {
      array = array.filter( row => row.user != null && row.user.toLowerCase().indexOf(filterUser.toLowerCase()) > -1);
    }
    return array;
  }

}
