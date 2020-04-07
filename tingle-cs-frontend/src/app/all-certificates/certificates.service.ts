import { Injectable } from '@angular/core'
import { HttpClient } from '@angular/common/http';
import { Certificate } from '../shared/certificate.model';
import { environment } from '../../environments/environment'
import { BehaviorSubject, Subject, Observable } from 'rxjs';
import { skipWhile, take} from 'rxjs/operators';
import { EndEntityCertificate } from '../shared/end-entity-cert.model';

@Injectable({
    providedIn: 'root'
})
export class CertificatesService {

    url = environment.rootUrl + environment.certificate;

    certificates: BehaviorSubject<Certificate[]> = new BehaviorSubject<Certificate[]>(null);

    selectedCertificate: BehaviorSubject<Certificate[]> = new BehaviorSubject<Certificate[]>(null);

    displayDetails = new Subject<boolean>();

    constructor(private http: HttpClient) {}

    getCertificates() {
        this.http.get<Certificate[]>(this.url + '/all').subscribe((response) => {
            this.certificates.next(response)
        });
        return this.certificates.asObservable();
    }
    
    
    getCertificate(serialNumber: string, role: string) {
        this.http.get<Certificate[]>(this.url+ '/get/'+ serialNumber +'/' + role).subscribe((response: Certificate[])=> {
            //no idea what im doing
            this.certificates.pipe(skipWhile(value => !value), take(1)).subscribe( (value:Certificate[]) => {
                for(let i = 0; i < value.length; i++) {
                    if(value[i].serialNumber === response[1].serialNumber) {

                      response[1].revokationReason = value[i].revokationReason;
                      this.selectedCertificate.next(response)
                    }
                  }
            })
        })
        this.displayDetails.next(true);
        return this.selectedCertificate.asObservable();
    }

    getOnlyCA(): Observable<Certificate[]> {
        return this.http.get<Certificate[]>(this.url + '/all/ca');
    }

    makeNewEndEntity(cert : EndEntityCertificate) : Observable<Certificate>{
        return this.http.post<Certificate>(this.url + '/new/end-entity', cert);
    }

    makeNewRoot(cert: Certificate) : Observable<Certificate[]> {
        return this.http.post<Certificate[]>(this.url + '/new/root', cert);
    }

    makeNewCA(cert: Certificate) : Observable<Certificate[]> {
        return this.http.post<Certificate[]>(this.url + '/new/ca', cert);
    }


    revokeCertificate(certificate: Certificate) {
        this.http.put<Certificate>(this.url+'/revoke', certificate).subscribe(response => {
            return this.getCertificates();
        })
    }
    
    downloadCertificate(serialNumber: String) : Observable<boolean>{
        return this.http.get<boolean>(`${this.url}/downloadCertificate/${serialNumber}`);
    }
}