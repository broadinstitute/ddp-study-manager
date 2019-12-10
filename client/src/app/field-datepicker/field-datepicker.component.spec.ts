import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { FieldDatepickerComponent } from './field-datepicker.component';

describe('FieldDatepickerComponent', () => {
  let component: FieldDatepickerComponent;
  let fixture: ComponentFixture<FieldDatepickerComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ FieldDatepickerComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(FieldDatepickerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
