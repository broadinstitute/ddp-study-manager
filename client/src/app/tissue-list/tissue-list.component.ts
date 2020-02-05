import {Component, OnInit, ViewChild} from "@angular/core";
import {Participant} from "../participant-list/participant-list.model";
import {RoleService} from "../services/role.service";
import {DSMService} from "../services/dsm.service";
import {ComponentService} from "../services/component.service";
import {ActivatedRoute, Router} from "@angular/router";
import {Auth} from "../services/auth.service";
import {Statics} from "../utils/statics";
import {Filter} from "../filter-column/filter-column.model";
import {NameValue} from "../utils/name-value.model";
import {OncHistoryDetail} from "../onc-history-detail/onc-history-detail.model";
import {ViewFilter} from "../filter-column/models/view-filter.model";
import {Result} from "../utils/result.model";
import {PatchUtil} from "../utils/patch.model";
import {ModalComponent} from "../modal/modal.component";
import {Utils} from "../utils/utils";
import {Http} from "@angular/http";
import {TissueListWrapper} from "./tissue-list-wrapper.model";
import {FieldSettings} from "../field-settings/field-settings.model";

@Component({
  selector: "app-tissue-view-page",
  templateUrl: "./tissue-list.component.html",
  styleUrls: [ "./tissue-list.component.css" ],
})
export class TissueListComponent implements OnInit {

  @ViewChild(ModalComponent)
  public modal: ModalComponent;

  tissueListWrappers: TissueListWrapper[] = [];
  copyTissueListWrappers: TissueListWrapper[] = [];
  originalTissueListWrappers: TissueListWrapper[] = [];
  tissueListsMap = {};
  tissueListOncHistories: TissueListWrapper[] = [];
  selectedTab: string;
  showParticipantInformation: boolean = false;
  showTissue: boolean = false;
  loading: boolean = false;
  participant: Participant;
  oncHistoryDetail: OncHistoryDetail;
  errorMessage = "";
  additionalMessage = "";
  parent = "tissueList";
  loadedTimeStamp: string;
  selectedTissueStatus: string;

  showFilters = false;
  showCustomizeViewTable = false;
  showSavedFilters = false;
  showQueues = false;
  showModal = false;
  dup: boolean = false;
  edit: boolean = true;
  newFilterModal = false;
  openTissueModal = false;


  sortField: string = null;
  sortDir: string = null;
  sortParent: string = null;
  sortColumn: Filter = null;
  currentFilter: Array<Filter>;
  currentView: string = null;
  currentQuickFilterName = null;
  oncHistoryId: string;
  tissueId: string;

  settings = {};

  defaultFilterName: string;
  defaultFilter: ViewFilter;
  isDefaultFilter: boolean = false;
  hasESData: boolean = false;
  realm: string;
  allColumns = {};
  allAdditionalColumns = {};
  selectedColumns = {};
  dataSources = [ Statics.ES_ALIAS, Statics.ONCDETAIL_ALIAS, Statics.TISSUE_ALIAS ];
  dataSourceNames = {
    "data": "Participant",
    "oD": "Onc History",
    "t": "Tissue",
  };

  selectedFilterName = "";
  defaultOncHistoryColumns = [ Filter.ACCESSION_NUMBER, Filter.DATE_PX, Filter.TYPE_PX, Filter.LOCATION_PX, Filter.HISTOLOGY, Filter.GENDER, Filter.FACILITY, Filter.DESTRUCTION_POLICY ];
  defaultTissueColumns = [ Filter.COLLABORATOR_SAMPLE_ID ];
  defaultESColumns = [ Filter.SHORT_ID, Filter.FIRST_NAME, Filter.LAST_NAME ];
  destructionPolicyColumns = {
    "data": [ Filter.SHORT_ID, Filter.FIRST_NAME, Filter.LAST_NAME, Filter.FACILITY_PHONE, Filter.FACILITY_FAX ],
    "oD": this.defaultOncHistoryColumns.concat([ Filter.FACILITY_PHONE, Filter.FACILITY_FAX ]),
    "t": [],
  };
  savedFilters: ViewFilter[] = [];
  quickFilters: ViewFilter[] = [];
  drugs: string[] = [];
  newFilterName: string;
  filterQuery: string = " ";
  textQuery: string;
  wrongQuery: boolean = false;
  allFieldNames = new Set();
  showHelp: boolean;

  constructor(public role: RoleService, private dsmService: DSMService, private compService: ComponentService,
              private router: Router, private auth: Auth, private route: ActivatedRoute, http: Http) {
    if (!auth.authenticated()) {
      auth.logout();
    }

    this.route.queryParams.subscribe(params => {
      this.realm = params[DSMService.REALM] || null;
      if (this.realm != null) {
        if (localStorage.getItem(ComponentService.MENU_SELECTED_REALM) === "Angio" || this.realm === "Brain") {
          this.hasESData = true;
        }
        else {
          this.hasESData = false;
        }
        this.checkRight(true);
      }
    });
  }

  private resetEverything(resetData?: boolean) {
    if (resetData != undefined) {
      if (resetData) {
        this.tissueListWrappers = [];
      }
    }
    this.setSelectedFilterName("");
    for (let source of this.dataSources) {
      if (this.allColumns[source] != undefined) {
        for (let col of this.allColumns[source]) {
          col = col.clearFilter(col);
        }
      }
    }
    this.setAllColumns();
    this.setDefaultColumns(false);
    this.currentFilter = null;
    this.currentQuickFilterName = null;
    this.quickFilters = [];
    this.errorMessage = null;
    this.additionalMessage = null;
    this.sortDir = null;
    this.sortField = null;
    this.sortParent = null;
    this.sortColumn = null;
    this.currentQuickFilterName = null;
    this.showFilters = false;
    this.showCustomizeViewTable = false;
    this.showSavedFilters = false;
    this.showQueues = false;
    this.filterQuery = null;
    this.edit = true;
    this.wrongQuery = false;
    this.textQuery = "";
  }

  private setAllColumns() {
    for (let source of this.dataSources) {
      this.allColumns[source] = new Array<Filter>();
      this.selectedColumns[source] = new Array<Filter>();
    }
    //    this.getESColumns();
    this.getFieldSettings();
    for (let col of this.allColumns[Statics.TISSUE_ALIAS]) {
      this.allFieldNames.add(col.participantColumn.tableAlias + Statics.DELIMITER_ALIAS + col.participantColumn.name);
    }
    for (let col of this.allColumns[Statics.ONCDETAIL_ALIAS]) {
      this.allFieldNames.add(col.participantColumn.tableAlias + Statics.DELIMITER_ALIAS + col.participantColumn.name);
    }
    for (let col of this.allColumns["data"]) {
      let t = col.participantColumn.object !== null && col.participantColumn.object !== undefined ? col.participantColumn.object : col.participantColumn.tableAlias;
      this.allFieldNames.add(t + Statics.DELIMITER_ALIAS + col.participantColumn.name);
    }
  }

