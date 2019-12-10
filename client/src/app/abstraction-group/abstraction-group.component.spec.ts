import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { AbstractionGroupComponent } from './abstraction-group.component';

describe('AbstractionGroupComponent', () => {
  let component: AbstractionGroupComponent;
  let fixture: ComponentFixture<AbstractionGroupComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ AbstractionGroupComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AbstractionGroupComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
