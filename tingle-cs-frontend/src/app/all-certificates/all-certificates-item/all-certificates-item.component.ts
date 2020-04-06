import { Component, OnInit, OnDestroy } from '@angular/core';
import { Certificate } from '../../shared/certificate.model';
import { Subscription } from 'rxjs';
import { CertificatesService } from '../certificates.service';

import {MatDialog} from '@angular/material/dialog';

import { RevokeDialogComponent } from '../revoke-dialog/revoke-dialog.component';

@Component({
  selector: 'app-all-certificates-item',
  templateUrl: './all-certificates-item.component.html',
  styleUrls: ['./all-certificates-item.component.css']
})
export class AllCertificatesItemComponent implements OnInit, OnDestroy {
  
  selectedCertificate: Certificate[];
  private subscription: Subscription;
  serialNumber: any;
  downloadCert = false;

  constructor(private certificateService: CertificatesService, private dialog: MatDialog) { }


  onRevoke() {
    const dialogRef = this.dialog.open(RevokeDialogComponent);

    dialogRef.afterClosed().subscribe(result => {
      console.log(`Dialog result: ${result}`);
    });
  }


  ngOnInit(): void {
    this.subscription = this.certificateService.selectedCertificate.subscribe((data: Certificate[]) => {
      this.selectedCertificate = data;
    })
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  downloadCertificate(serialNumber: String){
    this.certificateService.downloadCertificate(serialNumber).subscribe(data=>{
      this.downloadCert = true;
    })

  }

}
