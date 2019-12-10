import {Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges} from "@angular/core";
import {Question} from "../field-question/field-question.model";
import {RoleService} from "../services/role.service";
import {Questions} from "./field-question-array.model";

@Component( {
  selector: "app-field-question-array",
  templateUrl: "./field-question-array.component.html",
  styleUrls: [ "./field-question-array.component.css" ]
} )
export class FieldQuestionArrayComponent implements OnInit, OnChanges {

  @Input() questionJsonArray: any;
  @Input() disabled: boolean = false;
  @Output() quesChanged = new EventEmitter();

  questions: Question[];
  resolved: boolean = false;

  constructor( private role: RoleService ) {
  }

  ngOnInit() {
    this.setQuestions();
  }

  ngOnChanges( changes: SimpleChanges ) {
    this.setQuestions();
  }

  setQuestions() {
    if (this.questionJsonArray != null && this.questionJsonArray !== "") {
      let questionsModal: Questions = JSON.parse( this.questionJsonArray );
      this.resolved = questionsModal.done;
      this.questions = questionsModal.questions;
    }
    else {
      if (this.questions == null) {
        this.questions = [];
        this.questions.push( new Question( null, null, null, null, null, null, null, false ) );
      }
    }
  }

  questionChanged( change: any, index: number ) {
    if (change.checked != null) {
      this.resolved = change.checked;
    }
    else {
      let tmp: Question = Question.parse( change );
      this.questions[ index ] = tmp;
    }
    let questionsModal: Questions = new Questions( this.resolved, this.questions );
    this.quesChanged.emit( JSON.stringify( questionsModal ) );
  }

  addQuestion() {
    if (this.questions == null) {
      this.questions = [];
    }
    this.questions.push( new Question( null, null, null, null,null, null, null, false ) );
    this.resolved = false;
  }

  hasRole(): RoleService {
    return this.role;
  }
}
