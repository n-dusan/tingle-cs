import { Component, OnInit } from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { Certificate } from '../shared/certificate.model';
import { CertificatesService } from '../certificates.service';
import { EndEntityCertificate } from '../shared/end-entity-cert.model';
import { Extensions } from '../shared/extensions.model';
import { ExtensionsKeyUsage } from '../shared/key-usage.model';
import { ExtensionsBasicConstraints } from '../shared/basic-constraints.model';
import { ExtendedKeyUsage } from '../shared/extended-key-usage.model';

@Component({
  selector: 'app-certificate-issuing',
  templateUrl: './certificate-issuing.component.html',
  styleUrls: ['./certificate-issuing.component.css']
})
export class CertificateIssuingComponent implements OnInit {

  isLinear = true;
  spin = false;
  success = false;
  isDSSelected = false;
  isKESelected = false;
  isNESelected = false;
  isDESelected = false;
  isKASelected = false;
  isKCSSelected = false;
  isCRLSelected = false;
  isEncSelected = false;
  isDecSelected = false;

  isClientAuth = false;
  isCodeSigning = false;
  isEmailProtection = false;
  isOcspSigning = false;
  isServerAuth = false;
  isTimeStamping = false;


  firstFormGroup: FormGroup;
  secondFormGroup: FormGroup;
  thirdFormGroup: FormGroup;
  fourthFormGroup: FormGroup;

  cas: Certificate[] = [];

