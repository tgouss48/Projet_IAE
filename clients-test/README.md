# Clients de test

Clients minimaux pour verifier l'acces aux deux systemes fournis (Lot 0).
Le starter kit doit etre demarre avant (`docker compose up -d`).

## rail-test-client (CORBA)

```bash
cd clients-test/rail-test-client
docker build -t mobilite/rail-test-client:0.1.0 .
docker run --rm --network mobilite-net -v "$(pwd)/../../ior:/ior:ro" mobilite/rail-test-client:0.1.0
```

Le client doit tourner dans le reseau `mobilite-net` car l'IOR annonce l'hote `rail`.

Probleme rencontre : JacORB 3.9 utilise `javax.rmi.CORBA`, supprime du JDK depuis Java 11.
On ajoute ce package depuis `glassfish-corba-omgapi` (filtre dans le shade) et on
declare un `StubDelegate` vide (`NoRmiStubDelegate`) puisqu'on n'utilise pas RMI-IIOP.

## air-test-client (SOAP)

```bash
cd clients-test/air-test-client
docker build -t mobilite/air-test-client:0.1.0 .
docker run --rm --network mobilite-net mobilite/air-test-client:0.1.0
```

Le WSDL est une copie de `http://localhost:8081/ws/air.wsdl`. Les classes JAXB sont
generees a partir de ce fichier (`jaxb2-maven-plugin`) et les appels passent par
`WebServiceTemplate` (Spring WS). Un fault SOAP arrive en `SoapFaultClientException`.
