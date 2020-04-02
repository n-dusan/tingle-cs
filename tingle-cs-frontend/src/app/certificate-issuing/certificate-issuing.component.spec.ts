import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { CertificateIssuingComponent } from './certificate-issuing.component';

describe('CertificateIssuingComponent', () => {
  let component: CertificateIssuingComponent;
  let fixture: ComponentFixture<CertificateIssuingComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ CertificateIssuingComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CertificateIssuingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
