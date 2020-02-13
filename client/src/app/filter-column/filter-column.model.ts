import {ParticipantColumn} from "./models/column.model";
import {NameValue} from "../utils/name-value.model";
import {FieldSettings} from "../field-settings/field-settings.model";
import {Value} from "../utils/value.model";
import {Statics} from "../utils/statics";

export class Filter {

  public static DATE_TYPE = "DATE";
  public static EPOCH_DATE_TYPE = "EPOCHDATE";
  public static SHORT_DATE_TYPE = "DATE_SHORT";
  public static TEXT_TYPE = "TEXT";
  public static OPTION_TYPE = "OPTIONS";
  public static NUMBER_TYPE = "NUMBER";
  public static BOOLEAN_TYPE = "BOOLEAN"; //ES boolean value true/false
  public static CHECKBOX_TYPE = "CHECKBOX"; //DSM UI checkbox (unable to obtain/problem)
  public static ADDITIONAL_VALUE_TYPE = "ADDITIONALVALUE";
  public static COMPOSITE_TYPE = "COMPOSITE";

  //ES data
  public static REALM = new Filter(ParticipantColumn.REALM, Filter.TEXT_TYPE);
  public static SHORT_ID = new Filter(ParticipantColumn.SHORT_ID, Filter.TEXT_TYPE);
  public static LEGACY_SHORT_ID = new Filter(ParticipantColumn.LEGACY_SHORT_ID, Filter.TEXT_TYPE);
  public static PARTICIPANT_ID = new Filter(ParticipantColumn.PARTICIPANT_ID, Filter.TEXT_TYPE);
  public static LEGACY_PARTICIPANT_ID = new Filter(ParticipantColumn.LEGACY_PARTICIPANT_ID, Filter.TEXT_TYPE);
  public static FIRST_NAME = new Filter(ParticipantColumn.FIRST_NAME, Filter.TEXT_TYPE);
  public static LAST_NAME = new Filter(ParticipantColumn.LAST_NAME, Filter.TEXT_TYPE);
  public static COUNTRY = new Filter(ParticipantColumn.COUNTRY, Filter.TEXT_TYPE);
  public static ENROLLMENT_STATUS = new Filter(ParticipantColumn.ENROLLMENT_STATUS, Filter.OPTION_TYPE, [
    new NameValue("REGISTERED", "Registered"),
    new NameValue("EXITED_BEFORE_ENROLLMENT", "Exited before Enrollment"),
    new NameValue("EXITED_AFTER_ENROLLMENT", "Exited after Enrollment"),
    new NameValue("ENROLLED", "Enrolled"),
    new NameValue("CONSENT_SUSPENDED", "Lost to Followup")
  ]);
  public static EMAIL = new Filter(ParticipantColumn.EMAIL, Filter.TEXT_TYPE);
  public static REGISTRATION_DATE = new Filter(ParticipantColumn.REGISTRATION_DATE, Filter.DATE_TYPE);
  public static DO_NOT_CONTACT = new Filter(ParticipantColumn.DO_NOT_CONTACT, Filter.BOOLEAN_TYPE);
  public static DATE_OF_MAJORITY = new Filter(ParticipantColumn.DATE_OF_MAJORITY, Filter.SHORT_DATE_TYPE);
  public static HAS_CONSENTED_TO_BLOOD = new Filter(ParticipantColumn.HAS_CONSENTED_TO_BLOOD, Filter.BOOLEAN_TYPE);
  public static HAS_CONSENTED_TO_TISSUE = new Filter(ParticipantColumn.HAS_CONSENTED_TO_TISSUE, Filter.BOOLEAN_TYPE);
  public static DATE_OF_BIRTH = new Filter(ParticipantColumn.DATE_OF_BIRTH, Filter.SHORT_DATE_TYPE);
  public static DIAGNOSIS_MONTH = new Filter(ParticipantColumn.DIAGNOSIS_MONTH, Filter.NUMBER_TYPE);
  public static DIAGNOSIS_YEAR = new Filter(ParticipantColumn.DIAGNOSIS_YEAR, Filter.NUMBER_TYPE);

