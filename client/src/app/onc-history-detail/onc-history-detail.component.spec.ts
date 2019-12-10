import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { OncHistoryDetailComponent } from './onc-history-detail.component';

describe('OncHistoryComponent', () => {
  let component: OncHistoryDetailComponent;
  let fixture: ComponentFixture<OncHistoryDetailComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ OncHistoryDetailComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(OncHistoryDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
