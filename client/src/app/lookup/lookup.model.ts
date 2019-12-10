import {NameValue} from "../utils/name-value.model";

export class Lookup {

  constructor(public field1: NameValue, public field2: NameValue, public field3: NameValue, public field4: NameValue, public field5: NameValue) {
    this.field1 = field1;
    this.field2 = field2;
    this.field3 = field3;
    this.field4 = field4;
    this.field5 = field5;
  }

  static parse(json): Lookup {
    return new Lookup(json.field1, json.field2, json.field3, json.field4, json.field5);
  }
}
