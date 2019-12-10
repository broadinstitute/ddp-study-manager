export class ScanError {

  constructor(public kit: string, public error: string) {
    this.kit = kit;
    this.error = error;
  }

  static parse(json): ScanError {
    return new ScanError(json.kit, json.error);
  }
}
