import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ParticipantEventComponent } from './participant-event.component';

describe('ParticipantEventComponent', () => {
  let component: ParticipantEventComponent;
  let fixture: ComponentFixture<ParticipantEventComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ParticipantEventComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ParticipantEventComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