  private checkRight(defaultFilter: boolean) {
    let allowedToSeeInformation = false;
    this.resetEverything(true);
    let jsonData: any[];
    this.isDefaultFilter = defaultFilter;
    this.dsmService.getRealmsAllowed(Statics.MEDICALRECORD).subscribe(
      data => {
        jsonData = data;
        jsonData.forEach((val) => {
          if (localStorage.getItem(ComponentService.MENU_SELECTED_REALM) === val) {
            allowedToSeeInformation = true;
            this.defaultFilterName = this.role.getUserSetting().defaultTissueFilter;
            this.clearFilters();
            this.setAllColumns();
            this.getDefaultFilterName();
            this.getAllFilters(true);
            this.getTissueListData(defaultFilter);
          }
        });
        if (!allowedToSeeInformation) {
          this.errorMessage = "You are not allowed to see information of the selected realm at that category";
        }
      },
      err => {
        return null;
      },
    );
  }

  ngOnInit() {
    this.loading = false;
    if (localStorage.getItem(ComponentService.MENU_SELECTED_REALM) != null) {
      this.realm = localStorage.getItem(ComponentService.MENU_SELECTED_REALM);
      //      this.compService.realmMenu = this.realm;
    }
    else {
      this.errorMessage = "Please select a realm";
    }
    window.scrollTo(0, 0);
    if (localStorage.getItem(ComponentService.MENU_SELECTED_REALM) === null || localStorage.getItem(ComponentService.MENU_SELECTED_REALM) === undefined) {
      this.errorMessage = "Please select a realm";
      return;
    }
    else {
      //      this.setAllColumns();
      for (let source of this.dataSources) {
        this.selectedColumns[source] = [];
      }

      this.checkRight(true);
    }
  }


  public onclickDropDown(e) {
    e.stopPropagation();

  }

  private getFieldSettings() {
    this.dsmService.getFieldSettings(localStorage.getItem(ComponentService.MENU_SELECTED_REALM)).subscribe(
      data => {
        this.allAdditionalColumns = {};
        this.settings = {};
        for (let source of this.dataSources) {
          this.allColumns[source] = [];
        }
        Object.keys(this.dataSourceNames).forEach((key) => {
          if (data[key] != undefined) {
            for (let fieldSetting of data[key]) {
              if (this.settings[key] == undefined || this.settings[key] == null) {
                this.settings[key] = [];
              }
              this.settings[key].push(FieldSettings.parse(fieldSetting));
              if (this.allAdditionalColumns[key] == null || this.allAdditionalColumns[key] == undefined) {
                this.allAdditionalColumns[key] = [];
                this.allColumns[key] = [];
              }
              let f = Filter.parseFieldSettingsToColumns(fieldSetting, key);
              this.allAdditionalColumns[key].push(f);
              this.allColumns[key].push(f);
            }
          }
          for (let filter of Filter.ALL_COLUMNS) {
            if (filter.participantColumn.tableAlias === key) {
              this.allColumns[key].push(filter);
            }
          }
        });
        for (let col of this.allColumns[Statics.TISSUE_ALIAS]) {
          this.allFieldNames.add(col.participantColumn.tableAlias + Statics.DELIMITER_ALIAS + col.participantColumn.name);
        }
        for (let col of this.allColumns[Statics.ONCDETAIL_ALIAS]) {
          this.allFieldNames.add(col.participantColumn.tableAlias + Statics.DELIMITER_ALIAS + col.participantColumn.name);
        }
        for (let col of this.allColumns["data"]) {
          let t = col.participantColumn.object !== null && col.participantColumn.object !== undefined ? col.participantColumn.object : col.participantColumn.tableAlias;
          this.allFieldNames.add(t + Statics.DELIMITER_ALIAS + col.participantColumn.name);
        }
        for (let data of this.dataSources) {
          this.allColumns[data].sort((a, b) => {
            return a.participantColumn.display.localeCompare(b.participantColumn.display);
          });
        }
      },
      err => {
        this.errorMessage = "Could not getting the field settings for this realm. Please contact your DSM developer\n " + err;
      },
    );

  }


  //display additional value
  getOncHisAdditionalValue(index: number, colName: string): string {
    if (this.tissueListWrappers[index].tissueList.oncHistoryDetails.additionalValues != null && this.tissueListWrappers[index].tissueList.oncHistoryDetails.additionalValues != undefined) {
      if (this.tissueListWrappers[index].tissueList.oncHistoryDetails.additionalValues[colName] != undefined) {
        return this.tissueListWrappers[index].tissueList.oncHistoryDetails.additionalValues[colName];
      }
    }
    return null;
  }

  getTissueAdditionalValue(tissueListIndex, colName: string): string {
    if (this.tissueListWrappers[tissueListIndex].tissueList.tissue != null && this.tissueListWrappers[tissueListIndex].tissueList.tissue.additionalValues != null && Object.keys(this.tissueListWrappers[tissueListIndex].tissueList.tissue.additionalValues).length > 0) {
      return this.tissueListWrappers[tissueListIndex].tissueList.tissue.additionalValues[colName] === undefined ? null : this.tissueListWrappers[tissueListIndex].tissueList.tissue.additionalValues[colName];
    }
    return null;
  }

  public isASettingsFilter(tableAlias, columnName) {

    if (this.allAdditionalColumns[tableAlias] == null || this.allAdditionalColumns[tableAlias] == undefined) {
      return false;
    }
    for (let c of this.allAdditionalColumns[tableAlias]) {
      if (c.participantColumn.name === columnName) {
        return true;
      }
    }
    return false;
  }


  private setDefaultColumns(isDefaultFilter: boolean) {
    if (!isDefaultFilter || this.defaultFilter == null) {
      this.selectedColumns[Statics.TISSUE_ALIAS] = new Array().concat(this.defaultTissueColumns);
      this.selectedColumns[Statics.ONCDETAIL_ALIAS] = new Array().concat(this.defaultOncHistoryColumns);
      this.selectedColumns[Statics.ES_ALIAS] = new Array().concat(this.defaultESColumns);
    }
    else {
      if (this.defaultFilter == undefined) {
        this.getAllFilters(isDefaultFilter);
      }
      this.selectedColumns = this.defaultFilter.columns;
    }
  }

