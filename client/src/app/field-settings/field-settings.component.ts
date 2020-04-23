import {ChangeDetectorRef, Component, OnInit} from "@angular/core";
import {Result} from "../utils/result.model";
import {FieldSettings} from "./field-settings.model";
import {ActivatedRoute} from "@angular/router";
import {ComponentService} from "../services/component.service";
import {DSMService} from "../services/dsm.service";
import {RoleService} from "../services/role.service";
import {Auth} from "../services/auth.service";
import {Statics} from "../utils/statics";
import {FieldType} from "./field-type.model";
import {Value} from "../utils/value.model";

@Component( {
  selector: "app-field-settings",
  templateUrl: "./field-settings.component.html",
  styleUrls: [ "./field-settings.component.css" ]
} )
export class FieldSettingsComponent implements OnInit {

  errorMessage: string;
  additionalMessage: string;

  loading: boolean = false;
  saving: boolean = false;

  realmsAllowed: Array<string> = [];
  realm: string;

  fieldSettings: Map<string, Array<FieldSettings>> = new Map();
  selectedType: FieldType;
  settingsOfSelectedType: Array<FieldSettings> = [];
  allowedToSeeInformation: boolean = false;
  possibleTypes: Array<FieldType> = [ new FieldType( "Participant", "r" ),//because additional values of participants live in ddp_participant_record table
    new FieldType( "Medical Record", "m" ),
    new FieldType( "Onc History", "oD" ),
    new FieldType( "Tissue", "t" ) ];

  constructor( private _changeDetectionRef: ChangeDetectorRef, private dsmService: DSMService, private auth: Auth,
               private role: RoleService, private compService: ComponentService, private route: ActivatedRoute ) {
    if (!auth.authenticated()) {
      auth.logout();
    }
    this.route.queryParams.subscribe( params => {
      this.realm = params[ DSMService.REALM ] || null;
      if (this.realm != null) {
        //        this.compService.realmMenu = this.realm;
        for (let posType of this.possibleTypes) {
          if (posType.selected) {
            posType.selected = false;
          }
        }
        this.checkRight();
      }
    } );
  }

  ngOnInit() {
    if (localStorage.getItem( ComponentService.MENU_SELECTED_REALM ) != null) {
      this.realm = localStorage.getItem( ComponentService.MENU_SELECTED_REALM );
      this.checkRight();
    }
    else {
      this.additionalMessage = "Please select a realm";
    }
    window.scrollTo( 0, 0 );
  }

  private checkRight() {
    this.selectedType = null;
    this.settingsOfSelectedType = [];
    this.loadFieldSettings();
  }

  typeChecked( type: FieldType ) {
    if (type.selected) {
      this.selectedType = type;
      this.changeSelectedType( type );
    }
    else {
      this.selectedType = null;
      this.errorMessage = null;
      this.settingsOfSelectedType = [];
    }
    for (let posType of this.possibleTypes) {
      if (posType !== type) {
        if (posType.selected) {
          posType.selected = false;
        }
      }
    }
  }

  changeSelectedType( type: FieldType ) {
    this.additionalMessage = "";
    this.settingsOfSelectedType = [];
    if (this.fieldSettings.has( type.name )) {
      let existingList: Array<FieldSettings> = this.fieldSettings.get( type.name );
      for (let setting of existingList) {
        this.settingsOfSelectedType.push( setting );
      }
    }
    let newSetting: FieldSettings = new FieldSettings( null, null, null, this.selectedType.name,
      null, null );
    newSetting.addedNew = true;
    this.settingsOfSelectedType.push( newSetting );
  }

