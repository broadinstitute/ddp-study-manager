export class FollowUp {
  constructor(public fRequest1: string, public fRequest2: string, public fRequest3: string, public fReceived: string) {
    this.fReceived = fReceived;
    this.fRequest1 = fRequest1;
    this.fRequest2 = fRequest2;
    this.fRequest3 = fRequest3;
  }

  public isEmpty(): boolean {
    return (this.fReceived === null || this.fReceived === "") && (this.fRequest1 === null || this.fRequest1 === "") &&
      (this.fRequest2 === null || this.fRequest2 === "") && (this.fRequest3 === null || this.fRequest3 === "");
  }
}
