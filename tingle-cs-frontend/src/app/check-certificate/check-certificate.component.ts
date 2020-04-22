import { Component, OnInit } from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { CertificatesService } from '../certificates.service';

@Component({
  selector: 'app-check-certificate',
  templateUrl: './check-certificate.component.html',
  styleUrls: ['./check-certificate.component.css']
})
export class CheckCertificateComponent implements OnInit {

  serialForm: FormGroup;
  loading: boolean = false;

  status: string = '';

  constructor(private _formBuilder: FormBuilder, private certificateService: CertificatesService) { }

  ngOnInit(): void {
    this.serialForm = this._formBuilder.group({
      serialNumber: ['', Validators.required]
    });
  }

  onObtainClick() {
    console.log('value', this.serialForm.value)
    this.loading = true;
    //perform ocsp request
    this.certificateService.ocspRequest(this.serialForm.value.serialNumber).subscribe((data) => {
      this.status = data;
      this.loading = false;
    })
  }
}
