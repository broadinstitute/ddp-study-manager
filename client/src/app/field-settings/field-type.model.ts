export class FieldType {
  selected: boolean = false;

  constructor(public name: string, public tableAlias: string) {
    this.name = name;
    this.tableAlias = tableAlias;
  }
}
