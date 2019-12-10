export class Sample {

  static DEACTIVATED: string = "deactivated";
  static SENT: string = "sent";
  static RECEIVED: string = "received";
  static IN_QUEUE: string = "queue";
  static IN_ERROR: string = "error";

  constructor( public collaboratorSampleId: string, public kitType: string, public scanDate: number, public error: boolean, public receiveDate: number, public deactivatedDate: number,
               public trackingNumberTo: string, public scannedTrackingNumber: string, public kitLabel: string) {
    this.collaboratorSampleId = collaboratorSampleId;
    this.kitType = kitType;
    this.scanDate = scanDate;
    this.error = error;
    this.receiveDate = receiveDate;
    this.deactivatedDate = deactivatedDate;
    this.trackingNumberTo = trackingNumberTo;
    this.scannedTrackingNumber = scannedTrackingNumber;
    this.kitLabel = kitLabel;
  }

  get sampleQueue() {
    if (this.deactivatedDate !== 0) {
      return Sample.DEACTIVATED;
    }
    if (this.receiveDate !== 0) {
      return Sample.RECEIVED;
    }
    if (this.scanDate !== 0) {
      return Sample.SENT;
    }
    if (this.error) {
      return Sample.IN_ERROR;
    }
    return Sample.IN_QUEUE;
  }

  static parse( json ): Sample {
    return new Sample( json.collaboratorSampleId, json.kitType, json.scanDate, json.error, json.receiveDate, json.deactivatedDate, json.trackingNumberTo,
      json.scannedTrackingNumber, json.kitLabel );
  }
}
