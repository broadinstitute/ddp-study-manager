import {Injectable} from "@angular/core";
import {DatePipe} from "@angular/common";
import {FormControl} from "@angular/forms";
import {AbstractionGroup} from "../abstraction-group/abstraction-group.model";
import {ActivityDefinition} from "../activity-data/models/activity-definition.model";
import {OptionDetail} from "../activity-data/models/option-detail.model";
import {Option} from "../activity-data/models/option.model";
import {QuestionAnswer} from "../activity-data/models/question-answer.model";
import {Filter} from "../filter-column/filter-column.model";
import {AbstractionField} from "../medical-record-abstraction/medical-record-abstraction-field.model";
import {Participant} from "../participant-list/participant-list.model";
import {Tissue} from "../tissue/tissue.model";
import {OncHistoryDetail} from "../onc-history-detail/onc-history-detail.model";
import {NameValue} from "./name-value.model";
import {DateFormatPipe} from '../pipe/custom-date.pipe';

var fileSaver = require( "file-saver/filesaver.js" );
const Json2csvParser = require( "json2csv" ).Parser;

@Injectable()
export class Utils {
  //methods which might be useful for other components as well

  static PARTIAL_DATE_STRING: string = "yyyy-MM";
  static DATE_STRING: string = "yyyy-MM-dd";
  static DATE_STRING_CVS: string = "MMddyy";
  static DATE_STRING_IN_CVS: string = "MM/dd/yyyy";
  static DATE_STRING_IN_EVENT_CVS: string = "MMM dd, yyyy, hh:mm:ss a";
  static DATE_PARTIAL: string = "partial date";

  YES: string = "Yes";
  NO: string = "No";

  public getDateFormatted( str ): string {
    return Utils.getDateFormatted( this.getNiceUserDate( str ), Utils.DATE_STRING_IN_CVS );
  }

  public getYesNo( value: any ): string {
    if (value != null) {
      if (value === 1 || value === "1" || value === "true" || value) {
        return this.YES;
      }
      else {
        return this.NO;
      }
    }
    return "-";
  }

  //get date of string
  public getNiceUserDate( dateString: string ): Date {
    if (dateString != null && typeof dateString === "string" && dateString.indexOf( "-" ) > 0) {
      let dateParts: string[] = dateString.split( "-" );
      if (dateParts.length == 3) {
        let check: Date = null;
        if (dateParts[ 0 ].length == 4) {
          check = new Date( Number( dateParts[ 0 ] ), Number( dateParts[ 1 ] ) - 1, Number( dateParts[ 2 ] ) );
        }
        else if (dateParts[ 2 ].length == 4) {
          check = new Date( Number( dateParts[ 2 ] ), Number( dateParts[ 0 ] ) - 1, Number( dateParts[ 1 ] ) );
        }
        if (check != null && !isNaN( check.getTime() )) {
          return check;
        }
      }
      else if (dateParts.length == 2) {
        let check: Date = new Date( Number( dateParts[ 0 ] ), Number( dateParts[ 1 ] ) - 1 );
        if (!isNaN( check.getTime() )) {
          return check;
        }
      }
    }
    else {
      let check: Date = new Date( Number( dateString ) );
      if (!isNaN( check.getTime() )) {
        return check;
      }
    }
    return null;
  }

  public getNiceUserText( text: string ) {
    if (text != null && text.indexOf( "-null" ) > -1) {
      return text.replace( "-null", "" ).replace( "-null", "" );
    }
    return text;
  }

  public getNameValue( nameValues: Array<NameValue>, text: string ) {
    let nameValue = nameValues.find( x => x.name === text );
    if (nameValue != null) {
      return nameValue.value;
    }
    return "";
  }

  isOptionSelected( selected: Array<string>, optionStableId: string ) {
    return selected.find( x => x === optionStableId );
  }

  getOptionText( options: Array<Option>, stableId: string ) {
    let option = options.find( x => x.optionStableId === stableId );
    if (option != null) {
      return option.optionText;
    }
    return "";
  }

  getOptionDetails( optionDetails: Array<OptionDetail>, stableId: string ) {
    return optionDetails.find( x => x.option === stableId );
  }

  getQuestionDefinition( activities: Array<ActivityDefinition>, activity: string, stableId: string ) {
    let questions = activities.find( x => x.activityCode === activity ).questions;
    return questions.find( x => x.stableId === stableId );
  }

  getAbstractionGroup( groups: Array<AbstractionGroup>, groupId: string ) {
    return groups.find( x => x.abstractionGroupId.toString() === groupId );
  }

  getAbstractionField( fields: Array<AbstractionField>, fieldId: string ) {
    return fields.find( x => x.medicalRecordAbstractionFieldId.toString() === fieldId );
  }

