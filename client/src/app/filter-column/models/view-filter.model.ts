import {Filter} from "../filter-column.model";

export class ViewFilter {

  selected: boolean = false;

  constructor(public filters: Filter[], public filterName: string, public columns: {}, public shared: boolean,
              public deleted: string, public userId: string, public id: string, public parent: string, public icon: string,
              public quickFilterName: string, public queryItems: string, public message?: string) {
    this.filters = filters;
    this.filterName = filterName;
    this.columns = columns;
    this.deleted = deleted;
    this.shared = shared;
    this.userId = userId;
    this.id = id;
    this.parent = parent;
    this.quickFilterName = quickFilterName;
    this.queryItems = queryItems;
    this.message = null;
  }

  public static parseFilter( json, allColumns: {} ): ViewFilter {
    let columns = json.columns;
    let currentFilter = null;
    let parsedColumns: {} = Filter.parseToColumnArray( columns, allColumns );
    if (json.userId !== "System" && json.filters != undefined) {
      currentFilter = Filter.parseToCurrentFilterArray(json, allColumns);
      let p = {};
      for (let key of Object.keys( parsedColumns )) {
        let tmp = new Array<Filter>();
        for (let f of parsedColumns[ key ]) {
          let filteredColumn = currentFilter.find( filter => {
            return filter.participantColumn.name === f.participantColumn.name && filter.participantColumn.tableAlias === key;
          } );
          if (filteredColumn == undefined) {
            tmp.push( f );
          }
          else {
            tmp.push( filteredColumn );
          }
        }
        if (key === "o" || key === "ex"
          || key === "r") {
          p[ "p" ] = tmp;
        }
        else {
          p[ key ] = tmp;
        }
      }
      parsedColumns = p;
    }

    return new ViewFilter( currentFilter, json.filterName, parsedColumns, json.shared === "1" ? true : false, json.fDeleted, json.userId, json.id,
      json.parent, json.icon, json.quickFilterName, json.queryItems);
  }


}