  private getTissueListData(defaultFilter: boolean) {
    this.loading = true;
    if (this.defaultFilter == undefined && this.defaultFilterName !== undefined && this.defaultFilterName !== "") {
      this.defaultFilter = this.savedFilters.find(filter => filter.filterName === this.defaultFilterName);
      if (this.defaultFilter === undefined) {
        this.defaultFilter = this.quickFilters.find(filter => filter.filterName === this.defaultFilterName);
      }
    }
    if (defaultFilter && this.defaultFilter != undefined) {
      this.dsmService.applyFilter(this.defaultFilter, localStorage.getItem(ComponentService.MENU_SELECTED_REALM), this.parent, null).subscribe(
        data => {
          if (this.defaultFilter != null && this.defaultFilter != undefined && this.defaultFilter.filters != null) {
            this.adjustAllColumns(this.defaultFilter);
          }
          let date = new Date();
          this.loadedTimeStamp = Utils.getDateFormatted(date, Utils.DATE_STRING_IN_EVENT_CVS);
          let jsonData: any[];
          this.tissueListWrappers = [];
          this.originalTissueListWrappers = [];
          this.tissueListsMap = {};
          this.tissueListOncHistories = [];
          jsonData = data;
          jsonData.forEach((val) => {
            let tissueListWrapper = TissueListWrapper.parse(val);
            this.tissueListWrappers.push(tissueListWrapper);
          });
          console.log(this.tissueListWrappers);
          this.originalTissueListWrappers = this.tissueListWrappers;
          this.currentFilter = this.defaultFilter.filters;
          this.selectedFilterName = this.defaultFilter.filterName;
          this.selectedColumns = this.defaultFilter.columns;
          if (this.defaultFilter != null && this.defaultFilter.filters != null) {
            for (let filter of this.defaultFilter.filters) {
              if (filter.type === Filter.OPTION_TYPE) {
                filter.selectedOptions = filter.getSelectedOptionsBoolean();
              }
            }
          }
          for (let dataSource of this.dataSources) {
            if (this.selectedColumns[dataSource] == undefined) {
              this.selectedColumns[dataSource] = [];
            }
          }
          if (!this.hasESData) {
            this.filterProfileForNoESRelams(this.defaultFilter);
          }
          for (let tissueList of this.tissueListWrappers) {
            this.tissueListsMap[tissueList.tissueList.oncHistoryDetails.oncHistoryDetailId] = tissueList;
          }
          for (let key of Object.keys(this.tissueListsMap)) {
            this.tissueListOncHistories.push(this.tissueListsMap[key]);
          }
          this.edit = true;
          this.filterQuery = this.defaultFilter.queryItems;
          this.textQuery = this.defaultFilter.queryItems;
          this.loading = false;
          if (this.defaultFilter.quickFilterName !== undefined && this.defaultFilter.quickFilterName !== null && this.defaultFilter.quickFilterName !== "") {
            this.additionalMessage = "This filters is based on the quick filter " + this.defaultFilter.quickFilterName;
          }
        },
        err => {
          this.errorMessage = "Error getting tissue list. Please contact your DSM developer\n " + err;
        },
      );
    }
    else {
      this.dsmService.filterData(localStorage.getItem(ComponentService.MENU_SELECTED_REALM), null, this.parent, defaultFilter).subscribe(
        data => {
          let date = new Date();
          this.loadedTimeStamp = Utils.getDateFormatted(date, Utils.DATE_STRING_IN_EVENT_CVS);
          let jsonData: any[];
          this.tissueListWrappers = [];
          this.tissueListsMap = {};
          this.tissueListOncHistories = [];
          jsonData = data;
          let i = 0;
          jsonData.forEach((val) => {
            i += 1;
            let tissueListWrapper = TissueListWrapper.parse(val);
            this.tissueListWrappers.push(tissueListWrapper);
            //          this.tissueListsMap[tissueListWrapper.tissueList.oncHistoryDetails.oncHistoryDetailId] = tissueListWrapper;
          });
          this.originalTissueListWrappers = this.tissueListWrappers;
          for (let tissueList of this.tissueListWrappers) {
            this.tissueListsMap[tissueList.tissueList.oncHistoryDetails.oncHistoryDetailId] = tissueList;
          }
          for (let key of Object.keys(this.tissueListsMap)) {
            this.tissueListOncHistories.push(this.tissueListsMap[key]);
          }


          if (!this.isDefaultFilter || this.defaultFilterName === undefined || this.defaultFilterName === null || this.defaultFilterName === "") {
            this.setDefaultColumns(false);
          }
          else {
            if (this.defaultFilter == undefined) {
              this.getAllFilters(this.currentQuickFilterName === null && this.currentFilter === null);
            }
            else {
              this.selectedColumns = this.defaultFilter.columns;
              if (!this.hasESData) {
                this.filterProfileForNoESRelams(this.defaultFilter);
              }
              for (let tissueList of this.tissueListWrappers) {
                this.tissueListsMap[tissueList.tissueList.oncHistoryDetails.oncHistoryDetailId] = tissueList;
              }
              for (let key of Object.keys(this.tissueListsMap)) {
                this.tissueListOncHistories.push(this.tissueListsMap[key]);
              }

            }
          }
          this.loading = false;

        },
        err => {
          if (err._body === Auth.AUTHENTICATION_ERROR) {
            this.auth.logout();
          }
          this.errorMessage = "Error - Loading Tissues for tissue view, Please contact your DSM developer\n " + err;
        },
      );
    }
  }

  showCustomizeView() {
    this.showFilters = false;
    this.showSavedFilters = false;
    this.showQueues = false;
    this.showCustomizeViewTable = !this.showCustomizeViewTable;
  }


  showFiltersTable() {
    this.showCustomizeViewTable = false;
    this.showSavedFilters = false;
    this.showFilters = !this.showFilters;
    this.showQueues = false;
  }

  showSavedFiltersPanel() {
    this.showCustomizeViewTable = false;
    this.showSavedFilters = !this.showSavedFilters;
    this.showFilters = false;
    this.showQueues = false;
    this.getAllFilters(false);
  }


  addOrRemoveColumn(column: Filter, parent: string) {
    if (this.selectedColumns[parent].includes(column)) {
      let index = this.selectedColumns[parent].indexOf(column);
      this.selectedColumns[parent].splice(index, 1);
    }
    else {
      this.selectedColumns[parent].push(column);
    }
  }

  public reload(bool) {
    this.resetEverything(true);
    this.checkRight(bool);
  }

  public clearFilters() {
    for (let source of this.dataSources) {
      for (let filter of this.selectedColumns[source]) {
        filter = filter.clearFilter();
      }
      for (let filter of this.allColumns[source]) {
        filter = filter.clearFilter();
      }
    }
    this.currentQuickFilterName = "";
  }

  hasRole(): RoleService {
    return this.role;
  }

  public doFilter() {
    this.isDefaultFilter = false;
    this.additionalMessage = "";
    let json = [];
    this.filterQuery = "";
    this.textQuery = null;
    this.loading = true;
    json.concat(this.currentFilter);
    for (let array of this.dataSources) {
      if (this.selectedColumns[array] == undefined) {
        continue;
      }
      for (let filter of this.selectedColumns[array]) {
        let filterText = Filter.getFilterText(filter, array, this.allAdditionalColumns);
        if (filterText != null && array === Statics.ES_ALIAS) {
          filterText["exactMatch"] = true;
          filterText["parentName"] = filter.participantColumn.object;
        }
        if (filterText != null) {
          json.push(filterText);
        }


      }
    }

    let data = {
      "filters": json,
      "parent": this.parent,
      "quickFilterName": this.currentQuickFilterName,
    };
    let jsonPatch = JSON.stringify(data);
    this.currentFilter = json;
    this.currentView = jsonPatch;
    this.dsmService.filterData(localStorage.getItem(ComponentService.MENU_SELECTED_REALM), jsonPatch, this.parent, null).subscribe(
      data => {
        this.loading = false;
        window.scrollTo(0, 0);
        let date = new Date();
        this.loadedTimeStamp = Utils.getDateFormatted(date, Utils.DATE_STRING_IN_EVENT_CVS);
        let jsonData: any[];
        this.tissueListWrappers = [];
        this.tissueListsMap = {};
        this.tissueListOncHistories = [];
        jsonData = data;
        jsonData.forEach((val) => {
          let tissueList = TissueListWrapper.parse(val);
          this.tissueListWrappers.push(tissueList);
        });
        console.log(this.tissueListWrappers);
        this.originalTissueListWrappers = this.tissueListWrappers;
        if (!this.hasESData) {
          //check if it was a tableAlias data filter -> filter client side
          this.filterProfileForNoESRelams(null);
        }
        for (let tissueList of this.tissueListWrappers) {
          this.tissueListsMap[tissueList.tissueList.oncHistoryDetails.oncHistoryDetailId] = tissueList;
        }
        for (let key of Object.keys(this.tissueListsMap)) {
          this.tissueListOncHistories.push(this.tissueListsMap[key]);
        }
      },
      err => {
        this.loading = null;
        this.errorMessage = "Error - Loading Tissue List, Please contact your DSM developer\n " + err;
      },
    );

    if (!this.hasESData) {
      //check if it was a tableAlias data filter -> filter client side
      this.filterProfileForNoESRelams(null);
    }

  }


