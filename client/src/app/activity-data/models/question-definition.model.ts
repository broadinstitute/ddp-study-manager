import {Option} from "./option.model";

export class QuestionDefinition {

  constructor( public stableId: string, public questionType: string, public questionText: string,
               public options: Array<Option>, public childQuestions: Array<QuestionDefinition>, public selectMode: string, public allowMultiple: boolean ) {
    this.stableId = stableId;
    this.questionType = questionType;
    this.questionText = questionText;
    this.options = options;
    this.childQuestions = childQuestions;
    this.selectMode = selectMode;
    this.allowMultiple = allowMultiple;
  }

  static parse( json ): QuestionDefinition {
    return new QuestionDefinition( json.stableId, json.questionType, json.questionText, json.options, json.childQuestions, json.selectMode, json.allowMultiple );
  }
}
