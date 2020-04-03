import { Component, OnInit, OnDestroy } from '@angular/core';
import { Certificate } from '../../shared/certificate.model';
import { Subscription } from 'rxjs';
import { CertificatesService } from '../certificates.service';

@Component({
  selector: 'app-all-certificates-item',
  templateUrl: './all-certificates-item.component.html',
  styleUrls: ['./all-certificates-item.component.css']
})
export class AllCertificatesItemComponent implements OnInit, OnDestroy {
  
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