  //participant columns
  public static ONC_HISTORY_CREATED = new Filter( ParticipantColumn.ONC_HISTORY_CREATED, Filter.DATE_TYPE );
  public static ONC_HISTORY_REVIEWED = new Filter( ParticipantColumn.ONC_HISTORY_REVIEWED, Filter.DATE_TYPE );
  public static PAPER_CR_SENT = new Filter( ParticipantColumn.PAPER_CR_SENT, Filter.DATE_TYPE );
  public static PAPER_CR_RECEIVED = new Filter( ParticipantColumn.PAPER_CR_RECEIVED, Filter.DATE_TYPE );
  public static PARTICIPANT_NOTES = new Filter( ParticipantColumn.PARTICIPANT_NOTES, Filter.TEXT_TYPE );
  public static MINIMAL_RECORDS = new Filter( ParticipantColumn.MINIMAL_RECORDS, Filter.CHECKBOX_TYPE );
  public static ABSTRACTION_READY = new Filter( ParticipantColumn.ABSTRACTION_READY, Filter.CHECKBOX_TYPE );
  public static ASSIGNEE_MR = new Filter( ParticipantColumn.ASSIGNEE_MR, Filter.OPTION_TYPE );
  public static ASSIGNEE_TISSUE = new Filter( ParticipantColumn.ASSIGNEE_TISSUE, Filter.OPTION_TYPE );
  public static EXIT_DATE = new Filter( ParticipantColumn.EXIT_DATE, Filter.DATE_TYPE );

  //mr columns
  public static MR_INSTITUTION_NAME = new Filter( ParticipantColumn.MR_INSTITUTION_NAME, Filter.TEXT_TYPE );
  public static MR_INSTITUTION_CONTACT = new Filter( ParticipantColumn.MR_INSTITUTION_CONTACT, Filter.TEXT_TYPE );
  public static MR_INSTITUTION_PHONE = new Filter( ParticipantColumn.MR_INSTITUTION_PHONE, Filter.TEXT_TYPE );
  public static MR_INSTITUTION_FAX = new Filter( ParticipantColumn.MR_INSTITUTION_FAX, Filter.TEXT_TYPE );
  public static MR_FAX_SENT = new Filter( ParticipantColumn.MR_FAX_SENT, Filter.DATE_TYPE );
  public static MR_FAX_SENT_2 = new Filter( ParticipantColumn.MR_FAX_SENT_2, Filter.DATE_TYPE );
  public static MR_FAX_SENT_3 = new Filter( ParticipantColumn.MR_FAX_SENT_3, Filter.DATE_TYPE );
  public static MR_RECEIVED = new Filter( ParticipantColumn.MR_RECEIVED, Filter.DATE_TYPE );
  public static MR_DOCUMENT = new Filter( ParticipantColumn.MR_DOCUMENT, Filter.OPTION_TYPE, [
    new NameValue( "Full", "Full" ),
    new NameValue( "Partial", "Partial" ) ] );
  public static MR_DOCUMENT_FILES = new Filter( ParticipantColumn.MR_DOCUMENT_FILES, Filter.TEXT_TYPE );
  public static MR_PROBLEM = new Filter( ParticipantColumn.MR_PROBLEM, Filter.CHECKBOX_TYPE );
  public static MR_PROBLEM_TEXT = new Filter( ParticipantColumn.MR_PROBLEM_TEXT, Filter.TEXT_TYPE );
  public static MR_UNABLE_TO_OBTAIN = new Filter( ParticipantColumn.MR_UNABLE_TO_OBTAIN, Filter.CHECKBOX_TYPE );
  public static MR_DUPLICATE = new Filter( ParticipantColumn.MR_DUPLICATE, Filter.CHECKBOX_TYPE );
  public static MR_INTERNATIONAL = new Filter( ParticipantColumn.MR_INTERNATIONAL, Filter.CHECKBOX_TYPE );
  public static MR_PAPER_CR = new Filter( ParticipantColumn.MR_PAPER_CR, Filter.CHECKBOX_TYPE );
  public static PATHOLOGY_RESENT = new Filter( ParticipantColumn.PATHOLOGY_RESENT, Filter.OPTION_TYPE, [
    new NameValue( "yes", "Yes" ),
    new NameValue( "no", "No" ) ] );
  public static MR_NOTES = new Filter( ParticipantColumn.MR_NOTES, Filter.TEXT_TYPE );
  public static MR_REVIEW = new Filter( ParticipantColumn.MR_REVIEW, Filter.OPTION_TYPE, [
    new NameValue( "yes", "Yes" ),
    new NameValue( "no", "No" ) ] );
  public static MR_FOLLOW_UP = new Filter( ParticipantColumn.MR_FOLLOW_UP, Filter.CHECKBOX_TYPE );

