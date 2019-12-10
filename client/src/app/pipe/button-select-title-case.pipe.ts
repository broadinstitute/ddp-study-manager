import { Pipe, PipeTransform } from "@angular/core";

@Pipe({
  name: 'buttonSelect'
})
export class ButtonSelectTitleCasePipe implements PipeTransform {
  /**
   * @param value The string to transform to title case.
   */
  transform(value: string): string {
    if (!value) return value;
    if (typeof value !== 'string') {
      return value;
    }

    if (value === "na") {
      return value.toUpperCase();
    }
    else if (value === "not done") {
      return "Not Done";
    }
    else if (value === "I" || value === "II" || value === "III" || value === "IV" || value === "I-II" || value === "II-III" ) {
      return value;
    }
    else {
      return value[0].toUpperCase() + value.substr(1).toLowerCase();
    }
  }
}
