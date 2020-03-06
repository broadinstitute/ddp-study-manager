import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DrugListComponent } from './drug-list.component';

describe('DrugListComponent', () => {
  let component: DrugListComponent;
  let fixture: ComponentFixture<DrugListComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ DrugListComponent ]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DrugListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
