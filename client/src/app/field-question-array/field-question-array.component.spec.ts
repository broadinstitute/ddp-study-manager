import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { FieldQuestionArrayComponent } from './field-question-array.component';

describe('FieldQuestionArrayComponent', () => {
  let component: FieldQuestionArrayComponent;
  let fixture: ComponentFixture<FieldQuestionArrayComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ FieldQuestionArrayComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(FieldQuestionArrayComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
