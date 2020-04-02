import { Injectable } from '@angular/core'
import { HttpClient, HttpParams } from '@angular/common/http';
import { Certificate } from '../shared/certificate.model';
import { environment } from '../../environments/environment'
import { BehaviorSubject } from 'rxjs';

@Injectable({
    providedIn: 'root'
})
export class CertificatesService {

    url = environment.rootUrl + environment.certificate;

    certificates: BehaviorSubject<Certificate[]> = new BehaviorSubject<Certificate[]>(null);

    selectedCertificate: BehaviorSubject<Certificate[]> = new BehaviorSubject<Certificate[]>(null);

    constructor(private http: HttpClient) {}

    getCertificates() {
        this.http.get<Certificate[]>(this.url + '/all').subscribe((response) => {
            this.certificates.next(response)
        });
        return this.certificates.asObservable();
    }

    
    getCertificate(serialNumber: string, role: string) {
        this.http.get<Certificate[]>(this.url+ '/get/serial-number='+serialNumber+'/role='+role).subscribe((response)=> {
            this.selectedCertificate.next(response)
        })
        return this.selectedCertificate.asObservable();
    }
}