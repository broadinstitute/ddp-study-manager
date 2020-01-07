import {Component, OnInit, ViewChild} from "@angular/core";
import {ActivatedRoute, Router} from "@angular/router";
import {AbstractionGroup} from "../abstraction-group/abstraction-group.model";
import {ActivityDefinition} from "../activity-data/models/activity-definition.model";
import {Option} from "../activity-data/models/option.model";
import {QuestionAnswer} from "../activity-data/models/question-answer.model";
import {Assignee} from "../assignee/assignee.model";
import {Filter} from "../filter-column/filter-column.model";
import {ModalComponent} from "../modal/modal.component";
import {ParticipantColumn} from "../filter-column/models/column.model";
import {OncHistoryDetail} from "../onc-history-detail/onc-history-detail.model";
import {Auth} from "../services/auth.service";
import {ComponentService} from "../services/component.service";
import {DSMService} from "../services/dsm.service";
import {RoleService} from "../services/role.service";
import {NameValue} from "../utils/name-value.model";
import {PatchUtil} from "../utils/patch.model";
import {Result} from "../utils/result.model";
import {Statics} from "../utils/statics";
import {Utils} from "../utils/utils";
import {ViewFilter} from "../filter-column/models/view-filter.model";
import {Value} from "../utils/value.model";
import {AssigneeParticipant} from "./models/assignee-participant.model";
import {Participant} from "./participant-list.model";
import {FieldSettings} from "../field-settings/field-settings.model";

@Component({
  selector: "app-participant-list",
  templateUrl: "./participant-list.component.html",
  styleUrls: ["./participant-list.component.css"]
})
export class ParticipantListComponent implements OnInit {

  @ViewChild(ModalComponent)
  public modal: ModalComponent;

  modalAnchor: string;
  assignMR: boolean = false;
  assignTissue: boolean = false;
  isAssignButtonDisabled: boolean = true;
  assignee: Assignee;

  loadedTimeStamp: string;

  errorMessage: string;
  additionalMessage: string;
  loadingParticipants = null;
  parent = "participantList";

  showFilters = false;
  showCustomizeViewTable = false;
  showSavedFilters = false;
  hasESData = false;

  currentFilter: Array<Filter>;
  selectedFilterName: string = "";

  participantList: Participant[] = [];
  copyParticipantList: Participant[] = [];
  activePage: number = 1;
  activityDefinitionList: ActivityDefinition[] = [];
  participant: Participant;
  oncHistoryDetail: OncHistoryDetail;
  assignees: Array<Assignee> = [];
  showParticipantInformation: boolean = false;
  showTissue: boolean = false;
  selectedTab: string;
  selectedMR = "";
  selectedOncOrTissue = "";

  dataSources: Map<string, string> = null;
  sourceColumns = {};
  allFieldNames = new Set();
  filterName: string = null;
  filterQuery: string = null;
  activityDefinitions = new Map();

  selectedColumns = {};
  defaultColumns = [Filter.REALM, Filter.SHORT_ID, Filter.FIRST_NAME, Filter.LAST_NAME, Filter.ENROLLMENT_STATUS];

  selectedFilter: Filter = null;
  savedFilters: ViewFilter[] = [];
  quickFilters: ViewFilter[] = [];

  settings = {};
  drugs: string[] = [];
  cancers: string[] = [];

  sortField: string = null;
  sortDir: string = null;
  sortParent: string = null;
  currentView: string = null;
  showHelp: boolean = false;
  filtered: boolean = false;

  constructor(private role: RoleService, private dsmService: DSMService, private compService: ComponentService,
              private router: Router, private auth: Auth, private route: ActivatedRoute, private util: Utils) {
    if (!auth.authenticated()) {
      auth.logout();
    }
    this.route.queryParams.subscribe(params => {
      let realm = params[DSMService.REALM] || null;
      if (realm != null) {
        //        this.compService.realmMenu = realm;
        this.additionalMessage = null;
        this.checkRight();
      }
    });
  }

  ngOnInit() {
    this.additionalMessage = null;
    if (localStorage.getItem(ComponentService.MENU_SELECTED_REALM) == null || localStorage.getItem(ComponentService.MENU_SELECTED_REALM) === undefined) {
      this.additionalMessage = "Please select a realm";
    }
    else {
      this.checkRight();
    }
    window.scrollTo(0, 0);
  }

  private checkRight() {
    //assumption for now: profile parameters are always the same, only survey will be dynamic per ddp
    this.loadingParticipants = localStorage.getItem(ComponentService.MENU_SELECTED_REALM);
    // this.loadAssignees();
    this.setSelectedFilterName("");
    this.currentFilter = null;
    this.filterQuery = null;
    let allowedToSeeInformation = false;
    this.errorMessage = null;
    this.participantList = null;
    let jsonData: any[];
    this.dsmService.getRealmsAllowed(Statics.MEDICALRECORD).subscribe(
      data => {
        jsonData = data;
        jsonData.forEach((val) => {
          if (localStorage.getItem(ComponentService.MENU_SELECTED_REALM) === val) {
            allowedToSeeInformation = true;
            this.loadSettings();
          }
        });
        if (!allowedToSeeInformation) {
          this.loadingParticipants = null;
          this.compService.customViews = null;
          this.errorMessage = "You are not allowed to see information of the selected realm at that category";
        }
      },
      err => {
        this.loadingParticipants = null;
        return null;
      }
    );
  }

