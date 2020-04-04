export class CertificateX500Name {
    CN: String;
    O: String;
    L: String;
    ST: String;
    C: String;
    E: String;
    OU: String;
    serialNumber: String;
    certificateRole: String;
    alias: String;

    constructor(CN?: String, O?: String, L?: String, ST?: String, C?: String,
        E?: String, OU?: String, serialNumber?: String, certificateRole?: String,
        alias?: String) {
        this.CN = CN;
        this.O = O;
        this.ST = ST;
        this.C = C;
        this.E = E;
        this.OU = OU;
        this.serialNumber = serialNumber;
        this.certificateRole = certificateRole;
        this.alias = alias;
    }
}