import {Component, OnInit, Output, EventEmitter, Input} from "@angular/core";

import { Assignee } from "./assignee.model";

@Component({
  selector: 'app-assignee',
  templateUrl: './assignee.component.html',
  styleUrls: ['./assignee.component.css']
})
export class AssigneesComponent implements OnInit {

  @Input() assignees: Array<Assignee>;
  @Output() selectedAssignee = new EventEmitter();

  constructor() { }

  ngOnInit() {
  }

  selectAssignee(assignee: Assignee) {
    this.selectedAssignee.next(assignee);
  }
}