  //oncHistory columns
  public static ACCESSION_NUMBER = new Filter( ParticipantColumn.ACCESSION_NUMBER, Filter.TEXT_TYPE );
  public static DATE_PX = new Filter( ParticipantColumn.DATE_PX, Filter.DATE_TYPE );
  public static TYPE_PX = new Filter( ParticipantColumn.TYPE_PX, Filter.TEXT_TYPE );
  public static FACILITY = new Filter( ParticipantColumn.FACILITY, Filter.TEXT_TYPE );
  public static FACILITY_PHONE = new Filter( ParticipantColumn.FACILITY_PHONE, Filter.TEXT_TYPE );
  public static FACILITY_FAX = new Filter( ParticipantColumn.FACILITY_FAX, Filter.TEXT_TYPE );
  public static HISTOLOGY = new Filter( ParticipantColumn.HISTOLOGY, Filter.TEXT_TYPE );
  public static LOCATION_PX = new Filter( ParticipantColumn.LOCATION_PX, Filter.TEXT_TYPE );
  public static ONC_HISTORY_NOTES = new Filter( ParticipantColumn.ONC_HISTORY_NOTES, Filter.TEXT_TYPE );
  public static ONC_HISTORY_REQUEST = new Filter( ParticipantColumn.ONC_HISTORY_REQUEST, Filter.OPTION_TYPE, [
    new NameValue( "review", "Needs Review" ),
    new NameValue( "no", "Don't Request" ),
    new NameValue( "hold", "on Hold" ),
    new NameValue( "request", "Request" ),
    new NameValue( "unable To Obtain", "Unable To Obtain" ),
    new NameValue( "sent", "Sent" ),
    new NameValue( "received", "Received" ),
    new NameValue( "returned", "Returned" )
  ] );
  public static TISSUE_FAX = new Filter( ParticipantColumn.TISSUE_FAX, Filter.DATE_TYPE );
  public static TISSUE_FAX_2 = new Filter( ParticipantColumn.TISSUE_FAX_2, Filter.DATE_TYPE );
  public static TISSUE_FAX_3 = new Filter( ParticipantColumn.TISSUE_FAX_3, Filter.DATE_TYPE );
  public static TISSUE_RECEIVED = new Filter( ParticipantColumn.TISSUE_RECEIVED, Filter.DATE_TYPE );
  public static GENDER = new Filter( ParticipantColumn.GENDER, Filter.OPTION_TYPE, [
    new NameValue( "male", "Male" ),
    new NameValue( "female", "Female" ) ] );
  public static TISSUE_PROBLEM_OPTION = new Filter( ParticipantColumn.TISSUE_PROBLEM_OPTION, Filter.OPTION_TYPE, [
    new NameValue( "insufficientPath", "Insufficient material per path" ),
    new NameValue( "insufficientSHL", "Insufficient material per SHL" ),
    new NameValue( "noESign", "No e signatures" ),
    new NameValue( "pathPolicy", "Path department policy" ),
    new NameValue( "pathNoLocate", "Path department unable to locate" ),
    new NameValue( "destroyed", "Tissue destroyed" ),
    new NameValue( "other", "Other" ) ] );
  public static UNABLE_OBTAIN_TISSUE = new Filter( ParticipantColumn.UNABLE_OBTAIN_TISSUE, Filter.CHECKBOX_TYPE );
  public static DESTRUCTION_POLICY = new Filter( ParticipantColumn.DESTRUCTION_POLICY, Filter.TEXT_TYPE );