  loadSettings() {
    let jsonData: any;
    this.dsmService.getSettings(localStorage.getItem(ComponentService.MENU_SELECTED_REALM), this.parent).subscribe(
      data => {
        this.assignees = [];
        this.drugs = [];
        this.cancers = [];
        this.activityDefinitionList = [];
        this.quickFilters = [];
        this.savedFilters = [];
        this.assignees.push(new Assignee("-1", "Remove Assignee", ""));
        jsonData = data;
        this.dataSources = new Map([
          ["data", "Participant"],
          ["p", "Participant - DSM"],
          ["m", "Medical Record"],
          ["oD", "Onc History"],
          ["t", "Tissue"],
          ["k", "Sample"],
          ["a", "Abstraction"]]);
        this.sourceColumns = {};
        this.selectedColumns = {};
        this.settings = {};
        this.getSourceColumnsFromFilterClass();
        if (jsonData.assignees != null) {
          jsonData.assignees.forEach((val) => {
            this.assignees.push(Assignee.parse(val));
          });
        }
        if (jsonData.drugs != null) {
          jsonData.drugs.forEach((val) => {
            this.drugs.push(val);
          });
        }
        if (jsonData.cancers != null) {
          jsonData.cancers.forEach((val) => {
            this.cancers.push(val);
          });
        }
        if (jsonData.fieldSettings != null) {
          Object.keys(jsonData.fieldSettings).forEach((key) => {
            jsonData.fieldSettings[key].forEach((fieldSetting: FieldSettings) => {
              let options: Array<NameValue> = null;
              if (fieldSetting.displayType === "OPTIONS") {
                options = new Array<NameValue>();
                fieldSetting.possibleValues.forEach((value: Value) => {
                  options.push(new NameValue(value.value, value.value));
                });
              }
              let filter = new Filter(new ParticipantColumn(fieldSetting.columnDisplay, fieldSetting.columnName, key), Filter.ADDITIONAL_VALUE_TYPE, options, new NameValue(fieldSetting.columnName, null),
                false, true, null, null, null, null, false, false, false, false, false, fieldSetting.displayType);
              if (this.settings[key] == null || this.settings[key] == undefined) {
                this.settings[key] = [];
              }
              if (key === "r") {
                if (this.sourceColumns["p"] == null || this.sourceColumns["p"] == undefined) {
                  this.sourceColumns["p"] = [];
                }
                this.sourceColumns["p"].push(filter);
              }
              else {
                if (this.sourceColumns[key] == null || this.sourceColumns[key] == undefined) {
                  this.sourceColumns[key] = [];
                }
                this.sourceColumns[key].push(filter);
              }
              this.settings[key].push(fieldSetting);
              this.allFieldNames.add(filter.participantColumn.tableAlias + "." + filter.participantColumn.name);
            });
          });
        }
        this.hasESData = false;
        if (jsonData.activityDefinitions != null) {
          Object.keys(jsonData.activityDefinitions).forEach((key) => {
            this.hasESData = true;
            let activityDefinition: ActivityDefinition = ActivityDefinition.parse(jsonData.activityDefinitions[key]);
            let possibleColumns: Array<Filter> = [];
            possibleColumns.push(new Filter(new ParticipantColumn("Survey Created", "createdAt", activityDefinition.activityCode, null, true), Filter.DATE_TYPE));
            possibleColumns.push(new Filter(new ParticipantColumn("Survey Completed", "completedAt", activityDefinition.activityCode, null, true), Filter.DATE_TYPE));
            possibleColumns.push(new Filter(new ParticipantColumn("Survey Last Updated", "lastUpdatedAt", activityDefinition.activityCode, null, true), Filter.DATE_TYPE));
            possibleColumns.push(new Filter(new ParticipantColumn("Survey Status", "status", activityDefinition.activityCode, null, true), Filter.OPTION_TYPE, [
              new NameValue("COMPLETE", "Done"),
              new NameValue("CREATED", "Not Started"),
              new NameValue("IN_PROGRESS", "In Progress")], null, false, true, null, null, null, null, false, true));
            if (activityDefinition != null && activityDefinition.questions != null) {
              for (let question of activityDefinition.questions) {
                if (question.stableId != null) {
                  let options: Array<NameValue> = null;
                  let type: string = question.questionType;
                  if (question.questionType === "PICKLIST") {
                    options = new Array<NameValue>();
                    type = Filter.OPTION_TYPE;
                    question.options.forEach((value: Option) => {
                      options.push(new NameValue(value.optionStableId, value.optionText));
                    });
                  }
                  let displayName = question.questionText != null && question.questionText !== "" ? question.questionText : question.stableId;
                  let filter = new Filter(new ParticipantColumn(displayName, question.stableId, activityDefinition.activityCode, null, true), type, options, null, false, true,
                    null, null, null, null, false, true);
                  filter.alwaysExact = true;
                  possibleColumns.push(filter);
                }
              }
              this.dataSources.set(activityDefinition.activityCode, activityDefinition.activityName);
              this.sourceColumns[activityDefinition.activityCode] = possibleColumns;
              this.selectedColumns[activityDefinition.activityCode] = [];
              //add now all these columns to allFieldsName for the search-bar
              possibleColumns.forEach(filter => {
                let tmp = filter.participantColumn.object != null ? filter.participantColumn.object : filter.participantColumn.tableAlias;
                this.allFieldNames.add(tmp + "." + filter.participantColumn.name);
              });
            }
            this.activityDefinitionList.push(activityDefinition);
          });
        }
        if (jsonData.abstractionFields != null && jsonData.abstractionFields.length > 0) {
          //only add abstraction columns if there is a abstraction form setup
          jsonData.abstractionFields.forEach((key) => {
            let abstractionGroup = AbstractionGroup.parse(key);
            abstractionGroup.fields.forEach((field) => {
              let tmp: string = field.medicalRecordAbstractionFieldId.toString();
              let tmpValues: NameValue[] = [];
              let tmpType = Filter.TEXT_TYPE;
              if ((field.type === "button_select" || field.type === "options" || field.type === "multi_options")
                && field.possibleValues != null) {
                tmpType = Filter.OPTION_TYPE;
                field.possibleValues.forEach((value) => {
                  tmpValues.push(new NameValue(value.value, value.value));
                });
              }
              this.sourceColumns["a"].push(new Filter(new ParticipantColumn(field.displayName, tmp, abstractionGroup.abstractionGroupId.toString(), "final"), tmpType, tmpValues, new NameValue(tmp, null)));
            });
          });
          //add now all these columns to allFieldsName for the search-bar
          this.sourceColumns["a"].forEach(filter => {
            let tmp = filter.participantColumn.object != null ? filter.participantColumn.object : filter.participantColumn.tableAlias;
            //add when abstraction is searchable
            // this.allFieldNames.add( tmp + "." + filter.participantColumn.name );
          });
        }
        else {
          this.dataSources.delete("a");
        }
        if (jsonData.filters != null) {
          console.log("299");
          console.log(jsonData.filters);
          jsonData.filters.forEach((val) => {
            let view: ViewFilter = ViewFilter.parseFilter(val, this.sourceColumns);
            console.log(view);
            if (val.userId.includes("System")) {
              // view = ViewFilter.parseFilter(val, this.sourceColumns);
              this.quickFilters.push(view);
            }
            else {
              // view = ViewFilter.parseFilter(val, this.sourceColumns);
              this.savedFilters.push(view);
            }
          });

        }
        this.orderColumns();
        this.getData();
      },
      err => {
        if (err._body === Auth.AUTHENTICATION_ERROR) {
          this.auth.logout();
        }
        throw "Error - Loading display settings" + err;
      }
    );
  }

