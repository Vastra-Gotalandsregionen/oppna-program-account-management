The .crt files have been exported from https://activate.vgregion.se/.

Then GlobalSignDomainValidationCA.crt was imported to GlobalSignDomainValidationCA.ts by:

keytool -importcert -alias GlobalSignDomainValidationCA -keystore GlobalSignDomainValidationCA.ts -file GlobalSignDomainValidationCA.crt

password to keystore: changeit