  //tissue participantColumn
  public static COUNT_RECEIVED = new Filter( ParticipantColumn.COUNT_RECEIVED, Filter.NUMBER_TYPE );
  public static TISSUE_NOTES = new Filter( ParticipantColumn.TISSUE_NOTES, Filter.TEXT_TYPE );
  public static TISSUE_TYPE = new Filter( ParticipantColumn.TISSUE_TYPE, Filter.OPTION_TYPE, [
    new NameValue( "block", "Block" ),
    new NameValue( "slide", "Slide" ),
    new NameValue( "scrolls", "Scrolls" ) ] );
  public static TISSUE_SITE = new Filter( ParticipantColumn.TISSUE_SITE, Filter.TEXT_TYPE );
  public static TUMOR_TYPE = new Filter( ParticipantColumn.TUMOR_TYPE, Filter.OPTION_TYPE, [
    new NameValue( "Primary", "Primary" ),
    new NameValue( "Met", "Met" ),
    new NameValue( "Recurrent", "Recurrent" ),
    new NameValue( "Unknown", "Unknown" ) ] );
  public static H_E = new Filter( ParticipantColumn.H_E, Filter.OPTION_TYPE, [
    new NameValue( "yes", "Yes" ),
    new NameValue( "no", "No" ) ] );
  public static PATHOLOGY_REPORT = new Filter( ParticipantColumn.PATHOLOGY_REPORT, Filter.OPTION_TYPE, [
    new NameValue( "yes", "Yes" ),
    new NameValue( "no", "No" ) ] );
  public static COLLABORATOR_SAMPLE_ID = new Filter( ParticipantColumn.COLLABORATOR_SAMPLE_ID, Filter.TEXT_TYPE );
  public static BLOCK_SENT = new Filter( ParticipantColumn.BLOCK_SENT, Filter.DATE_TYPE );
  public static SCROLL_RECEIVED = new Filter( ParticipantColumn.SCROLL_RECEIVED, Filter.DATE_TYPE );
  public static SK_ID = new Filter( ParticipantColumn.SK_ID, Filter.TEXT_TYPE );
  public static SM_ID = new Filter( ParticipantColumn.SM_ID, Filter.TEXT_TYPE );
  public static SENT_GP = new Filter( ParticipantColumn.SENT_GP, Filter.DATE_TYPE );
  public static TISSUE_RETURNED = new Filter( ParticipantColumn.TISSUE_RETURNED, Filter.DATE_TYPE );
  public static TISSUE_EXPECTED_RETURN = new Filter( ParticipantColumn.TISSUE_EXPECTED_RETURN, Filter.DATE_TYPE );
  public static TISSUE_TRACKING_NUMBER = new Filter( ParticipantColumn.TISSUE_TRACKING_NUMBER, Filter.TEXT_TYPE );
  public static TISSUE_SHL_NUMBER = new Filter( ParticipantColumn.TISSUE_SHL_NUMBER, Filter.TEXT_TYPE );
  public static TISSUE_FIRST_SM_ID = new Filter( ParticipantColumn.TISSUE_FIRST_SM_ID, Filter.TEXT_TYPE );
  public static TISSUE_TUMOR_PERCENT = new Filter( ParticipantColumn.TISSUE_TUMOR_PERCENT, Filter.TEXT_TYPE );
  public static TISSUE_SEQUENCE = new Filter( ParticipantColumn.TISSUE_SEQUENCE, Filter.OPTION_TYPE, [
    new NameValue( "success", "Success" ),
    new NameValue( "failureSHL", "Failure at SHL" ),
    new NameValue( "abandonedGP", "Abandoned at GP" )
  ] );
  public static SCROLLS_COUNT = new Filter( ParticipantColumn.SCROLLS_COUNT, Filter.NUMBER_TYPE );
  public static BLOCKS_COUNT = new Filter( ParticipantColumn.BLOCKS_COUNT, Filter.NUMBER_TYPE );
  public static USS_COUNT = new Filter( ParticipantColumn.USS_COUNT, Filter.NUMBER_TYPE );
  public static H_E_COUNT = new Filter( ParticipantColumn.H_E_COUNT, Filter.NUMBER_TYPE );

  //sample columns
  public static SAMPLE_TYPE = new Filter( ParticipantColumn.SAMPLE_TYPE, Filter.OPTION_TYPE, [
    new NameValue( "SALIVA", "Saliva" ),
    new NameValue( "BLOOD", "Blood" ) ] );
  public static SAMPLE_SENT = new Filter( ParticipantColumn.SAMPLE_SENT, Filter.DATE_TYPE );
  public static COLLABORATOR_SAMPLE = new Filter( ParticipantColumn.COLLABORATOR_SAMPLE, Filter.TEXT_TYPE );
  public static SAMPLE_RECEIVED = new Filter( ParticipantColumn.SAMPLE_RECEIVED, Filter.DATE_TYPE );
  public static SAMPLE_DEACTIVATION = new Filter( ParticipantColumn.SAMPLE_DEACTIVATION, Filter.DATE_TYPE );
  public static SAMPLE_QUEUE = new Filter( ParticipantColumn.SAMPLE_QUEUE, Filter.OPTION_TYPE, [
    new NameValue( "queue", "Waiting on GP" ),
    new NameValue( "error", "GP manual Label" ),
    new NameValue( "deactivated", "Deactivated" ),
    new NameValue( "sent", "Sent" ),
    new NameValue( "received", "Received" ) ], null, false, true, null, null, null, null, true, false, false, true );
  public static TRACKING_TO_PARTICIPANT = new Filter( ParticipantColumn.TRACKING_TO_PARTICIPANT, Filter.TEXT_TYPE );
  public static TRACKING_RETURN = new Filter( ParticipantColumn.TRACKING_RETURN, Filter.TEXT_TYPE );
  public static MF_BARCODE = new Filter( ParticipantColumn.MF_BARCODE, Filter.TEXT_TYPE );

