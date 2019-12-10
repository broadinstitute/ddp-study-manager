export class MailingListContact {

  constructor(public firstName: string, public lastName: string, public email: string, public dateCreated: number) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    this.dateCreated = dateCreated;
  }

  public getDate(): number{
    return this.dateCreated * 1000;
  }

  static parse(json): MailingListContact {
    return new MailingListContact(json.firstName, json.lastName, json.email, json.dateCreated);
  }
}