  orderColumns() {
    this.dataSources.forEach((value: string, key: string) => {
      this.sourceColumns[key].sort((a: Filter, b: Filter) => this.sort(a.participantColumn.display, b.participantColumn.display, 1));
    });
  }

  getSourceColumnsFromFilterClass() {
    this.dataSources.forEach((value: string, key: string) => {
      this.selectedColumns[key] = [];
      this.sourceColumns[key] = [];
    });
    for (let filter of Filter.ALL_COLUMNS) {
      if (filter.participantColumn.tableAlias === "o" || filter.participantColumn.tableAlias === "ex"
        || filter.participantColumn.tableAlias === "r") {
        this.sourceColumns["p"].push(filter);
      }
      if (this.sourceColumns[filter.participantColumn.tableAlias] != null && this.sourceColumns[filter.participantColumn.tableAlias] != undefined) {
        this.sourceColumns[filter.participantColumn.tableAlias].push(filter);
        let tmp = filter.participantColumn.object != null ? filter.participantColumn.object : filter.participantColumn.tableAlias;
        this.allFieldNames.add(tmp + "." + filter.participantColumn.name);
      }
    }
  }

  private getData() {
    //find viewFilter by filterName
    let defaultFilter = null;
    if (this.role.getUserSetting().defaultParticipantFilter != null) {
      defaultFilter = this.savedFilters.find(filter => {
        return filter.filterName === this.role.getUserSetting().defaultParticipantFilter;
      });
      if (defaultFilter == null) {
        defaultFilter = this.quickFilters.find(filter => {
          return filter.filterName === this.role.getUserSetting().defaultParticipantFilter;
        });
      }
      console.log(this.savedFilters);
      console.log(defaultFilter);
      console.log(this.role.getUserSetting().defaultParticipantFilter);

      if (defaultFilter != null && defaultFilter != undefined) {
        this.selectFilter(defaultFilter);
      }
      else if (this.role.getUserSetting().defaultParticipantFilter !== "" && this.role.getUserSetting().defaultParticipantFilter !== null && this.role.getUserSetting().defaultParticipantFilter !== undefined) {
        this.additionalMessage = "The default filter seems to be deleted, however it is still the default filter as long as not changed in the user settings.";
        this.loadingParticipants = localStorage.getItem(ComponentService.MENU_SELECTED_REALM);
        this.dsmService.filterData(localStorage.getItem(ComponentService.MENU_SELECTED_REALM), null, this.parent, true).subscribe(
          data => {
            if (data != null) {
              this.participantList = [];
              let jsonData: any[];
              jsonData = data;
              console.log(jsonData);
              jsonData.forEach((val) => {
                let participant = Participant.parse(val);
                this.participantList.push(participant);
              });
              this.copyParticipantList = this.participantList;
              console.log(this.participantList);
              let date = new Date();
              this.loadedTimeStamp = Utils.getDateFormatted(date, Utils.DATE_STRING_IN_EVENT_CVS);
            }
            this.loadingParticipants = null;
          },
          err => {
            if (err._body === Auth.AUTHENTICATION_ERROR) {
              this.auth.logout();
            }
            this.loadingParticipants = null;
            this.errorMessage = "Error - Loading Participant List, Please contact your DSM developer";
          }
        );
      }
    }
    else {
      this.selectFilter(null);
    }
  }

