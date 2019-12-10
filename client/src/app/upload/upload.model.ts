export class UploadResponse {

  constructor(public invalidKitAddressList: string[], public duplicateKitList: string[]) {
    this.invalidKitAddressList = invalidKitAddressList;
    this.duplicateKitList = duplicateKitList;
  }

  static parse(json): UploadResponse {
    return new UploadResponse(json.invalidKitAddressList, json.duplicateKitList);
  }
}

export class UploadParticipant {

  selected: boolean = false;

  constructor(public externalOrderNumber: string, public participantId: string, public shortId: string, public firstName: string, public lastName: string,
              public name: string, public street1: string, public street2: string, public city: string, public postalCode: string,
              public state: string, public country: string) {
    this.externalOrderNumber = externalOrderNumber;
    this.participantId = participantId;
    this.shortId = shortId;
    this.firstName = firstName;
    this.lastName = lastName;
    this.name = name;
    this.street1 = street1;
    this.street2 = street2;
    this.city = city;
    this.postalCode = postalCode;
    this.state = state;
    this.country = country;
  }

  static parse(json): UploadParticipant {
    return new UploadParticipant(json.externalOrderNumber, json.participantId, json.shortId, json.firstName, json.lastName,
      json.name, json.street1, json.street2, json.city, json.postalCode,
      json.state, json.country);
  }

}
