export class AssigneeParticipant {
  participantId: string;
  assigneeId: string;
  email: string;
  shortId: string;

  constructor( participantId: string, assigneeId: string, email: string, shortId: string ) {
    this.participantId = participantId;
    this.assigneeId = assigneeId;
    this.email = email;
    this.shortId = shortId;
  }
}
