export class EmailEvent {

  public expanded: boolean = false;

  constructor(public templateId: string, public name: string, public emails: Array<Email>, public email: string,
              public templates: Array<Template>, public workflowId: number, public needsAttention: boolean){
    this.templateId = templateId;
    this.name = name;
    this.emails = emails;
    this.email = email;
    this.templates = templates;
    this.workflowId = workflowId;
    this.needsAttention = needsAttention;
  }

  static parse(json): EmailEvent {
    return new EmailEvent(json.templateId, json.name, json.emails, json.email, json.templates, json.workflowId, json.needsAttention);
  }
}

export class Template {

  templateExpanded: boolean = false;

  followUpChanged: boolean = false;

  constructor(public templateId: string, public name: string, public workflowId: number, public followUpDateString: string,
              public sgeId: string, public events: Array<Event>) {
    this.templateId = templateId;
    this.name = name;
    this.workflowId = workflowId;
    this.followUpDateString = followUpDateString;
    this.sgeId = sgeId;
    this.events = events;
  }

  static parse(json): Template {
    return new Template(json.templateId, json.name, json.workflowId, json.followUpDateString, json.sgeId, json.events);
  }
}

export class Email {

  emailExpanded: boolean = false;

  constructor(public email: string, public events: Array<Event>) {
    this.email = email;
    this.events = events;
  }

  static parse(json): Email {
    return new Email(json.email, json.events);
  }
}

export class Event {

  constructor(public event: string, public timestamp: number, public url: string) {
    this.event = event;
    this.timestamp = timestamp;
    this.url = url;
  }

  static parse(json): Event {
    return new Event(json.event, json.timestamp, json.url);
  }
}