  public static getFormattedDate( date: Date ): string {
    return Utils.getDateFormatted( date, Utils.DATE_STRING );
  }

  public static getDateFormatted( date: Date, format: string ): string {
    if (date instanceof Date && !isNaN(date.getTime())) {
      if (format != null) {
        return new DatePipe( "en-US" ).transform( date, format );
      }
      return new DatePipe( "en-US" ).transform( date, Utils.DATE_STRING_IN_CVS );
    }
    if (date != null) {
      return date.toString();
    }
    return "";
  }

  public maxDate(): Date {
    return new Date();
  }

  //used to get db saved date string (yyyy-MM-dd) as date
  public static getDate( dateString: string ): Date {
    if (dateString.indexOf( "-" ) > 0) {
      var dateParts: string[] = dateString.split( "-" );
      if (dateParts.length == 3) {
        return new Date( Number( dateParts[ 0 ] ), Number( dateParts[ 1 ] ) - 1, Number( dateParts[ 2 ] ) );
      }
    }
    return null;
  }

  //used to change db saved date (yyyy-MM) to user format
  public static getPartialFormatDate( dateString: string, format: string ) {
    if (Utils.DATE_STRING === format) {
      return dateString;
    }
    else if (Utils.DATE_STRING_IN_CVS === format) {
      if (dateString.indexOf( "/" ) > -1) {
        return dateString;
      }
      else {
        var dateParts: string[] = dateString.split( "-" );
        return dateParts[ 1 ] + "/" + dateParts[ 0 ];
      }
    }
  }

  public static downloadCurrentData( data: any[], paths: any[], columns: {}, fileName: string, isSurveyData ?: boolean ) {
    let headers = "";
    for (let path of paths) {
      for (let i = 1; i < path.length; i += 2) {
        let source = path[ i ];
        if (columns[ source ] != null) {
          for (let name of columns[ source ]) {
            let headerColumnName = name.participantColumn.display;
            if (isSurveyData) {
              headerColumnName = name.participantColumn.name;
            }
            headers += headerColumnName + ",";
          }
        }
      }
    }
    let csv = this.makeCSV(data, paths, columns);
    csv = headers + "\r\n" + csv;
    let blob = new Blob( [ csv ], {type: "text/csv;charset=utf-8;"} );
    if (navigator.msSaveBlob) { // IE 10+
      navigator.msSaveBlob( blob, fileName );
    }
    else {
      let link = document.createElement( "a" );
      if (link.download !== undefined) { // feature detection
        // Browsers that support HTML5 download attribute
        let url = URL.createObjectURL( blob );
        link.setAttribute( "href", url );
        link.setAttribute( "download", fileName );
        link.style.visibility = "hidden";
        document.body.appendChild( link );
        link.click();
        document.body.removeChild( link );
      }
    }
  }

  private static makeCSV (data: any[], paths: any[], columns: {}): string {
    let input = [];
    let result = [];
    for (let d of data) {
      let input = [];
      for (let path of paths) {
        let output = this.makeCSVForObjectArray(d, path, columns, 0);
        let temp = [];

        for (let o of output) {
          for (let i of input) {
            temp.push( i + o );
          }
        }
        if (input.length == 0) {
          temp = output;
        }
        input = temp;
      }
      result = result.concat( input );
    }
    let mainStr = result.join( "\r\n" );
    return mainStr;
  }


  public static makeCSVForObjectArray (data: Object, paths: any[], columns: {}, index: number): string[] {
    let result: string[] = [];
    if (index > paths.length - 1) {
      return null;
    }
    else {
      let objects = null;
      if (!( data[ paths[ index ] ] instanceof Array )) {
        objects = [ data[ paths[ index ] ] ];
      }
      else {
        objects = data[ paths[ index ] ];
      }
      if (objects != null) {
        for (let o of objects) {
          let oString = this.makeCSVString(o, columns[paths[index + 1]], data);
          let a = this.makeCSVForObjectArray(o, paths, columns, index + 2);
          if (a != null && a.length > 0) {
            for (let t of a) {
              result.push( oString + t );
            }
          }
          else {
            result.push( oString );
          }
        }
        if (objects.length == 0) {
          let oString = this.makeCSVString(null, columns[paths[index + 1]]);
          result.push( oString );
        }
      }
      return result;
    }
  }

  private static getObjectAdditionalValue( o: Object, fieldName: string, column: any ) {
    if (o[ fieldName ] != null) {
      return o[fieldName][column.participantColumn.name];
      // for (let nv of o[ fieldName ]) {
      //   if (nv.name === column.participantColumn.name) {
      //     return nv.value;
      //   }
      // }
    }
    return "";
  }

