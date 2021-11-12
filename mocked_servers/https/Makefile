# Header above mocked_servers cert in bundle
CERT_HEADER=mocked_servers

CERT_PATH=certificates/ssl/certificate.crt
KEY_PATH=certificates/private.key
BUNDLE_PATH=certificates/ssl/ca-bundle.crt

.PHONY: update-certs
update-certs: gen-cert update-bundle

# Expects mocked_servers cert to be the last cert in the bundle
.PHONY: update-bundle
update-bundle:
	(sed '/${CERT_HEADER}/q' $(BUNDLE_PATH) | cat - $(CERT_PATH)) > tmp.crt
	cat tmp.crt > $(BUNDLE_PATH) && rm tmp.crt

.PHONY: gen-cert
gen-cert:
	openssl req -config openssl.conf -new -x509 -nodes -days 365 -out $(CERT_PATH) -keyout $(KEY_PATH)
