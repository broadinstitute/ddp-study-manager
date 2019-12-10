import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { NDIUploadComponent } from './ndiupload.component';

describe('NDIUploadComponent', () => {
  let component: NDIUploadComponent;
  let fixture: ComponentFixture<NDIUploadComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ NDIUploadComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(NDIUploadComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
