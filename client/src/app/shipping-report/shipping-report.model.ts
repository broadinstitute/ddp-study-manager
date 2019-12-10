export class ShippingReport {

  constructor(public ddpName: string, public summaryKitTypeList: SummaryKitType[]){
    this.ddpName = ddpName;
    this.summaryKitTypeList = summaryKitTypeList;
  }

  static parse(json): ShippingReport {
    return new ShippingReport(json.ddpName, json.summaryKitTypeList);
  }
}

export class SummaryKitType {
  constructor(public kitType: string, public newK: number, public sent: number, public received: number,
              public newPeriod: number, public sentPeriod: number, public receivedPeriod: number, public month: string) {
    this.kitType = kitType;
    this.newK = newK;
    this.sent = sent;
    this.received = received;
    this.newPeriod = newPeriod;
    this.sentPeriod = sentPeriod;
    this.receivedPeriod = receivedPeriod;
    this.month = month;
  }
}