  openTissue(oncHis: OncHistoryDetail, participant: Participant, tissueId) {
    if (participant == null) {
      this.loading = false;
      return;
    }
    this.oncHistoryDetail = oncHis;
    this.compService.editable = true;
    this.participant = participant;
    this.showTissue = true;
    this.tissueId = tissueId;
    let tissuesArray = [];
    for (let o of this.tissueListWrappers) {
      if (o.tissueList.oncHistoryDetails.oncHistoryDetailId === oncHis.oncHistoryDetailId) {
        tissuesArray.push(o.tissueList.tissue);
      }
    }
    this.oncHistoryDetail.tissues = tissuesArray;
    this.loading = false;

  }

  showSavedModal() {
    this.showModal = true;
    this.newFilterModal = true;
    this.openTissueModal = false;
    this.modal.show();
  }

  saveCurrentFilter() {
    if (this.newFilterName == undefined) {
      return;
    }
    let cols = [];
    for (let source of this.dataSources) {
      for (let o of this.selectedColumns[source]) {
        cols.push(o.participantColumn.tableAlias + "." + o.participantColumn.name);
      }
    }

    let jsonData = {
      "columnsToSave": cols,
      "filterName": this.newFilterName,
      "shared": "0",
      "fDeleted": "0",
      "filters": this.currentFilter,
      "parent": this.parent,
      "quickFilterName": this.currentQuickFilterName,
      "queryItems": this.filterQuery,
    };
    let jsonPatch = JSON.stringify(jsonData);
    this.currentView = jsonPatch;
    this.dsmService.saveCurrentFilter(jsonPatch, localStorage.getItem(ComponentService.MENU_SELECTED_REALM), this.parent).subscribe(
      data => {
        let result = Result.parse(data);
        if (result.code === 500 && result.body != null) {
          this.newFilterModal = true;
          this.dup = true;
          return;
        }
        else if (result.code !== 500) {
          this.dup = false;
          this.newFilterName = "";
          this.modal.hide();
          this.newFilterModal = false;
          this.openTissueModal = false;
        }
      },
      err => {
        if (err.status == 500) {
          this.newFilterModal = true;
          this.dup = true;
          return;
        }
        this.errorMessage = "Error saving the filter. Please contact your DSM developer\n " + err;
      });
    this.showModal = false;
    this.newFilterModal = false;
    this.openTissueModal = false;
  }

  getAllFilters(applyDefault: boolean) {
    this.dsmService.getFiltersForUserForRealm(localStorage.getItem(ComponentService.MENU_SELECTED_REALM), this.parent).subscribe(data => {
        //        console.log("received: " + JSON.stringify(data, null, 2));
        this.savedFilters = [];
        this.quickFilters = [];
        let jsonData = data;
        jsonData.forEach((val) => {
          let view: ViewFilter;
          view = ViewFilter.parseFilter(val, this.allColumns);

          if (val.userId.includes("System")) {
            this.quickFilters.push(view);
          }
          else {
            this.savedFilters.push(view);
          }
        });
        this.savedFilters.sort((vf1, vf2) => vf1.filterName.localeCompare(vf2.filterName));
        if (this.isDefaultFilter && applyDefault) {
          this.selectedFilterName = this.defaultFilterName;
          this.defaultFilter = this.savedFilters.find(f => {
            return f.filterName === this.defaultFilterName;
          });
          if (this.defaultFilter == undefined) {
            this.defaultFilter = this.quickFilters.find(f => {
              return f.filterName === this.defaultFilterName;
            });
          }
          if (this.defaultFilter != undefined) {
            this.currentFilter = this.defaultFilter.filters;
            this.selectedColumns = this.defaultFilter.columns;
            for (let dataSource of this.dataSources) {
              if (this.selectedColumns[dataSource] == undefined) {
                this.selectedColumns[dataSource] = [];
              }
            }
            this.edit = true;
            this.filterQuery = this.defaultFilter.queryItems;
            this.textQuery = this.defaultFilter.queryItems;
            this.loading = false;
            if (this.defaultFilter.quickFilterName !== undefined && this.defaultFilter.quickFilterName !== null && this.defaultFilter.quickFilterName !== "") {
              this.additionalMessage = "This filters is based on the quick filter " + this.defaultFilter.quickFilterName;
            }
          }
          else if (this.defaultFilterName !== "" && this.defaultFilterName !== null && this.defaultFilterName !== undefined) {
            this.setDefaultColumns(false);
            if (this.isDefaultFilter) {
              this.additionalMessage = "The default filter seems to be deleted, however it is still the default filter as long as not changed in the user settings.";
            }
          }
        }

      },
      err => {
        this.errorMessage = "Error getting all the filters. Please contact your DSM developer\n " + err;
      });
  }

  public shareFilter(savedFilter: ViewFilter, i) {
    let value = savedFilter.shared ? "0" : "1";
    let patch1 = new PatchUtil(savedFilter.id, this.role.userMail(),
      {name: "shared", value: value}, null, this.parent, null);
    let patch = patch1.getPatch();
    this.dsmService.patchParticipantRecord(JSON.stringify(patch)).subscribe(data => {
      let result = Result.parse(data);
      if (result.code == 200) {
        this.savedFilters[i].shared = (value === "1");
      }
    }, err => {
    });

  }

  public deleteView(savedFilter) {
    let patch1 = new PatchUtil(savedFilter.id, this.role.userMail(),
      {name: "fDeleted", value: "1"}, null, this.parent, null);
    let patch = patch1.getPatch();
    this.dsmService.patchParticipantRecord(JSON.stringify(patch)).subscribe(data => {
      let result = Result.parse(data);
      if (result.code == 200) {
        this.getAllFilters(false);
      }
    }, err => {
    });
  }

