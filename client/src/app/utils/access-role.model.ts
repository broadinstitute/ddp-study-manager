export class AccessRole {

  constructor(public roleName: string, public permissions: String[]) {
    this.roleName = roleName;
    this.permissions = permissions;
  }
  static parse(json): AccessRole {
    return new AccessRole(json.roleName, json.permissions);
  }
}
