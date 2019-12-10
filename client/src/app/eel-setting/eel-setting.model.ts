export class EmailSettings {

  constructor(public templateId: string, public name: string, public workflowId: string, public responseDays: number){
    this.templateId = templateId;
    this.name = name;
    this.workflowId = workflowId;
    this.responseDays = responseDays;
  }

  static parse(json): EmailSettings {
    return new EmailSettings(json.templateId, json.name, json.workflowId, json.responseDays);
  }

}
