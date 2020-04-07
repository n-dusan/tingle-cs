import { Component, OnInit, OnDestroy, Input } from '@angular/core';
import { Certificate } from '../../shared/certificate.model';
import { Subscription } from 'rxjs';
import { CertificatesService } from '../certificates.service';

import { MatDialog } from '@angular/material/dialog';

import { RevokeDialogComponent } from '../revoke-dialog/revoke-dialog.component';

@Component({
  selector: 'app-all-certificates-item',
  templateUrl: './all-certificates-item.component.html',
  styleUrls: ['./all-certificates-item.component.css']
})
export class AllCertificatesItemComponent implements OnInit, OnDestroy {
  
  selectedCertificate: Certificate[];
  
  private selectedSubscription: Subscription;

  serialNumber: any;
  downloadCert = false;
  showError = false;

  constructor(private certificateService: CertificatesService, private dialog: MatDialog) { }


  onRevoke() {
    const dialogRef = this.dialog.open(RevokeDialogComponent);

    dialogRef.afterClosed().subscribe(result => {
      console.log(`Dialog result: ${result}`);
    });
  }


  ngOnInit(): void {
    this.selectedSubscription = this.certificateService.selectedCertificate.subscribe((data: Certificate[]) => {
      this.selectedCertificate = data;
    })
  }

  ngOnDestroy() {
    this.selectedSubscription.unsubscribe();
  }


  downloadCertificate(serialNumber: String){
    this.certificateService.downloadCertificate(serialNumber).subscribe(data=>{
      this.downloadCert = data;
    }, error=> {
      //something went wrong...
      this.showError = true;
    })

  }

}
