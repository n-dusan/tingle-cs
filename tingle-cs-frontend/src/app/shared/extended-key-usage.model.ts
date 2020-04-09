export class ExtendedKeyUsage {

    public constructor(public critical?: boolean,
                       public clientAuth?: boolean,
                       public codeSigning?: boolean,
                       public emailProtection?: boolean,
                       public ocspSigning?: boolean,
                       public serverAuth?: boolean,
                       public timeStamping?: boolean) {}

}