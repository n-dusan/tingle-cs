import { Component, OnInit, OnDestroy } from '@angular/core';
import { Certificate } from '../shared/certificate.model';
import { Subscription } from 'rxjs';
import { CertificatesService } from './certificates.service';

@Component({
  selector: 'app-all-certificates',
  templateUrl: './all-certificates.component.html',
  styleUrls: ['./all-certificates.component.css']
})
export class AllCertificatesComponent implements OnInit, OnDestroy {

  selectedCertificate: Certificate[];
  private subscription: Subscription;

  constructor(private certificateService: CertificatesService) { }

  ngOnInit(): void {
    this.subscription = this.certificateService.selectedCertificate.subscribe((data: Certificate[]) => {
      this.selectedCertificate = data;
    })
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }
}
