import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { MedicalRecordAbstractionComponent } from './medical-record-abstraction.component';

describe('MedicalRecordAbstractionComponent', () => {
  let component: MedicalRecordAbstractionComponent;
  let fixture: ComponentFixture<MedicalRecordAbstractionComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ MedicalRecordAbstractionComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(MedicalRecordAbstractionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