  //abstraction
  public static ABSTRACTION_STATUS = new Filter( ParticipantColumn.ABSTRACTION_STATUS, Filter.OPTION_TYPE, [
    new NameValue( "done", "Done" ),
    new NameValue( "in_progress", "In Progress" ),
    new NameValue( "not_started", "Not Started" ),
    new NameValue( "clear", "Lock Broken" ) ] );
  public static ABSTRACTION_ACTIVITY = new Filter( ParticipantColumn.ABSTRACTION_ACTIVITY, Filter.OPTION_TYPE, [
    new NameValue( "abstraction", "First Abstraction" ),
    new NameValue( "review", "Second Abstraction" ),
    new NameValue( "qc", "QC" ) ] );
  public static ABSTRACTION_USER = new Filter( ParticipantColumn.ABSTRACTION_USER, Filter.TEXT_TYPE );

  public static ALL_COLUMNS = [
    Filter.REALM, Filter.SHORT_ID, Filter.LEGACY_SHORT_ID, Filter.LEGACY_PARTICIPANT_ID, Filter.PARTICIPANT_ID, Filter.FIRST_NAME, Filter.LAST_NAME,
    Filter.COUNTRY, Filter.ENROLLMENT_STATUS, Filter.EMAIL, Filter.REGISTRATION_DATE, Filter.DO_NOT_CONTACT,
    Filter.DATE_OF_MAJORITY, Filter.HAS_CONSENTED_TO_BLOOD, Filter.HAS_CONSENTED_TO_TISSUE, Filter.DATE_OF_BIRTH,
    Filter.DIAGNOSIS_MONTH, Filter.DIAGNOSIS_YEAR,
    Filter.ONC_HISTORY_CREATED, Filter.ONC_HISTORY_REVIEWED, Filter.PAPER_CR_SENT, Filter.PAPER_CR_RECEIVED,
    Filter.PARTICIPANT_NOTES, Filter.MINIMAL_RECORDS, Filter.ABSTRACTION_READY, Filter.ASSIGNEE_MR, Filter.ASSIGNEE_TISSUE, Filter.EXIT_DATE,
    Filter.MR_INSTITUTION_NAME, Filter.MR_INSTITUTION_CONTACT, Filter.MR_INSTITUTION_PHONE, Filter.MR_INSTITUTION_FAX,
    Filter.MR_FAX_SENT, Filter.MR_FAX_SENT_2, Filter.MR_FAX_SENT_3, Filter.MR_RECEIVED,
    Filter.MR_DOCUMENT, Filter.MR_DOCUMENT_FILES, Filter.MR_PROBLEM, Filter.MR_PROBLEM_TEXT, Filter.MR_UNABLE_TO_OBTAIN,
    Filter.MR_DUPLICATE, Filter.MR_INTERNATIONAL, Filter.MR_PAPER_CR, Filter.PATHOLOGY_RESENT, Filter.MR_NOTES, Filter.MR_REVIEW,
    Filter.MR_FOLLOW_UP,
    Filter.ACCESSION_NUMBER, Filter.DATE_PX, Filter.FACILITY, Filter.FACILITY_PHONE,
    Filter.FACILITY_FAX, Filter.HISTOLOGY, Filter.LOCATION_PX, Filter.ONC_HISTORY_NOTES,
    Filter.ONC_HISTORY_REQUEST, Filter.TISSUE_FAX, Filter.TISSUE_FAX_2, Filter.TISSUE_FAX_3,
    Filter.TISSUE_RECEIVED, Filter.TYPE_PX,
    Filter.GENDER, Filter.TISSUE_PROBLEM_OPTION, Filter.UNABLE_OBTAIN_TISSUE, Filter.DESTRUCTION_POLICY,
    Filter.COUNT_RECEIVED, Filter.TISSUE_TYPE,
    Filter.TISSUE_SITE, Filter.TUMOR_TYPE, Filter.TISSUE_NOTES, Filter.H_E,
    Filter.COLLABORATOR_SAMPLE_ID, Filter.BLOCK_SENT, Filter.PATHOLOGY_REPORT,
    Filter.SCROLL_RECEIVED, Filter.SK_ID, Filter.SM_ID, Filter.SENT_GP,
    Filter.TISSUE_EXPECTED_RETURN, Filter.TISSUE_RETURNED, Filter.TISSUE_TRACKING_NUMBER,
    Filter.TISSUE_FIRST_SM_ID, Filter.TISSUE_SHL_NUMBER, Filter.TISSUE_TUMOR_PERCENT, Filter.TISSUE_SEQUENCE, Filter.SCROLLS_COUNT,
    Filter.USS_COUNT, Filter.H_E_COUNT, Filter.BLOCKS_COUNT,
    Filter.COLLABORATOR_SAMPLE, Filter.SAMPLE_TYPE, Filter.SAMPLE_SENT, Filter.SAMPLE_RECEIVED, Filter.SAMPLE_DEACTIVATION, Filter.SAMPLE_QUEUE,
    Filter.TRACKING_TO_PARTICIPANT, Filter.TRACKING_RETURN, Filter.MF_BARCODE,
    Filter.ABSTRACTION_ACTIVITY, Filter.ABSTRACTION_STATUS, Filter.ABSTRACTION_USER ];