  private static makeCSVString(o: Object, columns: any[], data?: any): string {
    let str = "";
    let col: Filter;
    if (columns != null) {
      if (o != null) {
        for (col of columns) {
          if (col.type === "ADDITIONALVALUE") {
            let fieldName = "additionalValues";
            if (fieldName !== "") {
              let value = this.getObjectAdditionalValue (o, fieldName, col);
              value = value == undefined ? "" : value;
              str = str + "\"" + value + "\"" + ",";
            }
          }
          else if (col.participantColumn.object != null && col.participantColumn.object === "final" && o instanceof AbstractionGroup && col.participantColumn.tableAlias === o[ "abstractionGroupId" ].toString()) {
            if (o[ "fields" ] != null && o[ "fields" ] instanceof Array) {
              let abstractionField: AbstractionField = o[ "fields" ].find( field => {
                return field.medicalRecordAbstractionFieldId.toString() === col.participantColumn.name;
              } );
              if (abstractionField != null && abstractionField.fieldValue != null) {
                let value = abstractionField.fieldValue.value;
                value = value == undefined ? "" : value;
                if (value !== "") {
                  let tmp = "";
                  let multiObject: Object[] = this.getMultiObjects( value );
                  multiObject.forEach( ( object ) => {
                    let keys: string[] = this.getMultiKeys( object );
                    keys.forEach( ( key ) => {
                      if (key === "other") {
                        object[ key ].forEach( ( oV ) => {
                          let otherKeys: string[] = this.getMultiKeys( oV );
                          otherKeys.forEach( ( otherKey ) => {
                            let tmp2 = oV[ otherKey ] == undefined ? "" : oV[ otherKey ];
                            tmp = tmp + "Other - " + otherKey + ": " + tmp2 + " ";
                          } );
                        } );
                      }
                      else {
                        if (this.isDateValue( object[ key ] )) {
                          let tmp2 = this.getDateValue( object[ key ] ) == undefined ? "" : this.getDateValue( object[ key ] );
                          tmp = tmp + key + ": " + tmp2 + " ";
                        }
                        else {
                          let tmp2 = object[ key ] == undefined ? "" : object[ key ];
                          tmp = tmp + key + ": " + tmp2 + " ";
                        }
                      }
                    } );
                  } );
                  value = tmp.trim();
                }
                str = str + "\"" + value + "\"" + ","; //TODO make answer pretty
              }
            }
          }
          else {
            let value = o[ col.participantColumn.name ];
            if (col.participantColumn.object != null && o[ col.participantColumn.object ] != null) {
              value = o[ col.participantColumn.object ][ col.participantColumn.name ];
            }
            if (col.type === Filter.DATE_TYPE) {
              value = this.getDateFormatted(value, Utils.DATE_STRING_IN_CVS);
            }
            value = value == undefined ? "" : value;
            str = str + "\"" + value + "\"" + ",";
          }
        }
      }
      else {
        for (col of columns) {
          let value = "";
          if (data != null) {
            //check for survey data
            let activityData = this.getSurveyData( data, col.participantColumn.tableAlias );
            if (activityData != null) {
              if (( col.participantColumn.name === "createdAt" || col.participantColumn.name === "completedAt"
                || col.participantColumn.name === "lastUpdatedAt" ) && activityData[ col.participantColumn.name ] != null) {
                value = this.getDateFormatted( new Date( activityData[ col.participantColumn.name ] ), this.DATE_STRING_IN_CVS );
              }
              else if (col.participantColumn.name === "status" && activityData[ col.participantColumn.name ] != null) {
                value = activityData[ col.participantColumn.name ];
              }
              else {
                let questionAnswer = this.getQuestionAnswerByName( activityData.questionsAnswers, col.participantColumn.name );
                if (questionAnswer != null) {
                  if (col.type === "DATE") {
                    value = questionAnswer.date;
                  }
                  else {
                    value = questionAnswer.answer; //TODO react to what kind of answer it is and make pretty
                  }
                }
              }
            }
          }
          str = str + "\"" + value + "\"" + ",";
        }
      }
    }
    return str;
  }

  public static getSurveyData( participant: Participant, code: string ) {
    if (participant != null && participant.data != null && participant.data.activities != null) {
      return participant.data.activities.find( x => x.activityCode === code );
    }
    return null;
  }

  public static getQuestionAnswerByName( questionsAnswers: Array<QuestionAnswer>, name: string ) {
    return questionsAnswers.find( x => x.stableId === name );
  }

  public static getMultiObjects( fieldValue: string | string[] ) {
    if (!( fieldValue instanceof Array )) {
      let o: any = JSON.parse( fieldValue );
      return o;
    }
    return null;
  }

  public static getMultiKeys( o: any ) {
    if (o != null) {
      return Object.keys( o );
    }
    return null;
  }

