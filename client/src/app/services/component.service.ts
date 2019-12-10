import {Injectable} from "@angular/core";

import {DiscardSample} from "../discard-sample/discard-sample.model";
import {FieldSettings} from "../field-settings/field-settings.model";

// Service to send information to a childComponent, which is called per router!
@Injectable()
export class ComponentService {

  static MENU_SELECTED_REALM = "selectedRealm";

  //  realmMenu: string;
  justReturning: boolean = false;

  customViews: any;

  editable: boolean;

  discardSample: DiscardSample;

  fieldSettings: Map<string, Array<FieldSettings>>;

  public getRealm(): string {
    return localStorage.getItem(ComponentService.MENU_SELECTED_REALM);
  }
}