  public selectFilter(viewFilter: ViewFilter) {
    console.log(viewFilter);
    this.loadingParticipants = localStorage.getItem(ComponentService.MENU_SELECTED_REALM);
    this.currentView = JSON.stringify(viewFilter);
    let filterName = null;
    if (viewFilter != null) {
      filterName = viewFilter.filterName;
      this.filtered = true;
    }
    else {
      this.filtered = false;
    }
    this.dsmService.applyFilter(viewFilter, localStorage.getItem(ComponentService.MENU_SELECTED_REALM), this.parent, null).subscribe(
      data => {
        if (data != null) {
          if (viewFilter!= null && viewFilter.filters != null){
          for (let filter of viewFilter.filters){
            for ( let f of this.sourceColumns[filter.participantColumn.tableAlias]) {
              if (f.participantColumn.name === filter.participantColumn.name) {
                let index = this.sourceColumns[filter.participantColumn.tableAlias].indexOf(f);
                if (index !== -1) {
                  this.sourceColumns[filter.participantColumn.tableAlias].splice(index, 1);
                  this.sourceColumns[filter.participantColumn.tableAlias].push(filter);
                }
              }
            }
          }
          }
          this.participantList = [];
          let jsonData: any[];
          jsonData = data;
          console.log(jsonData);
          jsonData.forEach((val) => {
            let participant = Participant.parse(val);
            this.participantList.push(participant);
          });
          this.copyParticipantList = this.participantList;
          if (viewFilter != null) {
            this.filterQuery = viewFilter.queryItems;
          }
          console.log(this.participantList);
          if (viewFilter != null) {
            viewFilter.selected = true;
            for (let f of this.quickFilters) {
              if (viewFilter.filterName !== f.filterName) {
                f.selected = false;
              }
            }
            if (viewFilter != null && viewFilter.filters != null) {
              for (let filter of viewFilter.filters) {
                if (filter.type === Filter.OPTION_TYPE) {
                  filter.selectedOptions = filter.getSelectedOptionsBoolean();
                }
              }
            }
            this.selectedFilterName = viewFilter.filterName;
            this.filterQuery = viewFilter.queryItems.replace(",", "");
            this.selectedColumns = viewFilter.columns;
          }
          else {
            //if selected columns are not set, set to default columns
            if (this.selectedColumns["data"].length == 0) {
              this.dataSources.forEach((value: string, key: string) => {
                this.selectedColumns[key] = [];
              });
              this.selectedColumns["data"] = this.defaultColumns;
            }
          }
          let date = new Date();
          this.loadedTimeStamp = Utils.getDateFormatted(date, Utils.DATE_STRING_IN_EVENT_CVS);
        }
        this.loadingParticipants = null;
      },
      err => {
        if (err._body === Auth.AUTHENTICATION_ERROR) {
          this.auth.logout();
        }
        this.loadingParticipants = null;
        this.errorMessage = "Error - Loading Participant List, Please contact your DSM developer";
      }
    );
  }

  getColSpan() {
    let columnCount = 1; //1 = checkbox column
    this.dataSources.forEach((value: string, key: string) => {
      if (this.selectedColumns[key] != null) {
        this.selectedColumns[key].forEach(col => columnCount = columnCount + 1);
      }
    });
    return columnCount;
  }

  getFilters() {
    this.dsmService.getFiltersForUserForRealm(localStorage.getItem(ComponentService.MENU_SELECTED_REALM), this.parent).subscribe(
      data => {
        this.savedFilters = [];
        let jsonData = data;
        console.log(jsonData);
        jsonData.forEach((val) => {
          let view: ViewFilter;
          if (!val.userId.includes("System")) {
            view = ViewFilter.parseFilter(val, this.sourceColumns);
            console.log(view);
            this.savedFilters.push(view);
          }
        });
      },
      err => {
        this.showSavedFilters = false;
        this.errorMessage = "Error - Loading Filter List, Please contact your DSM developer";
      });
  }

  public onclickDropDown(e) {
    e.stopPropagation();
  }

  showCustomizeView() {
    // this.loadSettings();
    this.showFilters = false;
    this.showSavedFilters = false;
    this.showCustomizeViewTable = !this.showCustomizeViewTable;

  }

  showFiltersTable() {
    let assigneesMap = [];
    this.assignees.forEach(assignee => {
      if (assignee.assigneeId !== "-1") {
        assigneesMap.push(new NameValue(assignee.assigneeId, assignee.name));
      }
    });
    //fixing assignee filters
    if (this.selectedColumns["p"] != null) {
      this.selectedColumns["p"].forEach((col, i) => {
        if (col.participantColumn.name === Filter.ASSIGNEE_MR.participantColumn.name) {
          this.selectedColumns["p"][i] = new Filter(ParticipantColumn.ASSIGNEE_MR, Filter.OPTION_TYPE, assigneesMap);
        }
      });
      this.selectedColumns["p"].forEach((col, i) => {
        if (col.participantColumn.name === Filter.ASSIGNEE_TISSUE.participantColumn.name) {
          this.selectedColumns["p"][i] = new Filter(ParticipantColumn.ASSIGNEE_TISSUE, Filter.OPTION_TYPE, assigneesMap);
        }
      });
    }
    this.showCustomizeViewTable = false;
    this.showSavedFilters = false;
    this.showFilters = !this.showFilters;
  }

  showSavedFiltersPanel() {
    this.showCustomizeViewTable = false;
    this.showFilters = false;
    this.showSavedFilters = !this.showSavedFilters;
    if (this.showSavedFilters) {
      this.getFilters();
    }
  }

  public clearFilter() {
    this.filterQuery = null;
    this.deselectQuickFilters();
    this.clearManualFilters();
    this.getData();
  }

  public clearManualFilters() {
    this.dataSources.forEach((value: string, key: string) => {
      if (this.selectedColumns[key] != null) {
        for (let filter of this.selectedColumns[key]) {
          if (filter != null && filter != undefined) {
            filter.clearFilter();
          }
        }
      }
    });
  }

