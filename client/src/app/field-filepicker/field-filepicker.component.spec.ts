import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { FieldFilepickerComponent } from './field-filepicker.component';

describe('FieldFilepickerComponent', () => {
  let component: FieldFilepickerComponent;
  let fixture: ComponentFixture<FieldFilepickerComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ FieldFilepickerComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(FieldFilepickerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
