import { Component, OnInit, OnDestroy, Output, ViewChild } from '@angular/core';
import { Certificate } from '../../shared/certificate.model';
import { CertificatesService } from '../../certificates.service';
import { Subscription } from 'rxjs';
import { MatPaginator } from '@angular/material/paginator';
import { MatTableDataSource } from '@angular/material/table';


@Component({
  selector: 'app-all-certificates-list',
  templateUrl: './all-certificates-list.component.html',
  styleUrls: ['./all-certificates-list.component.css']
})
export class AllCertificatesListComponent implements OnInit, OnDestroy {

  //all certificates, fetched from service
  certificates: Certificate[];
  private subscription: Subscription;

  //array of certificates displayed based on radio button filter
  displayedCertificates: Certificate[];

  displayedColumns: string[] = ['serial number', 'active', 'certificate authority', 'radio'];

  filterTypes: string[] = ['All', 'Active', 'Inactive'];
  selectedFilter: string = 'All';

  //filter for ocsp request
  ocspFilter: string = '';

  dataSource: MatTableDataSource<Certificate> = new MatTableDataSource;

  @ViewChild(MatPaginator, {static: true}) paginator: MatPaginator;

  isLoadingResults: boolean = true;

  constructor(private certificateService: CertificatesService) { }

  ngOnInit(): void {
    this.dataSource.data = [];

    this.subscription =  this.certificateService.getCertificates().subscribe((data: Certificate[]) => {
      this.certificates = data;
      this.isLoadingResults = false;
      //update datasource
      this.dataSource.data = this.certificates;
      //initialize paginator only after the datasource
      this.dataSource.paginator = this.paginator;
    })
  }

  onCertificateDetails(certificate: Certificate) {
    this.certificateService.getCertificate(certificate.serialNumber, certificate.certificateRole);
  }


  applyFilter() {

    console.log('ocsp filter', this.ocspFilter)

    this.dataSource.filter = this.ocspFilter.trim().toLowerCase();

    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }


  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  handleChange() {
    if(this.selectedFilter === 'All') {
      this.dataSource.data = this.certificates;
    } else if(this.selectedFilter === 'Active') {
      this.dataSource.data = this.certificates.filter(cert => {
        if(cert.active) { return cert; }
      })
    } else {
      this.dataSource.data = this.certificates.filter(cert => {
        if(!cert.active) { return cert; }
      })
    }

  }

}
