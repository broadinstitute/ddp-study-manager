export class Tissue {

  deleted: boolean = false;

  constructor(public tissueId: string, public oncHistoryDetailId: string, public tNotes: string, public countReceived: number,
              public tissueType: string, public tissueSite: string, public tumorType: string,
              public hE: string, public pathologyReport: string, public collaboratorSampleId: string, public blockSent: string,
              public scrollsReceived: string, public skId: string, public smId: string, public sentGp: string, public firstSmId: string,
              public additionalValues: {}, public expectedReturn: string, public tissueReturnDate: string,
              public returnFedexId: string, public shlWorkNumber: string, public sequenceResults: string, public tumorPercentage: string,
              public scrollsCount: number, public ussCount: number, public blocksCount: number, public hECount: number) {
    this.tissueId = tissueId;
    this.oncHistoryDetailId = oncHistoryDetailId;
    this.tNotes = tNotes;
    this.countReceived = countReceived;
    this.tissueType = tissueType;
    this.tissueSite = tissueSite;
    this.tumorType = tumorType;
    this.hE = hE;
    this.pathologyReport = pathologyReport;
    this.collaboratorSampleId = collaboratorSampleId;
    this.blockSent = blockSent;
    this.scrollsReceived = scrollsReceived;
    this.skId = skId;
    this.smId = smId;
    this.sentGp = sentGp;
    this.firstSmId = firstSmId;
    this.additionalValues = additionalValues;
    this.expectedReturn = expectedReturn;
    this.tissueReturnDate = tissueReturnDate;
    this.returnFedexId = returnFedexId;
    this.shlWorkNumber = shlWorkNumber;
    this.sequenceResults = sequenceResults;
    this.tumorPercentage = tumorPercentage;
    this.scrollsCount = scrollsCount;
    this.ussCount = ussCount;
    this.blocksCount = blocksCount;
    this.hECount = hECount;
  }

  static parse(json): Tissue {
    let additionalValues: {};
    let jsonData = json.additionalValues;
    if (jsonData != null) {
      jsonData = "{" + jsonData.substring(1, jsonData.length - 1) + "}";
      additionalValues = JSON.parse(jsonData);
    }
    return new Tissue(json.tissueId, json.oncHistoryDetailId, json.tNotes, json.countReceived, json.tissueType,
      json.tissueSite, json.tumorType, json.hE, json.pathologyReport, json.collaboratorSampleId, json.blockSent,
      json.scrollsReceived, json.skId, json.smId, json.sentGp, json.firstSmId, additionalValues, json.expectedReturn,
      json.tissueReturnDate, json.returnFedexId, json.shlWorkNumber, json.sequenceResults, json.tumorPercentage,
      json.scrollsCount, json.ussCount, json.blocksCount, json.hECount);
  }
}
