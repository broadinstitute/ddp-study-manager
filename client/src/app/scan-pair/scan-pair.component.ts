import { Component, OnInit, ViewChild, Output, EventEmitter, Input } from '@angular/core';

@Component({
  selector: 'app-scan-pair',
  templateUrl: './scan-pair.component.html',
})
export class ScanPairComponent implements OnInit {

  @ViewChild('leftInput') leftInput;
  @ViewChild('rightInput') rightInput;

  @Input() positionScanPair: number;
  @Input() countScanPair: number;
  @Input() isLeftValueDuplicate: boolean = false;
  @Input() isRightValueDuplicate: boolean = false;
  @Input() hadErrorSending: boolean = false;
  @Input() leftInputPlaceholder: string = "Kit Label";
  @Input() rightInputPlaceholder: string = "DSM Label";
  @Input() errorMessage: string;

  @Output() pairScanned = new EventEmitter();
  @Output() removeScanPair = new EventEmitter();
  @Output() leftLabelAdded = new EventEmitter();

  constructor() {
  }

  ngOnInit() {
    this.leftInput.nativeElement.focus();
  }

  moveFocus(leftValue: string) {
    this.rightInput.nativeElement.focus();
    this.leftLabelAdded.next([leftValue, this.positionScanPair]);
  }

  nextPair(leftValue: string, rightValue: string) {
    this.pairScanned.next([leftValue, rightValue, this.positionScanPair]);
  }

  removeMe() {
    this.removeScanPair.next(this.positionScanPair);
  }
}
