import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { FieldQuestionComponent } from './field-question.component';

describe('FieldQuestionComponent', () => {
  let component: FieldQuestionComponent;
  let fixture: ComponentFixture<FieldQuestionComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ FieldQuestionComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(FieldQuestionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
