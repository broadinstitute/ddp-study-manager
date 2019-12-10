export class Question {

  constructor( public qDate: string, public qUser: string, public email: string, public question: string, public status: string, public aDate: string, public answer: string, public del: boolean ) {
    this.qDate = qDate;
    this.qUser = qUser;
    this.email = email;
    this.question = question;
    this.status = status;
    this.aDate = aDate;
    this.answer = answer;
    this.del = del;
  }

  static parse( json ): Question {
    let _json = JSON.parse( json );
    return new Question( _json.qDate, _json.qUser, _json.email, _json.question, _json.status, _json.aDate, _json.answer, _json.del );
  }
}