  public selectFilter(savedFilter) {
    this.loading = true;
    this.sortParent = null;
    this.sortColumn = null;
    this.sortField = null;
    this.sortDir = null;
    this.currentView = JSON.stringify(savedFilter);
    this.currentQuickFilterName = null;
    this.additionalMessage = "";
    for (let dataSource of this.dataSources) {
      if (this.allColumns[dataSource] != undefined) {
        for (let col of this.allColumns[dataSource]) {
          col = col.clearFilter(col);
        }
      }
    }
    let filters: Filter[];
    this.dsmService.applyFilter(savedFilter, this.realm, this.parent, null).subscribe(
      data => {
        if (savedFilter != null && savedFilter.filters != null) {
          this.adjustAllColumns(savedFilter);
        }
        let date = new Date();
        this.loadedTimeStamp = Utils.getDateFormatted(date, Utils.DATE_STRING_IN_EVENT_CVS);
        let jsonData: any[];
        this.tissueListWrappers = [];
        this.tissueListsMap = {};
        this.tissueListOncHistories = [];
        jsonData = data;
        jsonData.forEach((val) => {
          let tissueListWrapper = TissueListWrapper.parse(val);
          this.tissueListWrappers.push(tissueListWrapper);
        });
        this.originalTissueListWrappers = this.tissueListWrappers;
        this.currentFilter = savedFilter.filters;
        this.selectedFilterName = savedFilter.filterName;
        this.selectedColumns = savedFilter.columns;
        if (savedFilter != null) {
          for (let filter of savedFilter.filters) {
            if (filter.type === Filter.OPTION_TYPE) {
              filter.selectedOptions = filter.getSelectedOptionsBoolean();
            }
          }
        }
        for (let dataSource of this.dataSources) {
          if (this.selectedColumns[dataSource] == undefined) {
            this.selectedColumns[dataSource] = [];
          }
        }
        if (!this.hasESData) {
          //check if it was a tableAlias data filter -> filter client side
          this.filterProfileForNoESRelams(savedFilter);
        }
        for (let tissueList of this.tissueListWrappers) {
          this.tissueListsMap[tissueList.tissueList.oncHistoryDetails.oncHistoryDetailId] = tissueList;
        }
        for (let key of Object.keys(this.tissueListsMap)) {
          this.tissueListOncHistories.push(this.tissueListsMap[key]);
        }
        this.edit = true;
        this.filterQuery = savedFilter.queryItems;
        this.textQuery = savedFilter.queryItems;
        this.loading = false;
        if (savedFilter.quickFilterName !== undefined && savedFilter.quickFilterName !== null && savedFilter.quickFilterName !== "") {
          this.additionalMessage = "This filters is based on the quick filter " + savedFilter.quickFilterName;
        }
      },
      err => {
        this.errorMessage = "Error applying the selected filter. Please contact your DSM developer\n " + err;
      },
    );
  }

  public doQuickFilter(quickFilter) {
    this.loading = true;
    this.currentQuickFilterName = quickFilter.name;
    this.currentView = JSON.stringify(quickFilter);
    this.dsmService.applyFilter(quickFilter, localStorage.getItem(ComponentService.MENU_SELECTED_REALM), this.parent, null).subscribe(
      data => {
        if (quickFilter != null && quickFilter.filters != null) {
          this.adjustAllColumns(quickFilter);
        }
        let date = new Date();
        this.loadedTimeStamp = Utils.getDateFormatted(date, Utils.DATE_STRING_IN_EVENT_CVS);
        this.currentQuickFilterName = quickFilter.filterName;
        let jsonData: any[];
        this.tissueListWrappers = [];
        this.tissueListsMap = {};
        this.tissueListOncHistories = [];
        jsonData = data;
        jsonData.forEach((val) => {
          let tissueListWrapper = TissueListWrapper.parse(val);
          this.tissueListWrappers.push(tissueListWrapper);
          this.tissueListsMap[tissueListWrapper.tissueList.oncHistoryDetails.oncHistoryDetailId] = tissueListWrapper;
        });
        this.originalTissueListWrappers = this.tissueListWrappers;
        for (let key of Object.keys(this.tissueListsMap)) {
          this.tissueListOncHistories.push(this.tissueListsMap[key]);
        }
        if (quickFilter != null && quickFilter.filters != null) {
          for (let filter of quickFilter.filters) {
            if (filter.type === Filter.OPTION_TYPE) {
              filter.selectedOptions = filter.getSelectedOptionsBoolean();
            }
          }
        }
        for (let s of this.dataSources) {
          if (quickFilter.columns[s] != undefined) {
            this.selectedColumns[s] = quickFilter.columns[s];
          }
          else {
            this.selectedColumns[s] = [];
          }
        }

        this.getFieldSettings();
        this.filterQuery = quickFilter.queryItems;
        this.textQuery = quickFilter.queryItems;
        this.edit = false;
        this.loading = false;
      },
      err => {
        this.errorMessage = "Error applying the quick filter. Please contact your DSM developer\n " + err;
      },
    );
  }

  public getDestroyingQueue(filterName: string) {
    this.loading = true;
    this.currentQuickFilterName = filterName;
    this.textQuery = "";
    this.filterQuery = "";
    this.wrongQuery = false;
    let destroyingViewFilter = new ViewFilter(null, filterName, this.destructionPolicyColumns, false, "0",
      null, null, this.parent, null, null, null);
    this.dsmService.applyFilter(destroyingViewFilter, localStorage.getItem(ComponentService.MENU_SELECTED_REALM), this.parent, null).subscribe(
      data => {
        if (destroyingViewFilter.filters != null) {
          this.adjustAllColumns(destroyingViewFilter);
        }
        let date = new Date();
        this.loadedTimeStamp = Utils.getDateFormatted(date, Utils.DATE_STRING_IN_EVENT_CVS);
        let jsonData: any[];
        this.tissueListWrappers = [];
        this.tissueListsMap = {};
        this.tissueListOncHistories = [];
        jsonData = data;
        jsonData.forEach((val) => {
          let tissueListWrapper = TissueListWrapper.parse(val);
          this.tissueListWrappers.push(tissueListWrapper);
          this.tissueListsMap[tissueListWrapper.tissueList.oncHistoryDetails.oncHistoryDetailId] = tissueListWrapper;
        });
        this.originalTissueListWrappers = this.tissueListWrappers;
        for (let key of Object.keys(this.tissueListsMap)) {
          this.tissueListOncHistories.push(this.tissueListsMap[key]);
        }
        this.loading = false;
        //        this.setDefaultColumns();
        this.selectedColumns = this.destructionPolicyColumns;
        for (let dataSource of this.dataSources) {
          if (this.selectedColumns[dataSource] == undefined) {
            this.selectedColumns[dataSource] = [];
          }
        }
        this.getFieldSettings();

      },
      err => {
        this.errorMessage = "Error applying the quick filter. Please contact your DSM developer\n " + err;
      },
    );

  }

  public setSelectedFilterName(filterName) {
    this.selectedFilterName === filterName;
  }

  onRequestChange(index: number) {
    this.valueChanged(this.tissueListWrappers[index].tissueList.oncHistoryDetails.request, "request", index);
  }

  valueChanged(value: any, parameterName: string, index: number) {
    let v;
    if (typeof value === "string") {
      this.tissueListWrappers[index].tissueList.oncHistoryDetails[parameterName] = value;
      v = value;
    }
    else if (value != null) {
      if (value.srcElement != null && typeof value.srcElement.value === "string") {
        v = value.srcElement.value;
      }
      else if (value.value != null) {
        v = value.value;
      }
      else if (value.checked != null) {
        v = value.checked;
      }
      else if (typeof value === "object") {
        v = JSON.stringify(value);
      }
    }
    if (v != null) {
      let patch1 = new PatchUtil(this.tissueListWrappers[index].tissueList.oncHistoryDetails.oncHistoryDetailId, this.role.userMail(),
        {
          name: parameterName,
          value: v,
        }, null, "participantId", this.tissueListWrappers[index].tissueList.oncHistoryDetails.participantId,
        Statics.ONCDETAIL_ALIAS);
      let patch = patch1.getPatch();
      this.patch(patch, index);
    }
  }

