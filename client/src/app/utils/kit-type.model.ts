export class KitType {

  selected: boolean = false;

  constructor(public kitId: number, public name: string, public displayName: string, public manualSentTrack: boolean, public externalShipper: boolean) {
    this.kitId = kitId;
    this.name = name;
    this.displayName = displayName;
    this.manualSentTrack = manualSentTrack;
    this.externalShipper = externalShipper;
  }

  static parse(json): KitType {
    return new KitType(json.kitId, json.name, json.displayName, json.manualSentTrack, json.externalShipper);
  }
}
