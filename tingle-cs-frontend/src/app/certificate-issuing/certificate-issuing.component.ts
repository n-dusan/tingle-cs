import { Component, OnInit } from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { Certificate } from '../shared/certificate.model';
import { Observable } from 'rxjs';
import { CertificatesService } from '../all-certificates/certificates.service';
import { EndEntityCertificate } from '../shared/end-entity-cert.model';

@Component({
  selector: 'app-certificate-issuing',
  templateUrl: './certificate-issuing.component.html',
  styleUrls: ['./certificate-issuing.component.css']
})
export class CertificateIssuingComponent implements OnInit {

  isLinear = true;
  spin = false;
  firstFormGroup: FormGroup;
  secondFormGroup: FormGroup;
  thirdFormGroup: FormGroup;

  cas: Certificate[] = [];

  isSelfSigned = false;
  selectedCA: Certificate;
  selectedSubjectType: string;
  digitalSignature = false;
  keyEncipherment = false;
  nonRepudiation = false;
  dataEncipherment = false;
  keyAgreement = false;
  keyCertSign = false;
  crlSign = false;
  encipherOnly = false;
  decipherOnly = false;

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
      ST: ['', Validators.required],
      CO: ['', Validators.required],
      E: ['', Validators.required]
    });
    this.secondFormGroup = this._formBuilder.group({
      ca: ['']
    });
    this.thirdFormGroup = this._formBuilder.group({
      subjectType: ['', Validators.required],
      password: [''],
      alias: ['']
    });

  }

  onNext1Click() {
    console.log('Self signed: ' + this.isSelfSigned)
    console.log(this.firstFormGroup)

  }


  onCASelected(ca: Certificate) {
    this.selectedCA = ca;
  }

  onSubjectTypeClick(type: string) {
    this.selectedSubjectType = type;
  }

  onDoneClick() {
    const alias = this.thirdFormGroup.value.alias;
    const active = true;
    var role = '';
    if (this.isSelfSigned) {
      role = 'ROOT';
    } else if (this.thirdFormGroup.value.subjectType === 'CA') {
      role = 'INTERMEDIATE'
    } else {
      role = 'END_ENTITY'
    }
    const cn = this.firstFormGroup.value.CN;
    const o = this.firstFormGroup.value.ON;
    const l = this.firstFormGroup.value.LN;
    const st = this.firstFormGroup.value.ST;
    const c = this.firstFormGroup.value.CO;
    const ou = this.firstFormGroup.value.OU;
    const e = this.firstFormGroup.value.E;

    const cert = new Certificate(null, null, alias, active, role, cn, o, l, st, c, e, ou);
    const issuer = this.selectedCA;
    const endEntityCert: EndEntityCertificate = new EndEntityCertificate(cert, issuer);
    
    const newCert = new Certificate(null, null, alias, true, role, cn, o, l , st, c, e, ou);

    // Make request based on certificate role
    if(role === 'ROOT'){
      this.makeRootRequest(newCert);
    }
    if (role === 'END_ENTITY') {
      this.makeEndEntityRequest(endEntityCert);
    }

  }

  // Self-Signed slider click ([checked]="isSelfSigned")
  sliderClick() {
    this.isSelfSigned = !this.isSelfSigned;
    if (this.isSelfSigned) {
      this.thirdFormGroup.value.subjectType = "CA"
      console.log(this.thirdFormGroup.value.subjectType)
    }
    console.log('clicked: ' + this.isSelfSigned)
  }


  makeEndEntityRequest(endEntityCert: any) {
    this.certificateService.makeNewEndEntity(endEntityCert).subscribe(
      data => {
        console.log('SUCCESS')
        console.log(data);
      },
      error => {
        console.log('ERROR')
        console.log(error)
      }
    )
  }

  makeRootRequest(cert: Certificate) {
    this.spin = true;
    this.certificateService.makeNewRoot(cert).subscribe(
      data => {
        this.spin = false;
        console.log(data);
      },
      error => {
        this.spin = false;
        console.log(error)
      }
    );
  }

}
