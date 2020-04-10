
import { ExtensionsBasicConstraints } from './basic-constraints.model';
import { ExtensionsKeyUsage } from './key-usage.model';
import { ExtendedKeyUsage } from './extended-key-usage.model';

export class Extensions {

    public constructor(public basicConstraints? : ExtensionsBasicConstraints,
                       public keyUsage?: ExtensionsKeyUsage,
                       public extendedKeyUsage?: ExtendedKeyUsage) {}

}