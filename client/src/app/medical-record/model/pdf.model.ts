export class PDFModel {
  configName: string;
  displayName: string;

  constructor( configName: string, displayName: string ) {
    this.configName = configName;
    this.displayName = displayName;
  }

  static parse( json ): PDFModel {
    return new PDFModel( json.configName, json.displayName );
  }
}