  public setSelectedFilterName(filterName) {
    this.selectedFilterName === filterName;
  }


  addOrRemoveColumn(column: Filter, parent: string) {
    if (this.selectedColumns[parent] == null) {
      this.selectedColumns[parent] = [];
    }
    if (this.selectedColumns[parent].includes(column)) {
      let index = this.selectedColumns[parent].indexOf(column);
      this.selectedColumns[parent].splice(index, 1);
    }
    else {
      this.selectedColumns[parent].push(column);
    }
  }

  openParticipant(participant: Participant, colSource: string, id?) {
    if (participant != null) {
      let tabAnchor = "Survey Data";
      if (colSource === "m" || participant.data.activities == null) {
        tabAnchor = "Medical Records";
        this.selectedMR = "";
        this.selectedOncOrTissue = "";
      }
      else if (colSource === "oD" || colSource === "t") {
        tabAnchor = "Onc History";
        this.selectedMR = "";
        this.selectedOncOrTissue = "";
      }
      if (this.filtered && participant.participant != null && participant.participant.ddpParticipantId != null) {
        this.loadingParticipants = localStorage.getItem(ComponentService.MENU_SELECTED_REALM);
        this.dsmService.getParticipantData(localStorage.getItem(ComponentService.MENU_SELECTED_REALM), participant.participant.ddpParticipantId, this.parent).subscribe(
          data => {
            let pt: Participant = Participant.parse(data[0]);
            if (pt == null || pt == undefined) {
              this.errorMessage = "Participant  not found";
            }
            else {
              if (pt.participant != null && pt.participant.ddpParticipantId != null
                && pt.participant.ddpParticipantId === participant.participant.ddpParticipantId) {
                this.selectedTab = tabAnchor;
                this.participant = pt;
                this.showParticipantInformation = true;
                this.showTissue = false;
              }
            }
            this.loadingParticipants = null;
          },
          err => {
            this.errorMessage = "Error - Loading Participant Information, Please contact your DSM developer";
          }
        );
      }
      else {
        this.selectedTab = tabAnchor;
        this.participant = participant;
        this.showParticipantInformation = true;
        this.showTissue = false;
      }
    }
  }

  openTissue(participant: Participant, oncHistory: OncHistoryDetail) {
    if (participant != null && oncHistory != null) {
      this.participant = participant;
      this.oncHistoryDetail = oncHistory;
      this.showTissue = true;
    }
  }

  getRealm(): string {
    return localStorage.getItem(ComponentService.MENU_SELECTED_REALM);
  }

  getFilterButtonColorStyle(isOpened: boolean): string {
    if (isOpened) {
      return Statics.COLOR_ACCENT;
    }
    return Statics.COLOR_BASIC;
  }

  getButtonColorStyle(isOpened: boolean): string {
    if (isOpened) {
      return Statics.COLOR_PRIMARY;
    }
    return Statics.COLOR_BASIC;
  }

  public doFilter() {

    //    this.c
    let json = [];
    this.dataSources.forEach((value: string, key: string) => {
      this.createFilterJson(json, key);
      }
    );
    //nothing to filter on the server
    if (json.length != 0) {
      this.filterQuery = null;
      this.selectedFilterName = null;
      this.deselectQuickFilters();
      let data = {
        "filters": json,
        "parent": this.parent
      };
      let jsonPatch = JSON.stringify(data);
      this.currentFilter = json;
      this.currentView = jsonPatch;
      this.filtered = true;
      this.loadingParticipants = localStorage.getItem(ComponentService.MENU_SELECTED_REALM);
      this.dsmService.filterData(localStorage.getItem(ComponentService.MENU_SELECTED_REALM), jsonPatch, this.parent, null).subscribe(
        data => {
          if (data != undefined && data != null && data !== "") {
            let jsonData: any[];
            this.participantList = [];
            this.filterQuery = "";
            jsonData = data;
            jsonData.forEach((val) => {
              let participant = Participant.parse(val);
              this.participantList.push(participant);
            });
            let date = new Date();
            this.loadedTimeStamp = Utils.getDateFormatted(date, Utils.DATE_STRING_IN_EVENT_CVS);
            this.additionalMessage = null;
          }
          else {
            this.additionalMessage = "Something went wrong while filtering - List was not filtered!";
          }

          this.loadingParticipants = null;
          this.errorMessage = null;
          window.scrollTo(0, 0);
        },
        err => {
          this.loadingParticipants = null;
          this.errorMessage = "Error - Loading Participant List, Please contact your DSM developer\n " + err;
        }
      );
    }
    else {
      this.filtered = false;
      this.filterQuery = "";
      this.deselectQuickFilters();
      //TODO - can be changed later to all using the same - after all studies are migrated!
      //check if it was a tableAlias data filter -> filter client side
      let didClientSearch = false;
      if (this.selectedColumns["data"].length != 0) {
        for (let filter of this.selectedColumns["data"]) {
          let tmp = filter.participantColumn.object != null ? filter.participantColumn.object : filter.participantColumn.tableAlias;
          let filterText = Filter.getFilterText(filter, tmp, this.settings);
          if (filterText != null) {
            this.participantList = this.copyParticipantList.filter(participant =>
              participant.data != null && participant.data.profile[filterText["filter1"]["name"]] === filterText["filter1"]["value"]
            );
            didClientSearch = true;
          }
        }
      }
      if (!didClientSearch) {
        //get unfiltered pt list
        this.selectFilter(null);
      }
    }
  }