  patch(patch: any, index: number) {
    this.dsmService.patchParticipantRecord(JSON.stringify(patch)).subscribe(// need to subscribe, otherwise it will not send!
      data => {
        let result = Result.parse(data);
        if (result.code === 200 && result.body != null && result.body !== "") {
          let jsonData: any | any[] = JSON.parse(result.body);
          if (jsonData instanceof Array) {
            jsonData.forEach((val) => {
              let nameValue = NameValue.parse(val);
              this.tissueListWrappers[index].tissueList.oncHistoryDetails[nameValue.name] = nameValue.value;
            });
          }
          else {
            this.tissueListWrappers[index].tissueList.oncHistoryDetails.oncHistoryDetailId = jsonData.oncHistoryDetailId;
            //set oncHistoryDetailId to tissue as well
            for (let tissue of this.tissueListWrappers[index].tissueList.oncHistoryDetails.tissues) {
              tissue.oncHistoryDetailId = this.tissueListWrappers[index].tissueList.oncHistoryDetails.oncHistoryDetailId;
            }
            //set other workflow values
            if (jsonData.NameValue != null) {
              let innerJson: any | any[] = JSON.parse(jsonData.NameValue);
              if (innerJson instanceof Array) {
                innerJson.forEach((val) => {
                  let nameValue = NameValue.parse(val);
                  if (nameValue.name === "createdOncHistory") {
                    this.participant.participant[nameValue.name] = nameValue.value;
                  }
                  else {
                    this.tissueListWrappers[index].tissueList.oncHistoryDetails[nameValue.name] = nameValue.value;
                  }
                });
              }
            }
          }
        }
      },
      err => {
        if (err._body === Auth.AUTHENTICATION_ERROR) {
          this.router.navigate([ Statics.HOME_URL ]);
        }
      },
    );
  }

  openParticipant(participant: Participant, oncHistoryId) {
    if (participant != null) {
      this.loading = true;
      this.participant = participant;
      this.selectedTab = "Onc History";
      this.showParticipantInformation = true;
      this.oncHistoryId = oncHistoryId;
    }
    this.loading = false;
  }

  public getParticipant(tissueListWrapper: TissueListWrapper, name, tissueId?) {
    this.loading = true;
    if (name === "t" && (tissueListWrapper.tissueList.oncHistoryDetails == null || (tissueListWrapper.tissueList.oncHistoryDetails.request !== "sent"
      && tissueListWrapper.tissueList.oncHistoryDetails.request !== "received" && tissueListWrapper.tissueList.oncHistoryDetails.request !== "returned"))) {
      this.selectedTissueStatus = this.getRequestStatusDisplay(tissueListWrapper.tissueList.oncHistoryDetails.request);
      this.newFilterModal = false;
      this.openTissueModal = true;
      this.modal.show();
      this.loading = false;
    }
    else {
      this.dsmService.getParticipantData(localStorage.getItem(ComponentService.MENU_SELECTED_REALM), tissueListWrapper.tissueList.ddpParticipantId, this.parent).subscribe(
        data => {
          let participant: Participant = Participant.parse(data[0]);
          if (participant == null || participant == undefined) {
            this.errorMessage = "Participant  not found";
          }
          if (name === "t") {
            this.openTissue(tissueListWrapper.tissueList.oncHistoryDetails, participant, tissueId);
          }
          else if (name === "oD") {
            this.openParticipant(participant, tissueListWrapper.tissueList.oncHistoryDetails.oncHistoryDetailId);

          }
        },
        err => {
          this.errorMessage = "Could not get participant. Please contact your DSM developer\n " + err;
        },
      );
    }
  }

  stopIfRequest(e, tissueList, name) {
    if (name === "request") {
      e.stopPropagation();
    }
    else {
      this.getParticipant(tissueList, "oD");
    }

  }

  sortByColumnName(col: Filter, sortParent: string) {
    this.sortDir = this.sortField === col.participantColumn.name ? (this.sortDir === "asc" ? "desc" : "asc") : "asc";
    this.sortField = col.participantColumn.name;
    this.sortParent = sortParent;
    this.sortColumn = col;
    this.doSort();
    if (this.sortColumn.type === "ADDITIONALVALUE") {
      this.sortField = col.participantColumn.name;
    }
  }

  public isSortField(name: string) {
    return name === this.sortField;
  }

  private checkAdditionalValues(savedFilter: ViewFilter): string {
    let message = "";
    if (savedFilter.filters != null && savedFilter.filters != undefined) {
      for (let filter of savedFilter.filters) {
        if (filter.type === "ADDITIONALVALUE" && filter.participantColumn.display == undefined) {
          message = message + filter.filter2.name + ", ";
        }
      }
    }
    if (message.length > 0) {
      message = "The following columns do not apply in this realm: " + message;
    }
    return message;
  }

  downloadCurrentData() {
    let date = new Date();
    let fileName = "Tissue_" + Utils.getDateFormatted(date, Utils.DATE_STRING_CVS) + Statics.CSV_FILE_EXTENSION;
    if (this.selectedColumns["t"] != null && this.selectedColumns["t"].length > 0) {
      Utils.downloadCurrentData(this.tissueListWrappers, [ [ "data", Statics.ES_ALIAS ], [ "tissueList", "", "oncHistoryDetails", "oD" ], [ "tissueList", "", "tissue", "t" ] ], this.selectedColumns, fileName);
    }
    else {
      Utils.downloadCurrentData(this.tissueListOncHistories, [ [ "data", Statics.ES_ALIAS ], [ "tissueList", "", "oncHistoryDetails", "oD" ], [ "tissue", "t" ] ], this.selectedColumns, fileName);
    }
  }


  public doFilterByQuery(queryText: string) {
    this.dsmService.applyFilter(null, localStorage.getItem(ComponentService.MENU_SELECTED_REALM), this.parent, queryText).subscribe(data => {
      let date = new Date();
      this.additionalMessage = null;
      this.loadedTimeStamp = Utils.getDateFormatted(date, Utils.DATE_STRING_IN_EVENT_CVS);
      let jsonData: any[];
      this.tissueListWrappers = [];
      this.tissueListsMap = {};
      this.tissueListOncHistories = [];
      jsonData = data;
      let i = 0;
      jsonData.forEach((val) => {
        i += 1;
        let tissueListWrapper = TissueListWrapper.parse(val);
        this.tissueListWrappers.push(tissueListWrapper);
        this.tissueListsMap[tissueListWrapper.tissueList.oncHistoryDetails.oncHistoryDetailId] = tissueListWrapper;
      });
      this.originalTissueListWrappers = this.tissueListWrappers;
      for (let key of Object.keys(this.tissueListsMap)) {
        this.tissueListOncHistories.push(this.tissueListsMap[key]);
      }
      this.textQuery = queryText;
      this.filterQuery = this.textQuery;
    }, err => {
      this.errorMessage = "Error searching. Please contact your DSM developer\n " + err;
    });
  }


  getButtonColorStyle(isOpened: boolean): string {
    if (isOpened) {
      return Statics.COLOR_PRIMARY;
    }
    return Statics.COLOR_BASIC;
  }

  isCurrentFilter(filterName: string) {
    if (filterName === this.currentQuickFilterName) {
      return Statics.COLOR_PRIMARY;
    }
    return Statics.COLOR_BASIC;
  }

