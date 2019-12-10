export class EmailEventRawData {

  constructor(public template: string, public name:string, public email: string, public event: string, public time: string, public url: string) {
    this.template = template;
    this.name = name;
    this.email = email;
    this.event = event;
    this.time = time;
    this.url = url;
  }

  static parse(json): EmailEventRawData {
    return new EmailEventRawData(json.template, json.name, json.email, json.event, json.time, json.url);
  }
}
