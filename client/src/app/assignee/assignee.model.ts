export class Assignee {

  constructor(public assigneeId: string, public name: string, public email: string){
    this.assigneeId = assigneeId;
    this.name = name;
    this.email = email;
  }

  static parse(json): Assignee {
    return new Assignee(json.assigneeId, json.name, json.email);
  }

}
