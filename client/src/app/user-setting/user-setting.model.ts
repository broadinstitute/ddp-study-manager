import {Utils} from "../utils/utils";

export class UserSetting {

  private defaultRowsOnPage: number = 10;
  private defaultRowsSet0: number = 10;
  private defaultRowsSet1: number = 25;
  private defaultRowsSet2: number = 50;
  private defaultDateFormat: string = Utils.DATE_STRING_IN_CVS;

  constructor(public rowsOnPage: number, public rowSet0: number, public rowSet1: number, public rowSet2: number,
              public favViews: any, public dateFormat: string, public defaultTissueFilter: string, public defaultParticipantFilter: string){
    this.setToDefaultValues();

    // if value is not 0 fill with actual value from user settings table
    if (rowsOnPage !== 0) {
      this.rowsOnPage = rowsOnPage;
    }
    if (rowSet0 !== 0) {
      this.rowSet0 = rowSet0;
    }
    if (rowSet1 !== 0) {
      this.rowSet1 = rowSet1;
    }
    if (rowSet2 !== 0) {
      this.rowSet2 = rowSet2;
    }
    if (this.dateFormat != null) {
      this.dateFormat = dateFormat;
    }
    this.defaultParticipantFilter = defaultParticipantFilter;
    this.defaultTissueFilter = defaultTissueFilter;
  }


  private setToDefaultValues() {
    this.rowsOnPage = this.defaultRowsOnPage;
    this.rowSet0 = this.defaultRowsSet0;
    this.rowSet1 = this.defaultRowsSet1;
    this.rowSet2 = this.defaultRowsSet2;
    this.dateFormat = this.defaultDateFormat;
  }

  public getRowsPerPage(): number {
    return this.rowsOnPage;
  }

  public getRowSet0(): number {
    return this.rowSet0;
  }

  public getRowSet1(): number {
    return this.rowSet1;
  }

  public getRowSet2(): number {
    return this.rowSet2;
  }


  public getDateFormat(): string {
    return this.dateFormat;
  }

  static parse(json): UserSetting {
    return new UserSetting(json.rowsOnPage, json.rowSet0, json.rowSet1, json.rowSet2,
      json.favViews, json._dateFormat, json.defaultTissueFilter, json.defaultParticipantFilter);
  }
}
