import {Component, EventEmitter, Input, OnInit, Output} from "@angular/core";
import {AbstractionGroup} from "../abstraction-group/abstraction-group.model";
import {Participant} from "../participant-list/participant-list.model";
import {AbstractionField} from "./medical-record-abstraction-field.model";

@Component( {
  selector: "app-medical-record-abstraction",
  templateUrl: "./medical-record-abstraction.component.html",
  styleUrls: [ "./medical-record-abstraction.component.css" ]
} )
export class MedicalRecordAbstractionComponent implements OnInit {

  @Input() participant: Participant;
  @Input() abstractionFormControls: Array<AbstractionGroup> = [];
  @Input() editForm: boolean = false;
  @Input() abstractionActivity: string;
  @Input() status: string;
  @Input() fileList: string[];
  @Input() drugList: string[];
  @Input() cancerList: string[];
  @Output() emitFileName = new EventEmitter();

  nameCurrentFile: string;

  newGroup: string;
  notUniqueError: boolean;

  constructor() {
  }

  ngOnInit() {
  }

  addGroup() {
    if (!this.notUniqueError) {
      let fields: AbstractionField[] = [];
      let newAbstractionGroup = new AbstractionGroup( null, this.newGroup, this.abstractionFormControls.length, fields );
      newAbstractionGroup.changed = true;
      newAbstractionGroup.newAdded = true;
      this.abstractionFormControls.push( newAbstractionGroup );
      this.newGroup = null;
    }
  }

  addFileToList( fileName: string ) {
    if (fileName != null && fileName !== "") {
      let found: boolean = false;
      for (let file of this.fileList) {
        if (file === fileName) {
          found = true;
          break;
        }
      }
      if (!found) {
        this.fileList.push( fileName );
      }
      this.nameCurrentFile = fileName;
      this.emitFileName.emit( fileName );
    }
  }

  checkName( displayName: string ) {
    this.notUniqueError = false;
    for (let field of this.abstractionFormControls) {
      if (field.displayName.toLowerCase() === displayName.toLowerCase()) {
        this.notUniqueError = true;
      }
    }
    return null;
  }

  findGroup( group: Array<AbstractionGroup>, medicalRecordAbstractionGroupId: number ): AbstractionGroup {
    if (group != null) {
      return group.find( gr => {
        return gr.abstractionGroupId == medicalRecordAbstractionGroupId;
      } );
    }
  }

}
