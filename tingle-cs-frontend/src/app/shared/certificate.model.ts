import { Extensions } from './extensions.model';

export class Certificate {

    constructor(
        public id?: number,
        public serialNumber?: string,
        public alias?: string,
        public active?: boolean,
        public certificateRole?: string,
        public cn?: string,
        public o?: string,
        public l?: string,
        public st?: string,
        public c?: string,
        public e?: string,
        public ou?: string,
        public revokationReason?: string,
        public extensions?: Extensions) {}
}