export class Certificate {

    constructor(
        public id: number,
        public serialNumber: string,
        public alias: string,
        public active: boolean,
        public certificateRole: string,
        public CN: string,
        public O: string,
        public L: string,
        public ST: string,
        public C: string,
        public E: string,
        public OU: string) {}
}