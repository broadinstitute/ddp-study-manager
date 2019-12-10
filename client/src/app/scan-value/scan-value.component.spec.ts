import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ScanValueComponent } from './scan-value.component';

describe('ScanValueComponent', () => {
  let component: ScanValueComponent;
  let fixture: ComponentFixture<ScanValueComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ScanValueComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ScanValueComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
