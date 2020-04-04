import { Component, OnInit } from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { Certificate } from '../shared/certificate.model';
import { Observable } from 'rxjs';
import { CertificatesService } from '../all-certificates/certificates.service';
import { CertificateX500Name } from '../shared/certificateX500Name.model';

@Component({
  selector: 'app-certificate-issuing',
  templateUrl: './certificate-issuing.component.html',
  styleUrls: ['./certificate-issuing.component.css']
})
export class CertificateIssuingComponent implements OnInit {

  isLinear = false;
  firstFormGroup: FormGroup;
  secondFormGroup: FormGroup;
  thirdFormGroup: FormGroup;
  isSelfSigned = false;

  cas: Certificate[] = []; 
  selectedCA: Certificate;

  constructor(private _formBuilder: FormBuilder,
    private certificateService: CertificatesService) { }

  ngOnInit(): void {
    this.certificateService.getOnlyCA().subscribe(
      data => {
        // console.log(data)
        this.cas = data;
      },
      error => {
        console.log(error.error)
      }
    );
    this.firstFormGroup = this._formBuilder.group({
      CN: ['', Validators.required],
      OU: ['', Validators.required],
      ON: ['', Validators.required],
      LN: ['', Validators.required],
      SN: ['', Validators.required],
      CO: ['', Validators.required]
    });
    this.secondFormGroup = this._formBuilder.group({
      ca: ['', Validators.required]
    });
    this.thirdFormGroup = this._formBuilder.group({
      secondCtrl: ['', Validators.required]
    });

  }

  onNext1Click() {
    console.log('next clicked')
    console.log(this.firstFormGroup)
    // if (isSelfSigned) { popuni polja na secondFormGroup }
  }

  onCASelected(ca: Certificate) {
    this.selectedCA = ca;
  }

}
