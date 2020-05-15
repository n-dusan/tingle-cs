import { Component, OnInit, OnDestroy } from '@angular/core';
import { Certificate } from '../../shared/certificate.model';
import { Subscription } from 'rxjs';
import { CertificatesService } from '../../certificates.service';

import { MatDialog } from '@angular/material/dialog';
import { ToastrService } from 'ngx-toastr';

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
  isLoading = true;

  constructor(
    private certificateService: CertificatesService,
    private dialog: MatDialog,
    private toastr: ToastrService) { }


  onRevoke() {
    const dialogRef = this.dialog.open(RevokeDialogComponent);

    dialogRef.afterClosed().subscribe(result => {
      console.log(`Dialog result: ${result}`);
    });
  }


  ngOnInit(): void {
    this.selectedSubscription = this.certificateService.selectedCertificate.subscribe((data: Certificate[]) => {
      this.selectedCertificate = data;
      this.isLoading = false;
    })
  }

  ngOnDestroy() {
    this.selectedSubscription.unsubscribe();
  }


  downloadCertificate(serialNumber: String){
    this.certificateService.downloadCertificate(serialNumber).subscribe(data=>{
      this.downloadCert = data;
      this.toastr.success("Aww yeah", "You done did it buddy.")
    }, error=> {
      //something went wrong...
      this.showError = true;
      this.toastr.error(":(", "Something went wrong with your download")
    })

  }

  trustCertificate(serialNumber: String) {
    this.certificateService.trustCertificate(serialNumber).subscribe( (response:Certificate[]) => {
      console.log('got a response', response);
      this.toastr.success("Aww yeah", "You done did it buddy.")
    }, error => {
      this.toastr.error("!", "Something went wrong with your trust")
    })
  }

}
