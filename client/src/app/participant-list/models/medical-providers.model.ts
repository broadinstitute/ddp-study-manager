export class MedicalProvider {

  constructor( public city: string, public institutionName: string, public institutionType: string,
               public physicianName: string, public state: string, public street: string, public guid: string, public legacyGuid: number ) {
    this.city = city;
    this.institutionName = institutionName;
    this.institutionType = institutionType;
    this.physicianName = physicianName;
    this.state = state;
    this.street = street;
    this.guid = guid;
    this.legacyGuid = legacyGuid;
  }

  static parse( json ): MedicalProvider {
    return new MedicalProvider( json.city, json.institutionName, json.institutionType, json.physicianName, json.state, json.street,
      json.guid, json.legacyGuid );
  }
}
