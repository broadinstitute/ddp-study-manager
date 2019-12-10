import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';

@Component({
  selector: 'app-filepicker',
  templateUrl: './field-filepicker.component.html',
  styleUrls: ['./field-filepicker.component.css']
})
export class FieldFilepickerComponent implements OnInit {

  @Input() id: string;
  @Input() fileFormat: string;
  @Output() fileSelected = new EventEmitter();

  error: string = null;
  file: File = null;

  constructor() { }

  ngOnInit() {
  }

  userFile(event: EventTarget) {
    let eventObj: MSInputMethodContext = <MSInputMethodContext> event;
    let target: HTMLInputElement = <HTMLInputElement> eventObj.target;
    let files: FileList = target.files;
    if (this.fileFormat === "*") {
      this.file = files[0];
      this.fileSelected.emit(this.file);
      this.error = null;
    }
    else if (this.fileFormat === "image") {
      if (files[0].name.endsWith("png") || files[0].name.endsWith("jpg") ) {
        this.file = files[0];
        this.fileSelected.emit(this.file);
        this.error = null;
      }
      else {
        this.error = "Wrong file type selected. Please select a png or jpg file.";
      }
    }
    else {
      if (files[0].type === "text/plain") {
        this.file = files[0];
        this.fileSelected.emit(this.file);
        this.error = null;
      }
      else {
        this.error = "Wrong file type selected. Please select a txt file.";
      }
    }
  }

  unselectFile() {
    this.file = null;
  }
}