  loadFieldSettings() {
    this.loading = true;
    let jsonData: Map<string, Array<FieldSettings>>;

    this.dsmService.getFieldSettings( this.realm ).subscribe(
      data => {
        let result = Result.parse( data );
        if (result.code === 500) {
          this.allowedToSeeInformation = false;
          this.errorMessage = "";
          this.additionalMessage = "You are not allowed to see information of the selected realm at that category";
        }
        else {
          this.allowedToSeeInformation = true;
          this.fieldSettings = new Map<string, Array<FieldSettings>>();
          jsonData = data;

          for (const [ key, value ] of Object.entries( jsonData )) {
            let type = this.possibleTypes.find( x => x.tableAlias === key );
            for (let setting of value) {
              let event = FieldSettings.parse( setting );
              FieldSettings.addSettingWithType( this.fieldSettings, event, type );
            }
          }
          if (this.selectedType) {
            this.settingsOfSelectedType = [];
            if (this.fieldSettings.has( this.selectedType.name )) {
              let existingList: Array<FieldSettings> = this.fieldSettings.get( this.selectedType.name );
              for (let setting of existingList) {
                this.settingsOfSelectedType.push( setting );
              }
            }
            let newSetting: FieldSettings = new FieldSettings( null, null, null, this.selectedType.name,
              null, null );
            newSetting.addedNew = true;
            this.settingsOfSelectedType.push( newSetting );
          }
        }
        this.loading = false;
      },
      err => {
        if (err._body === Auth.AUTHENTICATION_ERROR) {
          this.auth.logout();
        }
        this.loading = false;
        this.errorMessage = "Error - Loading FieldSettings\nPlease contact your DSM developer";
      }
    );
  }

  saveFieldSettings() {
    if (this.realm != null) {
      this.saving = true;
      let foundError: boolean = false;
      let cleanedFieldSettings: Array<FieldSettings>;
      if (this.settingsOfSelectedType != null && this.settingsOfSelectedType.length > 0) {
        cleanedFieldSettings = FieldSettings.removeUnchangedFieldSettings( this.settingsOfSelectedType, this.selectedType );
        for (let setting of cleanedFieldSettings) {
          if (setting.notUniqueError || setting.spaceError) {
            foundError = true;
          }
        }
      }
      else {
        cleanedFieldSettings = null;
      }
      if (foundError) {
        this.additionalMessage = "Please fix errors first!";
      }
      else if (cleanedFieldSettings === null) {
        this.additionalMessage = "Nothing to save!";
      }
      else {
        // Create an object that JSON can convert better...
        let updatedFieldSettings: object = {};
        updatedFieldSettings[ this.selectedType.tableAlias ] = cleanedFieldSettings;

        this.dsmService.saveFieldSettings( this.realm, JSON.stringify( updatedFieldSettings ) ).subscribe(
          data => {
            this.loadFieldSettings();
            if (data.hasOwnProperty( "code" ) && data[ "code" ] !== 200) {
              this.additionalMessage = "Error - Saving field settings\nPlease contact your DSM developer";
            }
            else {
              this.additionalMessage = "Data saved";
            }
          },
          err => {
            if (err._body === Auth.AUTHENTICATION_ERROR) {
              this.auth.logout();
              this.loading = false;
            }
            this.additionalMessage = "Error - Saving field settings\nPlease contact your DSM developer";
          }
        );
      }

      this.saving = false;
      window.scrollTo( 0, 0 );
    }
  }

  onDisplayTypeChange( index: number ) {
    let setting: FieldSettings = this.settingsOfSelectedType[ index ];
    setting.possibleValues = [];
    setting.possibleValues.push( new Value( null ) );

    setting.changedBy = this.role.userMail();
    setting.changed = true;
  }

  onChange( index: number ) {
    this.settingsOfSelectedType[ index ].changedBy = this.role.userMail();
    this.settingsOfSelectedType[ index ].changed = true;
    if (index === this.settingsOfSelectedType.length - 1) {
      this.addNewFieldSetting();
    }
  }

  addNewFieldSetting() {
    let fieldSetting: FieldSettings = new FieldSettings( null, null, null, this.selectedType.name, null, [] );
    fieldSetting.addedNew = true;
    this.settingsOfSelectedType.push( fieldSetting );
  }

  addValue( setting: FieldSettings ) {
    let value = new Value( null );
    setting.possibleValues.push( value );
    setting.changed = true;

  }

  deleteFieldSetting( index: number ) {
    this.settingsOfSelectedType[ index ].deleted = true;
    this.onChange( index );
    this.saveFieldSettings();
  }

  checkColName( index: number ) {
    let setting: FieldSettings = this.settingsOfSelectedType[ index ];
    setting.notUniqueError = false;
    setting.spaceError = setting.columnName.indexOf( " " ) > -1;
    for (let i = 0; i < this.settingsOfSelectedType.length; i++) {
      if (i !== index) {
        if (this.settingsOfSelectedType[ i ].columnName === setting.columnName) {
          setting.notUniqueError = true;
        }
      }
    }
  }
}