  private doSort() {
    let fieldName = "";
    if (this.sortColumn.type === "ADDITIONALVALUE") {
      fieldName = this.sortField;
    }
    let order = this.sortDir === "asc" ? 1 : -1;
    if (this.sortParent === Statics.ES_ALIAS) {
      if (this.selectedColumns["t"] != undefined && this.selectedColumns["t"].length > 0) {
        this.tissueListWrappers.sort((a, b) => {
            if (a.data[this.sortColumn.participantColumn.object] === null || a.data[this.sortColumn.participantColumn.object] === undefined || a.data[this.sortColumn.participantColumn.object][this.sortField] == undefined) {
              return 1;
            }
            if (b.data[this.sortColumn.participantColumn.object] === null || b.data[this.sortColumn.participantColumn.object] === undefined || b.data[this.sortColumn.participantColumn.object][this.sortField] == undefined) {
              return -1;
            }

          if (this.sortColumn.type !== "DATE" && this.sortColumn.type !== "NUMBER" && !(this.sortColumn.type === "ADDITIONALVALUE" && this.sortColumn.additionalType === "NUMBER")) {
              return (order * a.data[this.sortColumn.participantColumn.object][this.sortField].localeCompare(b.data[this.sortColumn.participantColumn.object][this.sortField]));
            }
            else {
              return (order * a.data[this.sortColumn.participantColumn.object][this.sortField] - b.data[this.sortColumn.participantColumn.object][this.sortField]);
            }
          },
        );
      }
      else {
        this.tissueListOncHistories.sort((a, b) => {
            if (a.data[this.sortColumn.participantColumn.object][this.sortField] == undefined) {
              return 1;
            }
            if (b.data[this.sortColumn.participantColumn.object][this.sortField] == undefined) {
              return -1;
            }

            if (typeof a.data[this.sortColumn.participantColumn.object][this.sortField] === "string") {
              return (order * a.data[this.sortColumn.participantColumn.object][this.sortField].localeCompare(b.data[this.sortColumn.participantColumn.object][this.sortField]));
            }
            else {
              return (order * a.data[this.sortColumn.participantColumn.object][this.sortField] - a.data[this.sortColumn.participantColumn.object][this.sortField]);
            }
          },
        );
      }
    }
    else if (this.sortParent === "oD") {
      if (this.selectedColumns["t"] != undefined && this.selectedColumns["t"].length > 0) {
        this.tissueListWrappers.sort((a, b) => {
          if (this.sortColumn.type === "ADDITIONALVALUE") {
            this.sortField = "additionalValues";
            if (a.tissueList.oncHistoryDetails[this.sortField] == null || a.tissueList.oncHistoryDetails[this.sortField] === undefined || a.tissueList.oncHistoryDetails[this.sortField][fieldName] == undefined || a.tissueList.oncHistoryDetails[this.sortField][fieldName] == "") {
              return 1;
            }
            if (b.tissueList.oncHistoryDetails[this.sortField] == null || b.tissueList.oncHistoryDetails[this.sortField] === undefined || b.tissueList.oncHistoryDetails[this.sortField][fieldName] == undefined || b.tissueList.oncHistoryDetails[this.sortField][fieldName] == "") {
              return -1;
            }

            if (typeof a.tissueList.oncHistoryDetails[this.sortField][fieldName] === "string") {
              return (order * a.tissueList.oncHistoryDetails[this.sortField][fieldName].localeCompare(b.tissueList.oncHistoryDetails[this.sortField][fieldName]));
            }
            else {
              return (order * a.tissueList.oncHistoryDetails[this.sortField][fieldName] - b.tissueList.oncHistoryDetails[this.sortField][fieldName]);
            }

          }
          else {

            if (a.tissueList.oncHistoryDetails[this.sortField] == undefined) {
              return 1;
            }
            if (b.tissueList.oncHistoryDetails[this.sortField] == undefined) {
              return -1;
            }

            if (typeof a.tissueList.oncHistoryDetails[this.sortField] === "string") {
              return (order * a.tissueList.oncHistoryDetails[this.sortField].localeCompare(b.tissueList.oncHistoryDetails[this.sortField]));
            }
            else {
              return (order * a.tissueList.oncHistoryDetails[this.sortField] - b.tissueList.oncHistoryDetails[this.sortField]);
            }
          }
          },
        );
      }
      else {
        this.tissueListOncHistories.sort((a, b) => {
          if (this.sortColumn.type === "ADDITIONALVALUE") {
            this.sortField = "additionalValues";
            if (a.tissueList.oncHistoryDetails[this.sortField] == null || a.tissueList.oncHistoryDetails[this.sortField] === undefined || a.tissueList.oncHistoryDetails[this.sortField][fieldName] == undefined || a.tissueList.oncHistoryDetails[this.sortField][fieldName] == "") {
              return 1;
            }
            if (b.tissueList.oncHistoryDetails[this.sortField] == null || b.tissueList.oncHistoryDetails[this.sortField] === undefined || b.tissueList.oncHistoryDetails[this.sortField][fieldName] == undefined || b.tissueList.oncHistoryDetails[this.sortField][fieldName] == "") {
              return -1;
            }

            if (typeof a.tissueList.oncHistoryDetails[this.sortField][fieldName] === "string") {
              return (order * a.tissueList.oncHistoryDetails[this.sortField][fieldName].localeCompare(b.tissueList.oncHistoryDetails[this.sortField][fieldName]));
            }
            else {
              return (order * a.tissueList.oncHistoryDetails[this.sortField][fieldName] - b.tissueList.oncHistoryDetails[this.sortField][fieldName]);
            }

          }
          else {
            if (a.tissueList.oncHistoryDetails[this.sortField] == undefined) {
              return 1;
            }
            if (b.tissueList.oncHistoryDetails[this.sortField] == undefined) {
              return -1;
            }

            if (typeof a.tissueList.oncHistoryDetails[this.sortField] === "string") {
              return (order * a.tissueList.oncHistoryDetails[this.sortField].localeCompare(b.tissueList.oncHistoryDetails[this.sortField]));
            }
            else {
              return (order * a.tissueList.oncHistoryDetails[this.sortField] - b.tissueList.oncHistoryDetails[this.sortField]);
            }
          }
          },
        );
      }
    }
    else if (this.sortParent === "t") {
      this.tissueListWrappers.sort((a, b) => {
        if (this.sortColumn.type === "ADDITIONALVALUE") {
          this.sortField = "additionalValues";
          if (a.tissueList.tissue == undefined || a.tissueList.tissue[this.sortField] == undefined) {
            return 1;
          }
          if (b.tissueList.tissue == undefined || b.tissueList.tissue[this.sortField] == undefined) {
            return -1;
          }
          if (a.tissueList.tissue[this.sortField] == null || a.tissueList.tissue[this.sortField] === undefined || a.tissueList.tissue[this.sortField][fieldName] == undefined || a.tissueList.tissue[this.sortField][fieldName] == "") {
            return 1;
          }
          if (b.tissueList.tissue[this.sortField] == null || b.tissueList.tissue[this.sortField] === undefined || b.tissueList.tissue[this.sortField][fieldName] == undefined || b.tissueList.tissue[this.sortField][fieldName] == "") {
            return -1;
          }

          if (typeof a.tissueList.tissue[this.sortField][fieldName] === "string") {
            return (order * a.tissueList.tissue[this.sortField][fieldName].localeCompare(b.tissueList.tissue[this.sortField][fieldName]));
          }
          else {
            return (order * a.tissueList.tissue[this.sortField][fieldName] - b.tissueList.tissue[this.sortField][fieldName]);
          }

        }
        else {
          if (a.tissueList.tissue == undefined || a.tissueList.tissue[this.sortField] == undefined) {
            return 1;
          }
          if (b.tissueList.tissue == undefined || b.tissueList.tissue[this.sortField] == undefined) {
            return -1;
          }

          if (typeof a.tissueList.tissue[this.sortField] === "string") {
            return (order * a.tissueList.tissue[this.sortField].localeCompare(b.tissueList.tissue[this.sortField]));
          }
          else {
            return (order * a.tissueList.tissue[this.sortField] - b.tissueList.tissue[this.sortField]);
          }
        }
        },
      );
    }
  }

