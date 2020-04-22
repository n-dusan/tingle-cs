import { Component, OnInit, OnDestroy } from '@angular/core';
import { Certificate } from '../../shared/certificate.model';
import { Subscription } from 'rxjs';
import { CertificatesService } from '../../certificates.service';

@Component({
  selector: 'app-revoke-dialog',
  templateUrl: './revoke-dialog.component.html',
  styleUrls: ['./revoke-dialog.component.css']
})
export class RevokeDialogComponent implements OnInit, OnDestroy {

  selectedCertificate: Certificate[];
  private subscription: Subscription;

  selected: string = 'KEY_COMPROMISE';

  constructor(private certificateService: CertificatesService) { }

  ngOnInit(): void {
    this.subscription = this.certificateService.selectedCertificate.subscribe((data: Certificate[]) => {
      this.selectedCertificate = data;
    })
  }

  onRealRevoke() {
    this.selectedCertificate[1].revokationReason = this.selected;
    this.certificateService.revokeCertificate(this.selectedCertificate[1]);
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

}
