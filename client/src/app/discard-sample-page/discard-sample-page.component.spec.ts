import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DiscardSamplePageComponent } from './discard-sample-page.component';

describe('DiscardSamplePageComponent', () => {
  let component: DiscardSamplePageComponent;
  let fixture: ComponentFixture<DiscardSamplePageComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ DiscardSamplePageComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DiscardSamplePageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
