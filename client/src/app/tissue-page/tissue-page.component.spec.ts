import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { TissuePageComponent } from './tissue-page.component';

describe('TissuePageComponent', () => {
  let component: TissuePageComponent;
  let fixture: ComponentFixture<TissuePageComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ TissuePageComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TissuePageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
