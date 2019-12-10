export class Option {

  constructor( public optionStableId: string, public optionText: string ) {
    this.optionStableId = optionStableId;
    this.optionText = optionText;
  }

  isSelected( stableId: string) {
    if (stableId === this.optionStableId) {
      return true;
    }
    return false;
  }

  static parse( json ): Option {
    return new Option( json.optionStableId, json.optionText );
  }
}
