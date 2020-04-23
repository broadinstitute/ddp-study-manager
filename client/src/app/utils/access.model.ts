import {AccessRole} from "./access-role.model";

export class Access {

  constructor(public exp: number, public accessRoles: AccessRole[] ){
    this.exp = exp;
    this.accessRoles = accessRoles;
  }

  static parse(json): Access {
    return new Access(json.exp, json.accessRoles);
  }
}
