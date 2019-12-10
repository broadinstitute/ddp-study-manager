import { Address } from "../address/address.model";

export class KitRequest {

  public TRACKING_LINK: string = "https://www.fedex.com/apps/fedextrack/?action=track&trackingnumber=";

  public isSelected: boolean = false;
  public setSent: boolean = false;

  constructor(public participantId: string, public collaboratorParticipantId: string, public realm: string,
              public kitType: string, public dsmKitRequestId: string, public dsmKitId: string,
              public shippingId: string, public labelUrlTo: string, public labelUrlReturn: string,
              public trackingNumberTo: string, public trackingNumberReturn: string, public scannedTrackingNumber: string, public trackingUrlTo: string,
              public trackingUrlReturn: string, public scanDate: number, public error: boolean, public message: string,
              public receiveDate: number, public deactivatedDate: number, public deactivationReason: string, public participant: Address,
              public easypostAddressId: string, public nameLabel: string, public kitLabel: string, public express: boolean, public labelTriggeredDate: number, public noReturn: boolean,
              public externalOrderNumber: string, public externalOrderStatus: string) {
    this.participantId = participantId;
    this.collaboratorParticipantId = collaboratorParticipantId;
    this.realm = realm;
    this.kitType = kitType;
    this.dsmKitRequestId = dsmKitRequestId;
    this.dsmKitId = dsmKitId;
    this.shippingId = shippingId;
    this.labelUrlTo = labelUrlTo;
    this.labelUrlReturn = labelUrlReturn;
    this.trackingNumberTo = trackingNumberTo;
    this.trackingNumberReturn = trackingNumberReturn;
    this.scannedTrackingNumber = scannedTrackingNumber;
    this.trackingUrlTo = trackingUrlTo;
    this.trackingUrlReturn = trackingUrlReturn;
    this.scanDate = scanDate;
    this.error = error;
    this.message = message;
    this.receiveDate = receiveDate;
    this.deactivatedDate = deactivatedDate;
    this.deactivationReason = deactivationReason;
    this.participant = participant;
    this.easypostAddressId = easypostAddressId;
    this.nameLabel = nameLabel;
    this.kitLabel = kitLabel;
    this.express = express;
    this.labelTriggeredDate = labelTriggeredDate;
    this.noReturn = noReturn;
    this.externalOrderNumber = externalOrderNumber;
    this.externalOrderStatus = externalOrderStatus;
  }

  public getID(): any {
    if (this.collaboratorParticipantId != null && this.collaboratorParticipantId !== "") {
      let idSplit: string[] = this.collaboratorParticipantId.split("_");
      if (idSplit.length === 2) {
        return idSplit[1];
      }
      if (idSplit.length > 2) { //RGP
        return this.collaboratorParticipantId.slice(this.collaboratorParticipantId.indexOf("_")+1);
      }
    }
    if (this.participant != null && this.participant.shortId !== "") {
      return this.participant.shortId;
    }
    return this.participantId;
  }

  public getShippingIdOrError(): any {
    if (this.error) {
      if (this.participant != null) {
        if (this.participant.country != null && this.participant.country === "CA") {
          return "Canadian Participant";
        }
        if (this.participant.street1 != null && this.participant.street1.toLowerCase().startsWith("po box")) {
          return "Participant w/ PO Box";
        }
      }
      return this.message;
    }
    else {
      return this.shippingId;
    }
  }

  public getError(): string {
    if (this.error) {
      if (this.participant != null) {
        if (this.participant.country != null && this.participant.country === "CA") {
          return "Canadian Participant";
        }
        if (this.participant.street1 != null && this.participant.street1.toLowerCase().startsWith("po box")) {
          return "Participant w/ PO Box";
        }
      }
      return this.message;
    }
    return "";
  }

  static parse(json): KitRequest {
    return new KitRequest(json.participantId, json.collaboratorParticipantId, json.realm, json.kitType, json.dsmKitRequestId, json.dsmKitId,
      json.shippingId, json.labelUrlTo, json.labelUrlReturn,
      json.trackingNumberTo, json.trackingNumberReturn, json.scannedTrackingNumber, json.trackingUrlTo,
      json.trackingUrlReturn, json.scanDate, json.error, json.message,
      json.receiveDate, json.deactivatedDate, json.deactivationReason, json.participant, json.easypostAddressId, json.nameLabel,
      json.kitLabel, json.express, json.labelTriggeredDate, json.noReturn, json.externalOrderNumber, json.externalOrderStatus);
  }

  getScannedTrackingUrl() {
    return this.TRACKING_LINK + this.scannedTrackingNumber;
  }

  static removeUnselectedKitRequests(array: Array<KitRequest>): Array<KitRequest> {
    let cleanedKitRequests: Array<KitRequest> = [];
    for (let kit of array) {
      if (kit.isSelected) {
        cleanedKitRequests.push(kit);
      }
    }
    return cleanedKitRequests;
  }
}

export class TriggerLabel {
  constructor(public dsmKitRequestId: string, public dsmKitId: string) {
    this.dsmKitRequestId = dsmKitRequestId;
    this.dsmKitId = dsmKitId;
  }
}