  isSelfSigned = false;
  selectedCA: Certificate;
  selectedSubjectType: string;

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
    this.secondFormGroup = this._formBuilder.group({
      CN: ['', Validators.required],
      OU: ['', Validators.required],
      ON: ['', Validators.required],
      LN: ['', Validators.required],
      ST: ['', Validators.required],
      CO: ['', Validators.required],
      E: ['', Validators.required]
    });
    this.thirdFormGroup = this._formBuilder.group({
      ca: ['']
    });
    this.fourthFormGroup = this._formBuilder.group({
      criticalKeyUsage: [false],
      criticalBasicConstraints: [false],
      criticalExtendedKeyUsage: [false],
      digitalSignature: [false],
      keyEncipherment: [false],
      nonRepudiation: [false],
      dataEncipherment: [false],
      keyAgreement: [false],
      keyCertSign: [false],
      crlSign: [false],
      encipherOnly: [false],
      decipherOnly: [false],
      clientAuth: [false],
      codeSigning: [false],
      emailProtection: [false],
      ocspSigning: [false],
      serverAuth: [false],
      timeStamping: [false],
      subjectType: ['', Validators.required],
      password: [''],
      alias: ['']
    });

  }

  onNext1Click() {
    // add some checking if needed
  }


  onCASelected(ca: Certificate) {
    this.selectedCA = ca;
  }

  onSubjectTypeClick(type: string) {
    this.selectedSubjectType = type;
  }

  onDoneClick() {
    //========== Take values from forms and construct a Certificate object ==========
    const alias = this.fourthFormGroup.value.alias;
    const active = true;

    var role = '';
    if (this.isSelfSigned) {
      role = 'ROOT';
    } else if (this.fourthFormGroup.value.subjectType === 'CA') {
      role = 'INTERMEDIATE'
    } else {
      role = 'END_ENTITY'
    }

    const cn = this.secondFormGroup.value.CN;
    const o = this.secondFormGroup.value.ON;
    const l = this.secondFormGroup.value.LN;
    const st = this.secondFormGroup.value.ST;
    const c = this.secondFormGroup.value.CO;
    const ou = this.secondFormGroup.value.OU;
    const e = this.secondFormGroup.value.E;

    // Make request based on certificate role
    if (role === 'ROOT') {
      const newCert = new Certificate(null, null, alias, true, role, cn, o, l, st, c, e, ou);
      newCert.extensions = this.attachExtensions();

      //CHECK
      console.log(newCert)

      this.makeRootRequest(newCert);
    } else if (role === 'INTERMEDIATE') {
      // set serial number to issuer.serialNumber
      const newCert = new Certificate(null, null, alias, true, role, cn, o, l, st, c, e, ou);
      newCert.extensions = this.attachExtensions();
      newCert.serialNumber = this.selectedCA.serialNumber;

      //CHECK
      console.log(newCert)

      this.makeCARequest(newCert);
    } else {
      const cert = new Certificate(null, null, alias, active, role, cn, o, l, st, c, e, ou);
      cert.extensions = this.attachExtensions();
      const issuer = this.selectedCA;
      const endEntityCert: EndEntityCertificate = new EndEntityCertificate(cert, issuer);
      this.makeEndEntityRequest(endEntityCert);
    }

  }

  // Self-Signed slider click ([checked]="isSelfSigned")
  sliderClick() {
    this.isSelfSigned = !this.isSelfSigned;
    if (this.isSelfSigned) {
      this.fourthFormGroup.value.subjectType = "CA"
    }
  }


  makeEndEntityRequest(endEntityCert: any) {
    this.spin = true;
    this.certificateService.makeNewEndEntity(endEntityCert).subscribe(
      data => {
        this.spin = false;
        this.success = true;
        console.log(data);
      },
      error => {
        this.spin = false;
        console.log(error)
      }
    )
  }

  makeRootRequest(cert: Certificate) {
    this.spin = true;
    this.certificateService.makeNewRoot(cert).subscribe(
      data => {
        this.spin = false;
        this.success = true;
        console.log(data);
      },
      error => {
        this.spin = false;
        console.log(error)
      }
    );
  }

  makeCARequest(cert: Certificate) {
    this.spin = true;
    this.certificateService.makeNewCA(cert).subscribe(
      data => {
        this.spin = false;
        this.success = true;
        console.log(data);
      },
      error => {
        this.spin = false;
        console.log(error)
      }
    );
  }

  selectFirstTemplate(){
    this.isDSSelected = true;
    this.isKASelected = true;
    this.isKESelected = false;
    this.isNESelected = false;
    this.isDESelected = false;
    this.isKCSSelected = false;
    this.isCRLSelected = false;
    this.isEncSelected = false;
    this.isDecSelected = false;
  }

  selectSecondTemplate(){
    this.isDSSelected = false;
    this.isKASelected = false;
    this.isKESelected = false;
    this.isNESelected = false;
    this.isDESelected = false;
    this.isKCSSelected = true;
    this.isCRLSelected = true;
    this.isEncSelected = false;
    this.isDecSelected = false;
  }

  selectThirdTemplate(){
    this.isDSSelected = false;
    this.isKASelected = false;
    this.isKESelected = false;
    this.isNESelected = true;
    this.isDESelected = true;
    this.isKCSSelected = false;
    this.isCRLSelected = false;
    this.isEncSelected = false;
    this.isDecSelected = false;
  }

  selectFourthTemplate(){
    this.isDSSelected = false;
    this.isKASelected = true;
    this.isKESelected = false;
    this.isNESelected = false;
    this.isDESelected = false;
    this.isKCSSelected = false;
    this.isCRLSelected = false;
    this.isEncSelected = true;
    this.isDecSelected = true;
  }

  selectFifthTemplate(){
    this.isDSSelected = true;
    this.isKASelected = false;
    this.isKESelected = false;
    this.isNESelected = false;
    this.isDESelected = false;
    this.isKCSSelected = false;
    this.isCRLSelected = false;
    this.isEncSelected = false;
    this.isDecSelected = false;
  }

  selectSixthTemplate(){
    this.isDSSelected = true;
    this.isKASelected = false;
    this.isKESelected = true;
    this.isNESelected = false;
    this.isDESelected = false;
    this.isKCSSelected = false;
    this.isCRLSelected = false;
    this.isEncSelected = false;
    this.isDecSelected = false;
  }

  attachExtensions(): Extensions {

    var extensions: Extensions = new Extensions();
    var keyUsage: ExtensionsKeyUsage = new ExtensionsKeyUsage();
    var basicConstraints: ExtensionsBasicConstraints = new ExtensionsBasicConstraints();
    var extendedKeyUsage: ExtendedKeyUsage = new ExtendedKeyUsage();

    // Attach key usages
    keyUsage.digitalSignature = this.fourthFormGroup.value.digitalSignature;
    keyUsage.nonRepudation = this.fourthFormGroup.value.nonRepudiation;
    keyUsage.keyEncipherment = this.fourthFormGroup.value.keyEncipherment;
    keyUsage.dataEncipherment = this.fourthFormGroup.value.dataEncipherment;
    keyUsage.keyAgreement = this.fourthFormGroup.value.keyAgreement;
    keyUsage.keyCertSign = this.fourthFormGroup.value.keyCertSign;
    keyUsage.crlSign = this.fourthFormGroup.value.crlSign;
    keyUsage.encipherOnly = this.fourthFormGroup.value.encipherOnly;
    keyUsage.decipherOnly = this.fourthFormGroup.value.decipherOnly;
    keyUsage.critical = this.fourthFormGroup.value.criticalKeyUsage;
    extensions.keyUsage = keyUsage;

    // Attach basic constraints
    if (this.isSelfSigned || this.fourthFormGroup.value.subjectType === "CA") {
      basicConstraints.certificateAuthority = true;
    } else {
      basicConstraints.certificateAuthority = false;
    }
    basicConstraints.critical = this.fourthFormGroup.value.criticalBasicConstraints;
    extensions.basicConstraints = basicConstraints;

    // Attach Extended Key Usage in case of end-entity cert

      extendedKeyUsage.critical = this.fourthFormGroup.value.criticalExtendedKeyUsage;
      extendedKeyUsage.clientAuth = this.fourthFormGroup.value.clientAuth;
      extendedKeyUsage.codeSigning = this.fourthFormGroup.value.codeSigning;
      extendedKeyUsage.emailProtection = this.fourthFormGroup.value.emailProtection;
      extendedKeyUsage.ocspSigning = this.fourthFormGroup.value.ocspSigning;
      extendedKeyUsage.serverAuth = this.fourthFormGroup.value.serverAuth;
      extendedKeyUsage.timeStamping = this.fourthFormGroup.value.timeStamping;

      extensions.extendedKeyUsage = extendedKeyUsage;


    console.log(extensions)

    return extensions;
  }




}
