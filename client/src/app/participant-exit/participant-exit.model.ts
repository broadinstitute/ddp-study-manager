export class ParticipantExit {

  constructor(public realm: string, public participantId: string, public user: string, public exitDate: number,
              public shortId: string, public legacyShortId: string, public inDDP: boolean) {
    this.realm = realm;
    this.participantId = participantId;
    this.user = user;
    this.exitDate = exitDate;
    this.shortId = shortId;
    this.legacyShortId = legacyShortId;
    this.inDDP = inDDP;
  }

  static parse(json): ParticipantExit {
    return new ParticipantExit(json.realm, json.participantId, json.user, json.exitDate, json.shortId, json.legacyShortId, json.inDDP);
  }
}