  public static isDateValue( value: string ): boolean {
    if (value != null && value != undefined && typeof value === "string" && value.indexOf( "dateString" ) > -1 && value.indexOf( "est" ) > -1) {
      return true;
    }
    return false;
  }

  public static getDateValue( value: string ) {
    if (value != null) {
      let o: any = JSON.parse( value );
      return o[ "dateString" ];
    }
    return "";
  }

  public static createCSV( fields: any[], dataArray: Array<any>, fileName: string ) {
    const json2csvParser = new Json2csvParser( {fields} );
    const csv = json2csvParser.parse( dataArray );
    Utils.fileSaverCreateCSV( fileName, csv );
  }

  public static fileSaverCreateCSV( fileName: string, csv: any ) {
    let data = new Blob( [ csv ], {type: "text/plain;charset=utf-8"} );
    fileSaver.saveAs( data, fileName );
  }

  phoneNumberValidator( control: FormControl ) {
    let validationError = {"invalidPhoneNumber": true};
    if (!control || !control.value) {
      return null;
    }
    if (control.value.match( /^\d{3}-\d{3}-\d{4}$/ )) {
      return null;
    }
    return validationError;
  }

  public static parseDate( dateString: string, format: string, allowUnknownDay: boolean ): string | Date {
    if (dateString != null && format != null) {
      var dateParts: string[] = null;
      if (Utils.DATE_STRING_IN_CVS === format) {
        dateParts = dateString.split( "/" );
        if (dateParts.length == 3) {
          return this.changePartialYearTo2000( dateParts[ 2 ], dateParts[ 0 ], dateParts[ 1 ] );
        }
        else if (allowUnknownDay && dateParts.length == 2) {
          if (dateParts[ 1 ].length == 4 && dateParts[ 0 ].length < 3) {
            if (Number( dateParts[ 0 ] ) > -1 && Number( dateParts[ 0 ] ) < 12) {
              return Utils.DATE_PARTIAL;
            }
          }
        }
        else if (allowUnknownDay && dateParts.length == 1) {
          if (dateParts[ 0 ].length == 4) {
            return Utils.DATE_PARTIAL;
          }
        }
      }
      else if (Utils.DATE_STRING === format) {
        dateParts = dateString.split( "-" );
        if (dateParts.length == 3) {
          return this.changePartialYearTo2000( dateParts[ 0 ], dateParts[ 1 ], dateParts[ 2 ] );
        }
        else if (allowUnknownDay && dateParts.length == 2) {
          if (dateParts[ 0 ].length == 4 && dateParts[ 1 ].length < 3) {
            if (Number( dateParts[ 1 ] ) > -1 && Number( dateParts[ 1 ] ) < 12) {
              return Utils.DATE_PARTIAL;
            }
          }
        }
        else if (allowUnknownDay && dateParts.length == 1) {
          if (dateParts[ 0 ].length == 4) {
            return Utils.DATE_PARTIAL;
          }
        }
      }
    }
    return null;
  }

  private static changePartialYearTo2000( year: string, month: string, day: string ): Date {
    if (year.length < 2) {
      return new Date( Number( "200" + year ), Number( month ) - 1, Number( day ) );
    }
    else if (year.length < 3) {
      return new Date( Number( "20" + year ), Number( month ) - 1, Number( day ) );
    }
    else if (year.length < 4) {
      return new Date( Number( "2" + year ), Number( month ) - 1, Number( day ) );
    }
    else {
      return new Date( Number( year ), Number( month ) - 1, Number( day ) );
    }
  }

  public static getPartialDateFormatted( dateString: string, format: string ): string {
    if (format != null) {
      var dateParts: string[] = null;
      if (Utils.DATE_STRING_IN_CVS === format) {
        dateParts = dateString.split( "/" );
        if (dateParts.length == 2) {
          if (dateParts[ 1 ].length == 4 && dateParts[ 0 ].length < 3) {
            if (Number( dateParts[ 0 ] ) > -1 && Number( dateParts[ 0 ] ) < 12) {
              return dateParts[ 1 ] + "-" + dateParts[ 0 ];
            }
          }
        }
        else if (dateParts.length == 1) {
          if (dateParts[ 0 ].length == 4) {
            return dateParts[ 0 ];
          }
        }
      }
      else if (Utils.DATE_STRING === format) {
        dateParts = dateString.split( "-" );
        if (dateParts.length == 2) {
          if (dateParts[ 0 ].length == 4 && dateParts[ 1 ].length < 3) {
            if (Number( dateParts[ 1 ] ) > -1 && Number( dateParts[ 1 ] ) < 12) {
              return dateString;
            }
          }
        }
        else if (dateParts.length == 1) {
          if (dateParts[ 0 ].length == 4) {
            return dateParts[ 0 ];
          }
        }
      }
    }
    return null;
  }
}
