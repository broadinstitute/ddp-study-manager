export class NameValue {

  constructor(public name: string, public value: string) {
    this.name = name;
    this.value = value;
  }

  static parse(json): NameValue {
    if (json === undefined) {
      return null;
    }
    if (json.name !== undefined && json.name !== null && json.name.includes("_")) {
      json.name = json.name.substring(json.name.indexOf("_") + 1);
    }

    if (json.name !== undefined && json.value === undefined) {
      return new NameValue(json.name, null);
    }

    if (json.name === undefined && json.value !== undefined) {
      return new NameValue(null, json.value);
    }
    return new NameValue(json.name, json.value);
  }
}
