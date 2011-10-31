The .crt files have been exported from https://regionkalender.proxy.vgregion.se/.

Then ThawtePremiumServerCA.crt was imported to ThawtePremiumServerCA.ts by:

keytool -importcert -alias ThawtePremiumServerCA -keystore ThawtePremiumServerCA.ts -file ThawtePremiumServerCA.crt

password to keystore: changeit