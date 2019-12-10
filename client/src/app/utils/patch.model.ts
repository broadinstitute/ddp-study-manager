export class PatchUtil {

  constructor(public id: string, public user: string, public nameValue: {}, public nameValues: any[],
              public parent: string, public parentId: string, public tableAlias?: string) {
    this.id = id;
    this.user = user;
    this.nameValue = nameValue;
    this.nameValues = nameValues;
    this.parent = parent;
    this.parentId = parentId;
    this.tableAlias = tableAlias;
  }

  public getPatch() {
    let patch = {
      id: this.id,
      user: this.user,
      nameValue: this.nameValue
    };
    if (this.parent !== null) {
      patch["parent"] = this.parent;
    }
    if (this.parentId !== null) {
      patch["parentId"] = this.parentId;
    }
    if (this.nameValues !== null) {
      patch["nameValues"] = this.nameValues;
    }
    if (this.tableAlias !== undefined && this.tableAlias !== null) {
      patch["tableAlias"] = this.tableAlias;
      if (this.nameValue != null) {
        this.nameValue["name"] = this.tableAlias + "." + this.nameValue["name"];
      }
    }
    return patch;
  }
}