  constructor(public participantColumn: ParticipantColumn, public type: string, public options?: NameValue[], public filter2?: NameValue,
              public range?: boolean, public exactMatch?: boolean, public filter1?: NameValue,
              public selectedOptions?: any, public value1?: any, public value2?: any, public sortAsc?: boolean,
              public empty?: boolean, public notEmpty?: boolean, public singleOption?: boolean, public additionalType?: string,
              public parentName?: string) {
    this.participantColumn = participantColumn;
    this.type = type;
    if (options != null) {
      this.options = options;
      if (selectedOptions == null || selectedOptions == undefined) {
        this.selectedOptions = [];
        for (let i = 0; i < this.options.length; ++i) {
          this.selectedOptions.push( false );
        }
      }
    }
    this.range = range;
    this.exactMatch = exactMatch;
    if (exactMatch == undefined || exactMatch == null) {
      this.exactMatch = true;
    }
    this.filter1 = filter1 == undefined ? null : filter1;
    this.filter2 = filter2;
    this.value1 = value1 == undefined ? null : value1;
    this.value2 = value2 == undefined ? null : value2;
    this.sortAsc = sortAsc;
    this.empty = empty;
    this.notEmpty = notEmpty;
    this.singleOption = singleOption;
    this.additionalType = additionalType;
    this.parentName = parentName;
  }

  public getSelectedOptionsName(): Array<string> {
    let selected = [];
    for (let [ key, value ] of Object.entries( this.selectedOptions )) {
      if (value) {
        selected.push( this.options[ key ].name );
      }
    }
    return selected;
  }

  public getSelectedOptionsBoolean(): Array<boolean> {
    let selected = [];
    if (this.selectedOptions != null && this.options != undefined) {
      for (let o of this.options) {
        selected.push( this.selectedOptions.includes( o.name ) );
      }
    }
    return selected;
  }

  public static parseToColumnArray( json, allColumns, surveyNames?, surveyColumns? ): {} {
    let result = {};
    for (let tableAlias of Object.keys( json )) {
      let colArray = json[ tableAlias ];
      let found = false;
      loop: for (let columnName of colArray) {
        if (tableAlias === "ES") { //backend will sent "ES" if it didn't find the columns in the dsm tables!
          //get profile column
          if (columnName.indexOf( Filter.SHORT_ID.participantColumn.tableAlias ) == 0) {
            for (let filter of Filter.ALL_COLUMNS) {
              let tableName = columnName.substr( 0, columnName.indexOf( "." ) );
              let cName = columnName.substr( columnName.indexOf( "." ) + 1 );
              if (cName === filter.participantColumn.name && tableName === filter.participantColumn.tableAlias) {
                if (result[ filter.participantColumn.tableAlias ] == undefined || result[ filter.participantColumn.tableAlias ] == null) {
                  result[ filter.participantColumn.tableAlias ] = [];
                }
                result[ filter.participantColumn.tableAlias ].push( filter );
                found = true;
                continue loop;
              }
            }
          }
          else if (columnName.includes( "." )) {
            let tableName = columnName.substr( 0, columnName.indexOf( "." ) );
            let cName = columnName.substr( columnName.indexOf( "." ) + 1 );
            if (allColumns[ tableName ] != undefined) {
              let f = allColumns[ tableName ].find( filter => {
                return filter.participantColumn.tableAlias === tableName && filter.participantColumn.name === cName;
              } );
              if (f != undefined) {
                if (result[ tableName ] == null || result[ tableName ] == undefined) {
                  result[ tableName ] = new Array();
                }
                result[ tableName ].push( f );
                found = true;
                continue loop;
              }
            }
          }
          else {
            //none profile columns (survey columns)
            if (!found && surveyNames != null && surveyColumns != null) {
              for (let survey of surveyNames) {
                for (let col of surveyColumns[ survey ]) {
                  if (col.participantColumn.name === columnName) {
                    if (result[ survey ] == undefined || result[ survey ] == null) {
                      result[ survey ] = [];
                    }
                    result[ survey ].push( col );
                    found = true;
                    continue loop;
                  }
                }
              }
            }
          }
        }
        for (let key of Object.keys( allColumns )) {
          for (let filter of allColumns[ key ]) {
            if (filter.participantColumn.name === columnName && filter.participantColumn.tableAlias === tableAlias) {
              if (result[ key ] == undefined || result[ key ] == null) {
                result[ key ] = [];
              }
              result[ key ].push( filter );
              continue loop;
            }
          }
        }
      }
    }
    return result;
  }

