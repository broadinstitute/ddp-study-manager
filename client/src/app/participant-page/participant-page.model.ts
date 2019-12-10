export class DDPParticipantInformation {

  constructor(public dob: string, public dateOfDiagnosis: string, public drawBloodConsent: number, public tissueSampleConsent: number,
              public institutions: Array<DDPInstitutionInformation>) {
    this.dob = dob;
    this.dateOfDiagnosis = dateOfDiagnosis;
    this.drawBloodConsent = drawBloodConsent;
    this.tissueSampleConsent = tissueSampleConsent;
    this.institutions = institutions;
  }

  static parse(json): DDPParticipantInformation {
    return new DDPParticipantInformation(json.dob, json.dateOfDiagnosis, json.drawBloodConsent, json.tissueSampleConsent,
      json.institutions);
  }
}

export class DDPInstitutionInformation {

  constructor(public type: string, public id: string, public physician: string,
              public institution: string, public streetAddress: string, public city: string, public state: string) {
    this.type = type;
    this.id = id;
    this.physician = physician;
    this.institution = institution;
    this.streetAddress = streetAddress;
    this.city = city;
    this.state = state;
  }
}
