import {Component, EventEmitter, Input, OnInit, Output, ViewChild} from '@angular/core';

@Component({
  selector: 'app-scan-value',
  templateUrl: './scan-value.component.html',
  styleUrls: ['./scan-value.component.css']
})
export class ScanValueComponent implements OnInit {

  @ViewChild('scanValue') scanValue;

  @Input() positionScanValue: number;
  @Input() countScanValue: number;
  @Input() isScanValueDuplicate: boolean = false;
  @Input() hadErrorSending: boolean = false;
  @Input() scanValuePlaceholder: string = "SM-ID";
  @Input() errorMessage: string;

  @Output() valueScanned = new EventEmitter();
  @Output() removeScanValue = new EventEmitter();

  constructor() {
  }

  ngOnInit() {
    this.scanValue.nativeElement.focus();
  }

  nextValue(value: string) {
    this.valueScanned.next([value, this.positionScanValue]);
  }

  removeMe() {
    this.removeScanValue.next(this.positionScanValue);
  }

}