  public static parseToCurrentFilterArray(json, allColumns): Filter[] {
    if (json.filters == undefined) {
      return null;
    }
    let filters: Filter[] = [];
    for (let filter of json.filters) {
      if (allColumns[ filter.participantColumn.tableAlias ] != undefined) {
        let f = allColumns[ filter.participantColumn.tableAlias ].find( f => {
          return f.participantColumn.tableAlias === filter.participantColumn.tableAlias && f.participantColumn.name === filter.participantColumn.name;
        } );
        if (f != undefined) {
          filter.type = f.type;
          filter.participantColumn = f.participantColumn;
          let selectedOptions = new Array();
          if (filter.selectedOptions != null && f.options != undefined) {
            for (let o of f.options) {
              selectedOptions.push( filter.selectedOptions.includes( o.name ) );
            }
          }
          if (filter.filter1 == undefined) {
            filter.filter1 = new NameValue(f.participantColumn.name, null);
          }
          if (filter.filter1.value != null) {
            filter.filter1.value = this.replace(filter.filter1.value);
          }
          let newFilter = new Filter(filter.participantColumn, filter.type, f.options, filter.filter2, filter.range, filter.exactMatch, filter.filter1,
            selectedOptions, (filter.filter1 == null || filter.filter1 == undefined) ? null : filter.filter1.value,
            (filter.filter2 == null || filter.filter2 == undefined) ? null : filter.filter2.value, null, filter.empty,
            filter.notEmpty, f.singleOption, f.additionalType);
          filters.push(newFilter);
        }

      }
      else {
        for (let source of Object.keys(allColumns)) {
          let f = allColumns[source].find(f => {
            return f.participantColumn.name === filter.participantColumn.name;
          });
          if (f != undefined) {
            filter.type = f.type;
            filter.participantColumn = f.participantColumn;
            filter.parentName = f.participantColumn.object;
            let selectedOptions = new Array();
            if (filter.selectedOptions != null && f.options != undefined) {
              for (let o of f.options) {
                selectedOptions.push( filter.selectedOptions.includes( o.name ) );
              }
            }
            filter.filter1.value = this.replace( filter.filter1.value );
            let newFilter = new Filter( filter.participantColumn, filter.type, f.options, filter.filter2, filter.range, filter.exactMatch, filter.filter1,
              selectedOptions, ( filter.filter1 == null || filter.filter1 == undefined ) ? null : filter.filter1.value,
              ( filter.filter2 == null || filter.filter2 == undefined ) ? null : filter.filter2.value, null,
              filter.empty, filter.notEmpty, f.singleOption, f.additionalType );
            filters.push( newFilter );
            break;
          }

        }
      }
    }
    for (let filter of filters) {
      if (filter.participantColumn.object !== undefined && filter.participantColumn.object !== null && filter.participantColumn.object !== "") {
        filter.parentName = filter.participantColumn.object;
      }

    }
    return filters;
  }

  private static replace( value: string ): string {
    if (value != null && typeof value === "string" && value.indexOf( "'" ) > -1) {
      value = value.replace( "'", "" );
      return this.replace( value );
    }
    return value;
  }

  public clearFilter() {
    this.selectedOptions = [];
    if (this.options != undefined) {
      for (let o of this.options) {
        this.selectedOptions.push( false );
      }
    }
    this.range = false;
    this.exactMatch = true;
    this.filter1 = new NameValue( null, null );
    this.filter2 = new NameValue( this.filter2 == null ? null : this.filter2.name, null );
    this.value1 = null;
    this.value2 = null;
    this.sortAsc = true;
    this.empty = false;
    this.notEmpty = false;

    return this;
  }