  createFilterJson(json, key: string) {
    if (this.selectedColumns[key] != null) {
      for (let filter of this.selectedColumns[key]) {
        //TODO - can be changed later to all using the same - after all studies are migrated!
        //only filter for ES data if ddp has ES data
        if (filter.participantColumn.tableAlias === "data" && this.hasESData) {
          this.addFilterToJson(filter, json);

        }
        //filter for all data which is not es
        else if (filter.participantColumn.tableAlias !== "data") {
          this.addFilterToJson(filter, json);
        }
      }
    }
  }

  addFilterToJson(filter: Filter, json) {
    let tmp = filter.participantColumn.object != null ? filter.participantColumn.object : filter.participantColumn.tableAlias;
    let filterText = null;
    // change filter to something backend knows!
    if (filter.participantColumn.name === "sampleQueue") {
      if (filter.type === Filter.OPTION_TYPE) {
        let status = null;
        for (let [key, value] of Object.entries(filter.selectedOptions)) {
          if (value) {
            status = filter.options[key].name;
            break;
          }
        }
        if (status != null) {
          if (status === "sent") {
            let filter1 = new NameValue("scanDate", null);
            let filter2 = new NameValue("scanDate", "true");
            filterText = Filter.getFilterJson(tmp, filter1, filter2, null, false, Filter.DATE_TYPE, false, false, true, filter.participantColumn);
            json.push(filterText);
            filter1 = new NameValue("receiveDate", "true");
            filter2 = new NameValue("receiveDate", null);
            filterText = Filter.getFilterJson(tmp, filter1, filter2, null, false, Filter.DATE_TYPE, false, true, false, filter.participantColumn);
          }
          if (status === "received") {
            let filter1 = new NameValue("receiveDate", null);
            let filter2 = new NameValue("receiveDate", "true");
            filterText = Filter.getFilterJson(tmp, filter1, filter2, null, false, Filter.DATE_TYPE, false, false, true, filter.participantColumn);
          }
          if (status === "deactivated") {
            let filter1 = new NameValue("deactivatedDate", null);
            let filter2 = new NameValue("deactivatedDate", "true");
            filterText = Filter.getFilterJson(tmp, filter1, filter2, null, false, Filter.DATE_TYPE, false, false, true, filter.participantColumn);
          }
          if (status === "error") {
            let filter1 = new NameValue("error", "true");
            let filter2 = new NameValue("error", null);
            filterText = Filter.getFilterJson(tmp, filter1, filter2, null, false, Filter.CHECKBOX_TYPE, false, false, false, filter.participantColumn);
            json.push(filterText);
            filter1 = new NameValue("scanDate", "true");
            filter2 = new NameValue("scanDate", null);
            filterText = Filter.getFilterJson(tmp, filter1, filter2, null, false, Filter.DATE_TYPE, false, true, false, filter.participantColumn);
          }
          if (status === "queue") {
            let filter1 = new NameValue("scanDate", "true");
            let filter2 = new NameValue("scanDate", null);
            filterText = Filter.getFilterJson(tmp, filter1, filter2, null, false, Filter.DATE_TYPE, false, true, false, filter.participantColumn);
          }
        }
      }
    }
    else {
      filterText = Filter.getFilterText(filter, tmp, this.settings);
    }
    if (filterText != null) {
      json.push(filterText);
    }
  }

