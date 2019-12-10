export class EstimatedDate {

  constructor(public dateString: string, public est: boolean){
    this.dateString = dateString;
    this.est = est;
  }

  static parse(json): EstimatedDate {
    let _json = JSON.parse(json);
    return new EstimatedDate(_json.dateString, _json.est);
  }
}