  private sort(x, y, order): number {
    if (x === null || x == undefined || x[this.sortField] == undefined || x[this.sortField] === null) {
      return 1;
    }
    else if (y === null || y == undefined || y[this.sortField] == undefined || y[this.sortField] === null) {
      return -1;
    }
    else {
      if (x[this.sortField].toLowerCase() < y[this.sortField].toLowerCase()) {
        return -1 * order;
      }
      else if (x[this.sortField].toLowerCase() > y[this.sortField].toLowerCase()) {
        return 1 * order;
      }
      else {
        return 0;
      }
    }
  }

  public getDefaultFilterName() {
    this.defaultFilterName = this.role.getUserSetting().defaultTissueFilter;
  }

  //  }


  private getRequestStatusDisplay(request: string): string {
    switch (request) {
      case "no":
        return "Don't Request";

      case "hold":
        return "On Hold";

      case "review":
        return "Needs Review";

      case "unable To Obtain":
        return "Unable To Obtain";

      case "other":
        return "Other";

      default:
        return request;

    }
  }

  public getDisplayValueForFilter(column: Filter, name: string) {
    if (column.type !== Filter.OPTION_TYPE) {
      return name;
    }
    for (let nameValue of column.options) {
      if (nameValue.name === name) {
        return nameValue.value;
      }
    }
    return name;

  }

  private filterProfileForNoESRelams(viewFilter: ViewFilter) {
    //check if it was a filter with tableAlias of data -> filter client side
    if (viewFilter != null && viewFilter.filters != null && viewFilter.filters.length != 0) {
      this.copyTissueListWrappers = this.originalTissueListWrappers;
      for (let filter of viewFilter.filters) {
        if (filter.participantColumn.tableAlias === "data") {
          let tmp = filter.participantColumn.object != null ? filter.participantColumn.object : filter.participantColumn.tableAlias;
          let filterText = Filter.getFilterText(filter, tmp, this.allAdditionalColumns);
          if (filterText != null) {
            console.log(filterText);
            if (filter.type === "TEXT") {
              let value = filterText["filter1"]["value"];
              if (value !== null) {
                if (value.includes("'")) {
                  let first = value.indexOf("'");
                  let last = value.lastIndexOf("'");
                  value = value.substring(first + 1, last);
                }
                else if (value.includes("\"")) {
                  let first = value.indexOf("\"");
                  let last = value.lastIndexOf("\"");
                  value = value.substring(first + 1, last);
                }
                this.copyTissueListWrappers = this.copyTissueListWrappers.filter(tissueListWrapper =>
                  tissueListWrapper.data[filterText["parentName"]][filterText["filter1"]["name"]] === value,
                );

              }
              else {
                let empt = filterText["empty"];
                if (empt === "true") {
                  this.copyTissueListWrappers = this.copyTissueListWrappers.filter(tissueListWrapper =>
                    tissueListWrapper.data[filterText["parentName"]][filterText["filter1"]["name"]] === null,
                  );
                }
                else {
                  let notempt = filterText["notEmpty"];
                  if (notempt === "true") {
                    this.copyTissueListWrappers = this.copyTissueListWrappers.filter(tissueListWrapper =>
                      tissueListWrapper.data[filterText["parentName"]][filterText["filter1"]["name"]] !== null,
                    );
                  }
                }
              }
            }
            else if (filterText["type"] === "OPTIONS") {
              console.log(this.copyTissueListWrappers);
              let results: TissueListWrapper[] = new Array();
              let temp: TissueListWrapper[] = new Array();
              for (let option of filterText["selectedOptions"]) {// status
                temp = this.copyTissueListWrappers.filter(tissueListWrapper =>
                  tissueListWrapper.data[filterText["filter1"]["name"]] === option,
                );
                for (let t of temp) {
                  results.push(t);
                }
              }
              this.copyTissueListWrappers = results;
              this.tissueListWrappers = results;
            }
          }
        }

      }
      this.tissueListWrappers = this.copyTissueListWrappers;
    }
    else if (viewFilter == null) {
      if (this.selectedColumns["data"].length != 0) {
        this.copyTissueListWrappers = this.originalTissueListWrappers;
        for (let filter of this.selectedColumns["data"]) {
          let tmp = filter.participantColumn.object != null ? filter.participantColumn.object : filter.participantColumn.tableAlias;
          let filterText = Filter.getFilterText(filter, tmp, this.allAdditionalColumns);
          if (filterText != null) {
            if (filterText["type"] === "TEXT") {
              let value = filterText["filter1"]["value"];
              if (value !== null) {
                if (value.includes("'")) {
                  let first = value.indexOf("'");
                  let last = value.lastIndexOf("'");
                  value = value.substring(first + 1, last);
                }
                this.copyTissueListWrappers = this.copyTissueListWrappers.filter(tissueListWrapper =>
                  tissueListWrapper.data[filterText["parentName"]][filterText["filter1"]["name"]] === filterText["filter1"]["value"],
                );
              }
              else {
                let empt = filterText["empty"];
                if (empt) {
                  this.copyTissueListWrappers = this.copyTissueListWrappers.filter(tissueListWrapper =>
                    tissueListWrapper.data[filterText["parentName"]] == null || tissueListWrapper.data[filterText["parentName"]][filterText["filter1"]["name"]] == undefined || tissueListWrapper.data[filterText["parentName"]][filterText["filter1"]["name"]] === null || tissueListWrapper.data[filterText["parentName"]][filterText["filter1"]["name"]] === "",
                  );
                }
                else {
                  let notempt = filterText["notEmpty"];
                  if (notempt) {
                    this.copyTissueListWrappers = this.copyTissueListWrappers.filter(tissueListWrapper =>
                      tissueListWrapper.data[filterText["parentName"]][filterText["filter1"]["name"]] !== null && tissueListWrapper.data[filterText["parentName"]][filterText["filter1"]["name"]] !== undefined && tissueListWrapper.data[filterText["parentName"]][filterText["filter1"]["name"]] !== "",
                    );
                  }
                }
              }
              console.log(this.copyTissueListWrappers);
              this.tissueListWrappers = this.copyTissueListWrappers;

            }
            else if (filterText["type"] === "OPTIONS") {
              console.log(this.copyTissueListWrappers);
              let results: TissueListWrapper[] = new Array();
              let temp: TissueListWrapper[] = new Array();
              for (let option of filterText["selectedOptions"]) {// status
                temp = this.copyTissueListWrappers.filter(tissueListWrapper =>
                  tissueListWrapper.data[filterText["filter1"]["name"]] === option,
                );
                for (let t of temp) {
                  results.push(t);
                }
              }
              this.copyTissueListWrappers = results;
              this.tissueListWrappers = results;
            }
          }

        }
      }
    }

  }

  private adjustAllColumns(viewFilter: ViewFilter) {
    for (let filter of viewFilter.filters) {
      let t = filter.participantColumn.tableAlias;
      if (t === "r" || t === "o" || t === "ex") {
        t = "p";
      }
      for (let f of this.allColumns[t]) {
        if (f.participantColumn.name === filter.participantColumn.name) {
          let index = this.allColumns[t].indexOf(f);
          if (index !== -1) {
            this.allColumns[t].splice(index, 1);
            this.allColumns[t].push(filter);
            break;
          }
        }
      }
    }
  }

}
