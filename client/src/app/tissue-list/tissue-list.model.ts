import {OncHistoryDetail} from "../onc-history-detail/onc-history-detail.model";
import {Tissue} from "../tissue/tissue.model";

export class TissueList {

  constructor(public oncHistoryDetails: OncHistoryDetail, public tissue: Tissue, public ddpParticipantId: string) {
    this.oncHistoryDetails = oncHistoryDetails;
    this.ddpParticipantId = ddpParticipantId;
    this.tissue = tissue;
  }

  static parse(json): TissueList {
    let oncHistory: OncHistoryDetail = OncHistoryDetail.parse(json.oncHistoryDetails);
    let tissue: Tissue;
    if (json.tissue != null && json.tissue != undefined) {
      tissue = Tissue.parse(json.tissue);
    }
    return new TissueList(oncHistory, tissue, json.ddpParticipantId);
  }
}
