import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { AbstractionSettingsComponent } from './abstraction-settings.component';

describe('AbstractionSettingsComponent', () => {
  let component: AbstractionSettingsComponent;
  let fixture: ComponentFixture<AbstractionSettingsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ AbstractionSettingsComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AbstractionSettingsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
