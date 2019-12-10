export class ParticipantEvent {

  constructor(public participantId: string, public shortId: string, public user: string, public date: number,
              public eventType: string) {
    this.participantId = participantId;
    this.shortId = shortId;
    this.user = user;
    this.date = date;
    this.eventType = eventType;
  }

  static parse(json): ParticipantEvent {
    return new ParticipantEvent(json.participantId, json.shortId, json.user, json.date, json.eventType);
  }

}

export class EventType {

  constructor(public name: string, public description: string) {
    this.name = name;
    this.description = description;
  }

  static parse(json): EventType {
    return new EventType(json.name, json.description);
  }
}
