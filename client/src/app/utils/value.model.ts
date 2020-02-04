export class Value {

  newAdded: boolean = false;
  selected: boolean = true;

  constructor(public value?: string, public type?: string, public type2?: string, public name?: string, public values?: Value[]) {
    this.value = value;
    this.type = type;
    this.type2 = type2;
    this.name = name;
    this.values = values;
  }

  static parse( json ): Value {
    return new Value(json.value, json.type, json.type2, json.name, json.values);
  }
}
