export class Address {

  constructor( public firstName: string, public lastName: string, public street1: string, public street2: string,
               public city: string, public postalCode: string, public state: string, public country: string, public shortId: string,
               public mailToName: string, public zip: string, public guid: string, public valid: boolean ) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.street1 = street1;
    this.street2 = street2;
    this.city = city;
    this.postalCode = postalCode;
    this.state = state;
    this.country = country;
    this.shortId = shortId;
    this.mailToName = mailToName;
    this.zip = zip;
    this.guid = guid;
    this.valid = valid;
  }

  static parse( json ): Address {
    return new Address( json.firstName, json.lastName, json.street1, json.street2,
      json.city, json.postalCode, json.state, json.country, json.shortId, json.mailToName, json.zip, json.guid, json.valid );
  }

}
