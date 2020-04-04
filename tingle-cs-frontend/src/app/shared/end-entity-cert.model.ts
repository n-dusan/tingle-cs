import { Certificate } from './certificate.model';

export class EndEntityCertificate {
    constructor(public subject: Certificate,
        public issuer: Certificate) {}
}