import {Pipe, PipeTransform} from "@angular/core";
import {MedicalRecordLog} from "../medical-record/model/medical-record-log.model";

@Pipe({
  name: "medicalRecordLogSort"
})
export class MedicalRecordLogSortPipe implements PipeTransform {

  transform(array: MedicalRecordLog[], args: string): MedicalRecordLog[] {
    array.sort((a, b) => {
      if (a.medicalRecordLogId < b.medicalRecordLogId) {
        return 1;
      }
      else if (a.medicalRecordLogId > b.medicalRecordLogId) {
        return -1;
      }
      else {
        return 0;
      }
    });
    return array;
  }
}
