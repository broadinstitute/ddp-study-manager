export class EasypostLabelRate {

  constructor(public express: string, public normal: string){
    this.express = express;
    this.normal = normal;
  }

  static parse(json): EasypostLabelRate {
    return new EasypostLabelRate(json.express, json.normal);
  }
}
