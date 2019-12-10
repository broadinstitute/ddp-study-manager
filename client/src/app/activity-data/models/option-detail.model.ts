export class OptionDetail {

  constructor( public details: string, public option: string ) {
    this.details = details;
    this.option = option;
  }

  static parse( json ): OptionDetail {
    return new OptionDetail( json.details, json.option );
  }
}
