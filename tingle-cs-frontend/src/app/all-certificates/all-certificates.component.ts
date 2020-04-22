import { Component, OnInit} from '@angular/core';
import { CertificatesService } from '../certificates.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-all-certificates',
  templateUrl: './all-certificates.component.html',
  styleUrls: ['./all-certificates.component.css']
})
export class AllCertificatesComponent implements OnInit {

  showDetails: boolean = false;

  private subscription: Subscription;

  constructor(private certificateService: CertificatesService) { }

  ngOnInit(): void {
    this.subscription = this.certificateService.displayDetails.subscribe(display => {
      this.showDetails = display;
    })
  }
}
