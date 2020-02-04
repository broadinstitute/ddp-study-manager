import {Component, Input, OnInit, ViewChild} from "@angular/core";
import {FieldSettings} from "../field-settings/field-settings.model";
import {OncHistoryDetail} from "../onc-history-detail/onc-history-detail.model";
import {Participant} from "../participant-list/participant-list.model";
import {Tissue} from "./tissue.model";
import {RoleService} from "../services/role.service";
import {DSMService} from "../services/dsm.service";
import {ComponentService} from "../services/component.service";
import {Lookup} from "../lookup/lookup.model";
import {Statics} from "../utils/statics";
import {Result} from "../utils/result.model";
import {Auth} from "../services/auth.service";
import {Router} from "@angular/router";
import {NameValue} from "../utils/name-value.model";
import {PatchUtil} from "../utils/patch.model";

@Component ({
  selector: "app-tissue",
  templateUrl: "./tissue.component.html",
  styleUrls: [ "./tissue.component.css" ],
})
export class TissueComponent implements OnInit {

  @ViewChild ("collaboratorSampleId") collaboratorSampleIdInputField;

  @Input () participant: Participant;
  @Input () oncHistoryDetail: OncHistoryDetail;
  @Input () additionalColumns: Array<FieldSettings>;
  @Input () tissue: Tissue;
  @Input () tissueId: string;
  @Input () editable: boolean;

  collaboratorS: string;
  currentPatchField: string;
  patchFinished: boolean = true;
  dup: boolean = false;

  constructor (private role: RoleService, private dsmService: DSMService, private compService: ComponentService,
               private router: Router) {
  }

  ngOnInit () {
    for ( let col of this.additionalColumns ) {
      console.log (col);
    }
  }

  public getCompService () {
    return this.compService;
  }

  public setTissueSite (object: any) {
    if ( object != null ) {
      if ( event instanceof MouseEvent ) {
        this.tissue.tissueSite = object.field1.value;
      }
      else {
        this.tissue.tissueSite = object;
      }
      this.valueChanged (this.tissue.tissueSite, "tissueSite");
    }
  }

  onAdditionalColChange (evt: any, colName: string) {
    let v;
    if ( typeof evt === "string" ) {
      v = evt;
    }
    else {
      if ( evt.srcElement != null && typeof evt.srcElement.value === "string" ) {
        v = evt.srcElement.value;
      }
      else if ( evt.value != null ) {
        v = evt.value;
      }
      else if ( evt.checked != null ) {
        v = evt.checked;
      }
    }
    if ( v !== null ) {
      if ( this.tissue.additionalValues != null ) {
        this.tissue.additionalValues[colName] = v;
      }
      else {
        let addArray = {};
        addArray[colName] = v;
        this.tissue.additionalValues = addArray;
      }
      this.valueChanged (this.tissue.additionalValues, "additionalValues");
    }
  }

  //display additional value
  getAdditionalValue (colName: string): string {
    if ( this.tissue.additionalValues != null ) {
      if ( this.tissue.additionalValues[colName] != undefined && this.tissue.additionalValues[colName] != null ) {
        return this.tissue.additionalValues[colName];
      }
    }
    return null;
  }

  valueChanged (value: any, parameterName: string) {
    let v;
    if ( parameterName === "additionalValues" ) {
      v = JSON.stringify (value);
    }
    else if ( typeof value === "string" ) {
      v = value;
    }
    else {
      if ( value.srcElement != null && typeof value.srcElement.value === "string" ) {
        v = value.srcElement.value;
      }
      else if ( value.value != null ) {
        v = value.value;
      }
      else if ( value.checked != null ) {
        v = value.checked;
      }
    }
    if ( v !== null ) {
      if ( parameterName !== "additionalValues" ) {
        for ( let oncTissue of this.oncHistoryDetail.tissues ) {
          if ( oncTissue.tissueId == this.tissue.tissueId ) {
            oncTissue[parameterName] = v;
          }
        }
      }
      let patch1 = new PatchUtil (this.tissue.tissueId, this.role.userMail (),
        {
          name: parameterName,
          value: v,
        }, null, "oncHistoryDetailId", this.tissue.oncHistoryDetailId, Statics.TISSUE_ALIAS);
      let patch = patch1.getPatch ();
      this.patchFinished = false;
      this.currentPatchField = parameterName;
      this.dsmService.patchParticipantRecord (JSON.stringify (patch)).subscribe (// need to subscribe, otherwise it will not send!
        data => {
          let result = Result.parse (data);
          if ( result.code == 200 && result.body != null && result.body !== "" && this.tissue.tissueId == null ) {
            let jsonData: any | any[] = JSON.parse (result.body);
            console.log (jsonData);
            this.tissue.tissueId = jsonData.tissueId;
            this.patchFinished = true;
            this.currentPatchField = null;
            this.dup = false;
            if ( jsonData instanceof Array ) {
              jsonData.forEach ((val) => {
                let nameValue = NameValue.parse (val);
                this.oncHistoryDetail[nameValue.name] = nameValue.value;
              });
            }

          }
          else if ( result.code === 500 && result.body != null ) {
            this.dup = true;
          }
          else if ( result.code === 200 ) {
            if ( result.body != null && result.body !== "" ) {
              let jsonData: any | any[] = JSON.parse (result.body);
              if ( jsonData instanceof Array ) {
                jsonData.forEach ((val) => {
                  let nameValue = NameValue.parse (val);
                  this.oncHistoryDetail[nameValue.name] = nameValue.value;
                });
              }
            }
            this.patchFinished = true;
            this.currentPatchField = null;
            this.dup = false;
          }
        },
        err => {
          if ( err._body === Auth.AUTHENTICATION_ERROR ) {
            this.router.navigate ([ Statics.HOME_URL ]);
          }
        },
      );
    }
  }

  deleteTissue () {
    this.tissue.deleted = true;
  }

  public getStyleDisplay () {
    if ( this.collaboratorS != null ) {
      return Statics.DISPLAY_BLOCK;
    }
    else {
      return Statics.DISPLAY_NONE;
    }
  }

  public checkCollaboratorId () {
    let jsonData: any[];
    if ( this.collaboratorS == null && (this.tissue.collaboratorSampleId == null || this.tissue.collaboratorSampleId === "") ) {
      this.dsmService.lookupCollaboratorId ("tCollab", this.participant.participant.participantId, this.participant.data.profile["hruid"], localStorage.getItem (ComponentService.MENU_SELECTED_REALM)).subscribe (// need to subscribe, otherwise it will not send!
        data => {
          //          console.log(`received: ${JSON.stringify(data, null, 2)}`);
          jsonData = data;
          jsonData.forEach ((val) => {
            let con = Lookup.parse (val);
            this.collaboratorS = con.field1.value + "_";
          });
        },
        err => {
          if ( err._body === Auth.AUTHENTICATION_ERROR ) {
            this.router.navigate ([ Statics.HOME_URL ]);
          }
        },
      );
    }
  }

  public setLookup () {
    this.tissue.collaboratorSampleId = this.collaboratorS;
    this.collaboratorS = null;
    this.collaboratorSampleIdInputField.nativeElement.focus ();
  }

  isPatchedCurrently (field: string): boolean {
    if ( this.currentPatchField === field ) {
      return true;
    }
    return false;
  }

  currentField (field: string) {
    if ( field != null || (field == null && this.patchFinished) ) {
      this.currentPatchField = field;
    }
  }
}
