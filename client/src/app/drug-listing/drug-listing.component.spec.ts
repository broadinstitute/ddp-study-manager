import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DrugListingComponent } from './drug-listing.component';

describe('DrugListingComponent', () => {
  let component: DrugListingComponent;
  let fixture: ComponentFixture<DrugListingComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ DrugListingComponent ]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DrugListingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
