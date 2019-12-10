import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ShippingReportComponent } from './shipping-report.component';

describe('ShippingReportComponent', () => {
  let component: ShippingReportComponent;
  let fixture: ComponentFixture<ShippingReportComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ShippingReportComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ShippingReportComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
