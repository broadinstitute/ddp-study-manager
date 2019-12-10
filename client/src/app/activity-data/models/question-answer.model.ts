import {DateField} from "./date-field.model";
import {OptionDetail} from "./option-detail.model";

export class QuestionAnswer {

  constructor( public stableId: string, public questionType: string, public text: string, public date: string, public answer: any,
               public dateFields: DateField, public optionDetails: Array<OptionDetail> ) {
    this.stableId = stableId;
    this.questionType = questionType;
    this.text = text;
    this.date = date;
    this.answer = answer;
    this.dateFields = dateFields;
    this.optionDetails = optionDetails;
  }

  static parse( json ): QuestionAnswer {
    return new QuestionAnswer( json.stableId, json.questionType, json.text, json.date, json.answer, json.dateFields, json.optionDetails );
  }
}

//questionType:
// PICKLIST will have selected and optionDetails
// DATE will have date (string) and dateFields (details about year/month/day)
// TEXT/BOOLEAN/AGREEMENT will have answer as string @ answer
