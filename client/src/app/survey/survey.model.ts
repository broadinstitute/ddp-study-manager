export class Survey {

  static NONREPEATING: string = "NONREPEATING";

  constructor(public name: string, public type: string) {
    this.name = name;
    this.type = type;
  }

  static parse(json): Survey {
    return new Survey(json.name, json.type);
  }
}

export class SurveyStatus {

  constructor(public surveyInfo: ParticipantSurveyInfo, public reason: string, public user: string, public triggeredDate: number) {
    this.surveyInfo = surveyInfo;
    this.reason = reason;
    this.user = user;
    this.triggeredDate = triggeredDate;
  }

  static parse(json): SurveyStatus {
    return new SurveyStatus(json.surveyInfo, json.reason, json.user, json.triggeredDate);
  }
}

export class ParticipantSurveyInfo {

  constructor(public participantId: string, public shortId: string, public legacyShortId: string, public survey: string, public followUpInstance: number,
              public surveyQueued: number, public surveyStatus: string, public triggerId: number) {
    this.participantId = participantId;
    this.shortId = shortId;
    this.legacyShortId = legacyShortId;
    this.survey = survey;
    this.followUpInstance = followUpInstance;
    this.surveyQueued = surveyQueued;
    this.surveyStatus = surveyStatus;
    this.triggerId = triggerId;
  }

  static parse(json): ParticipantSurveyInfo {
    return new ParticipantSurveyInfo(json.participantId, json.shortId, json.legacyShortId, json.survey, json.followUpInstance,
      json.surveyQueued, json.surveyStatus, json.triggerId);
  }
}

export class TriggerResponse {

  constructor(public failedToTrigger: string[], public alreadyTriggered: string[]) {
    this.failedToTrigger = failedToTrigger;
    this.alreadyTriggered = alreadyTriggered;
  }

  static parse(json): TriggerResponse {
    return new TriggerResponse(json.failedToTrigger, json.alreadyTriggered);
  }
}

export class ParticipantSurveyUploadObject {

  selected: boolean = false;

  constructor(public ddpParticipantId: string, public shortId: string, public firstName: string, public lastName: string, public email: string) {
    this.ddpParticipantId = ddpParticipantId;
    this.shortId = shortId;
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
  }

  static parse(json): ParticipantSurveyUploadObject {
    return new ParticipantSurveyUploadObject(json.ddpParticipantId, json.shortId, json.firstName, json.lastName, json.email);
  }
}
