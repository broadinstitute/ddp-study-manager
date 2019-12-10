import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { TissueListComponent } from './tissue-list.component';

describe('TissueListComponent', () => {
  let component: TissueListComponent;
  let fixture: ComponentFixture<TissueListComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ TissueListComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TissueListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
