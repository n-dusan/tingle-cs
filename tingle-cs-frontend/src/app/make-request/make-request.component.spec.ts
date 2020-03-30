import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { MakeRequestComponent } from './make-request.component';

describe('MakeRequestComponent', () => {
  let component: MakeRequestComponent;
  let fixture: ComponentFixture<MakeRequestComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ MakeRequestComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(MakeRequestComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
