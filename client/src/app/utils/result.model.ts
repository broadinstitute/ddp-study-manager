export class Result {

  constructor(public code: number, public body: string){
    this.code = code;
    this.body = body;
  }

  static parse(json): Result {
    return new Result(json.code, json.body);
  }
}
