import { Pipe, PipeTransform } from "@angular/core";

import { Template } from "../email-event/email-event.model";

@Pipe({
  name: "emailTemplateSort"
})
export class EmailTemplateSortPipe implements PipeTransform {

  transform(array: Template[]): Template[] {

    // console.log(array);
    array.sort((a, b) => {
      // console.log("template array sort");
      if (a.workflowId != null && b.workflowId != null) {
        return a.workflowId > b.workflowId ? 1 : -1;
      }
    });
    // console.log(array);
    return array;
  }
}
