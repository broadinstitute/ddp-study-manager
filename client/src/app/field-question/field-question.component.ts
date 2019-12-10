import {Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges} from "@angular/core";
import {RoleService} from "../services/role.service";
import {Question} from "./field-question.model";

@Component({
  selector: 'app-field-question',
  templateUrl: './field-question.component.html',
  styleUrls: ['./field-question.component.css']
})
export class FieldQuestionComponent implements OnInit , OnChanges {

  @Input() valueJson: string;
  @Input() disabled: boolean = false;
  @Input() isQuestionArray: boolean = false;
  @Input() question: Question;
  @Input() resolved: boolean = false;
  @Output() qChanged = new EventEmitter();

  _question: Question;

  constructor(private role: RoleService) { }

  ngOnInit() {
    this.setQuestion();
  }

  ngOnChanges( changes: SimpleChanges ) {
    this.setQuestion();
  }

  setQuestion() {
    if (this._question != null) {
      this._question = this._question;
    }
    else {
      if (this.valueJson != null && this.valueJson !== "") {
        this._question = Question.parse( this.valueJson );
      }
      else {
        this._question = new Question( null, null, null, null,null, null, null, false );
      }
    }
  }

  questionChanged( value: any, fieldName: string ) {
    if (fieldName === "question") {
      this.question.question = value.srcElement.value;
    }
    else if (fieldName === "answer") {
      this.question.answer = value.srcElement.value;
      this.question.aDate = Date.now().toString();
    }
    this.qChanged.emit( JSON.stringify( this.question ) );
  }

  deleteQuestion() {
    this.question.del = true;
    this.qChanged.emit( JSON.stringify( this.question ) );
  }

  getUser() {
    return this.role.getUserName();
  }

  hasRole(): RoleService {
    return this.role;
  }

}