  hasRole(): RoleService {
    return this.role;
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
      this.additionalMessage = "Error - Sharing Filter, Please contact your DSM developer";
    });

  }

  public deleteView(savedFilter: ViewFilter) {
    let patch1 = new PatchUtil(savedFilter.id, this.role.userMail(),
      {name: "fDeleted", value: "1"}, null, this.parent, null);
    let patch = patch1.getPatch();
    this.dsmService.patchParticipantRecord(JSON.stringify(patch)).subscribe(data => {
      let result = Result.parse(data);
      if (result.code == 200) {
        this.getFilters();
      }
    }, err => {
      this.additionalMessage = "Error - Deleting Filter, Please contact your DSM developer";
    });
  }

  saveCurrentFilter() {
    this.modal.hide();

    let columns = [];
    this.dataSources.forEach((value: string, key: string) => {
      if (this.selectedColumns != null && this.selectedColumns[key] != null) {
        for (let col of this.selectedColumns[key]) {
          columns.push(col.participantColumn.tableAlias + "." + col.participantColumn.name);
        }
      }
    });
    let tmpFilterName = this.selectedFilterName;
    let quickFilter = this.quickFilters.find(x => x.filterName === this.selectedFilterName);
    if (quickFilter == null) {
      tmpFilterName = null;
    }

    let jsonData = {
      "columnsToSave": columns,
      "filterName": this.filterName,
      "shared": "0",
      "fDeleted": "0",
      "filters": this.currentFilter,
      "parent": this.parent,
      "quickFilterName": tmpFilterName,
      "queryItems": this.filterQuery
    };
    let jsonPatch = JSON.stringify(jsonData);
    this.currentView = jsonPatch;
    this.dsmService.saveCurrentFilter(jsonPatch, localStorage.getItem(ComponentService.MENU_SELECTED_REALM), this.parent).subscribe(data => {
        this.filterName = null;
      },
      err => {
        this.additionalMessage = "Error - Saving Filter, Please contact your DSM developer";
      });
  }

  public isSortField(name: string) {
    return name === this.sortField;
  }

  sortByColumnName(col: Filter, sortParent: string) {
    this.sortDir = this.sortField === col.participantColumn.name ? (this.sortDir === "asc" ? "desc" : "asc") : "asc";
    this.sortField = col.participantColumn.name;
    this.sortParent = sortParent;
    this.doSort(col.participantColumn.object);
  }

  private doSort(object: string) {
    console.log(object);
    let order = this.sortDir === "asc" ? 1 : -1;
    if (this.sortParent === "data" && object != null) {
      this.participantList.sort((a, b) => this.sort(a.data[object][this.sortField], b.data[object][this.sortField], order));
    }
    else if (this.sortParent === "data" && object == null) {
      this.participantList.sort((a, b) => this.sort(a.data[this.sortField], b.data[this.sortField], order));
    }
    else if (this.sortParent === "p") {
      this.participantList.sort((a, b) => this.sort(a.participant[this.sortField], b.participant[this.sortField], order));
    }
    else if (this.sortParent === "m") {
      this.participantList.sort((a, b) => {
        if (a.medicalRecords === null || a.medicalRecords == undefined || a.medicalRecords.length < 1) {
          return 1;
        }
        else if (b.medicalRecords === null || b.medicalRecords == undefined || b.medicalRecords.length < 1) {
          return -1;
        }
        else {
          a.medicalRecords.sort((n, m) => this.sort(n[this.sortField], m[this.sortField], order));
          b.medicalRecords.sort((n, m) => this.sort(n[this.sortField], m[this.sortField], order));
        }
      });
    }
    else if (this.sortParent === "oD") {
      this.participantList.sort((a, b) => {
        if (a.oncHistoryDetails === null || a.oncHistoryDetails == undefined || a.oncHistoryDetails.length < 1) {
          return 1;
        }
        else if (b.oncHistoryDetails === null || b.oncHistoryDetails == undefined || b.oncHistoryDetails.length < 1) {
          return -1;
        }
        else {
          a.oncHistoryDetails.sort((n, m) => this.sort(n[this.sortField], m[this.sortField], order));
          b.oncHistoryDetails.sort((n, m) => this.sort(n[this.sortField], m[this.sortField], order));
        }
      });
    }
    else if (this.sortParent === "t") {
    }
    else {
      //activity data
      this.participantList.sort((a, b) => {
        let activityDataA = a.data.getActivityDataByCode(this.sortParent);
        let activityDataB = b.data.getActivityDataByCode(this.sortParent);
        if (activityDataA == null) {
          return 1;
        }
        else if (activityDataB == null) {
          return -1;
        }
        else {
          if (this.sortField === "createdAt" || this.sortField === "completedAt" || this.sortField === "lastUpdatedAt") {
            return this.sort(activityDataA[this.sortField], activityDataB[this.sortField], order);
          }
          else {
            let questionAnswerA = this.getQuestionAnswerByName(activityDataA.questionsAnswers, this.sortField);
            let questionAnswerB = this.getQuestionAnswerByName(activityDataB.questionsAnswers, this.sortField);
            if (questionAnswerA == null) {
              return 1;
            }
            else if (questionAnswerB == null) {
              return -1;
            }
            else {
              if (questionAnswerA.questionType === "DATE") {
                return this.sort(questionAnswerA.date, questionAnswerB.date, order);
              }
              else {
                return this.sort(questionAnswerA.answer, questionAnswerB.answer, order);
              }
            }
          }
        }
      });
    }
  }

  private sort(x, y, order) {
    if (x === null || x == undefined) {
      return 1;
    }
    else if (y === null || y == undefined) {
      return -1;
    }
    else {
      if (typeof x === "string") {
        if (x.toLowerCase() < y.toLowerCase()) {
          return -1 * order;
        }
        else if (x.toLowerCase() > y.toLowerCase()) {
          return 1 * order;
        }
        else {
          return 0;
        }
      }
      else {
        if (x < y) {
          return -1 * order;
        }
        else if (x > y) {
          return 1 * order;
        }
        else {
          return 0;
        }
      }
    }
  }

  public isSelectedFilter(filterName): boolean {
    return this.selectedFilterName === filterName;
  }

  getUtil(): Utils {
    return this.util;
  }

  getKeys() {
    return Array.from(this.dataSources.keys());
  }

  getTableAlias(col: ParticipantColumn) {
    return col.object != null ? col.object : col.tableAlias;
  }

  downloadCurrentData() {
    let date = new Date();
    let columns = {};
    this.dataSources.forEach((value: string, key: string) => {
      if (this.selectedColumns[key] != null && this.selectedColumns[key].length != 0) {
        columns[key] = this.selectedColumns[key];
      }
    });

    let fileCount: number = 0;

    for (let source of Object.keys(columns)) {
      if (source === "data" || source === "p" || source === "t") { //because data gets added to everything!
        continue;
      }

      if (source === "m") {
        Utils.downloadCurrentData(this.participantList, [["data", "data"], ["participant", "p"], ["medicalRecords", "m"]], columns, "Participants-MR-" + Utils.getDateFormatted(date, Utils.DATE_STRING_CVS) + Statics.CSV_FILE_EXTENSION);
        fileCount = fileCount + 1;
      }
      else if (source === "oD") {
        Utils.downloadCurrentData(this.participantList, [["data", "data"], ["participant", "p"], ["oncHistoryDetails", "oD", "tissues", "t"]], columns, "Participants-OncHistory-" + Utils.getDateFormatted(date, Utils.DATE_STRING_CVS) + Statics.CSV_FILE_EXTENSION);
        fileCount = fileCount + 1;
      }
      else if (source === "k") {
        Utils.downloadCurrentData(this.participantList, [["data", "data"], ["participant", "p"], ["kits", "k"]], columns, "Participants-Sample-" + Utils.getDateFormatted(date, Utils.DATE_STRING_CVS) + Statics.CSV_FILE_EXTENSION);
        fileCount = fileCount + 1;
      }
      else {
        Utils.downloadCurrentData(this.participantList, [["data", "data"], [source, source]], columns, "Participants-" + source + Utils.getDateFormatted(date, Utils.DATE_STRING_CVS) + Statics.CSV_FILE_EXTENSION, true);
        fileCount = fileCount + 1;
      }
    }

    if (fileCount == 0) {
      if (this.selectedColumns["data"] != null && this.selectedColumns["data"].length > 0) {
        fileCount = fileCount + 1;
        Utils.downloadCurrentData(this.participantList, [["data", "data"], ["participant", "p"]], columns, "Participants-" + Utils.getDateFormatted(date, Utils.DATE_STRING_CVS) + Statics.CSV_FILE_EXTENSION);
      }
    }

    if (fileCount > 1) {
      this.additionalMessage = "Table was downloaded in multiple documents, because of unrelated information";
    }
    else {
      this.additionalMessage = null;
    }
  }

  getOptionDisplay(options: NameValue[], key: string) {
    if (options != null) {
      let nameValue = options.find(obj => {
        return obj.name === key;
      });
      if (nameValue != null) {
        return nameValue.value;
      }
    }
    return "";
  }

  checkboxChecked() {
    this.isAssignButtonDisabled = true;
    for (let pt of this.participantList) {
      if (pt.isSelected) {
        this.isAssignButtonDisabled = false;
        break;
      }
    }
  }

  assign() { //arg[0] = selectedAssignee: Assignee
    this.additionalMessage = null;
    if (this.assignee != null && this.participantList.length > 0) {
      // console.log(`${this.assigneeMr.assigneeId} ${this.assigneeMr.email}`);
      let assignParticipants: Array<AssigneeParticipant> = [];
      for (let pt of this.participantList) {
        if (pt.isSelected) {
          if (this.assignMR) {
            if (this.assignee.assigneeId === "-1") {
              pt.participant.assigneeMr = null;
            }
            else {
              pt.participant.assigneeMr = this.assignee.name;
            }
          }
          if (this.assignTissue) {
            if (this.assignee.assigneeId === "-1") {
              pt.participant.assigneeMr = null;
            }
            else {
              pt.participant.assigneeTissue = this.assignee.name;
            }
          }
          assignParticipants.push(new AssigneeParticipant(pt.participant.participantId, this.assignee.assigneeId,
            this.assignee.email, pt.data.profile["shortId"]));
        }
      }
      // console.log(`assigned participant: ${JSON.stringify(assignParticipants, null, 2)}`);
      this.deselect();
      this.dsmService.assignParticipant(localStorage.getItem(ComponentService.MENU_SELECTED_REALM), this.assignMR, this.assignTissue, JSON.stringify(assignParticipants)).subscribe(// need to subscribe, otherwise it will not send!
        data => {
          // console.log(`assigned participant received: ${JSON.stringify(data, null, 2)}`);
          let result = Result.parse(data);
          if (result.code !== 200) {
            this.additionalMessage = result.body;
          }
          // this.loadParticipantData();
          this.assignMR = false;
          this.assignTissue = false;
        },
        err => {
          if (err._body === Auth.AUTHENTICATION_ERROR) {
            this.router.navigate([Statics.HOME_URL]);
          }
          this.additionalMessage = "Error - Assigning Participants, Please contact your DSM developer";
        }
      );
    }
    this.modal.hide();
    window.scrollTo(0, 0);
  }

  assigneeSelected(evt: any) {
    this.assignee = evt;
  }

  deselect() {
    for (let pt of this.participantList) {
      if (pt.isSelected) {
        pt.isSelected = false;
      }
    }
  }

  openModal(modalAnchor: string) {
    this.modalAnchor = modalAnchor;
    this.modal.show();
  }

  deselectQuickFilters() {
    this.deselectFilters(this.quickFilters);
  }

  deselectFilters(filterArray: ViewFilter[]) {
    if (filterArray != null) {
      filterArray.forEach(filter => {
        if (filter.selected) {
          filter.selected = false;
        }
      });
    }
  }

  public doFilterByQuery(queryText: string) {
    this.clearManualFilters();
    this.deselectQuickFilters();
    this.setSelectedFilterName("");
    let data = {
      "filterQuery": queryText,
      "parent": this.parent
    };
    let jsonPatch = JSON.stringify(data);
    if (queryText != null) {
      this.filtered = true;
    }
    else {
      this.filtered = false;
    }
    this.loadingParticipants = localStorage.getItem(ComponentService.MENU_SELECTED_REALM);
    this.dsmService.filterData(localStorage.getItem(ComponentService.MENU_SELECTED_REALM), jsonPatch, this.parent, null).subscribe(data => {
      this.participantList = [];
      if (data != null) {
        let jsonData: any[];
        jsonData = data;
        jsonData.forEach((val) => {
          let participant = Participant.parse(val);
          this.participantList.push(participant);
        });
        let date = new Date();
        this.loadedTimeStamp = Utils.getDateFormatted(date, Utils.DATE_STRING_IN_EVENT_CVS);
        this.additionalMessage = null;
      }
      this.loadingParticipants = null;
      this.filterQuery = queryText;
    }, err => {
      this.participantList = [];
      this.loadingParticipants = null;
      this.additionalMessage = "Error - Filtering Participant List, Please contact your DSM developer";
    });
  }

  getQuestionAnswerByName(questionsAnswers: Array<QuestionAnswer>, name: string) {
    return questionsAnswers.find(x => x.stableId === name);
  }
}
