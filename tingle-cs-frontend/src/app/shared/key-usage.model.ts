export class ExtensionsKeyUsage {

    public constructor(public critical?: boolean,
                       public digitalSignature?: boolean,
                       public nonRepudation?: boolean,
                       public keyEncipherment?: boolean,
                       public dataEncipherment?: boolean,
                       public keyAgreement?: boolean,
                       public keyCertSign?: boolean,
                       public crlSign?: boolean,
                       public encipherOnly?: boolean,
                       public decipherOnly?: boolean) {}

}