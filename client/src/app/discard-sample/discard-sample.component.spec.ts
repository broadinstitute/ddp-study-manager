import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DiscardSampleComponent } from './discard-sample.component';

describe('DiscardSampleComponent', () => {
  let component: DiscardSampleComponent;
  let fixture: ComponentFixture<DiscardSampleComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ DiscardSampleComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DiscardSampleComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