  public static getFilterText( filter: Filter, parent: string, fieldSettings: {} ): {} {

    let filterText = {};
    if (filter.type === Filter.TEXT_TYPE || filter.type === Filter.NUMBER_TYPE || filter.type === Filter.DATE_TYPE || filter.type === Filter.EPOCH_DATE_TYPE
      || filter.type === Filter.COMPOSITE_TYPE || filter.type === Filter.SHORT_DATE_TYPE ) {
      if ((filter.value1 !== null && filter.value1 !== "" && filter.value1 !== undefined) ||
        (filter.range && filter.value2 != null && filter.value2 != "" && filter.value2 !== undefined) || (filter.empty || filter.notEmpty)) {
        if (filter.value2 != undefined && filter.value2 != null) {
          if (filter.filter2 != undefined && filter.filter2 != null) {
            filter.filter2.value = filter.value2;
          }
          else {
            filter.filter2 = new NameValue( filter.participantColumn.name, null );
            //            filter.filter2.name = filter.participantColumn.name;
          }
        }
        let nv: NameValue;
        if (filter.type === Filter.EPOCH_DATE_TYPE) {
          let epoch = new Date(filter.value1).getTime();
          nv = new NameValue(filter.participantColumn.name, epoch + "");
        }
        else {
          nv = new NameValue(filter.participantColumn.name, filter.value1);
        }
        filterText = this.getFilterJson(parent, nv, filter.filter2, null,
          filter.exactMatch, filter.type, filter.range, filter.empty, filter.notEmpty, filter.participantColumn);

      }
      else {
        return null;
      }
    }
    else if (filter.type === Filter.OPTION_TYPE) {
      let selected = [];
      for (let [ key, value ] of Object.entries( filter.selectedOptions )) {
        if (value) {
          selected.push( filter.options[ key ].name );
        }
      }
      if (selected.length > 0 || filter.empty || filter.notEmpty) {
        filterText = this.getFilterJson( parent, new NameValue( filter.participantColumn.name, filter.value1 ), new NameValue( filter.participantColumn.name, filter.value2 ), selected,
          true, filter.type, filter.range, filter.empty, filter.notEmpty, filter.participantColumn );

      }
      else {
        return null;
      }
    }
    else if (filter.type === Filter.ADDITIONAL_VALUE_TYPE) {
      if (( filter.value1 !== null && filter.value1 !== "" && filter.value1 !== undefined ) || ( filter.empty || filter.notEmpty )) {
        filterText = this.getFilterJson( parent,
          new NameValue( "additionalValues", filter.value1 ),
          filter.filter2, null,
          filter.exactMatch, filter.type, filter.range, filter.empty, filter.notEmpty, filter.participantColumn );
      }
      else {
        return null;
      }
    }
    else if (filter.type === Filter.BOOLEAN_TYPE || filter.type === Filter.CHECKBOX_TYPE) {
      if (( filter.value1 !== null && filter.value1 == true )) {
        filterText = this.getFilterJson( parent,
          new NameValue( filter.participantColumn.name, "true" ),
          filter.filter2, null,
          filter.exactMatch, filter.type, false, filter.empty, filter.notEmpty, filter.participantColumn );
      }
      else if (( filter.value2 !== null && filter.value2 == true )) {
        filterText = this.getFilterJson( parent,
          new NameValue( filter.participantColumn.name, null ),
          new NameValue( filter.participantColumn.name, "true" ), null,
          filter.exactMatch, filter.type, false, filter.empty, filter.notEmpty, filter.participantColumn );
      }
      else {
        return null;
      }
    }
    if (filterText != null && filter.participantColumn.tableAlias === Statics.ES_ALIAS && filter.participantColumn.object !== undefined && filter.participantColumn.object !== null) {
      filterText["exactMatch"] = true;
      filterText["parentName"] = filter.participantColumn.object;
    }
    return filterText;
  }

  public static parseFieldSettingsToColumns( fieldSetting: FieldSettings, tableAlias ): Filter {
    let options: Array<NameValue> = null;
    if (fieldSetting.displayType === "OPTIONS") {
      options = new Array<NameValue>();
      fieldSetting.possibleValues.forEach( ( value: Value ) => {
        options.push( new NameValue( value.value, value.value ) );
      } );
    }
    let filter = new Filter( new ParticipantColumn( fieldSetting.columnDisplay, fieldSetting.columnName, tableAlias ), Filter.ADDITIONAL_VALUE_TYPE, options, new NameValue( fieldSetting.columnName, null ),
      false, true, null, null, null, null, false, false, false, false, fieldSetting.displayType );
    return filter;
  }

  public static getFilterJson( parent, filter1, filter2, selectedOptions, exact, type, range, empty, notEmpty, participantColumn ) {
    let filterText = {
      "parentName": parent,
      "filter1": filter1,
      "filter2": filter2,
      "exactMatch": exact,
      "selectedOptions": selectedOptions,
      "type": type,
      "range": range,
      "empty": empty,
      "notEmpty": notEmpty,
      "participantColumn": participantColumn,
    };
    return filterText;
  }

}
