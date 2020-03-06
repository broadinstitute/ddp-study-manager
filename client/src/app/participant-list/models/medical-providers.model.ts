export class MedicalProvider {

  constructor( public city: string, public institutionName: string, public type: string,
               public physicianName: string, public state: string, public street: string, public guid: string, public legacyGuid: number ) {
    this.city = city;
    this.institutionName = institutionName;
    this.type = type;
    this.physicianName = physicianName;
    this.state = state;
    this.street = street;
    this.guid = guid;
    this.legacyGuid = legacyGuid;
  }

  static parse( json ): MedicalProvider {
    return new MedicalProvider( json.city, json.institutionName, json.type, json.physicianName, json.state, json.street,
      json.guid, json.legacyGuid );
  }
}
